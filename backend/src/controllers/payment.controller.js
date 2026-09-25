const paymentService = require('../services/payment.service');

const getPlans = (req, res) => {
    try {
        const plans = paymentService.getPlans();
        res.json(plans);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const createOrder = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { planId } = req.body;
        const order = await paymentService.createOrder(userId, planId);
        res.json(order);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

const verifyPayment = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { paymentId, orderId, signature, planId } = req.body;
        const result = await paymentService.verifyPayment(userId, { paymentId, orderId, signature, planId });
        res.json(result);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};

module.exports = { getPlans, createOrder, verifyPayment };
