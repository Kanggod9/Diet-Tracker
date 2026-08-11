# Architecture

Diet Tracker separates deterministic local logic from explicit external boundaries.

## Android app

- `domain/`: journal and quick-food models, nullable nutrient vocabulary, field provenance, natural-language parsing, USDA scaling, score formula, logged-day trends, and regional suggestions.
- `data/`: app-private SQLite repository, versioned nutrient codec, USDA cache, and Android-Keystore-backed gateway configuration.
- `integration/`: bounded HTTPS client, OpenAI photo contract, USDA allowlist and normalization contract, and Health Connect nutrition/hydration mapping and read/write gateway.
- `ui/`: Compose dashboard, history, analysis, logging/review dialogs, Health Connect selection review, gateway settings, export, and delete-all confirmation.

All journal persistence is downstream of an explicit confirmation. Photo bytes and unconfirmed drafts remain in memory. Missing nutrient values are omitted rather than converted to zero. Edits replace an edited field's source with unverified manual provenance while untouched verified sources remain intact.

## Photo and USDA flow

1. Android's photo picker returns a content URI.
2. A per-photo consent dialog names the configured gateway boundary.
3. The app reads at most 8 MB and sends the image through authenticated HTTPS.
4. The stateless gateway calls OpenAI Responses with `store: false` and strict schema output.
5. The app validates the schema and creates an in-memory draft.
6. For a package photo, visible label fields take precedence. A reviewed Foundation/SR Legacy selection can fill only missing fields.
7. The user edits and confirms; only then is a local journal row created.

## Private gateway

`gateway/src/worker.js` is a stateless Cloudflare Worker module. It authenticates a personal app token with a constant-size digest comparison, accepts only three POST routes, caps request sizes, returns `no-store`, holds provider keys only in deployment bindings, and uses no KV/D1/R2 or application logging.

- `POST /v1/photo/analyze`
- `POST /v1/usda/search`
- `POST /v1/usda/food`

The Android app never calls OpenAI or data.gov directly. A multi-user deployment should replace the personal shared token with per-user authentication and rate limits.

## Health Connect

The app requests only Nutrition and Hydration read/write permissions. User-selected nutrition values map to `NutritionRecord`; water maps to `HydrationRecord`. Reads return in-memory entries for selection review, and writes occur only for selected local entries. No scheduler or background service exists.