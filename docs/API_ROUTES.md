# API Routes (Source of Truth)

This document reflects the live Express route registry in `backend/src/routes/routes.registry.js`.
All frontend API calls should target these `/api/...` bases.

## Route prefixes

- `/api/auth`
- `/api/meta`
- `/api/search`
- `/api/subscription`
- `/api/call`
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
- `/api/analytics`
- `/api/notifications`

## Health endpoint

- `GET /health`

## Port defaults

- Backend HTTP + Socket.IO: `http://localhost:5000`
- Frontend: `http://localhost:3000`
- Frontend env defaults:
  - `NEXT_PUBLIC_API_URL=http://localhost:5000/api`
  - `NEXT_PUBLIC_SOCKET_URL=http://localhost:5000`
