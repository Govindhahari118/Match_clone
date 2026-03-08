const prisma = require('../config/prisma');
const subscriptionService = require('./subscription.service');

const PRICING_CATALOG = [
    {
        id: 'SILVER_3M',
        tier: 'silver',
        name: 'Silver',
        durationMonths: 3,
        pricing: { IN: 1499, US: 39 },
        currencyByRegion: { IN: 'INR', US: 'USD' },
        features: ['Unlimited interests', 'Saved searches', 'See who viewed your profile'],
    },
    {
        id: 'GOLD_3M',
        tier: 'gold',
        name: 'Gold',
        durationMonths: 3,
        pricing: { IN: 2999, US: 69 },
        currencyByRegion: { IN: 'INR', US: 'USD' },
        features: ['Unlimited messages', 'Voice calls', 'Advanced filters'],
    },
    {
        id: 'PLATINUM_6M',
        tier: 'platinum',
        name: 'Platinum',
        durationMonths: 6,
        pricing: { IN: 4999, US: 109 },
        currencyByRegion: { IN: 'INR', US: 'USD' },
        features: ['Priority visibility', 'Voice + video calls', 'Top placement in discovery'],
    },
    {
        id: 'TILL_MARRIAGE',
        tier: 'till_marriage',
        name: 'Till Marriage',
        durationMonths: 24,
        pricing: { IN: 12999, US: 249 },
        currencyByRegion: { IN: 'INR', US: 'USD' },
        features: ['All Platinum features', 'Extended support', 'Long-term membership'],
    },
];

const COUPON_RULES = {
    WELCOME10: {
        discountPercent: 10,
        maxDiscountInr: 700,
        appliesToTiers: ['silver', 'gold', 'platinum'],
    },
    WINBACK20: {
        discountPercent: 20,
        maxDiscountInr: 1200,
        appliesToTiers: ['gold', 'platinum', 'till_marriage'],
        minDaysSinceExpiry: 7,
    },
};

function normalizeRegion(value) {
    const region = String(value || 'IN').trim().toUpperCase();
    if (region === 'US') return 'US';
    return 'IN';
}

function normalizeCoupon(code) {
    const value = String(code || '').trim().toUpperCase();
    return value || null;
}

function resolveLocalizedPricing(plan, region) {
    const normalizedRegion = normalizeRegion(region);
    const amount = plan.pricing[normalizedRegion] || plan.pricing.IN;
    const currency = plan.currencyByRegion[normalizedRegion] || 'INR';
    return { amount, currency, region: normalizedRegion };
}

async function isWinbackEligible(userId, minDaysSinceExpiry = 7) {
    const lastExpired = await prisma.subscription.findFirst({
        where: {
            userId,
            expiresAt: { lt: new Date() },
        },
        orderBy: { expiresAt: 'desc' },
        select: { expiresAt: true },
    });
    if (!lastExpired?.expiresAt) return false;
    const days = Math.floor((Date.now() - new Date(lastExpired.expiresAt).getTime()) / (24 * 60 * 60 * 1000));
    return days >= minDaysSinceExpiry;
}

async function evaluateCoupon(userId, couponCode, plan) {
    const code = normalizeCoupon(couponCode);
    if (!code) {
        return {
            valid: false,
            code: null,
            discountInr: 0,
            discountPercent: 0,
            reason: null,
        };
    }

    const rule = COUPON_RULES[code];
    if (!rule) {
        return {
            valid: false,
            code,
            discountInr: 0,
            discountPercent: 0,
            reason: 'invalid_coupon',
        };
    }
    if (rule.appliesToTiers && !rule.appliesToTiers.includes(plan.tier)) {
        return {
            valid: false,
            code,
            discountInr: 0,
            discountPercent: 0,
            reason: 'coupon_not_applicable_for_plan',
        };
    }
    if (rule.minDaysSinceExpiry) {
        const eligible = await isWinbackEligible(userId, rule.minDaysSinceExpiry);
        if (!eligible) {
            return {
                valid: false,
                code,
                discountInr: 0,
                discountPercent: 0,
                reason: 'coupon_not_yet_eligible',
            };
        }
    }

    return {
        valid: true,
        code,
        discountPercent: rule.discountPercent,
        maxDiscountInr: rule.maxDiscountInr || null,
    };
}

function generateMockOrderId() {
    return `order_${Date.now()}_${Math.floor(Math.random() * 100000)}`;
}

async function upsertSubscriptionFromPayment(userId, plan, expiryDate) {
    const active = await prisma.subscription.findFirst({
        where: {
            userId,
            status: 'active',
        },
        orderBy: { createdAt: 'desc' },
    });

    if (!active) {
        return prisma.subscription.create({
            data: {
                userId,
                plan: plan.tier,
                status: 'active',
                expiresAt: expiryDate,
            },
        });
    }

    return prisma.subscription.update({
        where: { id: active.id },
        data: {
            plan: plan.tier,
            status: 'active',
            expiresAt: expiryDate,
            updatedAt: new Date(),
        },
    });
}

const paymentService = {
    getPlans(context = {}) {
        const region = normalizeRegion(context.region || context.country || 'IN');
        return PRICING_CATALOG.map((plan) => {
            const localized = resolveLocalizedPricing(plan, region);
            return {
                id: plan.id,
                tier: plan.tier,
                name: plan.name,
                durationMonths: plan.durationMonths,
                amount: localized.amount,
                currency: localized.currency,
                region,
                features: plan.features,
            };
        });
    },

    async createOrder(userId, planId, options = {}) {
        const plan = PRICING_CATALOG.find((item) => item.id === planId);
        if (!plan) throw new Error('Invalid plan');

        const localized = resolveLocalizedPricing(plan, options.region || options.country || 'IN');
        const coupon = await evaluateCoupon(userId, options.couponCode, plan);
        const rawDiscount = coupon.valid
            ? Math.round((localized.amount * coupon.discountPercent) / 100)
            : 0;
        const discountInr = coupon.maxDiscountInr
            ? Math.min(rawDiscount, coupon.maxDiscountInr)
            : rawDiscount;
        const finalAmount = Math.max(0, localized.amount - discountInr);
        const orderId = generateMockOrderId();

        const payment = await prisma.payment.create({
            data: {
                userId,
                amountInr: finalAmount,
                plan: plan.tier,
                status: 'pending',
                razorpayOrderId: orderId,
            },
        });

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'payment_order_created',
                resourceType: 'payment',
                resourceId: payment.id,
                changes: {
                    orderId,
                    planId: plan.id,
                    tier: plan.tier,
                    region: localized.region,
                    amount: localized.amount,
                    finalAmount,
                    currency: localized.currency,
                    coupon: coupon.valid ? coupon.code : null,
                    discountInr,
                },
            },
        });

        return {
            orderId,
            amount: finalAmount,
            currency: localized.currency,
            planId: plan.id,
            tier: plan.tier,
            discountInr,
            coupon: coupon.valid ? coupon.code : null,
            couponStatus: coupon.valid ? 'applied' : (coupon.reason || 'none'),
        };
    },

    async verifyPayment(userId, { paymentId, orderId, planId }) {
        if (!paymentId) throw new Error('Payment failed');

        const plan = PRICING_CATALOG.find((item) => item.id === planId);
        if (!plan) {
            throw new Error('Invalid plan');
        }

        const payment = await prisma.payment.findFirst({
            where: {
                userId,
                razorpayOrderId: orderId,
            },
            orderBy: { createdAt: 'desc' },
        });
        if (!payment) {
            throw new Error('Pending payment not found for this order');
        }
        if (payment.status === 'completed') {
            const currentEntitlements = await subscriptionService.getEntitlements(userId);
            return {
                success: true,
                idempotent: true,
                expiryDate: currentEntitlements.expiresAt,
                plan: currentEntitlements.plan,
            };
        }

        const expiryDate = new Date();
        expiryDate.setMonth(expiryDate.getMonth() + plan.durationMonths);

        const updatedPayment = await prisma.payment.update({
            where: { id: payment.id },
            data: {
                status: 'completed',
                razorpayPaymentId: paymentId,
                plan: plan.tier,
            },
        });

        const subscription = await upsertSubscriptionFromPayment(userId, plan, expiryDate);

        await prisma.auditLog.create({
            data: {
                userId,
                action: 'payment_completed',
                resourceType: 'payment',
                resourceId: updatedPayment.id,
                changes: {
                    orderId,
                    paymentId,
                    subscriptionId: subscription.id,
                    plan: plan.tier,
                    expiresAt: expiryDate.toISOString(),
                },
            },
        });

        return {
            success: true,
            expiryDate,
            plan: plan.tier,
        };
    },
};

module.exports = paymentService;
