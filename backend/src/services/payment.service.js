const crypto = require('crypto');
const Razorpay = require('razorpay');
const prisma = require('../config/prisma');

const plans = [
    {
        id: 'GOLD_3M',
        name: 'Gold',
        price: 2999,
        durationMonths: 3,
        features: ['Unlimited Messages', 'See who liked you', '30 Contacts']
    },
    {
        id: 'PLATINUM_6M',
        name: 'Platinum',
        price: 4999,
        durationMonths: 6,
        features: ['Priority support', 'Highlight Profile', 'Unlimited Contacts']
    }
];

function getPlan(planId) {
    const plan = plans.find(p => p.id === planId);
    if (!plan) throw new Error('Invalid Plan');
    return plan;
}

function getGateway() {
    const keyId = process.env.RAZORPAY_KEY_ID || process.env.RAZORPAY_API_KEY;
    const keySecret = process.env.RAZORPAY_KEY_SECRET || process.env.RAZORPAY_API_SECRET;
    if (!keyId || !keySecret) {
        throw new Error('Payment gateway is not configured');
    }
    return {
        client: new Razorpay({ key_id: keyId, key_secret: keySecret }),
        keyId,
        keySecret
    };
}

const paymentService = {
    getPlans() {
        return plans;
    },

    async createOrder(userId, planId) {
        const plan = getPlan(planId);
        const { client } = getGateway();

        const order = await client.orders.create({
            amount: Math.round(plan.price * 100),
            currency: 'INR',
            receipt: `matree_${userId}_${Date.now()}`,
            notes: { userId, planId }
        });

        await prisma.payment.create({
            data: {
                userId,
                amountInr: plan.price,
                plan: plan.id,
                status: 'pending',
                razorpayOrderId: order.id
            }
        });

        return {
            orderId: order.id,
            amount: plan.price,
            amountPaise: order.amount,
            currency: order.currency,
            planId
        };
    },

    async verifyPayment(userId, { paymentId, orderId, signature, planId }) {
        if (!paymentId || !orderId || !signature) {
            throw new Error('paymentId, orderId and signature are required');
        }

        const plan = getPlan(planId);
        const { client, keySecret } = getGateway();

        const expected = crypto
            .createHmac('sha256', keySecret)
            .update(`${orderId}|${paymentId}`)
            .digest('hex');

        const signatureValid =
            signature.length === expected.length &&
            crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expected));
        if (!signatureValid) throw new Error('Invalid payment signature');

        const existingPayment = await prisma.payment.findUnique({ where: { razorpayOrderId: orderId } });
        if (!existingPayment || existingPayment.userId !== userId) {
            throw new Error('Payment order not found for this user');
        }
        if (existingPayment.plan !== plan.id) {
            throw new Error('Payment plan mismatch');
        }

        if (existingPayment.status === 'completed' && existingPayment.subscriptionId) {
            const subscription = await prisma.subscription.findUnique({ where: { id: existingPayment.subscriptionId } });
            return { success: true, subscription, recovered: true };
        }

        const [gatewayPayment, gatewayOrder] = await Promise.all([
            client.payments.fetch(paymentId),
            client.orders.fetch(orderId)
        ]);

        if (gatewayPayment.order_id !== orderId) throw new Error('Gateway payment/order mismatch');
        if (!['captured', 'authorized'].includes(gatewayPayment.status)) throw new Error('Payment is not successful');
        if (Number(gatewayPayment.amount) !== Math.round(plan.price * 100) || gatewayPayment.currency !== 'INR') {
            throw new Error('Gateway amount or currency mismatch');
        }
        if (Number(gatewayOrder.amount) !== Math.round(plan.price * 100) || gatewayOrder.currency !== 'INR') {
            throw new Error('Gateway order amount or currency mismatch');
        }

        const expiryDate = new Date();
        expiryDate.setMonth(expiryDate.getMonth() + plan.durationMonths);

        return prisma.$transaction(async tx => {
            const current = await tx.payment.findUnique({ where: { razorpayOrderId: orderId } });
            if (current.status === 'completed' && current.subscriptionId) {
                const subscription = await tx.subscription.findUnique({ where: { id: current.subscriptionId } });
                return { success: true, subscription, recovered: true };
            }

            const subscription = await tx.subscription.create({
                data: {
                    userId,
                    plan: plan.id,
                    status: 'active',
                    expiresAt: expiryDate,
                    autoRenew: false
                }
            });

            await tx.payment.update({
                where: { id: current.id },
                data: {
                    status: 'completed',
                    razorpayPaymentId: paymentId,
                    razorpaySignature: signature,
                    subscriptionId: subscription.id
                }
            });

            return { success: true, subscription, recovered: false };
        });
    }
};

module.exports = paymentService;
