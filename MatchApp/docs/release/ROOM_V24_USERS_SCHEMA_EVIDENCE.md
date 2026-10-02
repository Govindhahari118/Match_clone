# Room v24 users schema evidence

Source: Production CI #1290, exact head `664131783844b0dc803c07db448646177b228e58`.

Artifact: `android-code-evidence-664131783844b0dc803c07db448646177b228e58` (artifact id `11228131261`).

Full generated Room schema file SHA-256:

`4b53a9429dd66739f1bb2f6cf343d2a0c30abab2290f7ebf37f3cf84fb47467e`

The v24 generated `users` table contained 88 persisted columns, including the retired local-only `passwordHash` and `isSeed` fields, with unique indexes on `email` and `firebaseUid`.

The v24 -> v25 migration in `Migrations.kt` was derived from this generated schema. It rebuilds `users` with all retained columns, copies data by explicit column list, removes only `passwordHash` and `isSeed`, and recreates both unique indexes.

Production CI must generate the current Room schema and reject v25 if either retired column remains. Emulator-backed migration tests must migrate every supported historical version through v25 and assert retained rows survive.
