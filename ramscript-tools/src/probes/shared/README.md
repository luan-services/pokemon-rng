# Archived Shared smoke fixtures

These classes predate the production `PresetCatalog` + `PresetCompositionPlanner` composition path. They are retained because historical regression tests and explicit `Main legacy ...` commands reproduce important staging/runtime milestones.

They are **not production preset APIs** and must not be used as templates for new Shared features:

- `SharedHotkeySmokeTestPreset` — early Shared Field Script composition smoke fixture.
- `SharedHotkeyNativeSmokeTestPreset` — early Shared persistent-native/staging smoke fixture.
- `SharedHotkeyPartyIvSmokeTestPreset` — historical Party IV persistent-native Shared fixture.

New work belongs in `PresetCatalog`, deployment definitions and the normal composition planner/materializer.
