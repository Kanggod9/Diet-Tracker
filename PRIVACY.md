# Privacy

Diet Tracker is local-first. The app contains no analytics or advertising SDK, declares `android:allowBackup="false"`, and stores persistent data in app-private storage.

## Stored on the device

- Confirmed journal entries and nutrient provenance.
- Quick foods, target profile/custom values, consent and Health Connect preferences, locally generated suggestions, Health Connect write receipts, and cached USDA reference records.
- A user-configured gateway URL and app-to-gateway token. The token is encrypted with a non-exportable Android Keystore key.

The public APK contains no preconfigured gateway or credential. OpenAI, USDA, and signing credentials are never stored by the Android app. JSON exports exclude gateway credentials but include local journal and non-secret settings. Delete all removes the local database and encrypted gateway configuration from the device.

## Photo boundary

A photo leaves the device only after the user explicitly chooses Camera or Album. The app shows a consent dialog before transmission unless the user has deliberately enabled `Don't show next time`; that preference can be cleared with Delete all. The app reads the URI into memory with an 8 MB cap, sends it to the configured private HTTPS gateway, and does not copy it into SQLite or permanent app files. Cancelling or discarding review creates no journal entry.

The gateway sends the image to the OpenAI API with `store: false` and returns a temporary structured draft. `store: false` controls OpenAI application-state storage; the gateway operator remains responsible for disclosing deployment logs, subprocessors, abuse monitoring, region, and retention. The included Worker enables no application logging or storage, but platform-level controls must still be configured by its operator.

## USDA boundary

USDA search sends only the typed food query and allowed data types to the configured private gateway. Food detail sends an FDC id. Photos, journal records, targets, and Health Connect data are not included. Results are restricted to Foundation and SR Legacy and cached locally for up to 30 days. The Android app does not contain or receive the gateway's USDA API key.

## Health Connect boundary

The app requests Nutrition and Hydration read/write permissions only through the Health Connect permission screen. It reads the last 30 days only after the user requests import review and shows a selection dialog before saving anything locally. A write occurs after manual confirmation or for a newly confirmed log while the user has enabled Auto Write. Auto Write does not run a background service or retroactively upload existing logs.

## User control

Users can edit or delete individual journal records, export journal/quick-food/settings/suggestion data as JSON, delete all local app data, revoke Health Connect permissions in Android, or uninstall the app. An exported JSON file is outside the app sandbox and should be protected by the user.
