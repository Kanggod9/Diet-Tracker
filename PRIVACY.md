# Privacy

Diet Tracker is local-first. The app contains no analytics or advertising SDK, declares `android:allowBackup="false"`, and stores persistent data in app-private storage.

## Stored on the device

- Confirmed journal entries and nutrient provenance.
- Quick foods, selected guidance profile, locally generated suggestions, Health Connect write receipts, and cached USDA reference records.
- The configured gateway URL and an app-to-gateway token. The token is encrypted with a non-exportable Android Keystore key.

OpenAI, USDA, and signing credentials are never stored by the Android app. JSON exports explicitly exclude gateway credentials. Delete all removes the database and encrypted gateway configuration from the device.

## Photo boundary

A photo leaves the device only after the user chooses it and accepts a consent dialog for that specific photo. The app reads the URI into memory with an 8 MB cap, sends it to the configured private HTTPS gateway, and does not copy it into SQLite or app files. The gateway sends it to the OpenAI API with `store: false` and returns a temporary structured draft. Discarding the consent or review screen creates no journal entry.

`store: false` controls OpenAI application-state storage; the gateway operator remains responsible for disclosing deployment logs, subprocessors, abuse monitoring, region, and retention. The included Worker enables no application logging or storage, but platform-level controls must still be configured by its operator.

## USDA boundary

USDA search sends only the typed food query and allowed data types. Food detail sends an FDC id. Photos, journal history, guidance settings, and Health Connect data are not included. Results are restricted to Foundation and SR Legacy and cached locally for up to 30 days.

## Health Connect boundary

The app requests Nutrition and Hydration read/write permissions only through the Health Connect permission screen. It reads the last 30 days only after the user taps Review import, then shows a selection dialog before saving anything locally. It writes only user-selected local records after a second confirmation. There is no background or silent sync.

## User control

Users can edit or delete individual journal records, export journal/quick-food/settings/suggestion data as JSON, delete all local app data, revoke Health Connect permissions in Android, or uninstall the app. An exported JSON file is outside the app sandbox and should be protected by the user.