# Diet Tracker

Diet Tracker is a local-first Android food and drink journal built with Kotlin and Jetpack Compose. Version 1.0.0 targets Android 16 / API 36 and supports API 26+.

## What is included

- Food and drink logging by detailed manual entry, one-line text such as `lunch rice 250 g`, reviewed USDA search, reusable quick foods, or an AI-analyzed photo.
- A mandatory review step before every new draft is saved. Dismissing a text, USDA, Health Connect, or photo draft writes nothing.
- Per-photo consent. The selected image is read into bounded memory, sent only through the configured private HTTPS gateway, and is never copied into the journal database.
- Package-label-first AI behavior: visible label fields are preserved; a user-selected USDA match may fill only missing fields.
- USDA FoodData Central data restricted to Foundation and SR Legacy, scaled from per-100-g values and stored with field-level source, URL, version, retrieval time, and verification state.
- Comprehensive nullable nutrition fields aligned with Health Connect `NutritionRecord`, with water mapped separately to `HydrationRecord`. Missing is distinct from an explicit zero.
- Explicit Health Connect permission, import-review, and write-review actions. There is no background or silent sync and no Apple Health integration.
- Today totals, editable history, 7/30/90/all logged-day trends, daily summaries, coverage-aware suggestions, and separate US FDA, EU, and Singapore reference profiles.
- A transparent versioned nutrient-density score that uses only verified USDA Foundation/SR Legacy fields and explains every component.
- App-private SQLite storage, encrypted app-to-gateway configuration using Android Keystore, JSON export, delete-all, disabled Android backup, no ads, and no analytics SDK.
- A stateless private gateway component under `gateway/` for OpenAI Responses and USDA FoodData Central.

Diet Tracker is not a medical device. Nutrition estimates and reference comparisons may be incomplete or inaccurate and are not personalised medical advice.

## OPenAI API key

The Android app must never contain an Open AI API key. It stores only an app-to-gateway token encrypted on the device; the gateway holds `AI_API_KEY`, `OPENAI_MODEL`, and `USDA_API_KEY` as deployment secrets. See [gateway/README.md](gateway/README.md).

The app is fully buildable and usable for local/manual logging without any API key. A key is needed only when deploying and live-testing AI photo analysis.

## Build and test

Standard prerequisites are JDK 17 and Android SDK Platform 36. The repository includes a Gradle 8.13 wrapper:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
.\gradlew.bat assembleRelease
```

This workspace also includes a sibling `Diet_Tracker_build_environment` that reuses the existing CodexBar JDK, Gradle, SDK, system image, and dependency cache while keeping all generated output outside this repository:

```powershell
cd ..\Diet_Tracker_build_environment
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Task all
powershell -ExecutionPolicy Bypass -File .\scripts\emulator.ps1 -Action start
powershell -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Task connected
```

`all` runs JVM tests, debug assembly, minified release assembly, and Android-test compilation. `connected` requires a ready device or the reusable API 36 AVD.

Release signing is optional for source builds and is configured only through `DIET_TRACKER_KEYSTORE`, `DIET_TRACKER_KEY_ALIAS`, `DIET_TRACKER_STORE_PASSWORD`, and `DIET_TRACKER_KEY_PASSWORD`. Never commit signing material.

## Privacy and security

Read [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), [ARCHITECTURE.md](ARCHITECTURE.md), and [SOURCES.md](SOURCES.md). Provider credentials, signing files, local databases, `.env` files, deployment configuration, build output, and user exports must not be committed.

## License

MIT. See [LICENSE](LICENSE).
