# Uptime and Dashboard Runbook

Updated: 2026-03-08

## Scope

This runbook covers local/runtime monitoring for:

- Frontend (Next.js): `http://localhost:8000`
- Backend API (Express): `http://localhost:4000`
- Backend health endpoint: `http://localhost:4000/health`
- Infra containers: `matrimony-postgres`, `matrimony-redis`

## Quick Status Checks

Run from repo root:

```powershell
npm run dev:status
```

What it checks:

- Process listening on port `8000` (frontend)
- Process listening on port `4000` (backend)
- HTTP status for `/` and `/health`
- Docker container status for Postgres/Redis

## Startup and Shutdown

Start full local stack:

```powershell
npm run dev:up
```

Stop local stack:

```powershell
npm run dev:down
```

Keep infra up while stopping app servers:

```powershell
powershell -ExecutionPolicy Bypass -File ./scripts/dev-down.ps1 -KeepInfra
```

## Runtime Logs

Primary logs are written under `runtime/logs`:

- `runtime/logs/backend.out.log`
- `runtime/logs/backend.err.log`
- `runtime/logs/frontend.out.log`
- `runtime/logs/frontend.err.log`

Tail logs during incident response:

```powershell
Get-Content runtime/logs/backend.err.log -Wait
Get-Content runtime/logs/frontend.err.log -Wait
```

## API Health Baseline

Expected healthy response:

```json
{
  "status": "ok",
  "db": "connected",
  "socket": "active"
}
```

Manual check:

```powershell
Invoke-WebRequest http://localhost:4000/health -UseBasicParsing
```

## Recovery Playbook

1. Confirm health and status:
   - `npm run dev:status`
2. If backend is down:
   - check `runtime/logs/backend.err.log`
   - restart with `npm run dev:down` then `npm run dev:up`
3. If frontend is down:
   - check `runtime/logs/frontend.err.log`
   - restart with `npm run dev:down` then `npm run dev:up`
4. If DB or Redis containers are unhealthy:
   - run `docker ps`
   - run `docker compose stop postgres redis`
   - run `docker compose up -d postgres redis`
5. Re-check:
   - `npm run dev:status`
   - open `http://localhost:8000`
   - open `http://localhost:4000/health`

## Incident Notes Template

Capture the following for every incident:

- Start time (local + UTC)
- User-visible impact
- Error signatures from backend/frontend logs
- Status of `/health`
- Mitigation steps executed
- Recovery confirmation time
- Follow-up action items

