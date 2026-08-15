native-ramscript-tools v1
=========================

Current status
--------------

This is the cleaned version after the experimental Candidate 1/2/3 work.

Currently supported native effect:

    Fixed-RAM RNG initial-seed hotkey

Currently supported ROM:

    fr10 = Pokemon FireRed English 1.0

The old SaveBlock-relocating candidates are intentionally NOT included.


Architecture
------------

The WC3 RamScript is only an installer.

When the deliveryman executes it once:

    Wonder Card RamScript
        |
        +--> copies fixed runtime hook into tested RAM
        |
        +--> sets gIntrTable[VBLANK] to 03005311
        |
        `--> returns to the normal Mystery Gift script

After installation:

    VBlank
       |
       v
    03005310  fixed 32-byte main hook
       |
       +-- no hotkey --> tail stub --> original VBlankIntr
       |
       `-- R held + SELECT newly pressed
                         |
                         v
                    RNG extension
                         |
                    writes predecessor
                    to gRngValue
                         |
                         v
                    tail stub
                         |
                         v
                  original VBlankIntr
                         |
                         v
                      Random()
                         |
                         v
                  requested initial state

No SaveBlock1 pointer is read during runtime.
No native code is executed from the RamScript after installation.


Validated fixed RAM - FireRed EN 1.0
------------------------------------

Main hook:
    03005310..0300532F
    32 bytes

RNG extension:
    03003F94..03003F9F
    12 bytes

Tail stub:
    03003EB4..03003EB7
    4 bytes

Original VBlank pointer:
    03003EC0..03003EC3
    4 bytes

These regions were stability-tested during normal gameplay before being
used as executable runtime storage.


Compile
-------

Windows CMD:

    javac -encoding UTF-8 -d out src\*.java


Build
-----

Example:

    java -cp out Main build-seed-hotkey fr10 0x1234 seed-hotkey.bin

Expected important output:

    ROM:              FireRed English 1.0
    Initial seed:     0x1234
    Hotkey:           hold R, then press SELECT
    VBlank slot:      0x03003550
    Main hook:        0x03005310
    gRngValue:        0x03005000
    First state:      0x00001234
    Checksum valid:   true


Verify a generated RamScript
----------------------------

    java -cp out Main verify seed-hotkey.bin


WC3 workflow
------------

1. Build the native RamScript .bin with this project.

2. Import that .bin into the desired WC3 using ramscript-custom-builder.

3. Inject the WC3 into a COPY of the FireRed save using wc3-injector.

4. Boot the game.

5. Speak to the Mystery Gift deliveryman once.

6. The hook is now installed for the current game session.

7. Hold R, then press SELECT to restart the RNG sequence.

8. Reset/power-off removes the runtime hook. Speak to the deliveryman again
   after reboot to reinstall it.


Button Mode
-----------

Do not use the Help button mode while testing this payload.

Use a mode where R does not open the FRLG Help System.


What the seed hotkey means
--------------------------

The user-facing "initial seed" is a 16-bit value such as:

    1234

Internally, gRngValue is a 32-bit LCG state.

The hook writes the predecessor state so that the Random() call performed by
the original VBlankIntr advances the RNG to:

    00001234

For seed 1234, the beginning of the sequence is:

    00001234
    4DCBF897
    E1613ABE
    43404759
    FFF1DF58
    C21948EB
    69BE0682
    F670C1CD
    ...

Timing note
-----------

During testing, immediately after receiving/installing the Wonder Card it was
possible to observe 00001234 directly in RAM Watch.

After the game entered normal overworld flow through a map transition, the
first value commonly observable after the hotkey was 4DCBF897, which is
exactly the first advancement after 00001234.

The hook, VBlank pointer, and resident bytes remained intact.

For RNG hunting this should be treated as a timing/advancement observation,
not as evidence that the hotkey failed. Timers/calibration can account for
a consistent advancement offset.

This timing behavior can be instrumented more precisely later if needed.


Tested behavior
---------------

The fixed-RAM implementation was manually exercised through:

    - map transitions
    - Pokemon Center stairs
    - entering/leaving buildings
    - Party menu
    - Bag/menu interactions
    - NPC interactions
    - battles
    - healing
    - repeated R+SELECT activations

The previous SaveBlock-based candidates produced intermittent crashes during
map transitions. This fixed-RAM architecture eliminated those observed crashes
during the test session.


Scope / limitations
-------------------

Only FireRed English 1.0 is currently supported.

Do not copy these addresses to FireRed 1.1, LeafGreen, or another language.
Each ROM build needs its own validated RomProfile.

The fixed-RAM effect is session-only.

This project intentionally contains no Candidate 1/2/3 code and no generic
arbitrary native-code UI yet.


Future checklist
----------------

- Add validated profiles for:
    FireRed EN 1.1
    LeafGreen EN 1.0
    LeafGreen EN 1.1

- Add precise RNG timing instrumentation if useful for timer calibration.

- Consider a small fixed dispatcher + larger RamScript payload architecture
  for future native effects.

- Investigate effect-specific hooks for features such as:
    running shoes on more maps
    repel-expired prompt / reuse flow

- Keep large persistent payload/storage research separate from this v1.
