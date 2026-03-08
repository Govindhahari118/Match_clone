const paymentService = require('../services/payment.service');

const getPlans = (req, res) => {
    try {
        res.set('Cache-Control', 'public, max-age=300, stale-while-revalidate=1800');
        res.set('Vary', 'Accept-Encoding');
        const plans = paymentService.getPlans({
            region: req.query?.region,
            country: req.query?.country,
        });
        res.json(plans);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const createOrder = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { planId, couponCode, region, country } = req.body || {};
        const order = await paymentService.createOrder(userId, planId, { couponCode, region, country });
        res.json(order);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const verifyPayment = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { paymentId, orderId, planId } = req.body;
        const result = await paymentService.verifyPayment(userId, { paymentId, orderId, planId });
        res.json(result);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

module.exports = { getPlans, createOrder, verifyPayment };
