# Trust Hardening End-to-End Test Plan

## Discovery and freshness
- Create active, low-activity, paused, found-match, married, closed, deleted, banned, and >90-day stale accounts.
- Assert only eligible lifecycle states appear in normal discovery.
- Set hard age/religion/location criteria and assert non-matching profiles never appear after ranking.
- Skip a profile and verify cooldown; hide it and verify permanent suppression until explicit undo; block it and verify suppression across search, interests, chat, contact and photo requests.
- Verify search/filter state survives normal navigation/reload according to product policy.

## Trust and media
- Verify phone/photo/identity states render independently.
- Upload replacement photo with storage failure and confirm current approved primary remains unchanged.
- Upload photo pending moderation and confirm it does not silently replace the approved primary.
- Verify missing/private/unapproved photo never displays a random human face.

## Interests and consent
- Send interest, withdraw while pending, resend later, accept reciprocal interest, decline incoming interest.
- Request protected contact/photo access and verify target approval/decline controls disclosure.
- Confirm premium status alone never grants protected data.

## Messaging
- Tamper with senderId in socket payload and confirm server ignores it.
- Retry identical clientMessageId and confirm exactly one message row.
- Send while recipient offline: sender state remains `sent`.
- Recipient connects/receives and acknowledges: state becomes `delivered`.
- Recipient reads conversation: state becomes `read`.
- Deliver late `delivered` receipt after `read` and confirm no downgrade.
- Block either side and confirm further sends fail across socket and REST.

## Payments
- Create same order twice with same idempotency key and confirm same pending payment/order is returned.
- Reject invalid Razorpay signature.
- Verify successful signature activates exactly one subscription transactionally.
- Repeat verification callback and confirm no duplicate subscription/charge record.
- Simulate network loss after payment and recover through payment status/history.
- Confirm plan quota labels equal backend entitlement values.

## Support and safety
- Create ticket; verify event history.
- Agent resolves; verify ticket remains resolved, not closed.
- User confirms closure; verify closedAt and event.
- Submit report; verify risk signal/audit creation without automatic guilt or hidden punitive state unless policy threshold/manual review applies.

## Concurrency and deletion
- Fetch profile version on two clients; save A then save stale B; B must get conflict.
- Delete/deactivate account; discovery must stop immediately.

## Release evidence
- Prisma validation green.
- Backend syntax/trust checks green.
- Frontend lint green.
- Next production build green.
- PostgreSQL main-schema -> SQL migration -> target-schema drift check green.
