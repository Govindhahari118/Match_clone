# Matree

Production-oriented Android matrimony app with a Firebase backend. Repository CI certifies code-controlled gates; production release still requires exact-SHA external evidence, provider configuration, real-device acceptance, and Play promotion.

## Project Structure

```
Match_clone/
├── MatchApp/                  # Android application (Kotlin + Jetpack Compose)
├── docs/
│   ├── analysis/              # Market & competitive analysis reports
│   ├── architecture/          # Technical architecture documentation
│   ├── deployment/            # Deployment guides and checklists
│   └── README.md              # Detailed project documentation
├── assets/
│   ├── store/
│   │   ├── screenshots/       # Play Store screenshots
│   │   └── graphics/          # Feature graphics, banners
│   └── branding/              # Logos, icons, brand guidelines
├── scripts/
│   ├── deploy/                # Firebase & Play Store deployment scripts
│   └── setup/                 # Developer environment setup scripts
└── .github/
    └── workflows/             # CI/CD pipelines (Android build + UI tests)
```

## Quick Start

```bash
cd MatchApp
./gradlew assembleDebug
```

See [docs/README.md](docs/README.md) for product/engineering documentation and [docs/release/GO_NO_GO.md](docs/release/GO_NO_GO.md) for the exact production promotion contract.
