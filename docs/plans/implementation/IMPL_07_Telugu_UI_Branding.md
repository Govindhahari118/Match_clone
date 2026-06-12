# IMPL_07 — Telugu UI Branding & Theme
## Pin-to-Pin Implementation Plan

> **Gap:** Plan 03 Section 10 requires Telugu cultural branding (maroon/gold theme, Poppins font, Telugu language support). Currently using default Material3 purple/teal theme. No Telugu strings.  
> **Impact:** The app looks generic. BharatMatrimony and TeluguMatrimony.com have strong cultural branding. Without Telugu identity, the app won't resonate with target audience.  
> **Source Docs:** PLAN_03 Section 10, PLAN_09 Section 2

---

## DELIVERABLES

### Files to Modify

| # | File | Changes |
|---|------|---------|
| 1 | `ui/theme/Color.kt` | Replace primary/secondary/background with Telugu cultural colors |
| 2 | `ui/theme/Type.kt` | Add Poppins font family + Noto Sans Telugu for Telugu text |
| 3 | `ui/theme/Theme.kt` | Update light/dark color schemes |
| 4 | `ui/theme/Shape.kt` | 12px card corners, 8px button corners |

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `res/values-te/strings.xml` | Telugu language strings (priority screens first) |
| 2 | `res/font/poppins_regular.ttf` | Poppins Regular font |
| 3 | `res/font/poppins_medium.ttf` | Poppins Medium font |
| 4 | `res/font/poppins_semibold.ttf` | Poppins SemiBold font |
| 5 | `res/font/poppins_bold.ttf` | Poppins Bold font |
| 6 | `res/font/noto_sans_telugu_regular.ttf` | Noto Sans Telugu for Telugu text |
| 7 | `res/font/noto_sans_telugu_bold.ttf` | Noto Sans Telugu Bold |
| 8 | `ui/theme/MatchColors.kt` | Extended color palette (gradients, surfaces) |
| 9 | `ui/components/MatchTopBar.kt` | Branded app bar with logo + gold accent |
| 10 | `ui/components/MatchButton.kt` | Branded button with gradient (maroon → dark) |
| 11 | `assets/branding/logo_maroon.svg` | App logo in maroon theme |

### Color Palette (from PLAN_03 Section 10)

```kotlin
// Telugu Cultural Colors
object MatchColors {
    // Primary: Deep Maroon (traditional Telugu wedding color)
    val Primary = Color(0xFF8B1A2E)
    val PrimaryDark = Color(0xFF5E1220)
    val PrimaryLight = Color(0xFFB34055)
    
    // Secondary: Antique Gold (traditional jewelry color)
    val Secondary = Color(0xFFB8860B)
    val SecondaryDark = Color(0xFF8B6508)
    val SecondaryLight = Color(0xFFDAA520)
    
    // Background: Warm Cream (wedding invitation color)
    val Background = Color(0xFFFFFBF5)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFFFF5E6)
    
    // Text
    val OnPrimary = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF1A1A1A)
    val OnSurface = Color(0xFF2D2D2D)
    val TextSecondary = Color(0xFF666666)
    
    // Status
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFE53935)
    val Warning = Color(0xFFFFC107)
    
    // Score colors
    val ScoreExcellent = Color(0xFF4CAF50)
    val ScoreGood = Color(0xFFFFC107)
    val ScoreAverage = Color(0xFFFF9800)
    val ScoreLow = Color(0xFFF44336)
    
    // Dark theme variants
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkPrimary = Color(0xFFE57373) // Lighter maroon for dark mode
    val DarkSecondary = Color(0xFFFFD54F) // Lighter gold for dark mode
}
```

### Typography (Poppins + Noto Sans Telugu)

```kotlin
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

val NotoSansTeluguFamily = FontFamily(
    Font(R.font.noto_sans_telugu_regular, FontWeight.Normal),
    Font(R.font.noto_sans_telugu_bold, FontWeight.Bold),
)

val MatchTypography = Typography(
    displayLarge = TextStyle(fontFamily = PoppinsFamily, fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = PoppinsFamily, fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = PoppinsFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = PoppinsFamily, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = PoppinsFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = PoppinsFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = PoppinsFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = PoppinsFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium),
)
```

### Telugu Localization (Priority Screens)

| Screen | Key Strings to Translate |
|--------|-------------------------|
| Home | "Daily Matches", "New Profiles", "Complete Your Profile" |
| Discovery | "Interested", "Decline", "View Profile", "Why This Match" |
| Profile | "About Me", "Family Details", "Partner Preferences" |
| Chat | "Type a message", "Voice Call", "Video Call" |
| Settings | "My Plan", "Privacy", "Verification", "Help" |
| Subscription | Plan names, feature descriptions, pricing |
| Wizard | Step titles, field labels, validation messages |

### Dark Mode Support

```kotlin
@Composable
fun MatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = MatchColors.DarkPrimary,
            secondary = MatchColors.DarkSecondary,
            background = MatchColors.DarkBackground,
            surface = MatchColors.DarkSurface,
        )
    } else {
        lightColorScheme(
            primary = MatchColors.Primary,
            secondary = MatchColors.Secondary,
            background = MatchColors.Background,
            surface = MatchColors.Surface,
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MatchTypography,
        shapes = MatchShapes,
        content = content
    )
}
```

### Font Files Download

```
Download from Google Fonts (free, OFL license):
- Poppins: https://fonts.google.com/specimen/Poppins
  → Regular (400), Medium (500), SemiBold (600), Bold (700)
- Noto Sans Telugu: https://fonts.google.com/noto/specimen/Noto+Sans+Telugu
  → Regular (400), Bold (700)

Place in: app/src/main/res/font/
```

---

## DEFINITION OF DONE

- [ ] Primary color is deep maroon (#8B1A2E), secondary is gold (#B8860B)
- [ ] Background is warm cream (#FFFBF5) in light mode
- [ ] Poppins font applied to all English text
- [ ] Noto Sans Telugu renders Telugu text correctly
- [ ] Dark mode uses lighter maroon/gold variants
- [ ] Telugu strings.xml has at least 50 key strings translated
- [ ] Language selection option in Settings (English / Telugu)
- [ ] Profile cards, buttons, and headers reflect new branding
- [ ] BUILD SUCCESSFUL
