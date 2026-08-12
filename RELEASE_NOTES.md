# Diet Tracker 1.1.0

Version 1.1.0 makes daily logging faster and adds the requested public-safe camera build.

Highlights:

- New Logs dashboard with an energy summary, food score, nutrient progress, and expandable week/month calendar.
- AI photo logging from either Camera or Album, with user-controlled consent and transient image handling.
- Simpler food name and quantity entry with g, kg, mL, L, serving, and kcal units.
- Editable nutrient progress rows and `--` for genuinely unavailable values.
- New Target section with US, EU, Singapore, and custom nutrient references.
- Food score no longer requires verified USDA provenance; it uses available reported nutrients and still distinguishes missing values from zero.
- Optional Health Connect Auto Write for newly confirmed logs, with no background service.
- USDA FoodData Central access through the private gateway with limited `DEMO_KEY` fallback or an optional private key override.
- Public APK starts with no gateway configured and contains no gateway endpoint, access token, OpenAI key, or USDA key.

Validation completed with 23 JVM tests, 5 gateway contract tests, 7 API 36 Compose tests, a minified release build, APK signature verification, and source/binary secret scans.

AI photo analysis and in-app USDA search require the user to deploy `gateway/` and enter that deployment's URL and app token in Settings. ChatGPT Plus is not OpenAI API quota. All local/manual features require no provider key.

This software is informational and is not medical advice or a medical device.
