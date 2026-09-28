# Matree religion-specific visual system

## Production contract

Matree uses one Android product with eight presentation families:

- Matree Neutral
- Hindu
- Muslim
- Christian
- Sikh
- Buddhist
- Jain
- Parsi

The appearance family is presentation-only. It never changes canonical profile religion,
partner preferences, eligibility, ranking, pricing, verification, privacy, blocking,
messaging, contact access or account lifecycle.

## Resolution

Appearance has two independent dimensions:

1. Theme preference: Automatic, Matree Neutral or Manual.
2. Display mode: System, Light or Dark.

Automatic follows the signed-in member's confirmed profile religion. Missing, unsupported
or other religion values resolve to Matree Neutral. Manual selection may use any supported
visual family and does not alter the profile.

## Visual strategy

Production uses the approved hybrid approach:

- roughly 80% color-led identity across ordinary working screens;
- roughly 20% richer cultural expression on high-emotion surfaces such as Home hero,
  theme previews and neutral empty states.

Rich concept-board photography is not embedded as wallpaper in functional screens.
The production layer uses lightweight Compose geometry, theme colors and reusable
components so startup, readability and maintainability are not compromised.

## Approved color direction

- Hindu: saffron / vermilion / marigold / gold / cream / deep plum.
- Muslim: emerald / deep teal / green / ivory / sand / gold.
- Christian: white / ivory / chapel blue / royal blue / soft gold / stone.
- Sikh: gold / saffron / deep navy / ivory / royal blue / sage.
- Buddhist: ivory / sand / saffron / lotus pink / sage / earth.
- Jain: ivory / cream / pale saffron / muted gold / leaf green / stone.
- Parsi: warm ivory / antique gold / teal / navy / dusty rose / sage.
- Neutral: slate / stone / warm white / restrained rose-beige.

Primary button colors remain contrast-safe even when the decorative accent is brighter.

## Architecture

The runtime resolves an `AppPalette` once at the app root. `MatchTheme` provides both
Material 3 colors and a `MatreeVisualFamily` through Matree design tokens.

`MatreeVisualFamily` owns only presentation metadata:

- display label and description;
- hero presentation copy;
- motif family;
- decorative accent colors;
- ornament strength;
- non-authoritative visual value words.

The visual copy is deliberately generic and never claims personal religious practice,
verification, activity or compatibility.

Screens do not branch into separate Hindu/Muslim/Christian/etc implementations.

## Motifs

Lightweight Compose-drawn motifs are available for:

- Neutral abstract circles
- Hindu lotus/arch
- Muslim geometric arch
- Christian stained arch
- Sikh golden arch
- Buddhist lotus/stupa
- Jain marble/lotus
- Parsi heritage Art Deco

They are decorative only and excluded from semantics.

## High-emotion surfaces

- Home has an active-family `MatreeVisualHero`.
- Religion discovery cards preview their own family without changing appearance choice.
- Neutral empty states may show the current family motif.
- Settings shows a preview of the resolved family.
- Debug previews cover all eight families in light and dark.

Security, warning, error and trust surfaces keep stable semantic colors and are not
religion-decorated.

## Truthfulness

Production UI must never invent:

- match percentages;
- verification;
- premium status;
- online/recent activity;
- community;
- denomination or religious practice;
- dietary practice;
- horoscope compatibility;
- location proximity;
- response speed.

Concept-board examples are visual references only. Runtime labels must come from real
domain state or be omitted.

## Accessibility and CI

- minimum touch target is 48dp;
- primary action contrast is unit-tested across production palettes;
- semantic success/warning/interest contrast is tested in light and dark;
- unsupported manual palette keys fall back to Matree Neutral;
- display mode resolution is tested independently from theme family;
- the production truthfulness scan rejects raw Compose color literals in reachable UI.

Physical-device visual QA, font scaling and screenshot/golden evidence remain release
verification tasks rather than substitutes for code-level tests.
