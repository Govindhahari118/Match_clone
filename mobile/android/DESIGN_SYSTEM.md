# Matree Android Design System

## Design hierarchy

Implementation precedence:

1. approved canonical reference image;
2. approved Matree semantic tokens;
3. approved product UX requirements;
4. Android usability conventions;
5. Material 3 behavior;
6. local assumptions.

Material components are implementation primitives. They do not define the Matree visual identity.

## Shared visual DNA

All themes retain the same:

- information architecture;
- navigation destinations and behavior;
- component semantics;
- spacing rhythm;
- corner geometry;
- typography relationships;
- interaction target sizing;
- loading/error/offline behavior;
- accessibility and responsive behavior.

The current shared geometry uses 10/14/20/24dp rounded families, 18dp action radii, and a 4/8/12/16/24/32/40dp spacing scale. These values are centralized in `ThemeTokens.kt`.

## Appearance families

### Universal

Warm-neutral, premium, calm and non-religious. Uses a soft atmospheric halo and restrained wine/rose accents.

### Hindu

Warm cream, sophisticated saffron, deep maroon and muted gold. The atmosphere uses abstract warm arch/ring geometry only; it does not generate sacred imagery or scripture.

### Muslim

Emerald/deep green, refined teal, cream and restrained gold. The atmosphere uses low-contrast abstract geometry only; it never renders sacred text.

### Christian

White, soft/deep blue, warm light and restrained gold. The atmosphere uses soft light fields rather than poster-like religious graphics.

### Sikh

Deep/royal blue, saffron and restrained gold. The atmosphere uses a subtle indigo weave without inserting sacred symbols.

### Buddhist / Jain / Parsi / Other

Theme slots exist, but their reference status is `REFERENCE_REQUIRED`. Until approved reference images are present, they inherit Universal tokens and remain disabled in the appearance picker.

## Semantic tokens

`MatreeThemeTokens` exposes:

- background/surface/text/brand/action/state/border colors;
- hero/header/selected/background gradient recipes;
- radii;
- spacing;
- elevation;
- typography;
- atmosphere style.

No feature screen should define a religion-specific color directly.

## Components

Implemented reusable variants:

- top app bar;
- hero header;
- bottom navigation;
- primary/secondary/text/icon buttons;
- cards and list rows;
- section headers;
- search and text fields;
- OTP input;
- chips, filters and tabs;
- badges and avatars;
- slot-based image/media cards;
- respectful content cards;
- media controls;
- switches, checkboxes and radio controls;
- menus and tooltips;
- loading skeletons and progress;
- empty/error/offline states;
- confirmation dialogs;
- modal bottom sheets;
- snackbar host;
- adaptive content container.

## Data-integrity rules

UI components are intentionally slot/state-driven. They do not create member photos, profile attributes, compatibility values, verification badges, online activity, availability, testimonials, or religious attributes. Those values must come from authorized product data.

Saved profile religion is represented separately in `ProfileReligion.kt`; appearance preference is stored by `AppearanceThemeRepository`. Neither layer implicitly writes the other.

## Accessibility and platform behavior

- minimum interaction sizing is 48dp or larger;
- typography uses scalable `sp`;
- layout uses start/end semantics and supports RTL;
- root scaffold consumes safe drawing insets;
- search content responds to IME insets;
- content is width-constrained for larger displays;
- screen content scrolls so large fonts and localization expansion remain usable;
- core text and primary-action color pairs are guarded by contrast tests.

## Non-drift rule

New screens must not redefine radii, button shapes, icon family, card elevation, typography hierarchy, navigation treatment, or gradient style locally. Any approved global change belongs in the design tokens or shared component implementation.
