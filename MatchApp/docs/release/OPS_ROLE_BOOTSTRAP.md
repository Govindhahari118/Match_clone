# Matree operations-role bootstrap

Operations access is granted with Firebase Auth custom claims. It must never be granted from the
consumer app, Firestore client writes, or by editing a member document.

## Safe workflow

1. Create the operator Firebase Auth account through the approved production identity process.
2. Record the approved change/ticket and the human applying the change.
3. Authenticate locally/CI with an approved Google Application Default Credential for the exact
   production Firebase project. Do not place service-account JSON in the repository.
4. Run a dry-run first:

```bash
cd MatchApp/functions
MATREE_BOOTSTRAP_ACTOR="operator@example.invalid" \
npm run ops:bootstrap -- \
  --project <production-project-id> \
  --uid <firebase-uid> \
  --roles support,moderator \
  --approval <approved-change-reference>
```

5. Review the existing and requested roles. Then repeat with `--apply`.
6. Granting `ops_admin` additionally requires `--allow-ops-admin`.
7. The command revokes the account's refresh tokens so the new role set is not silently inherited by
   already-issued long-lived sessions.
8. The applied change writes a server-only `opsBootstrapAudit` record containing only the UID,
   before/after role set, project ID, human actor reference and approval reference.

Use `--roles none` to remove all Matree operations roles. Existing unrelated custom claims are
preserved.

## Role boundaries

- `support`: support queue/actions only.
- `moderator`: moderation and account-enforcement actions.
- `kyc_reviewer`: supported verification review.
- `payment_ops`: payment reconciliation views/actions.
- `ops_admin`: explicitly privileged administrative operations.

The callable backend remains authoritative: possessing the web console URL does not grant access.
