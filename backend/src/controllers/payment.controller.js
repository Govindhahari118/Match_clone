const crypto = require('crypto');
const paymentService = require('../services/payment.service');

function sendError(res, error) {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? 'Payment request failed' : error.message });
}

const getPlans = (req, res) => {
    try {
        return res.json(paymentService.getPlans());
    } catch (error) {
        return sendError(res, error);
    }
};

const createOrder = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { planId } = req.body || {};
        const idempotencyKey = req.header('Idempotency-Key') || req.body?.idempotencyKey || crypto.randomUUID();
        const order = await paymentService.createOrder(userId, planId, idempotencyKey);
        return res.json({ ...order, idempotencyKey });
    } catch (error) {
        return sendError(res, error);
    }
};

const verifyPayment = async (req, res) => {
    try {
        const userId = req.user.sub;
        const { paymentId, orderId, planId, signature } = req.body || {};
        const result = await paymentService.verifyPayment(userId, { paymentId, orderId, planId, signature });
        return res.json(result);
    } catch (error) {
        return sendError(res, error);
    }
};

const getStatus = async (req, res) => {
    try {
        return res.json(await paymentService.getPaymentStatus(req.user.sub, req.params.orderId));
    } catch (error) {
        return sendError(res, error);
    }
};

const getHistory = async (req, res) => {
    try {
        return res.json(await paymentService.getPaymentHistory(req.user.sub));
    } catch (error) {
        return sendError(res, error);
    }
};

module.exports = { getPlans, createOrder, verifyPayment, getStatus, getHistory };
