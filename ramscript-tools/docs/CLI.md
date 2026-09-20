# Canonical CLI

Compile recursively:

```powershell
$sources = Get-ChildItem -Recurse src,tests -Filter *.java; javac -encoding UTF-8 -d out $sources.FullName
```

Discover production commands and presets:

```text
java -cp out Main commands
java -cp out Main presets
```

Dry-run a desired composition before generating files:

```text
java -cp out Main plan-preset <rom> hotkey <preset-id> [preset-id ...]
```

The planner decides HotkeyRuntimeV1 vs SharedHotkeyRuntime and concrete placement automatically. A clean rejection means the requested set conflicts or exceeds a hard resource constraint.

Build production presets:

```text
java -cp out Main build-preset-wc3 <rom> <deliveryman|hotkey> <input.wc3> <output-or-prefix> <preset-id> [preset-id ...] [--seed <hex>]
```

Example validated LG1.0 Shared composition:

```text
java -cp out Main build-preset-wc3 lg10 hotkey ../dummy-saves/custom-design.wc3 ../dummy-saves/box14-repel-party-runbike seed-modifier-box14 repel party-iv-viewer run-bike-anywhere
```

Toolkit Cleaner:

```text
java -cp out Main build-toolkit-cleaner-wc3 <rom> <input.wc3> <output.wc3> [--wipe-flags] [--exclude <FLAG_ID>]...
```

Other production CLI utilities remain intentionally outside `PresetCatalog`:

```text
java -cp out Main inspect <input.wc3>
java -cp out Main inspect-bin <input.bin>
java -cp out Main extract-bin <input.wc3> <output.bin>
java -cp out Main inject-bin <input.wc3> <input.bin> <output.wc3>

java -cp out Main build-preset-object-wc3 <rom> <input.wc3> <output-prefix> <object-target-id> <preset-id> [preset-id ...] [--seed <hex>]

java -cp out Main build-gift-bin <aurora-ticket|mystic-ticket|rare-candy-test> <output.bin>
java -cp out Main build-gift-wc3 <aurora-ticket|mystic-ticket|rare-candy-test> <input.wc3> <output.wc3>
java -cp out Main build-item-gift-bin <output.bin> <item> <amount> <flag> [text options]
java -cp out Main build-item-gift-wc3 <input.wc3> <output.wc3> <item> <amount> <flag> [text options]
java -cp out Main build-repeatable-item-gift-bin <output.bin> <item> <amount> [text options]
java -cp out Main build-repeatable-item-gift-wc3 <input.wc3> <output.wc3> <item> <amount> [text options]
java -cp out Main build-clear-flag-bin <output.bin> <flag> [--message text]
java -cp out Main build-clear-flag-wc3 <input.wc3> <output.wc3> <flag> [--message text]
```

`preset-metadata`, `preset-validation`, `plan-preset`, and `toolkit-cleaner-metadata` are human-readable inspection/planning commands. `Main commands` remains the canonical discovery surface. These commands are production-supported even though several delegate internally to the older implementation class; `Main` is the public facade and historical/research-only entrypoints remain behind `Main legacy`.

Historical/research commands are intentionally hidden from the production facade:

```text
java -cp out Main legacy <old-command> [args ...]
```

`inject` is historical. If needed for the old save-injection workflow, use `Main legacy inject ...`; production import/export commands exposed by `Main commands` are `extract-bin` / `inject-bin`.

## Machine-facing desktop integration API

Human CLI output is not a stable UI contract. Desktop clients use the versioned
JSON boundary documented in `INTEGRATION_API.md`:

```powershell
java -cp out Main api version
java -cp out Main api list-content --rom lg10 --context hotkey-composition
java -cp out Main api plan-composition --rom lg10 --preset party-iv-viewer --preset repel
java -cp out Main api build-composition --rom lg10 --input ../dummy-saves/custom-design.wc3 --output ../dummy-saves/ui-pack --preset party-iv-viewer --preset repel
```

Do not parse human-readable `plan-preset`/`presets` output in the UI.


## Cross-version hotkey catalog

The same 11 current hotkey-composition preset IDs are now catalog-supported for `fr10`, `lg10`, `fr11`, and `lg11`, including `seed-modifier-box14`, `run-anywhere`, and `run-bike-anywhere`. Example syntax is unchanged; replace the ROM id as needed. These newly enabled non-LG1.0 ports are build-supported, not GAME-VALIDATED.
