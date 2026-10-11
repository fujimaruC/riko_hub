# v0.3 Materials Vault setup

The app implements native Google Drive authorization and REST operations. Live authorization and transfers still require the owner's Google Cloud configuration and device acceptance. No Google account or private content was accessed during implementation.

## Google Cloud configuration

1. Create/select an owner-controlled Google Cloud project. Enable **Google Drive API** and **Google Picker API**.
2. In Google Auth Platform, configure Branding, Audience and Data Access. Use an internal audience only if the organization permits it; otherwise use External/Testing and add the owner's Google account as a test user. Complete the required support/developer contact information. Workspace administrators may restrict access.
3. Declare `https://www.googleapis.com/auth/drive.readonly` and `https://www.googleapis.com/auth/drive.file`. Read-only access is a **restricted** scope; public distribution can require Google verification and applicable restricted-scope reviews. Testing-mode grants may expire and require reauthorization. This implementation is not a claim of public-release approval.
4. Create an OAuth client of type **Android** for package `id.riko.hub.debug` and the SHA-1 certificate fingerprint of the existing debug signing key. For a future signed release, create a separate Android client for `id.riko.hub` and its actual signing certificate (Play App Signing certificate if distributed through Play).
5. Obtain SHA-1 locally with `JAVA_HOME=/opt/android-studio/jbr rtk gradlew :app:signingReport --console plain`. Run this yourself; signing configuration/credentials were not inspected in this mission. Keep the signing identity used by the installed app. Do not generate a replacement key just to connect Drive.
6. Install the built debug APK over the existing matching debug app, preserving data. The device needs Google Play services and network access; Android 9 is supported by the pinned authorization SDK's minimum API level.

No web client ID, client secret, Firebase project, `google-services.json`, backend or redirect URL needs to be embedded for this native AuthorizationClient flow. Android package/signing registration is the owner-supplied configuration. Never put passwords, client secrets or tokens in the repository.

## Scopes and account behavior

- **Browse/open:** `drive.readonly` lists, searches and reads the user's existing Drive materials without modifying them. Global name search is visibly distinguished from the current-folder listing. The account label comes from Drive `about.user`, not an assumed identity from another sign-in flow.
- **Upload:** a separate Android Google Picker authorization requests only `drive.file`, opting out of previously granted scopes. The owner explicitly chooses a Drive folder and later an Android document. The returned folder and account are checked before upload. The app does not request full `drive` write access. If the folder is not writable, upload fails with a permission message.
- The native Picker may show files as well as folders depending on service behavior; non-folder/ambiguous results are rejected. Grant cancellation does not count as a successful connection or upload.
- OAuth access tokens stay in memory. Expiry/revocation produces a reconnect action. Disconnect clears local state/cache and requests grant revocation; if that remote request fails offline, remove the grant later from the Google Account's third-party connections page.
- No Room tasks, events, checklists, database files or local backups are automatically sent to Drive. Upload starts only after explicit SAF selection and destination confirmation. The picker offers PDF, Office/OpenDocument, text/CSV/Markdown, image and EPUB formats. A filename/URI policy rejects database extensions and recognizable Riko Hub backup JSON names before opening the source stream; this is not content inspection of renamed files.

## Device checks

1. Open Materials Vault while disconnected. Cancel account consent, retry, then connect the configured test account. Verify the displayed Drive account, empty/loading states and useful errors for missing configuration/offline access.
2. Browse root and nested folders, return using breadcrumbs/back, refresh, and load more than one page. Search for names containing apostrophes/backslashes and confirm search is global.
3. Choose a writable destination in Google Picker, select a disposable PDF/document in Android's document picker, inspect progress, and verify success refreshes the destination. Cancel a transfer, deny folder permission and test offline failure. Cancellation can leave a completed remote file if Drive already accepted its final bytes; check Drive before retrying after an ambiguous failure.
4. Open Google Docs/Sheets/Slides through their authenticated Google viewing links. Open a small ordinary PDF/file through a locally installed viewer. Verify missing-viewer and permission errors. No access token is included in external URLs.
5. Disconnect and reconnect; test grant revocation, token expiry and a different account. Verify no old account listing/download is shown after switching. Confirm normal local tasks/reminders/backups still work; no task sync exists.

## Limits

Live Google authorization, Picker folder grants, real transfer behavior and Android 9 runtime acceptance remain unverified until these checks. Transfers are foreground, ViewModel-scoped operations, not a persistent background upload service; they are not resumed across process death. Upload failure can require selecting the document again. Cancellation during a blocking source read/in-flight request is observed when the read returns or the request times out. Ordinary file opening uses a bounded private temporary cache (32 MiB per file, 64 MiB total); larger files use the Google viewing-link fallback when available. Google Workspace files open online; there is no Workspace export or general offline library in this slice. A listing/search is capped at 500 accumulated items with visible guidance to narrow the search or navigate folders. Shared-drive special cases and shortcuts are not promised. Account/token state is not persisted across process death; reconnect through the button and existing Google grants. The owner mission's browsing/search/upload/open flow takes precedence over the earlier roadmap's additional folder mappings/offline-mark features.

## Primary references

- [Android authorization and account separation](https://developer.android.com/identity/authorization)
- [Google Drive scope definitions](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)
- [Native Android Google Picker and folder-selection parameters](https://developers.google.com/workspace/drive/picker/guides/desktop-mobile-picker)
- [Android OAuth package/signing certificate registration](https://developers.google.com/android/guides/client-auth)
- [Drive resumable upload protocol](https://developers.google.com/workspace/drive/api/guides/manage-uploads)
