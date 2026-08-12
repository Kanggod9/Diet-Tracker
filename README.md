# Diet Tracker

Diet Tracker is a local-first Android food and drink journal built with Kotlin and Jetpack Compose. Version 1.1.0 targets Android 16 / API 36 and supports Android 8.0 / API 26 and later.

## What is included

- A health-style Logs dashboard with a collapsible week/month calendar, previous-day editing, logged-energy summary, food score, and nutrient progress rows.
- Food and drink logging by detailed manual entry, name plus quantity, reviewed USDA search, reusable quick foods, or an AI-analyzed photo from Camera or Album.
- A mandatory review step before a draft is saved. Missing nutrient values remain missing and appear as `--`, not as zero.
- User-controlled photo consent. Images are read into bounded memory, sent only through the configured private HTTPS gateway, and never copied into the journal database.
- Package-label-first AI behavior: visible label fields are preserved; a user-selected USDA match may fill only missing fields.
- USDA FoodData Central data restricted to Foundation and SR Legacy, scaled from per-100-g values and stored with field-level provenance. The gateway uses a private `USDA_API_KEY` when configured and otherwise falls back to the limited `DEMO_KEY`.
- Comprehensive nullable nutrition fields aligned with Health Connect `NutritionRecord`, with water mapped separately to `HydrationRecord`.
- A Target section with US, EU, and Singapore reference defaults plus editable custom nutrient targets.
- Explicit Health Connect permission and import review, manual write review, and an optional Auto Write switch for newly confirmed logs. No scheduler or background sync service is included.
- Analysis across 7/30/90/all logged days, daily summaries, and coverage-aware suggestions.
- A transparent versioned nutrient-density food score that uses available reported nutrients while preserving their source and missing-value status.
- App-private SQLite storage, Android-Keystore-encrypted gateway configuration, JSON export, delete-all, disabled Android backup, no ads, and no analytics SDK.
- A stateless private Cloudflare Worker under `gateway/` for OpenAI Responses and USDA FoodData Central.

Diet Tracker is not a medical device. Nutrition estimates, scores, and reference comparisons may be incomplete or inaccurate and are not personalised medical advice.

## Public build and gateway setup

The public source and release APK contain no gateway URL, app access token, OpenAI key, USDA key, or signing secret. Manual/local features work immediately. To enable AI photo analysis or in-app USDA search, deploy `gateway/`, store its credentials as encrypted deployment secrets, and enter only the resulting HTTPS gateway URL and app access token in the Android Settings screen.

### OpenAI API key

ChatGPT Plus cannot be used as the app's API quota. OpenAI API access is billed and configured separately. The Android app must never contain an OpenAI API key; the gateway holds `OPENAI_API_KEY`, `OPENAI_MODEL`, and the optional `USDA_API_KEY`. See [gateway/README.md](gateway/README.md).

## Build and test

Standard prerequisites are JDK 17 and Android SDK Platform 36. The repository includes a Gradle 8.13 wrapper:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat assembleRelease
```

The maintainer workspace may use a sibling `Diet_Tracker_build_environment` to reuse an existing JDK, Gradle, Android SDK, system image, and dependency cache while keeping generated output outside this repository:

```powershell
cd ..\Diet_Tracker_build_environment
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Task all
powershell -ExecutionPolicy Bypass -File .\scripts\emulator.ps1 -Action start
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Task connected
```

`all` runs JVM tests, debug assembly, minified release assembly, and Android-test compilation. `connected` requires a ready device or emulator.

Release signing is optional for source builds and is configured only through `DIET_TRACKER_KEYSTORE`, `DIET_TRACKER_KEY_ALIAS`, `DIET_TRACKER_STORE_PASSWORD`, and `DIET_TRACKER_KEY_PASSWORD`. Never commit signing material.

## Privacy and security

Read [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), [ARCHITECTURE.md](ARCHITECTURE.md), and [SOURCES.md](SOURCES.md). Provider credentials, app access tokens, signing files, local databases, `.env` files, deployment configuration, build output, and user exports must not be committed.

## License

MIT. See [LICENSE](LICENSE).
