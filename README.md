# Matrimony App

Full-stack matrimony platform with web, backend API, and mobile app placeholders.

## Stack
- `backend/`: Node.js + Express + Prisma + PostgreSQL
- `frontend/`: Next.js app (runs on port `8000`)
- `mobile/`: React Native/Expo placeholder
- `docs/`: product and API documentation
- `scripts/`: local lifecycle scripts (`dev-up`, `dev-down`, `dev-status`)

## Prerequisites
- Docker Desktop (running)
- Node.js 20+ (22 is also supported)

## Quick Start (Recommended)
From repo root:

```bash
npm run dev:up
```

This does:
1. Starts postgres + redis via Docker
2. Ensures env files exist
3. Syncs Prisma schema
4. Starts backend and frontend in background

Then open:
- Frontend: `http://localhost:8000`
- Backend health: `http://localhost:4000/health`

## Useful Commands
```bash
npm run dev:status   # show app/container status
npm run dev:down     # stop frontend/backend + postgres/redis
npm run infra:up     # start only postgres/redis
npm run infra:down   # stop only postgres/redis
```

## Environment Files
- Backend template: `backend/.env.example`
- Frontend template: `frontend/.env.local.example`

If missing, `npm run dev:up` auto-creates:
- `backend/.env`
- `frontend/.env.local`

## Project Structure
See `docs/PROJECT_STRUCTURE.md`.

## API Docs
- Postman collection: `docs/matrimony-api-postman.json`
- PRD: `docs/matrimony-app-prd.md`
