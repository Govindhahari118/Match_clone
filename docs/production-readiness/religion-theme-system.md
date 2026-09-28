# Matree visual theme system

## Production contract

Matree uses one Android product with a premium universal default plus seven optional religion-inspired presentation families:

- Matree Signature — default for every new/unspecified appearance preference
- Hindu
- Muslim
- Christian
- Sikh
- Buddhist
- Jain
- Parsi

The internal compatibility key for Matree Signature remains `VIVAH` / `NEUTRAL` so existing stored preferences do not need a destructive migration.

Appearance is presentation-only. It never changes canonical profile religion, partner preferences, eligibility, ranking, pricing, verification, privacy, blocking, messaging, contact access or account lifecycle.

## Resolution and opt-in behavior

Appearance has two independent dimensions:

1. Visual family.
2. Display mode: System, Light or Dark.

Fresh installs and missing/unsupported appearance preferences resolve to Matree Signature. Religion-inspired themes are never selected merely because a profile contains a religion. Legacy stored `AUTOMATIC` values also normalize to Signature because the old contract used Automatic as a default and therefore could not prove explicit opt-in.

Users can explicitly choose any religion-inspired theme. Settings previews a religion theme first and applies it only after the user taps **Apply theme**. Users may also explicitly enable **Match my profile religion**, which is an optional automatic mode that follows the confirmed profile religion when supported. Missing/unsupported/Other values fall back to Matree Signature.

Theme selection does not mutate the member profile or matchmaking state. Availability or monetization rules may be added later without changing this presentation architecture.

## Flagship Matree Signature

The default is intentionally the strongest general matrimonial design rather than a generic fallback:

- warm ivory / white surfaces;
- deep plum primary actions;
- rose-beige secondary warmth;
- restrained champagne/gold-inspired ornament;
- editorial matrimonial hierarchy;
- subtle gradients and neutral Matree motifs;
- dedicated warm dark mode rather than generic black.

It contains no religion-specific symbolism.

## Religion-inspired visual strategy

Production uses the approved hybrid approach:

- roughly 80% color-led identity across ordinary working screens;
- roughly 20% richer cultural expression on Home hero, theme previews, empty states and selected high-emotion surfaces.

Supported directions:

- Hindu: saffron / vermilion / marigold / gold / cream / deep plum.
- Muslim: emerald / deep teal / green / ivory / sand / gold.
- Christian: white / ivory / chapel blue / royal blue / soft gold / stone.
- Sikh: gold / saffron / deep navy / ivory / royal blue / sage.
- Buddhist: ivory / sand / saffron / lotus pink / sage / earth.
- Jain: ivory / cream / pale saffron / muted gold / leaf green / stone.
- Parsi: warm ivory / antique gold / teal / navy / dusty rose / sage.

Rich concept-board photography is not embedded as wallpaper in functional screens. Lightweight Compose motifs, theme colors and reusable components keep startup, readability and maintainability intact.

## Architecture

`AppearanceThemeResolver` resolves one `AppPalette` at the app root. `MatchTheme` provides Material 3 colors plus `MatreeVisualFamily` through centralized Matree design tokens. Screens do not fork into separate religion-specific implementations.

`ThemePreference.NEUTRAL` is retained as the stable enum for Matree Signature. `ThemePreference.MANUAL` represents an explicitly selected religion-inspired family. `ThemePreference.AUTOMATIC` exists only as an explicit user opt-in to profile-religion following and is persisted as `PROFILE_RELIGION`; the legacy `AUTOMATIC` storage value migrates to Signature.

## Accessibility, semantics and truthfulness

- minimum touch target is 48dp;
- light and dark variants exist for every production family;
- primary-action contrast is unit-tested;
- semantic success/warning/error/verified/premium meaning stays stable across themes;
- unsupported manual palette keys fall back to Matree Signature;
- production UI never invents match percentages, verification, premium status, activity, community, religious observance, horoscope compatibility, proximity or other member facts;
- theme choice is never used as evidence of religious identity.

Physical-device visual QA, font scaling and screenshot/golden evidence remain release verification tasks rather than substitutes for code-level tests.
