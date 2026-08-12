# Changelog

## 1.1.0 - 2026-08-13

- Redesigned the main screen as Logs with a health-style energy card, embedded food score, nutrient progress rows, and a collapsible week/month calendar for previous-day access.
- Added Camera and Album sources for AI photo logging, persisted optional consent-dialog suppression, and ensured images remain transient.
- Simplified manual logging into separate food name and quantity controls with g, kg, mL, L, serving, and kcal units.
- Replaced missing-value copy with `--` and made nutrient progress values editable in detailed manual and AI review flows.
- Added a Target section with US, EU, and Singapore defaults and per-nutrient custom values.
- Updated the food score to use available reported nutrients regardless of source verification while preserving provenance and missing-value handling.
- Added an opt-in Health Connect Auto Write setting for newly confirmed logs; imports and manual writes retain review flows.
- Added USDA `DEMO_KEY` fallback while retaining an optional private Worker `USDA_API_KEY` override.
- Removed personalized build-time gateway bootstrapping. Public source and APKs contain no gateway endpoint, app token, or provider key.
- Expanded automated coverage to 23 JVM tests, 5 gateway contract tests, and 7 API 36 Compose tests.

## 1.0.0 - 2026-08-11

- Added native Kotlin/Compose food and drink journal with mandatory draft review, quick foods, edit/delete, JSON export, and delete all.
- Added natural-language quantity/meal parsing in English and Chinese-safe Unicode form.
- Added stateless authenticated gateway for OpenAI photo analysis and USDA FoodData Central.
- Added package-label-first extraction, USDA Foundation/SR Legacy-only missing-field fill, persistent cache, and field provenance.
- Added comprehensive Health Connect Nutrition/Hydration mapping and explicit permission/import/write review UI.
- Added transparent USDA-only score, logged-day trends, and coverage-aware US/EU/Singapore suggestions.
- Disabled backup/cleartext traffic and added Android-Keystore-protected gateway configuration.
- Added JVM, gateway, and API 36 Compose test suites plus minified release builds.
