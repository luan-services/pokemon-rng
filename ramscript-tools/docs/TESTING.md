# Testing

Compile recursively:

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory out | Out-Null
$sources = Get-ChildItem src,tests -Recurse -Filter *.java
javac -encoding UTF-8 -d out $sources.FullName
```

Run both gates:

```powershell
java -cp out ProductionTestRunner
java -cp out TestRunner
```

## Current expected result

`ProductionTestRunner` must pass. It covers the promoted object-event binding, Trade Evolution production preset, Custom Trainer Runtime V2/Compact Transport, fixed Gym Leader authoring, and related production invariants.

`TestRunner` is a historical all-in-one regression suite. It currently stops at the known unrelated assertion:

```text
Party-IV-only AUTO plan should preserve standalone HotkeyRuntimeV1 when cheaper
```

This is an existing planner/test contract mismatch, not a Gym Leader/Custom Trainer regression. Do not hide it and do not claim the historical suite is fully green.

## Emulator validation policy

Use precise labels:

- `emulator-tested` / `validated in game`: that exact behavior/preset was exercised successfully.
- `Java/build-tested`: byte/layout/build assertions pass, but that exact preset was not played in emulator.
- profile-supported: addresses/build logic exist for the ROM profile; this does not imply gameplay validation.

For the current Gym Leader V1 status, see `GYM_LEADER_REMATCH_PRESETS.md`.

## Remaining cleaner regression

The cleaner code already exists. The remaining manual integration check is:

1. install/use toolkit content;
2. run Cleaner;
3. save normally and restart;
4. reinstall Custom Trainer Runtime V2;
5. install a trainer preset;
6. confirm reserved storage and completion flags behave as fresh state.
