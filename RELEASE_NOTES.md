# Diet Tracker 1.0.0

First public Android source and APK release.

Highlights:

- Local-first food and drink journal with manual, text-to-USDA, quick-food, and AI photo drafts.
- Per-photo consent, private stateless OpenAI gateway, strict structured output, and review before persistence.
- USDA Foundation/SR Legacy-only verification, field provenance, missing-field merge, and a transparent versioned score.
- Comprehensive nullable nutrients, explicit Nutrition/Hydration Health Connect import and write review, and no silent sync.
- 7/30/90/all logged-day trends plus separate US, EU, and Singapore coverage-aware guidance.
- Encrypted gateway configuration, local JSON export, delete all, disabled backup, no analytics, and no bundled keys.
- 20 JVM tests, 4 gateway contract tests, and 2 API 36 Compose instrumented tests.

AI photo analysis requires the user to deploy `gateway/` and configure separately billed OpenAI API access. ChatGPT Plus is not API quota. Manual/local features require no key.

This software is informational and not medical advice or a medical device.