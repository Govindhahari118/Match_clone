# Matree Data Safety implementation worksheet

This is a repository-derived engineering worksheet, **not a completed Google Play declaration**. The release owner must reconcile it with the exact production SDK/configuration and legal policy before submission.

| Data area | Repository use | Expected purpose | User control / deletion | Play declaration action |
|---|---|---|---|---|
| Account identifiers | Firebase Auth and account binding | Authentication, security, account lifecycle | Logout and account deletion flows | Verify identifiers collected/shared by production providers |
| Profile and religion | Member profile and matchmaking | Profile presentation, preferences, applicable compatibility | Profile editing; deletion lifecycle | Treat religion/profile fields as sensitive personal data and verify exact Play categories |
| Photos/media | Firebase Storage/protected media | Profile and authorized chat media | Replace/delete profile media; account deletion policy | Verify collection, storage and sharing semantics |
| Approximate/precise location | Foreground Nearby flow | Nearby discovery | Opt-in, stop sharing, deletion; no background permission | Declare exact/approximate location behavior and retention |
| Messages | Authorized mutual chat | Member communication | Subject to product retention/deletion policy | Verify collection and sharing; do not claim E2EE |
| Notification token | FCM | Delivery and account-bound routing | Rotated/cleared on lifecycle events | Verify device identifier treatment |
| Verification/KYC data | Verification lifecycle when enabled | Trust/safety | Provider/product retention rules apply | Reconcile provider data practices before launch |
| Purchase/entitlement data | Play Billing/server reconciliation | Paid membership and entitlement | Account-bound ledger; provider retention applies | Reconcile Google Play billing data categories |
| Analytics | Firebase Analytics where enabled | Product analytics | Production consent/policy controls | Verify enabled events contain no prohibited private payloads |
| Crash diagnostics | Crashlytics | Reliability/security diagnosis | Provider retention/policy | Verify diagnostics collection and redaction |

## Mandatory reconciliation

Before Play submission, inspect the exact release artifact and production Firebase/Google/verification providers. Confirm whether each category is collected, shared, ephemeral, required/optional, encrypted in transit, deletable, and its purpose. Do not infer a declaration from this worksheet alone.
