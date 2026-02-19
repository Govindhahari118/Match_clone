const { PrismaClient } = require('@prisma/client');

// Use a singleton instance for Prisma Client in development to prevent hot reload issues
const prisma = global.prisma || new PrismaClient();

if (process.env.NODE_ENV !== 'production') {
    global.prisma = prisma;
}

module.exports = prisma;
