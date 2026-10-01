# Matree master-data ownership and seed policy

Production must not depend on demo member profiles, fake activity, synthetic events or local passwords.

## Repository-owned catalog data

Stable application catalogs are source-controlled where they are part of the product contract, such as
supported religion/theme families, supported locale catalog, India profile/location option catalogs and
structured profile field schemas. These are configuration/catalog data, not synthetic production users.

## Server-owned dynamic data

Usernames, profiles, preferences, verification state, Trust/risk state, subscriptions, moderation,
notifications and recommendation feedback are created from real authenticated/server/provider events.
They must not be pre-seeded with fake production records.

## Seed procedure

For production infrastructure provisioning:

1. deploy Rules/Indexes/Functions;
2. configure approved Firebase/Play/provider environment;
3. deploy/activate only the supported catalog/configuration values;
4. do not create demo users or synthetic interactions;
5. run smoke tests using explicitly identified test accounts in staging/closed-test environments;
6. remove or isolate test records before production promotion.

Repository CI scans guard against reintroduction of demo/seed account authority and synthetic member
inventory in production source.
