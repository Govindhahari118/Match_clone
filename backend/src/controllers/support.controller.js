const supportService = require('../services/support.service');

function sendError(res, error) {
    const status = Number(error.statusCode) || 500;
    return res.status(status).json({ error: status >= 500 ? 'Support request failed' : error.message });
}

const supportController = {
    async create(req, res) {
        try {
            const ticket = await supportService.createTicket(req.user.sub, req.body || {});
            return res.status(201).json(ticket);
        } catch (error) {
            console.error('Create support ticket error:', error);
            return sendError(res, error);
        }
    },

    async list(req, res) {
        try {
            return res.json(await supportService.listTickets(req.user.sub));
        } catch (error) {
            console.error('List support tickets error:', error);
            return sendError(res, error);
        }
    },

    async get(req, res) {
        try {
            const ticket = await supportService.getTicket(req.user.sub, req.params.ticketId);
            if (!ticket) return res.status(404).json({ error: 'Ticket not found' });
            return res.json(ticket);
        } catch (error) {
            return sendError(res, error);
        }
    },

    async comment(req, res) {
        try {
            const ticket = await supportService.addUserComment(req.user.sub, req.params.ticketId, req.body?.message);
            return res.json(ticket);
        } catch (error) {
            return sendError(res, error);
        }
    },

    async confirmResolution(req, res) {
        try {
            const accepted = req.body?.accepted === true;
            const ticket = await supportService.confirmResolution(req.user.sub, req.params.ticketId, accepted);
            return res.json(ticket);
        } catch (error) {
            return sendError(res, error);
        }
    },
};

module.exports = supportController;
