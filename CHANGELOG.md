# Changelog

## 1.3.1 - 2026-08-20

- Added standard Android long-press deletion with confirmation for saved Quick foods.
- Added swipeable week navigation using natural timeline direction, bounded so the current week is the latest available week.
- Added dashboard-only Fat kcal and Vitamin B5 abbreviations while retaining full names elsewhere.
- Fixed previous-level navigation for Detailed manual, Quick food review, Photo source, cancelled photo selection, photo consent, and AI review.
- Added optional, independently configurable breakfast, lunch, and dinner reminders with notification permission and logged-meal suppression.

## 1.2.1 - 2026-08-13

- Expanded Food Score explanations with named add/deduct nutrients, density comparisons, completeness, and point caps.
- Changed calendar rings from Energy progress to each day’s Food Score.
- Added D/W/M/3M/Y Food Score histograms, per-log daily nutrient contributions, and scored-day period averages.
- Fixed USDA Back to return to the preceding log chooser or AI review while Cancel closes the flow.
- Added compact MUFA, PUFA, and Unsat. fat labels on dashboard tiles while retaining full names in detailed views and editors.
- Added Food, Drink, and Both entry kinds in Detailed manual and AI estimate, with dual Health Connect nutrition/hydration export when data is present.
## 1.2.0 - 2026-08-13

- Removed the Analysis tab and rebuilt Logs as a turquoise swipeable dashboard with score plus three nutrient tiles followed by six-tile pages.
- Added flame-orange exceeded-target states and tap-through D/W/M/3M/Y nutrient histograms.
- Consolidated manual, USDA, Camera, Album, and quick-food logging under one + Log button.
- Reorganized Journal entries into filterable, collapsible meal-category cards with counts and energy totals.
- Replaced Target progress rows with framed nutrient cards and separate edit actions.
- Added explicit back navigation to USDA search, entry review, and nutrient history.
- Replaced the optional Auto Write setting with foreground Health Connect create/replace/delete synchronization using stable client record IDs.
- Kept the build public-safe and provider-neutral; no gateway or provider credentials are embedded.

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
