# ============================================================================
# MATRIMONY APP - DOCKER SETUP
# This file configures Docker for the backend service
# ============================================================================

FROM node:20-alpine

# Set working directory
WORKDIR /app

# Install build tools for native modules (for bcrypt, etc.)
RUN apk add --no-cache python3 make g++

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application code
COPY . .

# Generate Prisma client
RUN npx prisma generate

# Expose port
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD node -e "require('http').get('http://localhost:5000/health', (r) => {if (r.statusCode !== 200) throw new Error(r.statusCode)})"

# Start application
CMD ["npm", "start"]

# Build multi-stage for production
# FROM node:20-alpine as builder
# ... build steps ...
# FROM node:20-alpine
# COPY --from=builder /app ...
