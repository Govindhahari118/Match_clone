# Project Structure

```text
match/
|- backend/
|  |- db/
|  |  \- migrations/
|  |- prisma/
|  |- scripts/
|  \- src/
|     |- config/
|     |- controllers/
|     |- middleware/
|     |- routes/
|     |- services/
|     \- utils/
|- frontend/
|  |- public/
|  \- src/
|     |- app/
|     |- components/
|     |- config/
|     |- context/
|     |- hooks/
|     \- services/
|- mobile/
|- docs/
|- scripts/
|  |- dev-up.ps1
|  |- dev-down.ps1
|  \- dev-status.ps1
|- runtime/           # generated at runtime (logs + pid files)
|- docker-compose.yml
|- package.json       # workspace utility scripts
\- README.md
```

## Folder Responsibilities

- `backend/src/config`: service initialization (Prisma, Firebase, Cloudinary)
- `backend/src/controllers`: request handlers
- `backend/src/routes`: API endpoints and route wiring
- `backend/src/services`: business logic and DB interactions
- `backend/src/middleware`: auth, admin, upload middleware
- `frontend/src/app`: Next.js routes/pages
- `frontend/src/components`: reusable UI pieces
- `frontend/src/services`: API client layer
- `scripts`: one-command local dev lifecycle management
- `runtime`: local generated artifacts (not committed)

## Local Ports

- Frontend: `8000`
- Backend: `4000`
- PostgreSQL: `5432`
- Redis: `6379`
