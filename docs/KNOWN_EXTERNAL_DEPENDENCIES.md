# Known External Provider Dependencies

The codebase must fail closed when these providers are not configured in production. Passing application CI does not prove provider integration.

- SMS/OTP delivery provider: secure code generation is implemented; production delivery credentials/provider must be configured and exercised.
- Razorpay: production key/secret, webhook verification, reconciliation and refund flows must be configured and tested in the target environment.
- Identity/liveness verification: granular states are implemented; automated KYC/liveness requires an approved provider and policy-specific integration.
- Media storage/moderation: storage errors are explicit and primary-photo replacement is atomic; production object storage and moderation provider configuration must be validated.
- Push/email notifications: delivery providers must be tested for retry, deduplication and user preference compliance before claiming production completeness.
