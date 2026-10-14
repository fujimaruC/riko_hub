# Project structure

This selective map follows the current working tree. New course and assignment source is present but unverified. Generated build outputs, caches and local credentials are intentionally omitted.

```text
riko_hub/
├── app/                         Android :app module
│   ├── build.gradle.kts          SDK, dependencies, signing boundaries
│   ├── schemas/                  exported Room schemas
│   └── src/
│       ├── main/java/id/riko/hub/
│       │   ├── MainActivity.kt, AppContainer.kt
│       │   ├── navigation/       destinations and spatial navigation
│       │   ├── ui/               Compose screens, ViewModels, theme, widget
│       │   ├── domain/           models and pure planning rules
│       │   ├── data/             Room, repositories, backup, prefs, remotes
│       │   └── work/             reminder scheduling/workers
│       ├── main/res/             strings, icons and other Android resources
│       ├── test/                 JVM unit test source
│       └── androidTest/          on-device test source
├── companion/
│   ├── pyproject.toml            Python package and rikohub entry point
│   ├── riko_companion/           CLI, dashboard, server, trust and handoff
│   └── tests/                    Python test source
├── docs/
│   ├── handbook/                contributor orientation (this series)
│   ├── renaissance/             proposed product and architecture plans
│   └── STATUS.md                evidence and pending owner checks
├── gradle/libs.versions.toml     Android dependency catalog
└── settings.gradle.kts           single-module Gradle project
```

| Looking for… | Start here |
|---|---|
| Android UI and navigation | `app/src/main/java/id/riko/hub/ui/` and `navigation/RikoNavHost.kt` |
| Domain rules | `domain/logic/` and `domain/model/` |
| Room entities, queries, migrations | `data/local/entity/`, `data/local/dao/`, `data/local/RikoDatabase.kt`, `data/local/Migrations.kt`, `app/schemas/` |
| Repositories and app wiring | `data/repository/`, `data/remote/`, `AppContainer.kt` |
| Drive Materials Vault | `data/remote/drive/`, `ui/vault/`, [Drive setup](../DRIVE_SETUP.md) |
| Companion networking / handoff | `data/remote/device/`, `ui/sync/`, `companion/riko_companion/{server,state,storage,handoff}.py` |
| Companion CLI and terminal dashboard | `companion/riko_companion/{cli,dashboard,__main__}.py` |
| Unit and device tests | `app/src/test/`, `app/src/androidTest/`, `companion/tests/` |
| Design resources | `ui/theme/`, `app/src/main/res/`, [design direction](07_DESIGN_DIRECTION.md) |
| Project decisions and evidence | [STATUS](../STATUS.md), [Renaissance hub](../renaissance/00_README.md), [protocols](../V04_COMPANION_PROTOCOL.md) |

The `docs/renaissance/` files are plans. When a plan and implementation differ, inspect current source and [STATUS](../STATUS.md) before describing behavior. Test filenames identify intended checks, not their results.

[Architecture](03_ARCHITECTURE.md) · [Getting started](05_GETTING_STARTED.md) · [Contributing](06_CONTRIBUTING.md)
