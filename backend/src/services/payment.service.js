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

const paymentService = {
    getPlans() {
        return plans;
    },

    async createOrder(userId, planId) {
        const plan = plans.find(p => p.id === planId);
        if (!plan) throw new Error("Invalid Plan");

        // Mock Order ID from Payment Gateway
        const orderId = `order_${Date.now()}_${Math.floor(Math.random() * 1000)}`;

        return {
            orderId,
            amount: plan.price,
            currency: "INR",
            planId
        };
    },

    async verifyPayment(userId, { paymentId, orderId, planId }) {
        // Mock verification logic
        if (!paymentId) throw new Error("Payment failed");

        const plan = plans.find(p => p.id === planId);

        // Calculate expiry
        const expiryDate = new Date();
        expiryDate.setMonth(expiryDate.getMonth() + plan.durationMonths);

        // Create Subscription Record
        await prisma.subscription.create({
            data: {
                userId,
                plan: plan.name,
                status: 'active',
                expiresAt: expiryDate,
                // Record specific payment usually goes to Payment table, but for now linking logic
            }
        });

        // Also create payment record
        await prisma.payment.create({
            data: {
                userId,
                amountInr: plan.price,
                plan: plan.name,
                status: 'completed',
                razorpayOrderId: orderId, // linking our mock order id
                razorpayPaymentId: paymentId
            }
        });

        return { success: true, expiryDate };
    }
};

module.exports = paymentService;
