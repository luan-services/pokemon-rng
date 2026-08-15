import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }

            switch (args[0].toLowerCase()) {
                case "build-seed-hotkey" -> buildSeedHotkey(args);
                case "build-dispatcher-candidate-1" -> buildDispatcherCandidate1(args);
                case "build-dispatcher-candidate-2" -> buildDispatcherCandidate2(args);
                case "build-dispatcher-candidate-3" -> buildDispatcherCandidate3(args);
                case "build-dispatcher-candidate-4" -> buildDispatcherCandidate4(args);
                case "build-dispatcher-candidate-5" -> buildDispatcherCandidate5(args);
                case "build-dispatcher-candidate-5a" -> buildDispatcherCandidate5a(args);
                case "verify" -> verify(args);
                case "profiles" -> printProfiles();
                case "effects" -> printEffects();
                case "layout" -> printLayout(args);
                case "help", "--help", "-h" -> printUsage();
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }





    private static void buildDispatcherCandidate5a(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-dispatcher-candidate-5a <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Path output = Path.of(args[2]);

        RamScript ramScript = NormalContextHotkeyCandidate5a.build(rom);
        ramScript.write(output);

        System.out.println("Dispatcher Candidate 5a built successfully.");
        System.out.println("Status:           DIAGNOSTIC - C5 regression isolation");
        System.out.println("ROM:              " + rom.displayName());
        System.out.println("Trigger:          hold R, then press SELECT");
        System.out.println("Action:           PlaySE(SE_SELECT) via C5-style call/return trampoline");
        System.out.printf("PlaySE Thumb:     0x%08X%n", NormalContextHotkeyCandidate5a.playSeThumb());
        System.out.printf("SE_SELECT:        0x%04X%n", NormalContextHotkeyCandidate5a.seSelect());
        System.out.printf("Trampoline:       0x%08X%n", NormalContextHotkeyCandidate5a.trampolineAddress());
        System.out.printf("Function literal: 0x%08X%n", NormalContextHotkeyCandidate5a.functionLiteralAddress());
        System.out.printf("Callback wrapper: 0x%08X%n", NormalContextHotkeyCandidate5a.hotkeyDetectorThumb());
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("IMPORTANT: Candidate 5a deliberately keeps the C5 trampoline at 03003F80.");
        System.out.println("It is diagnostic only. Do NOT test cable/wireless/link modes.");
    }

    private static void buildDispatcherCandidate5(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-dispatcher-candidate-5 <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Path output = Path.of(args[2]);

        RamScript ramScript = NormalContextHotkeyCandidate5.build(rom);
        ramScript.write(output);

        System.out.println("Dispatcher Candidate 5 built successfully.");
        System.out.println("Status:           EXPERIMENTAL - returned RamScript pointer test");
        System.out.println("ROM:              " + rom.displayName());
        System.out.println("Trigger:          hold R, then press SELECT");
        System.out.println("Action:           GetSavedRamScriptIfValid()");
        System.out.printf("Function Thumb:   0x%08X%n", NormalContextHotkeyCandidate5.getSavedRamScriptThumb());
        System.out.printf("Result slot:      0x%08X%n", NormalContextHotkeyCandidate5.resultSlotAddress());
        System.out.printf("gSaveBlock1Ptr:   0x%08X%n", NormalContextHotkeyCandidate5.gSaveBlock1PtrAddress());
        System.out.println("Expected result:  *gSaveBlock1Ptr + 0x3624");
        System.out.printf("Callback wrapper: 0x%08X%n", NormalContextHotkeyCandidate5.hotkeyDetectorThumb());
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("Do NOT test cable/wireless/link modes with Candidate 5.");
    }

    private static void buildDispatcherCandidate4(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-dispatcher-candidate-4 <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Path output = Path.of(args[2]);
        RamScript ramScript = NormalContextHotkeyCandidate4.build(rom);
        ramScript.write(output);

        System.out.println("Dispatcher Candidate 4 built successfully.");
        System.out.println("Status:           EXPERIMENTAL - first external ROM function call");
        System.out.println("ROM:              " + rom.displayName());
        System.out.println("Supervisor:       Candidate-3 auto-rearm design (unchanged)");
        System.out.println("Hotkey:           hold R, then press SELECT");
        System.out.println("Action:           PlaySE(SE_SELECT)");
        System.out.printf("PlaySE Thumb:     0x%08X%n", NormalContextHotkeyCandidate4.playSeThumb());
        System.out.printf("SE_SELECT:        0x%04X%n", NormalContextHotkeyCandidate4.soundEffect());
        System.out.printf("VBlank slot:      0x%08X -> 0x%08X%n", rom.vblankSlot, NormalContextHotkeyCandidate4.supervisorThumb(rom));
        System.out.printf("Callback1 slot:   0x%08X%n", NormalContextHotkeyCandidate4.callback1Address());
        System.out.printf("Callback wrapper: 0x%08X%n", NormalContextHotkeyCandidate4.hotkeyDetectorThumb());
        System.out.printf("Action literal:   0x%08X -> 0x%08X%n", NormalContextHotkeyCandidate4.actionLiteralAddress(), NormalContextHotkeyCandidate4.playSeThumb());
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("Trigger-frame behavior: PlaySE is tail-called; CB1_Overworld is skipped for that one frame.");
        System.out.println("Do NOT test cable/wireless/link modes with Candidate 4.");
    }

    private static void buildDispatcherCandidate3(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-dispatcher-candidate-3 <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Path output = Path.of(args[2]);
        RamScript ramScript = NormalContextHotkeyCandidate3.build(rom);
        ramScript.write(output);

        System.out.println("Dispatcher Candidate 3 built successfully.");
        System.out.println("Status:           EXPERIMENTAL - hotkey + auto-rearm integration test");
        System.out.println("ROM:              " + rom.displayName());
        System.out.println("Supervisor:       VBlank, re-arms only when callback1 == CB1_Overworld");
        System.out.println("Hotkey:           hold R, then press SELECT (normal callback context)");
        System.out.printf("VBlank slot:      0x%08X -> 0x%08X%n", rom.vblankSlot, NormalContextHotkeyCandidate3.supervisorThumb(rom));
        System.out.printf("Callback1 slot:   0x%08X%n", NormalContextHotkeyCandidate3.callback1Address());
        System.out.printf("Callback wrapper: 0x%08X%n", NormalContextHotkeyCandidate3.hotkeyDetectorThumb());
        System.out.printf("Detector storage: 0x%08X (gLinkTestBGInfo, offline test only)%n", NormalContextHotkeyCandidate3.hotkeyDetectorAddress());
        System.out.printf("Marker:           [0x%08X] <- 0x%02X on trigger%n", NormalContextHotkeyCandidate3.markerAddress(), NormalContextHotkeyCandidate3.markerValue());
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("Do NOT test cable/wireless/link modes with Candidate 3.");
    }

    private static void buildDispatcherCandidate2(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-dispatcher-candidate-2 <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Path output = Path.of(args[2]);
        RamScript ramScript = NormalContextHotkeyCandidate2.build(rom);
        ramScript.write(output);

        System.out.println("Dispatcher Candidate 2d built successfully.");
        System.out.println("Status:           EXPERIMENTAL - auto-rearm test only");
        System.out.println("ROM:              " + rom.displayName());
        System.out.println("Supervisor:       VBlank, re-arms only when callback1 == CB1_Overworld");
        System.out.println("Hotkey:           intentionally disabled in Candidate 2d");
        System.out.printf("VBlank slot:      0x%08X -> 0x%08X%n", rom.vblankSlot, NormalContextHotkeyCandidate2.supervisorThumb(rom));
        System.out.printf("Callback1 slot:   0x%08X%n", NormalContextHotkeyCandidate2.callback1Address());
        System.out.printf("Re-arm gate CB1:  0x%08X%n", NormalContextHotkeyCandidate2.cb1OverworldThumb());
        System.out.printf("Callback wrapper: 0x%08X%n", NormalContextHotkeyCandidate2.callbackWrapperThumb(rom));
        System.out.printf("Original CB1:     0x%08X%n", NormalContextHotkeyCandidate2.cb1OverworldThumb());
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("Expected: callback1 may change during transitions/battles; when the game restores CB1_Overworld, a later VBlank re-arms 03003F95.");
    }

    private static void buildDispatcherCandidate1(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-dispatcher-candidate-1 <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Path output = Path.of(args[2]);
        RamScript ramScript = NormalContextHotkeyCandidate1.build(rom);
        ramScript.write(output);

        System.out.println("Dispatcher Candidate 1 built successfully.");
        System.out.println("Status:           EXPERIMENTAL - does not replace the known-good seed modifier");
        System.out.println("ROM:              " + rom.displayName());
        System.out.println("Execution:        gMain.callback1 / normal main-loop context");
        System.out.println("Hotkey:           hold R, then press SELECT");
        System.out.printf("Callback1 slot:   0x%08X%n", NormalContextHotkeyCandidate1.callback1Address());
        System.out.printf("Runtime wrapper:  0x%08X%n", rom.mainHook | 1L);
        System.out.printf("Original CB1:     0x%08X%n", NormalContextHotkeyCandidate1.originalCallbackThumb());
        System.out.printf("Debug write:      0x%08X <- 0x12345678%n", NormalContextHotkeyCandidate1.debugAddress());
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("Expected Candidate-1 limitation: map/menu transitions may overwrite callback1 and disable the hotkey.");
        System.out.println("That is acceptable for this test. A crash is NOT acceptable.");
    }

    private static void buildSeedHotkey(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main build-seed-hotkey <rom> <seed> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        Path output = Path.of(args[3]);

        SeedHotkeyEffect effect = new SeedHotkeyEffect(rom, seed);
        RamScript ramScript = effect.build();
        ramScript.write(output);

        System.out.println("Fixed-RAM seed hotkey built successfully.");
        System.out.println("Effect:           " + effect.id());
        System.out.println("ROM:              " + rom.displayName());
        System.out.printf("Initial seed:     0x%04X (%d)%n", seed, seed);
        System.out.println("Hotkey:           " + effect.hotkey());
        System.out.println();
        System.out.printf("VBlank slot:      0x%08X%n", rom.vblankSlot);
        System.out.printf("Main hook:        0x%08X%n", rom.mainHook);
        System.out.printf("RNG extension:    0x%08X%n", rom.rngExtension);
        System.out.printf("Tail stub:        0x%08X%n", rom.tailStub);
        System.out.printf("gRngValue:        0x%08X%n", rom.rngValue);
        System.out.printf("Predecessor:      0x%08X%n", effect.predecessor());
        System.out.printf("First state:      0x%08X%n", RngMath.nextState(effect.predecessor()));
        System.out.println();
        System.out.printf("Checksum:         0x%04X%n", ramScript.storedChecksum());
        System.out.println("Checksum valid:   " + ramScript.isChecksumValid());
        System.out.println("Output:           " + output.toAbsolutePath());
        System.out.println();
        System.out.println("Lifetime: RAM-only. Lost on reset/power-off.");
    }

    private static void verify(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main verify <ramscript.bin>"
            );
        }

        Path input = Path.of(args[1]);
        RamScript ramScript = RamScript.read(input);

        System.out.printf("Stored checksum:     0x%04X%n", ramScript.storedChecksum());
        System.out.printf("Calculated checksum: 0x%04X%n", ramScript.calculatedChecksum());
        System.out.println("Checksum valid:      " + ramScript.isChecksumValid());
    }

    private static void printProfiles() {
        System.out.println("Supported ROM profiles:");

        for (RomProfile profile : RomProfile.values()) {
            System.out.println("  " + profile.id() + " - " + profile.displayName());
        }
    }

    private static void printEffects() {
        System.out.println("Implemented native effects:");

        for (NativeEffectCatalog.Entry entry : NativeEffectCatalog.entries()) {
            System.out.println();
            System.out.println("  " + entry.id() + " - " + entry.name());
            System.out.println("    status:  " + entry.status());
            System.out.println("    ROM:     " + entry.romSupport());
            System.out.println("    trigger: " + entry.trigger());
            System.out.println("    " + entry.description());
        }
    }

    private static void printLayout(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: java -cp out Main layout <rom>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);

        System.out.println("Native runtime layout: " + rom.displayName());
        System.out.printf("VBlank table slot: 0x%08X%n", rom.vblankSlot);
        System.out.printf("Original VBlank:   0x%08X%n", rom.originalVBlankThumb);
        System.out.printf("Input word:        0x%08X%n", rom.heldKeysRaw);
        System.out.printf("gRngValue:         0x%08X%n", rom.rngValue);
        System.out.println();

        for (RuntimeRegion region : NativeRuntimeLayout.regions(rom)) {
            System.out.printf(
                    "%-24s 0x%08X..0x%08X  %3d bytes  %s%n",
                    region.name(),
                    region.start(),
                    region.endInclusive(),
                    region.size(),
                    region.validated() ? "validated runtime region" : "temporary/staging"
            );
            System.out.println("  " + region.purpose());
        }
    }

    private static int parseSeed(String value) {
        try {
            String digits = (value.startsWith("0x") || value.startsWith("0X"))
                    ? value.substring(2)
                    : value;

            int seed = Integer.parseUnsignedInt(digits, 16);

            if (seed > 0xFFFF) {
                throw new IllegalArgumentException(
                        "Initial seed must fit in 16 bits (0000..FFFF)"
                );
            }

            return seed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid hexadecimal seed: " + value);
        }
    }

    private static void printUsage() {
        System.out.println("native-ramscript-tools v1");
        System.out.println();
        System.out.println("Commands:");
        System.out.println(
                "  java -cp out Main build-seed-hotkey fr10 0x1234 output.bin"
        );
        System.out.println(
                "  java -cp out Main build-dispatcher-candidate-1 fr10 output.bin"
        );
        System.out.println(
                "  java -cp out Main build-dispatcher-candidate-2 fr10 output.bin"
        );
        System.out.println(
                "  java -cp out Main build-dispatcher-candidate-3 fr10 output.bin"
        );
        System.out.println(
                "  java -cp out Main build-dispatcher-candidate-4 fr10 output.bin"
        );
        System.out.println(
                "  java -cp out Main build-dispatcher-candidate-5 fr10 output.bin"
        );
        System.out.println(
                "  java -cp out Main build-dispatcher-candidate-5a fr10 output.bin"
        );
        System.out.println(
                "  java -cp out Main verify output.bin"
        );
        System.out.println(
                "  java -cp out Main profiles"
        );
        System.out.println(
                "  java -cp out Main effects"
        );
        System.out.println(
                "  java -cp out Main layout fr10"
        );
    }
}
