# Matree Native Android

This directory contains the native Android UI/UX foundation for Matree.

## Purpose

The implementation follows the production rules in the Matree religion-specific UI/UX master specification:

- one application architecture and interaction model across all themes;
- semantic design tokens instead of per-screen hard-coding;
- independent appearance preference that never mutates or infers saved profile religion;
- religion-specific visual families for reference-backed directions;
- no invented scripture, sacred symbols, compatibility scores, verification states, activity, availability, testimonials, or profile data;
- edge-to-edge Android behavior, safe insets, IME handling, RTL support, scalable typography, and 48dp+ interactive targets;
- state coverage for loading, empty, populated, error, offline, dialogs, and sheets;
- neutral fallback for religion families that do not yet have approved canonical references.

## Current reference-backed appearance directions

The supplied written specification defines visual directions for:

- Default / Universal
- Hindu
- Muslim
- Christian
- Sikh

Buddhist, Jain, Parsi, and Other have explicit theme slots but deliberately use the neutral family and are disabled in the picker until approved canonical references are added. This prevents the implementation from inventing culturally or religiously sensitive visual language.

## Structure

- `app/src/main/java/com/matree/app/design` — semantic tokens, theme registry, atmosphere renderer
- `app/src/main/java/com/matree/app/ui/components` — reusable production components
- `app/src/main/java/com/matree/app/ui/screens` — screen integration
- `app/src/main/java/com/matree/app/data` — appearance preference persistence
- `app/src/main/java/com/matree/app/domain` — domain types kept separate from appearance
- `app/src/debug` — theme review previews
- `app/src/test` — token/contrast/reference-safety tests

## Build

Open `mobile/android` in Android Studio with JDK 17 and let Gradle sync.

The module intentionally does not fabricate an API layer. Connect existing authenticated Matree services/repositories to the screen state models before showing real member data.

## Visual QA

Use `ThemeGalleryPreview.kt` to compare the five reference-backed visual families on a 390dp baseline phone.

For every new screen:

1. use `MatreeTheme.tokens`;
2. reuse components from `ui/components`;
3. use slot-based media surfaces for authorized member images;
4. provide meaningful loading/empty/error/offline behavior;
5. verify long text, font scaling, small phones, large phones, RTL and keyboard-visible layouts;
6. do not add sacred symbols/text unless an approved reference explicitly establishes the treatment.
