const prisma = require('../config/prisma');

const USER_MUTABLE_STATUSES = new Set(['closed', 'open']);

function normalizePriority(value) {
    const priority = String(value || 'normal').toLowerCase();
    return ['low', 'normal', 'high', 'critical'].includes(priority) ? priority : 'normal';
}

async function createTicket(userId, { category, subject, description, priority }) {
    if (!category || !subject || !description) {
        const error = new Error('category, subject and description are required');
        error.statusCode = 400;
        throw error;
    }

    return prisma.$transaction(async (tx) => {
        const ticket = await tx.supportTicket.create({
            data: {
                userId,
                category: String(category).trim(),
                subject: String(subject).trim().slice(0, 180),
                description: String(description).trim(),
                priority: normalizePriority(priority),
            },
        });
        await tx.supportEvent.create({
            data: {
                ticketId: ticket.id,
                actorType: 'user',
                actorId: userId,
                eventType: 'created',
                message: ticket.description,
            },
        });
        return ticket;
    });
}

async function listTickets(userId) {
    return prisma.supportTicket.findMany({
        where: { userId },
        orderBy: { updatedAt: 'desc' },
        include: { events: { orderBy: { createdAt: 'asc' } } },
    });
}

async function getTicket(userId, ticketId) {
    return prisma.supportTicket.findFirst({
        where: { id: ticketId, userId },
        include: { events: { orderBy: { createdAt: 'asc' } } },
    });
}

async function addUserComment(userId, ticketId, message) {
    if (!message || !String(message).trim()) {
        const error = new Error('Message is required');
        error.statusCode = 400;
        throw error;
    }

    const ticket = await prisma.supportTicket.findFirst({ where: { id: ticketId, userId } });
    if (!ticket) {
        const error = new Error('Ticket not found');
        error.statusCode = 404;
        throw error;
    }

    if (ticket.status === 'closed') {
        const error = new Error('Closed ticket must be reopened before adding a comment');
        error.statusCode = 409;
        throw error;
    }

    await prisma.supportEvent.create({
        data: {
            ticketId,
            actorType: 'user',
            actorId: userId,
            eventType: 'comment',
            message: String(message).trim(),
        },
    });
    await prisma.supportTicket.update({
        where: { id: ticketId },
        data: { status: ticket.status === 'resolved' ? 'open' : ticket.status },
    });
    return getTicket(userId, ticketId);
}

async function confirmResolution(userId, ticketId, accepted) {
    const ticket = await prisma.supportTicket.findFirst({ where: { id: ticketId, userId } });
    if (!ticket) {
        const error = new Error('Ticket not found');
        error.statusCode = 404;
        throw error;
    }

    if (accepted) {
        if (ticket.status !== 'resolved') {
            const error = new Error('Only a resolved ticket can be closed');
            error.statusCode = 409;
            throw error;
        }
        const now = new Date();
        await prisma.$transaction([
            prisma.supportTicket.update({ where: { id: ticketId }, data: { status: 'closed', closedAt: now } }),
            prisma.supportEvent.create({
                data: {
                    ticketId,
                    actorType: 'user',
                    actorId: userId,
                    eventType: 'closed',
                    message: 'User confirmed the resolution.',
                },
            }),
        ]);
    } else {
        await prisma.$transaction([
            prisma.supportTicket.update({ where: { id: ticketId }, data: { status: 'open', resolvedAt: null, closedAt: null } }),
            prisma.supportEvent.create({
                data: {
                    ticketId,
                    actorType: 'user',
                    actorId: userId,
                    eventType: 'reopened',
                    message: 'User indicated that the issue is not resolved.',
                },
            }),
        ]);
    }

    return getTicket(userId, ticketId);
}

module.exports = {
    USER_MUTABLE_STATUSES,
    createTicket,
    listTickets,
    getTicket,
    addUserComment,
    confirmResolution,
};
