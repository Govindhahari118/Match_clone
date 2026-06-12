# MatrimonyConnect

Production-grade Android matrimony app with Firebase backend.

## Project Structure

```
Match/
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

See [docs/README.md](docs/README.md) for full documentation.
