# Getting started

This is a source checkout for Android and an optional **Linux** Companion. The instructions describe repository-supported paths, not a verified installation on your machine. Riko performs builds, tests, lint and device acceptance.

## Requirements and checkout

Clone the repository through the project’s Git remote, then open the checkout in Android Studio for the Android app. The Gradle project has one `:app` module. Its build configuration declares compile/target SDK **37**, minimum SDK **26** (Android 8.0), JVM target **17**, and version metadata **0.4.0 / 400**. The historical project setup used Android Studio’s bundled JDK and SDK build tools 36.0.0 on the owner’s machine; use the versions required by the checked-in Gradle configuration and your local Android tooling. Do not copy machine-specific absolute paths from old examples.

Set the Android SDK location in ignored root `local.properties` (`sdk.dir=...`) or use `ANDROID_HOME`. Keep signing material in ignored `keystore.properties` only if you are responsible for a release build. No release key or password belongs in a PR. The debug application ID is `id.riko.hub.debug`; release is `id.riko.hub`.

The owner’s build and verification procedures live in [STATUS](../STATUS.md) and existing project docs. This documentation does not claim those checks ran for the current working tree. Existing test source is under `app/src/test/` and `app/src/androidTest/`.

## Linux Companion

The Companion package requires Python **3.11+** and is documented for Linux, with an [Arch Linux user-install path](../../companion/README.md). Use a project virtual environment; `companion/pyproject.toml` supplies the `rikohub` CLI and its `cryptography` and `zeroconf` dependencies. From the checkout:

```sh
cd companion
python3 -m venv .venv
.venv/bin/python -m pip install -e .
.venv/bin/rikohub --help
```

The command can also be invoked as `.venv/bin/rikohub` without adding a global command or changing shell startup files. `rikohub` opens the terminal dashboard; `rikohub status`, `devices` and `doctor` inspect local state. `rikohub start --bind ACTUAL_PRIVATE_IPV4` starts the foreground server after you supply and confirm the laptop’s actual private LAN address. The module entry point `.venv/bin/python -m riko_companion --bind ACTUAL_PRIVATE_IPV4` is also documented. Pairing approval happens in the running server console. See the [Companion setup guide](../V04_LAPTOP_COMPANION_SETUP.md) before using a real phone or transferring disposable files.

LAN handoff needs a dedicated empty transfer directory and a separate per-phone grant; it is source-level v0.5 work awaiting owner verification. Remote networking is unavailable in the current source. The CLI is not a background service.

## Optional Google Drive

The planner does not need a Google account. Vault development and live QA require owner-configured Google Cloud/OAuth settings for the correct application ID and signing certificate. Follow [Drive setup](../DRIVE_SETUP.md), but check the current authorization source before configuring scopes: `GoogleDriveAuthorization.kt` declares both `drive.readonly` and `drive.file`, while the older setup guide discusses only `drive.file`. Never add credentials, tokens or local account details to repository Markdown. Drive consent and provider behavior remain pending live verification.

## If something does not work

| Symptom | First place to check |
|---|---|
| Android SDK or JDK resolution | `local.properties`, `ANDROID_HOME`, installed SDK platform and checked-in Gradle config |
| Course/assignment schema mismatch | `RikoDatabase.kt`, `Migrations.kt`, `app/schemas/` (currently exports only v1/v2), and the pending v0.6 owner gate in [STATUS](../STATUS.md) |
| Vault authorization fails | Package/signing certificate OAuth registration and [Drive setup](../DRIVE_SETUP.md) |
| Laptop is not found | Private LAN address, same-network discovery, selected bind, and [Companion setup](../V04_LAPTOP_COMPANION_SETUP.md) |
| Pairing fails | Recheck the directly obtained one-time payload, laptop approval and pinned identity; do not bypass TLS checks |
| Handoff stops | Reopen Sync, inspect peer grant and transfer status, then follow the [v2 recovery contract](../V05_HANDOFF_PROTOCOL.md) |

The current source and historical documents may describe different phases. Start with [STATUS](../STATUS.md), then inspect the relevant code and protocol. Report the device, OS, command or UI action, redacted error and exact source revision when asking for help.

[Start here](00_START_HERE.md) · [Structure](04_PROJECT_STRUCTURE.md) · [Contributing](06_CONTRIBUTING.md)
