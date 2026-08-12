# Architecture

Diet Tracker separates deterministic local logic from explicit external boundaries.

## Android app

- `domain/`: journal and quick-food models, nullable nutrient vocabulary, field provenance, natural-language parsing, USDA scaling, score formula, logged-day trends, and regional/custom targets.
- `data/`: app-private SQLite repository, versioned nutrient codec, USDA cache, and Android-Keystore-backed gateway configuration.
- `integration/`: bounded HTTPS client, OpenAI photo contract, USDA allowlist and normalization contract, and Health Connect nutrition/hydration mapping and read/write gateway.
- `ui/`: Compose Logs calendar/dashboard, Analysis, Target, Settings, logging/review dialogs, Health Connect review, export, and delete-all confirmation.

All journal persistence is downstream of an explicit draft confirmation. Photo bytes and unconfirmed drafts remain transient. Missing nutrient values are omitted rather than converted to zero. Edits replace an edited field's source with unverified manual provenance while untouched sources retain their provenance.

Public builds start with no gateway configured. The endpoint and shared app token can be entered only through Settings and are not generated into `BuildConfig` or another APK resource.

## Photo and USDA flow

1. Android Camera or Album selection provides a temporary image URI.
2. A consent dialog names the configured gateway boundary unless the user previously enabled `Don't show next time`.
3. The app reads at most 8 MB and sends the image through authenticated HTTPS.
4. The stateless gateway calls OpenAI Responses with `store: false` and strict schema output.
5. The app validates the schema and creates an in-memory draft.
6. For a package photo, visible label fields take precedence. A reviewed Foundation/SR Legacy selection can fill only missing fields.
7. The user edits and confirms; only then is a local journal row created.

Manual USDA search follows Android -> authenticated gateway -> FoodData Central. The Worker uses its private `USDA_API_KEY` when present and otherwise the rate-limited `DEMO_KEY`. Android never receives either USDA credential.

## Private gateway

`gateway/src/worker.js` is a stateless Cloudflare Worker module. It authenticates a personal app token with a constant-size digest comparison, accepts only three POST routes, caps request sizes, returns `no-store`, holds provider keys only in deployment bindings, and uses no KV/D1/R2 or application logging.

- `POST /v1/photo/analyze`
- `POST /v1/usda/search`
- `POST /v1/usda/food`

The Android app never calls OpenAI or data.gov directly. A multi-user deployment should replace the personal shared token with per-user authentication and rate limits.

## Health Connect

The app requests only Nutrition and Hydration read/write permissions. User-selected nutrition values map to `NutritionRecord`; water maps to `HydrationRecord`. Reads return in-memory entries for selection review. Writes occur after manual confirmation or for a newly confirmed entry while the user has enabled Auto Write. Auto Write is foreground event-driven; no scheduler or background service exists.
