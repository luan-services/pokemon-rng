import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }

            String command = args[0].toLowerCase();

            switch (command) {
                case "inspect" -> requireArgs(args, 2, () ->
                        inspect(RamScript.fromWc3(Path.of(args[1]))));

                case "inspect-bin" -> requireArgs(args, 2, () ->
                        inspect(RamScript.fromBinary(Path.of(args[1]))));

                /* Import / export: these commands keep the WC3 design and RamScript concepts separate. */
                case "extract-bin" -> requireArgs(args, 3, () ->
                        extractBinary(Path.of(args[1]), Path.of(args[2])));

                case "inject-bin" -> requireArgs(args, 4, () ->
                        injectBinary(Path.of(args[1]), Path.of(args[2]), Path.of(args[3])));

                case "commands" -> requireArgs(args, 1, Main::printBuilderCatalog);
                case "presets" -> requireArgs(args, 1, Main::printPresets);

                case "build-preset-bin" -> buildPresetBin(args);
                case "build-preset-wc3" -> buildPresetWc3(args);

                case "build-item-gift-bin" -> buildItemGiftBin(args);
                case "build-item-gift-wc3" -> buildItemGiftWc3(args);

                case "build-repeatable-item-gift-bin" -> buildRepeatableItemGiftBin(args);
                case "build-repeatable-item-gift-wc3" -> buildRepeatableItemGiftWc3(args);

                case "build-clear-flag-bin" -> buildClearFlagBin(args);
                case "build-clear-flag-wc3" -> buildClearFlagWc3(args);

                case "build-trigger-test-bin" -> buildTriggerTestBin(args);
                case "build-trigger-test-wc3" -> buildTriggerTestWc3(args);
                case "build-compact-installer-c1-bin" -> buildCompactInstallerC1Bin(args);
                case "build-compact-installer-c1-wc3" -> buildCompactInstallerC1Wc3(args);
                case "build-compact-installer-c2-bin" -> buildCompactInstallerC2Bin(args);
                case "build-compact-installer-c2-wc3" -> buildCompactInstallerC2Wc3(args);
                case "build-compact-installer-c2a-bin" -> buildCompactInstallerC2aBin(args);
                case "build-compact-installer-c2a-wc3" -> buildCompactInstallerC2aWc3(args);
                case "build-compact-runtime-c3-bin" -> buildCompactRuntimeC3Bin(args);
                case "build-compact-runtime-c3-wc3" -> buildCompactRuntimeC3Wc3(args);
                case "build-compact-runtime-c4-bin" -> buildCompactRuntimeC4Bin(args);
                case "build-compact-runtime-c4-wc3" -> buildCompactRuntimeC4Wc3(args);
                case "build-compact-runtime-c5a-bin" -> buildCompactRuntimeC5aBin(args);
                case "build-compact-runtime-c5a-wc3" -> buildCompactRuntimeC5aWc3(args);

                /* Compatibility aliases kept from the experimental v5 CLI. */
                case "build-aurora" -> requireArgs(args, 3, () ->
                        buildIntoWc3(
                                OfficialGiftScripts.buildAuroraTicket(),
                                Path.of(args[1]),
                                Path.of(args[2])
                        ));

                case "build-mystic" -> requireArgs(args, 3, () ->
                        buildIntoWc3(
                                OfficialGiftScripts.buildMysticTicket(),
                                Path.of(args[1]),
                                Path.of(args[2])
                        ));

                case "build-aurora-bin" -> requireArgs(args, 2, () ->
                        buildBinary(OfficialGiftScripts.buildAuroraTicket(), Path.of(args[1])));

                case "build-mystic-bin" -> requireArgs(args, 2, () ->
                        buildBinary(OfficialGiftScripts.buildMysticTicket(), Path.of(args[1])));

                case "build-custom-test" -> requireArgs(args, 3, () ->
                        buildIntoWc3(
                                CustomGiftScripts.buildRareCandyTest(),
                                Path.of(args[1]),
                                Path.of(args[2])
                        ));

                case "build-custom-test-bin" -> requireArgs(args, 2, () ->
                        buildBinary(CustomGiftScripts.buildRareCandyTest(), Path.of(args[1])));

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

    private static void extractBinary(Path inputWc3, Path outputBin) throws Exception {
        RamScript ramScript = RamScript.fromWc3(inputWc3);
        ramScript.writeBinary(outputBin);

        System.out.println("RamScript extracted successfully.");
        System.out.println("Input WC3: " + inputWc3.toAbsolutePath());
        System.out.println("Output BIN: " + outputBin.toAbsolutePath());
        printValidation(ramScript);
    }

    private static void injectBinary(Path inputWc3, Path inputBin, Path outputWc3) throws Exception {
        RamScript ramScript = RamScript.fromBinary(inputBin);

        /* Intentionally permissive for research/custom payloads.
           An unusual header/checksum is reported, not forbidden. */
        printWarnings(ramScript);

        ramScript.replaceInWc3(inputWc3, outputWc3);

        System.out.println("RamScript imported into WC3 successfully.");
        System.out.println("Input WC3: " + inputWc3.toAbsolutePath());
        System.out.println("Input BIN: " + inputBin.toAbsolutePath());
        System.out.println("Output WC3: " + outputWc3.toAbsolutePath());
        printValidation(ramScript);
    }

    private static void buildPresetBin(String[] args) throws Exception {
        requireArgs(args, 3, () -> {
            RamScript script = preset(args[1]);
            buildBinary(script, Path.of(args[2]));
        });
    }

    private static void buildPresetWc3(String[] args) throws Exception {
        requireArgs(args, 4, () -> {
            RamScript script = preset(args[1]);
            buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        });
    }

    private static RamScript preset(String name) {
        return switch (name.toLowerCase()) {
            case "aurora-ticket", "aurora" -> OfficialGiftScripts.buildAuroraTicket();
            case "mystic-ticket", "mystic" -> OfficialGiftScripts.buildMysticTicket();
            case "rare-candy-test", "rare-candy" -> CustomGiftScripts.buildRareCandyTest();
            default -> throw new IllegalArgumentException(
                    "Unknown preset: " + name + ". Run `presets` to list available presets."
            );
        };
    }








    private static void buildCompactRuntimeC5aBin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: build-compact-runtime-c5a-bin <rom> <output.bin>");
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactRuntimeCandidate5a.build(rom);
        buildBinary(script, Path.of(args[2]));
        printCompactRuntimeC5a(rom);
    }

    private static void buildCompactRuntimeC5aWc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: build-compact-runtime-c5a-wc3 <rom> <input.wc3> <output.wc3>");
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactRuntimeCandidate5a.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printCompactRuntimeC5a(rom);
    }

    private static void printCompactRuntimeC5a(RomProfile rom) {
        int total = CompactRuntimeCandidate5a.scriptSize(rom);
        int payload = CompactRuntimeCandidate5a.payloadSize();
        System.out.println();
        System.out.println("Compact Runtime Candidate 5a:");
        System.out.println("  ROM:               " + rom.displayName());
        System.out.println("  status:            RELEASE CANDIDATE - C4 layout + generic payload fix");
        System.out.println("  trigger:           R + SELECT");
        System.out.println("  payload:           \"Hello from the Wonder Card!\"");
        System.out.println("  bootstrap bytes:   " + CompactRuntimeCandidate5a.bootstrapBytes(rom).length);
        System.out.println("  table bytes:       " + CompactRuntimeCandidate5a.TABLE_SIZE);
        System.out.println("  native blob bytes: " + CompactRuntimeCandidate5a.NATIVE_BLOB_SIZE);
        System.out.println("  field installer:   " + CompactRuntimeCandidate5a.fieldInstallerSize(rom));
        System.out.println("  payload bytes:     " + payload);
        System.out.println("  runtime overhead:  " + (total - payload));
        System.out.println("  total script:      " + total + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + (RamScript.SCRIPT_SIZE - total));
        System.out.println();
        System.out.println("Hello build is byte-identical to validated C4.");
        System.out.println("Difference: arbitrary payload lengths now generate the correct bootstrap target.");
    }

    private static void buildCompactRuntimeC4Bin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-runtime-c4-bin <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactRuntimeCandidate4.build(rom);
        buildBinary(script, Path.of(args[2]));
        printCompactRuntimeC4(rom);
    }

    private static void buildCompactRuntimeC4Wc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-runtime-c4-wc3 <rom> <input.wc3> <output.wc3>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactRuntimeCandidate4.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printCompactRuntimeC4(rom);
    }

    private static void printCompactRuntimeC4(RomProfile rom) {
        int total = CompactRuntimeCandidate4.scriptSize(rom);
        int payload = CompactRuntimeCandidate4.payloadSize();

        System.out.println();
        System.out.println("Compact Runtime Candidate 4:");
        System.out.println("  ROM:               " + rom.displayName());
        System.out.println("  status:            EXPERIMENTAL - sAddressOffset bootstrap");
        System.out.println("  trigger:           R + SELECT");
        System.out.println("  payload:           \"Hello from the Wonder Card!\"");
        System.out.printf("  sAddressOffset:    0x%08X%n",
                CompactRuntimeCandidate4.S_ADDRESS_OFFSET);
        System.out.println("  bootstrap bytes:   "
                + CompactRuntimeCandidate4.bootstrapBytes(rom).length);
        System.out.println("  native blob bytes: "
                + CompactRuntimeCandidate4.NATIVE_BLOB_SIZE);
        System.out.println("  field installer:   "
                + CompactRuntimeCandidate4.fieldInstallerSize(rom));
        System.out.println("  payload bytes:     " + payload);
        System.out.println("  runtime overhead:  " + (total - payload));
        System.out.println("  total script:      " + total + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + (RamScript.SCRIPT_SIZE - total));
        System.out.println();
        System.out.println("C3 resident runtime is otherwise unchanged.");
        System.out.println("Expected after installation:");
        System.out.println("  callback1 = 03005311");
        System.out.println("  VBlank    = 03003F43");
        System.out.println("  R+SELECT  = Hello from the Wonder Card!");
    }

    private static void buildCompactRuntimeC3Bin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-runtime-c3-bin <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactRuntimeCandidate3.build(rom);
        buildBinary(script, Path.of(args[2]));
        printCompactRuntimeC3(rom);
    }

    private static void buildCompactRuntimeC3Wc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-runtime-c3-wc3 <rom> <input.wc3> <output.wc3>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactRuntimeCandidate3.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printCompactRuntimeC3(rom);
    }

    private static void printCompactRuntimeC3(RomProfile rom) {
        System.out.println();
        System.out.println("Compact Runtime Candidate 3:");
        System.out.println("  ROM:               " + rom.displayName());
        System.out.println("  status:            EXPERIMENTAL - compact full runtime + active hotkey");
        System.out.println("  payload:           Hello from the Wonder Card!");
        System.out.printf("  signature:         0x%04X at script+0x%02X%n",
                CompactRuntimeCandidate3.FORMAT_SIGNATURE,
                CompactRuntimeCandidate3.SIGNATURE_OFFSET);
        System.out.printf("  payload offset:    0x%02X%n", CompactRuntimeCandidate3.PAYLOAD_OFFSET);
        System.out.println("  payload bytes:     " + CompactRuntimeCandidate3.payloadSize());
        System.out.printf("  native blob offset:0x%02X%n", CompactRuntimeCandidate3.nativeBlobOffset());
        System.out.println("  alignment padding: " + CompactRuntimeCandidate3.alignmentPadding());
        System.out.println("  native blob bytes: " + CompactRuntimeCandidate3.NATIVE_BLOB_SIZE);
        System.out.println("  bootstrap bytes:   " + CompactRuntimeCandidate3.bootstrapBytes(rom).length);
        System.out.println("  field installer:   " + CompactRuntimeCandidate3.fieldInstallerSize(rom));
        System.out.println("  runtime overhead:  " + CompactRuntimeCandidate3.runtimeOverhead(rom));
        System.out.println("  total script:      " + CompactRuntimeCandidate3.scriptSize(rom)
                + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + CompactRuntimeCandidate3.freeBytes(rom));
        System.out.println("  VBlank hook:       active after compact resident copy");
        System.out.println();
        System.out.println("Expected after deliveryman:");
        System.out.println("  callback1 -> 03005311 after supervisor rearms it");
        System.out.println("  VBlank    -> 03003F43");
        System.out.println("  R+SELECT opens the Hello message");
    }

    private static void buildCompactInstallerC2aBin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-installer-c2a-bin <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactInstallerCandidate2a.build(rom);
        buildBinary(script, Path.of(args[2]));
        printCompactInstallerC2a(rom);
    }

    private static void buildCompactInstallerC2aWc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-installer-c2a-wc3 <rom> <input.wc3> <output.wc3>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactInstallerCandidate2a.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printCompactInstallerC2a(rom);
    }

    private static void printCompactInstallerC2a(RomProfile rom) {
        System.out.println();
        System.out.println("Compact installer Candidate 2a:");
        System.out.println("  ROM:               " + rom.displayName());
        System.out.println("  status:            EXPERIMENTAL - aligned full resident copy, hooks disabled");
        System.out.printf("  native blob offset:0x%02X%n", CompactInstallerCandidate2a.NATIVE_BLOB_OFFSET);
        System.out.println("  alignment padding: " + CompactInstallerCandidate2a.ALIGNMENT_PADDING + " bytes");
        System.out.println("  bootstrap bytes:   " + CompactInstallerCandidate2a.bootstrapBytes(rom).length);
        System.out.println("  native blob bytes: " + CompactInstallerCandidate2a.NATIVE_BLOB_SIZE);
        System.out.println("  resident bytes:    " + RuntimeV1ResidentBlocks.totalResidentBytes(rom));
        System.out.println("  field installer:   " + CompactInstallerCandidate2a.fieldInstallerSize(rom));
        System.out.println("  total script:      " + CompactInstallerCandidate2a.scriptSize(rom)
                + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + CompactInstallerCandidate2a.freeBytes(rom));
        System.out.println("  hooks installed:   NONE");
        System.out.println();
        System.out.println("C2 was rejected: native blob at script+0x0A was 2 mod 4 and broke ADR targets.");
        System.out.println("C2a aligns the native blob at script+0x0C.");
        System.out.println("R+SELECT remains intentionally inactive.");
    }

    private static void buildCompactInstallerC2Bin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-installer-c2-bin <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactInstallerCandidate2.build(rom);
        buildBinary(script, Path.of(args[2]));
        printCompactInstallerC2(rom);
    }

    private static void buildCompactInstallerC2Wc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-installer-c2-wc3 <rom> <input.wc3> <output.wc3>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactInstallerCandidate2.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printCompactInstallerC2(rom);
    }

    private static void printCompactInstallerC2(RomProfile rom) {
        System.out.println();
        System.out.println("Compact installer Candidate 2:");
        System.out.println("  ROM:               " + rom.displayName());
        System.out.println("  status:            EXPERIMENTAL - full resident copy, hooks disabled");
        System.out.printf("  bootstrap staging: 0x%08X%n", CompactInstallerCandidate2.BOOTSTRAP_ADDRESS);
        System.out.println("  bootstrap bytes:   " + CompactInstallerCandidate2.bootstrapBytes(rom).length);
        System.out.println("  native blob bytes: " + CompactInstallerCandidate2.NATIVE_BLOB_SIZE);
        System.out.println("  resident bytes:    " + RuntimeV1ResidentBlocks.totalResidentBytes(rom));
        System.out.println("  field installer:   " + CompactInstallerCandidate2.fieldInstallerSize(rom));
        System.out.println("  total script:      " + CompactInstallerCandidate2.scriptSize(rom)
                + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + CompactInstallerCandidate2.freeBytes(rom));
        System.out.println("  hooks installed:   NONE");
        System.out.println();
        System.out.println("Expected after deliveryman:");
        System.out.println("  all Runtime v1 resident blocks are present in RAM");
        System.out.println("  callback1 and VBlank remain unchanged");
        System.out.println("  R+SELECT remains intentionally inactive");
    }

    private static void buildCompactInstallerC1Bin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-installer-c1-bin <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactInstallerCandidate1.build(rom);
        buildBinary(script, Path.of(args[2]));
        printCompactInstallerC1(rom);
    }

    private static void buildCompactInstallerC1Wc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-compact-installer-c1-wc3 <rom> <input.wc3> <output.wc3>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = CompactInstallerCandidate1.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printCompactInstallerC1(rom);
    }

    private static void printCompactInstallerC1(RomProfile rom) {
        int total = CompactInstallerCandidate1.scriptSize(rom);
        int installer = CompactInstallerCandidate1.installerSize(rom);

        System.out.println();
        System.out.println("Compact installer Candidate 1:");
        System.out.println("  ROM:              " + rom.displayName());
        System.out.println("  status:           EXPERIMENTAL - one-block copier only");
        System.out.printf("  copier staging:   0x%08X%n", CompactInstallerCandidate1.COPIER_ADDRESS);
        System.out.printf("  destination:      0x%08X%n", CompactInstallerCandidate1.DESTINATION);
        System.out.println("  copied bytes:     " + CompactInstallerCandidate1.BLOB_SIZE);
        System.out.println("  copier bytes:     " + CompactInstallerCandidate1.copierBytes(rom).length);
        System.out.println("  installer bytes:  " + installer);
        System.out.println("  total script:     " + total + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:       " + (RamScript.SCRIPT_SIZE - total));
        System.out.println("  hooks installed:  NONE");
        System.out.println();
        System.out.println("Expected RAM at 03003F42..03003F4F:");
        System.out.println("18 A3 07 CB 03 68 8B 42 B3 D1 02 60 B1 E7");
        System.out.println("R+SELECT is intentionally inactive in this candidate.");
    }

    private static void buildTriggerTestBin(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-trigger-test-bin <deliveryman|hotkey> <rom> <output.bin>"
            );
        }
        EventTrigger trigger = EventTrigger.fromId(args[1]);
        RomProfile rom = RomProfile.fromId(args[2]);
        TriggerBuildResult result = TriggerComposer.compose(
                trigger, rom, TriggerTestPayloads.helloWonderCard()
        );
        buildBinary(result.ramScript(), Path.of(args[3]));
        printTriggerBuild(result);
    }

    private static void buildTriggerTestWc3(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: build-trigger-test-wc3 <deliveryman|hotkey> <rom> <input.wc3> <output.wc3>"
            );
        }
        EventTrigger trigger = EventTrigger.fromId(args[1]);
        RomProfile rom = RomProfile.fromId(args[2]);
        TriggerBuildResult result = TriggerComposer.compose(
                trigger, rom, TriggerTestPayloads.helloWonderCard()
        );
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        printTriggerBuild(result);
    }

    private static void printTriggerBuild(TriggerBuildResult result) {
        System.out.println();
        System.out.println("Trigger composition:");
        System.out.println("  trigger:          " + result.trigger());
        System.out.println("  ROM:              " + result.rom().displayName());
        System.out.println("  validation:       " + result.rom().validationStatus().label());
        System.out.println("  payload bytes:    " + result.payloadBytes());
        System.out.println("  runtime overhead: " + result.runtimeOverheadBytes());
        System.out.println("  total script:     " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:       " + result.freeScriptBytes());
    }

    private static void buildRepeatableItemGiftBin(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "Usage: build-repeatable-item-gift-bin <output.bin> <item> <amount> [--intro text] [--success text] [--bag-full text]"
            );
        }

        Path output = Path.of(args[1]);
        RepeatableItemGiftPreset config = parseRepeatableItemGift(args, 2);
        buildBinary(config.build(), output);
    }

    private static void buildRepeatableItemGiftWc3(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "Usage: build-repeatable-item-gift-wc3 <input.wc3> <output.wc3> <item> <amount> [text options]"
            );
        }

        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);
        RepeatableItemGiftPreset config = parseRepeatableItemGift(args, 3);
        buildIntoWc3(config.build(), input, output);
    }

    private static RepeatableItemGiftPreset parseRepeatableItemGift(String[] args, int start) {
        int item = parseNumber(args[start]);
        int amount = parseNumber(args[start + 1]);

        if (amount <= 0 || amount > 0xFFFF) {
            throw new IllegalArgumentException("amount must be between 1 and 65535");
        }

        RepeatableItemGiftPreset defaults = RepeatableItemGiftPreset.defaults(item, amount);
        Map<String, String> options = parseRepeatableOptions(args, start + 2);

        return new RepeatableItemGiftPreset(
                item,
                amount,
                options.getOrDefault("intro", defaults.introText()),
                options.getOrDefault("success", defaults.successText()),
                options.getOrDefault("bag-full", defaults.bagFullText())
        );
    }

    private static Map<String, String> parseRepeatableOptions(String[] args, int start) {
        Map<String, String> result = new LinkedHashMap<>();

        if (((args.length - start) & 1) != 0) {
            throw new IllegalArgumentException("Optional values must be provided as --name value pairs");
        }

        for (int i = start; i < args.length; i += 2) {
            String option = args[i];
            if (!option.startsWith("--")) {
                throw new IllegalArgumentException("Expected --option, got: " + option);
            }

            String name = option.substring(2).toLowerCase();
            if (!List.of("intro", "success", "bag-full").contains(name)) {
                throw new IllegalArgumentException("Unknown repeatable item-gift option: --" + name);
            }

            result.put(name, args[i + 1]);
        }

        return result;
    }

    private static void buildClearFlagBin(String[] args) throws Exception {
        if (args.length != 3 && args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: build-clear-flag-bin <output.bin> <flag> [--message text]"
            );
        }

        Path output = Path.of(args[1]);
        int flag = parseNumber(args[2]);
        String message = parseOptionalMessage(args, 3);

        RamScript script = SimpleGiftScripts.buildClearFlag(
                0x08010000L,
                flag,
                message
        );

        buildBinary(script, output);
    }

    private static void buildClearFlagWc3(String[] args) throws Exception {
        if (args.length != 4 && args.length != 6) {
            throw new IllegalArgumentException(
                    "Usage: build-clear-flag-wc3 <input.wc3> <output.wc3> <flag> [--message text]"
            );
        }

        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);
        int flag = parseNumber(args[3]);
        String message = parseOptionalMessage(args, 4);

        RamScript script = SimpleGiftScripts.buildClearFlag(
                0x08010000L,
                flag,
                message
        );

        buildIntoWc3(script, input, output);
    }

    private static String parseOptionalMessage(String[] args, int start) {
        if (args.length == start) {
            return "";
        }

        if (args.length != start + 2 || !args[start].equalsIgnoreCase("--message")) {
            throw new IllegalArgumentException("Expected optional --message text");
        }

        return args[start + 1];
    }

    private static void buildItemGiftBin(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "Usage: build-item-gift-bin <output.bin> <item> <amount> <flag> [--intro text] [--success text] [--already text] [--bag-full text]"
            );
        }

        Path output = Path.of(args[1]);
        ItemGiftPreset config = parseItemGift(args, 2);
        buildBinary(config.build(), output);
    }

    private static void buildItemGiftWc3(String[] args) throws Exception {
        if (args.length < 6) {
            throw new IllegalArgumentException(
                    "Usage: build-item-gift-wc3 <input.wc3> <output.wc3> <item> <amount> <flag> [text options]"
            );
        }

        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);
        ItemGiftPreset config = parseItemGift(args, 3);
        buildIntoWc3(config.build(), input, output);
    }

    private static ItemGiftPreset parseItemGift(String[] args, int start) {
        int item = parseNumber(args[start]);
        int amount = parseNumber(args[start + 1]);
        int flag = parseNumber(args[start + 2]);

        if (amount <= 0 || amount > 0xFFFF) {
            throw new IllegalArgumentException("amount must be between 1 and 65535");
        }

        ItemGiftPreset defaults = ItemGiftPreset.defaults(item, amount, flag);
        Map<String, String> options = parseOptions(args, start + 3);

        return new ItemGiftPreset(
                item,
                amount,
                flag,
                options.getOrDefault("intro", defaults.introText()),
                options.getOrDefault("success", defaults.successText()),
                options.getOrDefault("already", defaults.alreadyReceivedText()),
                options.getOrDefault("bag-full", defaults.bagFullText())
        );
    }

    private static Map<String, String> parseOptions(String[] args, int start) {
        Map<String, String> result = new LinkedHashMap<>();

        if (((args.length - start) & 1) != 0) {
            throw new IllegalArgumentException("Optional values must be provided as --name value pairs");
        }

        for (int i = start; i < args.length; i += 2) {
            String option = args[i];
            if (!option.startsWith("--")) {
                throw new IllegalArgumentException("Expected --option, got: " + option);
            }
            result.put(option.substring(2).toLowerCase(), args[i + 1]);
        }

        for (String name : result.keySet()) {
            if (!List.of("intro", "success", "already", "bag-full").contains(name)) {
                throw new IllegalArgumentException("Unknown item-gift option: --" + name);
            }
        }

        return result;
    }

    private static int parseNumber(String value) {
        try {
            long parsed = (value.startsWith("0x") || value.startsWith("0X"))
                    ? Long.parseLong(value.substring(2), 16)
                    : Long.parseLong(value);

            if (parsed < 0 || parsed > 0xFFFF) {
                throw new IllegalArgumentException("Value must fit in u16: " + value);
            }

            return (int) parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number: " + value);
        }
    }

    private static void buildIntoWc3(RamScript ramScript, Path inputWc3, Path outputWc3) throws Exception {
        ramScript.replaceInWc3(inputWc3, outputWc3);
        System.out.println("RamScript built and inserted successfully.");
        System.out.println("Input WC3: " + inputWc3.toAbsolutePath());
        System.out.println("Output WC3: " + outputWc3.toAbsolutePath());
        printValidation(ramScript);
    }

    private static void buildBinary(RamScript ramScript, Path output) throws Exception {
        ramScript.writeBinary(output);
        System.out.println("RamScript binary written successfully.");
        System.out.println("Output: " + output.toAbsolutePath());
        printValidation(ramScript);
    }

    private static void inspect(RamScript ramScript) {
        System.out.println("=== RamScript ===");
        System.out.printf("Stored checksum:     0x%08X%n", ramScript.storedChecksum());
        System.out.printf("Calculated checksum: 0x%04X%n", ramScript.calculatedChecksum());
        System.out.println("Checksum valid:      " + ramScript.isChecksumValid());
        System.out.println();

        System.out.printf("Magic:               %d (0x%02X)%n", ramScript.magic(), ramScript.magic());
        System.out.printf("Map group:           %d (0x%02X)%n", ramScript.mapGroup(), ramScript.mapGroup());
        System.out.printf("Map number:          %d (0x%02X)%n", ramScript.mapNum(), ramScript.mapNum());
        System.out.printf("Object ID:           %d (0x%02X)%n", ramScript.objectId(), ramScript.objectId());
        System.out.println("Wonder Card header:  " + ramScript.hasWonderCardHeader());
        System.out.printf("Final padding byte:  0x%02X%n", ramScript.paddingByte());
        printWarnings(ramScript);
        System.out.println();

        byte[] script = ramScript.scriptCopy();
        ScriptDisassembler disassembler = new ScriptDisassembler(script);
        List<ScriptInstruction> instructions = disassembler.disassemble();

        System.out.println("=== Field Script Disassembly ===");
        for (ScriptInstruction instruction : instructions) {
            String raw = bytesToHex(instruction.rawBytes());
            String text = instruction.name();
            if (!instruction.operands().isBlank()) {
                text += " " + instruction.operands();
            }

            System.out.printf(
                    "%04X  %-30s %-48s",
                    instruction.offset(),
                    raw,
                    text
            );

            if (!instruction.annotation().isBlank()) {
                System.out.print(" ; " + instruction.annotation());
            }

            System.out.println();
        }

        if (disassembler.virtualBase() != null) {
            System.out.printf("%nVirtual base: 0x%08X%n", disassembler.virtualBase());
        }

        System.out.println();
        System.out.println(
                "Note: only known control-flow paths are disassembled. "
                        + "Embedded text/data is intentionally not treated as bytecode."
        );
    }

    private static void printValidation(RamScript ramScript) {
        System.out.printf("RamScript checksum: 0x%04X%n", ramScript.calculatedChecksum());
        System.out.println("Checksum valid:     " + ramScript.isChecksumValid());
        System.out.println("WC header:          " + ramScript.hasWonderCardHeader());
        printWarnings(ramScript);
    }

    private static void printWarnings(RamScript ramScript) {
        boolean any = false;

        if (!ramScript.isChecksumValid()) {
            if (!any) System.out.println("Warnings:");
            System.out.println("  - RamScript checksum does not match.");
            any = true;
        }

        if (!ramScript.hasWonderCardHeader()) {
            if (!any) System.out.println("Warnings:");
            System.out.println("  - RamScript does not use the standard Wonder Card header 33 FF FF FF.");
        }
    }

    private static void printBuilderCatalog() {
        String currentCategory = null;

        for (BuilderCatalog.Entry entry : BuilderCatalog.entries()) {
            if (!entry.category().equals(currentCategory)) {
                if (currentCategory != null) System.out.println();
                currentCategory = entry.category();
                System.out.println("[" + currentCategory + "]");
            }

            System.out.printf("  %-22s %s%n", entry.name(), entry.description());
        }
    }

    private static void printPresets() {
        System.out.println("Available presets:");
        System.out.println("  aurora-ticket     Official Aurora Ticket reference reconstruction");
        System.out.println("  mystic-ticket     Official Mystic Ticket reference reconstruction");
        System.out.println("  rare-candy-test   Custom one-time Rare Candy test event");
        System.out.println();
        System.out.println(
                "Parametric presets:"
        );
        System.out.println(
                "  build-item-gift-*             one-time item gift using a receipt flag"
        );
        System.out.println(
                "  build-repeatable-item-gift-*  repeatable item gift with no checkFlag/setFlag"
        );
        System.out.println(
                "  build-clear-flag-*             utility event that clears one normal script flag"
        );
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i != 0) result.append(' ');
            result.append(String.format("%02X", Byte.toUnsignedInt(data[i])));
        }
        return result.toString();
    }

    private static void requireArgs(String[] args, int expected, ThrowingRunnable action) throws Exception {
        if (args.length != expected) {
            printUsage();
            throw new IllegalArgumentException(
                    "Expected " + (expected - 1) + " argument(s) after command " + args[0]
            );
        }
        action.run();
    }

    private static void printUsage() {
        System.out.println("ramscript-tools");
        System.out.println();
        System.out.println("Inspect:");
        System.out.println("  java -cp out Main inspect input.wc3");
        System.out.println("  java -cp out Main inspect-bin input.bin");
        System.out.println();
        System.out.println("Import / export:");
        System.out.println("  java -cp out Main extract-bin input.wc3 output.bin");
        System.out.println("  java -cp out Main inject-bin input.wc3 input.bin output.wc3");
        System.out.println();
        System.out.println("Discover building blocks:");
        System.out.println("  java -cp out Main commands");
        System.out.println("  java -cp out Main presets");
        System.out.println();
        System.out.println("Build named presets:");
        System.out.println("  java -cp out Main build-preset-bin aurora-ticket output.bin");
        System.out.println("  java -cp out Main build-preset-wc3 aurora-ticket input.wc3 output.wc3");
        System.out.println();
        System.out.println("Build a one-time item gift:");
        System.out.println("  java -cp out Main build-item-gift-bin output.bin 0x44 1 0x2AA");
        System.out.println("  java -cp out Main build-item-gift-wc3 input.wc3 output.wc3 0x44 1 0x2AA");
        System.out.println();
        System.out.println("Build a repeatable item gift (no receipt flag used):");
        System.out.println("  java -cp out Main build-repeatable-item-gift-bin output.bin 0x44 1");
        System.out.println("  java -cp out Main build-repeatable-item-gift-wc3 input.wc3 output.wc3 0x44 1");
        System.out.println();
        System.out.println("Build a flag-clearing utility event:");
        System.out.println("  java -cp out Main build-clear-flag-bin output.bin 0x2AA");
        System.out.println("  java -cp out Main build-clear-flag-wc3 input.wc3 output.wc3 0x2AA --message \"Flag cleared.\"");
        System.out.println();
        System.out.println("Optional item-gift text overrides:");
        System.out.println("  one-time:   --intro \"...\" --success \"...\" --already \"...\" --bag-full \"...\"");
        System.out.println("  repeatable: --intro \"...\" --success \"...\" --bag-full \"...\"");
        System.out.println();
        System.out.println();
        System.out.println("Trigger composition test:");
        System.out.println("  java -cp out Main build-trigger-test-bin hotkey fr10 output.bin");
        System.out.println("  java -cp out Main build-trigger-test-wc3 hotkey lg10 input.wc3 output.wc3");
        System.out.println("  trigger: deliveryman | hotkey");
        System.out.println("  ROM: fr10 | lg10 | fr11 | lg11");
        System.out.println();
        System.out.println("Legacy v5 build-* commands remain accepted for compatibility.");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
