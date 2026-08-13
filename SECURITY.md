# Security and threat model

Report vulnerabilities privately to the repository owner. Do not include real health data, photos, passwords, tokens, API keys, keystores, or exported journals in an issue.

## Protected assets

Journal and Health Connect data, selected photos in transit, gateway and provider credentials, release signing identity, nutrient provenance, and the user's decisions to persist or export data.

## Implemented controls

- Public source and APK builds contain no gateway endpoint, app access token, provider credential, or signing secret. Users enter their own HTTPS gateway URL and app token in Settings.
- The app-to-gateway token is encrypted by Android Keystore and excluded from backup and JSON export.
- The gateway keeps OpenAI/data.gov keys in deployment secrets, authenticates every route, caps bodies and responses, follows no client redirects, sanitizes errors, returns `no-store`, and has offline contract tests.
- Photo bytes cross the network only after the user selects Camera or Album and accepts consent, unless that user explicitly persists the `Don't show next time` preference. Unconfirmed drafts and images are not persisted.
- OpenAI output is strict-schema requested and independently validated on both gateway and Android.
- USDA data types are allowlisted on both gateway and Android. Field provenance distinguishes package label, AI estimate, USDA Foundation/SR Legacy, manual, and Health Connect sources.
- Missing values remain missing. The food score and coverage-aware guidance do not silently treat missing as zero.
- Health Connect permissions and imports remain explicit. Stable client record IDs allow permission-aware foreground create/edit/delete synchronization without duplicate versions. Imported third-party Health Connect records are not automatically written back. No scheduler or background service exists.
- Android backup and cleartext traffic are disabled. The app contains no analytics or ad SDK.
- Release signing values come only from environment variables; the persistent keystore remains outside the source repository.

## Deployment responsibilities

The included shared bearer-token design is intended for a personal gateway. A public or multi-user service needs per-user authentication, token rotation, request quotas, abuse protection, platform-log review, a retention disclosure, and monitoring that never records photos, prompts, tokens, or health data. Keep Worker deployment files and `.dev.vars` out of Git.

Users should treat JSON exports as sensitive. Device compromise, screenshots taken by the user, a malicious gateway operator, provider/platform abuse-retention requirements, and inaccurate source nutrition data are outside the app's complete control.
