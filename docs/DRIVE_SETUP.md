# Private Google Drive setup for v0.3

Vault uses optional Google authorization. Tasks and events do not require an
account and are not uploaded. Live Drive acceptance requires the owner to
configure Google Cloud; compilation cannot prove account access works.

## Owner setup

1. Choose a Google Cloud project. Enable **Google Drive API** and **Google Picker
   API** in that project.
2. Configure Google Auth Platform branding/audience and add the intended Google
   account as a test user while the application is in testing.
3. Create an **Android OAuth client** for `id.riko.hub.debug` using the SHA-1 of
   the debug signing certificate. Create a separate client for `id.riko.hub`
   using the release signing certificate before release acceptance.
4. Use only `https://www.googleapis.com/auth/drive.file`. The picker grants
   access to selected files; this scope does not grant unrestricted Drive
   browsing. Folder selection must not be presented as access to every child.
5. Follow the root README to configure the owner-created, private release
   keystore. Do not commit signing files, passwords, or tokens.

The Android client is identified by package and signing certificate. No OAuth
client secret belongs in the APK. No backend/server authorization flow is
needed for the planned foreground Vault integration.

## Live acceptance checklist

- Connect, cancel consent, retry, and select files with the intended account.
- Verify that the app lists only files it can actually access; pick an existing
  folder and check the limits of its child-file visibility.
- Refresh/search, download/open a file, mark it offline, and reopen it with the
  network disabled. Tasks and events must continue working offline.
- Upload a small owner-selected fixture and verify its contents in Drive.
  Do not overwrite or delete personal Drive files as a test.
- Revoke access externally and check that Vault offers authorization again
  without losing local task data or silently showing another account's cache.
- Check denied access, unavailable Google Play services, interrupted transfers,
  size limits, and cache cleanup. Test release-signed behavior separately.

No live acceptance result is recorded here yet. See STATUS.md for observed
results and remaining blockers.

## Primary references

- [Android authorization](https://developer.android.com/identity/authorization)
- [Google Picker for Android](https://developers.google.com/workspace/drive/picker/guides/desktop-mobile-picker)
- [Drive OAuth scopes](https://developers.google.com/workspace/drive/api/guides/api-specific-auth)
