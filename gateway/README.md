# Diet Tracker private gateway

This stateless Cloudflare Worker is the only component allowed to hold provider credentials. The Android app sends a selected photo only after per-photo consent and keeps the returned draft in memory until review. The worker uses the OpenAI Responses API with `store: false` and a strict schema, and accepts USDA FoodData Central records only from Foundation or SR Legacy.

Required encrypted Worker secrets:

- `APP_ACCESS_TOKEN`: random 16+ character token also entered into Android encrypted settings.
- `OPENAI_API_KEY`: OpenAI project API key. ChatGPT Plus does not include API billing.
- `OPENAI_MODEL`: a vision-capable Responses API model enabled for the project.
- `USDA_API_KEY`: data.gov key for FoodData Central.

Copy `wrangler.toml.example` to an untracked `wrangler.toml`, configure a custom HTTPS route, then set each value with `wrangler secret put NAME`. Do not paste secrets into source, Gradle files, issues, screenshots, or release assets.

The Worker uses no KV, D1, R2, analytics, or application logging. Responses are marked `no-store`. Configure platform abuse protection and rate limits for the deployment. The shared app token is suitable for a personal deployment; a multi-user service should replace it with per-user authentication.

Run contract tests with `npm test`. They need no provider keys and make no network requests.