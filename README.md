# Matrimony App

Full-stack matrimony platform with a Next.js web app, Node/Express API, PostgreSQL, Redis, and a mobile placeholder.

## Current Project Snapshot

- Monorepo-style layout under `match/` with separate `frontend` and `backend` apps
- Local developer lifecycle automated with PowerShell scripts in `scripts/`
- Database access through Prisma (`backend/prisma`)
- Real-time messaging using Socket.IO
- API surface includes auth, profiles, matching, chat, interactions, shortlists, reviews, media, verification, and admin flows

## Tech Stack

- Frontend: Next.js 16, React 19, Tailwind CSS 4
- Backend: Node.js, Express 5, Prisma, Socket.IO
- Database: PostgreSQL 15
- Cache/real-time support: Redis 7
- Tooling: Docker Compose, PowerShell automation scripts

## Repo Structure

```text
match/
|- backend/              # Express API + Prisma
|- frontend/             # Next.js web app (App Router)
|- mobile/               # React Native/Expo placeholder
|- docs/                 # Product + API docs
|- scripts/              # dev-up/dev-down/dev-status scripts
|- runtime/              # generated logs + PID files
|- docker-compose.yml    # postgres + redis (+ optional api service)
\- package.json          # workspace helper scripts
```

## Implemented Modules (from current codebase)

### Frontend Routes

- Auth: `/login`, `/otp`
- Onboarding: `/step-1` to `/step-5`
- Main app: `/search`, `/matches`, `/chat`, `/profile`, `/profile/[id]`, `/shortlists`, `/who-viewed`, `/notifications`, `/interests`, `/settings`, `/pricing`, `/help`, `/success-stories`, `/admin`, `/kundli`
- Protected: `/verification`

### Backend Route Groups

- `/api/auth`
- `/api/users`
- `/api/profiles`
- `/api/photos`
- `/api/matches`
- `/api/interactions`
- `/api/chat`
- `/api/payment`
- `/api/admin`
- `/api/posts`
- `/api/media`
- `/api/shortlist`
- `/api/reviews`
- `/api/verification`
- Health check: `/health`

## Prerequisites

- Docker Desktop running
- Node.js 20+
- npm

## Quick Start (Recommended)

From `match/`:

```bash
npm run dev:up
```

This flow:

1. Starts `postgres` and `redis` containers
2. Ensures env files exist from templates
3. Installs dependencies if missing
4. Runs Prisma generate + db push
5. Starts backend + frontend in background

Then open:

- Frontend: `http://localhost:8000`
- Backend health: `http://localhost:4000/health`

## Useful Commands

```bash
npm run dev:status   # check app + infra status
npm run dev:down     # stop frontend/backend and infra
npm run infra:up     # start only postgres + redis
npm run infra:down   # stop only postgres + redis
```

## Environment Files

- Backend template: `backend/.env.example`
- Frontend template: `frontend/.env.local.example`

Auto-created if missing by `npm run dev:up`:

- `backend/.env`
- `frontend/.env.local`

## Ports

- Frontend: `8000`
- Backend (local script mode): `4000`
- PostgreSQL: `5432`
- Redis: `6379`

Note: `docker-compose.yml` also defines an optional `api` service mapped to `5000`. The standard local script workflow (`npm run dev:up`) starts backend on `4000` using `backend/.env`.

## Notes

- `mobile/` is currently a starter placeholder (Expo init instructions only).
- `frontend/README.md` is still the default Next.js scaffold doc; this file is the primary root guide.
