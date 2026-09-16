const crypto = require('crypto');
const Razorpay = require('razorpay');
const prisma = require('../config/prisma');
const subscriptionService = require('./subscription.service');

const plans = [
    {
        id: 'GOLD_3M',
        key: 'gold',
        name: 'Gold',
        price: 2999,
        durationMonths: 3,
        currency: 'INR',
        autoRenewDefault: false,
        entitlements: subscriptionService.PLAN_ENTITLEMENTS.gold,
    },
    {
        id: 'PLATINUM_6M',
        key: 'platinum',
        name: 'Platinum',
        price: 4999,
        durationMonths: 6,
        currency: 'INR',
        autoRenewDefault: false,
        entitlements: subscriptionService.PLAN_ENTITLEMENTS.platinum,
    },
];

function appError(message, statusCode = 400) {
    const error = new Error(message);
    error.statusCode = statusCode;
    return error;
}

function getPlan(planId) {
    const plan = plans.find((item) => item.id === planId);
    if (!plan) throw appError('Invalid plan');
    return plan;
}

function razorpayClient() {
    if (!process.env.RAZORPAY_KEY_ID || !process.env.RAZORPAY_KEY_SECRET) return null;
    return new Razorpay({ key_id: process.env.RAZORPAY_KEY_ID, key_secret: process.env.RAZORPAY_KEY_SECRET });
}

function verifyCheckoutSignature(orderId, paymentId, signature) {
    if (!process.env.RAZORPAY_KEY_SECRET) {
        if (process.env.NODE_ENV === 'production') throw appError('Payment verification is not configured', 503);
        return true;
    }
    if (!signature) return false;
    const expected = crypto
        .createHmac('sha256', process.env.RAZORPAY_KEY_SECRET)
        .update(`${orderId}|${paymentId}`)
        .digest('hex');
    const a = Buffer.from(expected);
    const b = Buffer.from(String(signature));
    return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function expiryFor(plan) {
    const date = new Date();
    date.setMonth(date.getMonth() + plan.durationMonths);
    return date;
}

const paymentService = {
    getPlans() {
        return plans.map((plan) => ({
            ...plan,
            planVersion: subscriptionService.PLAN_VERSION,
            taxes: 'Taxes, if applicable, are shown before final payment',
            renewal: plan.autoRenewDefault ? 'Auto-renewal enabled' : 'No automatic renewal by default',
            contactPrivacy: 'Contact details still require the other member’s configured consent.',
        }));
    },

    async createOrder(userId, planId, idempotencyKey) {
        const plan = getPlan(planId);
        const key = String(idempotencyKey || '').trim();
        if (!key) throw appError('idempotencyKey is required');

        const existing = await prisma.payment.findUnique({ where: { idempotencyKey: key } });
        if (existing) {
            if (existing.userId !== userId || subscriptionService.normalizePlan(existing.plan) !== plan.key) {
                throw appError('Idempotency key is already in use', 409);
            }
            return {
                orderId: existing.razorpayOrderId,
                amount: Number(existing.amountInr),
                currency: plan.currency,
                planId,
                status: existing.status,
                idempotentReplay: true,
            };
        }

        const client = razorpayClient();
        let orderId;
        if (client) {
            const order = await client.orders.create({
                amount: Math.round(plan.price * 100),
                currency: plan.currency,
                receipt: `mat_${key.slice(0, 28)}`,
                notes: { userId, planId },
            });
            orderId = order.id;
        } else {
            if (process.env.NODE_ENV === 'production') throw appError('Payment gateway is not configured', 503);
            orderId = `order_dev_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`;
        }

        await prisma.payment.create({
            data: {
                userId,
                amountInr: plan.price,
                plan: plan.key,
                status: 'pending',
                gateway: client ? 'razorpay' : 'development_mock',
                idempotencyKey: key,
                razorpayOrderId: orderId,
            },
        });

        return {
            orderId,
            amount: plan.price,
            currency: plan.currency,
            planId,
            status: 'pending',
            idempotentReplay: false,
        };
    },

    async verifyPayment(userId, { paymentId, orderId, planId, signature }) {
        if (!paymentId || !orderId || !planId) throw appError('paymentId, orderId and planId are required');
        const plan = getPlan(planId);
        const pending = await prisma.payment.findFirst({ where: { userId, razorpayOrderId: orderId } });
        if (!pending) throw appError('Payment order not found', 404);
        if (subscriptionService.normalizePlan(pending.plan) !== plan.key) throw appError('Payment plan does not match the order', 409);

        if (pending.status === 'completed') {
            return {
                success: true,
                idempotentReplay: true,
                paymentId: pending.razorpayPaymentId,
                subscriptionId: pending.subscriptionId,
                entitlements: await subscriptionService.getEntitlements(userId),
            };
        }

        if (!verifyCheckoutSignature(orderId, paymentId, signature)) {
            await prisma.payment.update({
                where: { id: pending.id },
                data: { status: 'failed', failureReason: 'invalid_signature' },
            });
            throw appError('Payment verification failed', 400);
        }

        const existingPaymentId = await prisma.payment.findFirst({ where: { razorpayPaymentId: paymentId } });
        if (existingPaymentId && existingPaymentId.id !== pending.id) {
            throw appError('Payment has already been applied', 409);
        }

        const expiresAt = expiryFor(plan);
        const result = await prisma.$transaction(async (tx) => {
            const subscription = await tx.subscription.create({
                data: {
                    userId,
                    plan: plan.key,
                    planVersion: subscriptionService.PLAN_VERSION,
                    status: 'active',
                    expiresAt,
                    autoRenew: plan.autoRenewDefault,
                    renewalStatus: plan.autoRenewDefault ? 'enabled' : 'none',
                },
            });

            const payment = await tx.payment.update({
                where: { id: pending.id },
                data: {
                    status: 'completed',
                    razorpayPaymentId: paymentId,
                    razorpaySignature: signature || null,
                    subscriptionId: subscription.id,
                    failureReason: null,
                },
            });

            await tx.auditLog.create({
                data: {
                    userId,
                    action: 'payment_completed',
                    resourceType: 'payment',
                    resourceId: payment.id,
                    changes: { plan: plan.key, subscriptionId: subscription.id },
                },
            });

            return { payment, subscription };
        });

        return {
            success: true,
            idempotentReplay: false,
            paymentId: result.payment.razorpayPaymentId,
            subscriptionId: result.subscription.id,
            expiresAt,
            entitlements: await subscriptionService.getEntitlements(userId),
        };
    },

    async getPaymentStatus(userId, orderId) {
        const payment = await prisma.payment.findFirst({
            where: { userId, razorpayOrderId: orderId },
            select: {
                id: true,
                plan: true,
                status: true,
                amountInr: true,
                razorpayOrderId: true,
                razorpayPaymentId: true,
                failureReason: true,
                subscriptionId: true,
                createdAt: true,
                updatedAt: true,
            },
        });
        if (!payment) throw appError('Payment order not found', 404);
        return payment;
    },

    async getPaymentHistory(userId) {
        return prisma.payment.findMany({
            where: { userId },
            orderBy: { createdAt: 'desc' },
            select: {
                id: true,
                plan: true,
                status: true,
                amountInr: true,
                gateway: true,
                razorpayOrderId: true,
                razorpayPaymentId: true,
                failureReason: true,
                subscriptionId: true,
                createdAt: true,
                updatedAt: true,
            },
        });
    },
};

module.exports = paymentService;
