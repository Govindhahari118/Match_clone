# CI Notes

The hardening pull request is expected to run three independent jobs:

1. Backend Prisma validation plus syntax/trust regression checks.
2. Frontend ESLint plus production Next.js build.
3. PostgreSQL migration drift validation from the `main` Prisma schema through `20260916_trust_freshness_hardening.sql` to the branch target schema.

Any red job is treated as a release blocker and must be corrected before merge.
