# Security and threat model

Report vulnerabilities privately to the repository owner. Do not include real health data, photos, passwords, tokens, API keys, keystores, or exported journals in an issue.

## Protected assets

Journal and Health Connect data, selected photos in transit, gateway and provider credentials, release signing identity, nutrient provenance, and the user's decision to persist or sync a draft.

## Implemented controls

- No bundled provider credentials. The APK accepts only an HTTPS gateway URL without embedded credentials, query, or fragment.
- The app-to-gateway token is encrypted by Android Keystore and excluded from backup and JSON export.
- The gateway keeps OpenAI/data.gov keys in deployment secrets, authenticates every route, caps bodies and responses, follows no client redirects, sanitizes errors, returns `no-store`, and has offline contract tests.
- Photo bytes cross the network only after per-photo consent; unconfirmed drafts are not persisted.
- OpenAI output is strict-schema requested and independently validated on both gateway and Android.
- USDA data types are allowlisted on both gateway and Android. Field provenance distinguishes package label, AI estimate, USDA Foundation/SR Legacy, manual, and Health Connect sources.
- Missing values remain missing. The USDA-only score and coverage-aware guidance do not silently treat missing as zero.
- Health Connect permissions and every import/write are explicit. No background service or silent sync exists.
- Android backup and cleartext traffic are disabled. The app contains no analytics or ad SDK.
- Release signing values come only from environment variables; the persistent keystore remains outside the source repository.

## Deployment responsibilities

The included shared bearer token is intended for a personal gateway. A public or multi-user service needs per-user authentication, token rotation, request quotas, abuse protection, platform-log review, a retention disclosure, and monitoring that never records photos, prompts, tokens, or health data. Keep Worker deployment files and `.dev.vars` out of Git.

Users should treat JSON exports as sensitive. Device compromise, screenshots taken by the user, a malicious gateway operator, provider/platform abuse-retention requirements, and inaccurate source nutrition data are outside the app's complete control.