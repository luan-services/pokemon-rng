# native-ramscript-tools

Java 21 tools for **native Wonder Card effects**: RamScripts whose deliveryman
execution installs small Thumb/ARM runtime payloads into GBA RAM.

This is intentionally separate from normal Field Script composition:

```text
wc3-builder
    card design / metadata

ramscript-tools
    normal Field Script events

native-ramscript-tools
    native Thumb/ARM installers and runtime effects

wc3-injector
    WC3 <-> FR/LG save integration
```

## v1 status

The only native effect currently exposed is:

```text
seed-hotkey
```

for:

```text
FireRed English 1.0
profile: fr10
```

Trigger:

```text
hold R, then press SELECT
```

Effect:

```text
restart the RNG sequence at a chosen 16-bit initial seed
```

The seed modifier is the implementation that survived the project's fixed-RAM
stability testing and manual gameplay/RNG validation.

## Important freeze rule

`FixedRamSeedModifier.java` is the **known-good byte generator**.

The framework classes added around it do not replace or rewrite its machine
code. Any future change that alters the generated payload must be treated as a
new candidate and tested again in-game.

The repository includes a golden output for:

```text
fr10
seed 0x1234
```

Expected file:

```text
size:   1004 bytes (0x3EC)
SHA256: 7a4d45dddb10288af7cd7eef6bdeec2d578c3f516f08e791f1fcf519279cab0d
```

Automated tests regenerate the same seed modifier and require the bytes to be
identical to this known-good fixture.

## Compile

```cmd
javac -encoding UTF-8 -d out src\*.java
```

## List effects

```cmd
java -cp out Main effects
```

## List ROM profiles

```cmd
java -cp out Main profiles
```

## Inspect the fixed-RAM layout

```cmd
java -cp out Main layout fr10
```

This prints the runtime regions used by the known-good implementation.

## Build the seed hotkey

```cmd
java -cp out Main build-seed-hotkey fr10 0x1234 seed-hotkey.bin
```

Example important values:

```text
initial seed     0x1234
hotkey           hold R, then press SELECT
main hook        0x03005310
RNG extension    0x03003F94
tail stub        0x03003EB4
gRngValue        0x03005000
predecessor      0x9A864C25
first state      0x00001234
```

The RamScript BIN is then attached to a Wonder Card with `ramscript-tools`:

```cmd
java -cp out Main inject-bin ...
```

(using the `ramscript-tools` project, not this project's Main class).

The resulting WC3 is injected into a save with `wc3-injector`.

## Runtime architecture

```text
deliveryman
    |
    v
Wonder Card RamScript installer
    |
    +-- writes fixed runtime bytes
    |
    `-- atomically redirects gIntrTable[VBLANK]
                    |
                    v
              fixed main hook
                    |
          +---------+---------+
          |                   |
       no hotkey          R + SELECT
          |                   |
          |             RNG extension
          |                   |
          +---------+---------+
                    |
               tail stub
                    |
                    v
          original VBlankIntr
```

After installation, the runtime no longer reads code from the relocatable
Wonder Card/SaveBlock1 region.

The hook is RAM-only and disappears after reset/power-off.

## Why the predecessor is written

FR/LG's original VBlank path calls `Random()` after the hook tail-chains back.
Therefore the effect writes the previous LCG state. The original `Random()`
advance then lands on the requested initial state.

For `0x1234`:

```text
predecessor  9A864C25
             |
             v Random()
frame 0      00001234
             |
             v
frame 1      4DCBF897
             |
             v
frame 2      E1613ABE
...
```

## Manual validation history

The known-good fixed-RAM architecture was tested through normal gameplay,
including map transitions, stairs, entering/leaving buildings, menus, battles,
healing, NPC interaction, and repeated hotkey use without reproducing the
crashes seen in the older SaveBlock-based candidates.

A practical RNG validation was also performed: after activating the chosen
seed and immediately generating a Pokémon, its PID appeared in the expected
RNG frame list (observed at frame 32 in that test).

A timing detail remains documented rather than "corrected": after normal
overworld flow is active, RAM Watch may first visibly catch the first successor
of `0x00001234` instead of `0x00001234` itself. The generated RNG sequence still
matched the expected sequence in practical hunting tests.

See `docs/TESTING.md`.

## Framework direction

The v1 cleanup introduces metadata around the frozen implementation:

```text
NativeEffect
SeedHotkeyEffect
NativeEffectCatalog
Hotkey
RuntimeRegion
NativeRuntimeLayout
RomProfile
```

This lets future work separate:

```text
trigger
installer
runtime storage
effect body
ROM profile
```

without pretending those pieces are already generic at the machine-code level.

## Research roadmap

The next experimental goal is **not** to enlarge the seed modifier.

It is to research a small fixed dispatcher that can:

```text
hotkey detected
    |
    v
set/queue pending action
    |
    v
return from VBlank quickly
    |
    v
safe main-loop/task context
    |
    v
execute a larger action / Field Script
```

If that becomes reliable, `ramscript-tools` can later support:

```text
Trigger: R + SELECT
Actions:
    Message
    Give Item
    Set/Clear Flag
    Special
    ...
```

while keeping those actions as ordinary Field Scripts rather than executing
large scripts directly inside VBlank.

## Compatibility

Do not reuse the `fr10` addresses for:

```text
FireRed 1.1
LeafGreen 1.0
LeafGreen 1.1
other languages
```

Each build needs its own researched/validated ROM profile and runtime layout.

The current compact Thumb encoding also depends on relationships between the
validated FR10 addresses; adding another profile is not merely filling in
different constants.

## Experimental Dispatcher Candidate 2

Candidate 2 isolates automatic re-arming of a normal-context callback wrapper.
It intentionally removes the hotkey from the experiment.

```cmd
java -cp out Main build-dispatcher-candidate-2 fr10 dispatcher-c2.bin
```

See `docs/DISPATCHER_CANDIDATE_2.md` before testing.


## Experimental Candidate 2b

See `docs/DISPATCHER_CANDIDATE_2B.md`. Candidate 2 is rejected and must not be used.


## Candidate status

- Candidate 2: REJECTED
- Candidate 2b: REJECTED (bad Thumb ADR and branch encodings)
- Candidate 2c: current experimental auto-rearm candidate

See `docs/DISPATCHER_CANDIDATE_2C.md`.


## Current dispatcher experiment

Use only Candidate 2d. Candidates 2, 2b, and 2c are rejected. See `docs/DISPATCHER_CANDIDATE_2D.md`.


## Dispatcher Candidate 4

First external ROM-function proof: persistent R+SELECT dispatcher tail-calls `PlaySE(SE_SELECT)`. See `docs/DISPATCHER_CANDIDATE_4.md`.


## Candidate 5

Current experiment: R+SELECT calls `GetSavedRamScriptIfValid()` and stores the returned current RamScript pointer at `03003FA4`. See `docs/DISPATCHER_CANDIDATE_5.md`.
