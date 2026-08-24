import java.nio.file.Files;
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
                case "preset-metadata" -> requireArgs(args, 2, () -> System.out.print(PresetCatalog.report(RomProfile.fromId(args[1]))));
                case "plan-presets" -> planPresets(args);
                case "plan-installation" -> planInstallation(args);
                case "build-planned-installation-wc3" -> buildPlannedInstallationWc3(args);

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
                case "build-custom-payload-bin" -> buildCustomPayloadBin(args);
                case "build-custom-payload-wc3" -> buildCustomPayloadWc3(args);
                case "build-show-secret-id-bin" -> buildShowSecretIdBin(args);
                case "build-show-secret-id-wc3" -> buildShowSecretIdWc3(args);
                case "build-show-secret-id-persistent-install-wc3" -> buildShowSecretIdPersistentInstallWc3(args);
                case "build-show-secret-id-persistent-launch-wc3" -> buildShowSecretIdPersistentLaunchWc3(args);
                case "build-seed-modifier-bin" -> buildSeedModifierBin(args);
                case "build-seed-modifier-wc3" -> buildSeedModifierWc3(args);
                case "build-party-iv-viewer-bin" -> buildPartyIvViewerBin(args);
                case "build-party-iv-viewer-wc3" -> buildPartyIvViewerWc3(args);
                case "build-repel-hotkey-bin" -> buildRepelHotkeyBin(args);
                case "build-repel-hotkey-wc3" -> buildRepelHotkeyWc3(args);
                case "build-seed-repel-combo-bin" -> buildSeedRepelComboBin(args);
                case "build-seed-repel-combo-wc3" -> buildSeedRepelComboWc3(args);
                case "build-persistence-probe-install-wc3" -> buildPersistenceProbeInstallWc3(args);
                case "build-persistence-probe-check-wc3" -> buildPersistenceProbeCheckWc3(args);
                case "build-persistence-400-install-wc3" -> buildPersistence400InstallWc3(args);
                case "build-persistence-400-check-wc3" -> buildPersistence400CheckWc3(args);
                case "build-persistence-1024-install-wc3" -> buildPersistence1024InstallWc3(args);
                case "build-persistence-1024-check-wc3" -> buildPersistence1024CheckWc3(args);
                case "build-persistent-storage-v1-install-wc3" -> buildPersistentStorageV1InstallWc3(args);
                case "build-persistent-storage-v1-launch-wc3" -> buildPersistentStorageV1LaunchWc3(args);
                case "build-persistent-storage-v2-install-wc3" -> buildPersistentStorageV2InstallWc3(args);
                case "build-persistent-storage-v2-launch-wc3" -> buildPersistentStorageV2LaunchWc3(args);
                case "build-persistent-storage-v3-install-a-wc3" -> buildPersistentStorageV3InstallAWc3(args);
                case "build-persistent-storage-v3-install-b-wc3" -> buildPersistentStorageV3InstallBWc3(args);
                case "build-persistent-storage-v3-launch-wc3" -> buildPersistentStorageV3LaunchWc3(args);
                case "build-cross-area-modules-install-wc3" -> buildCrossAreaModulesInstallWc3(args);
                case "build-cross-area-modules-launch-wc3" -> buildCrossAreaModulesLaunchWc3(args);
                case "build-real-modules-install-wc3" -> buildRealModulesInstallWc3(args);
                case "build-real-modules-launch-wc3" -> buildRealModulesLaunchWc3(args);
                case "build-persistent-hotkey-install-wc3" -> buildPersistentHotkeyInstallWc3(args);
                case "build-persistent-hotkey-runtime-wc3" -> buildPersistentHotkeyRuntimeWc3(args);
                case "build-direct-persistent-hotkey-install-wc3" -> buildDirectPersistentHotkeyInstallWc3(args);
                case "build-direct-persistent-hotkey-runtime-wc3" -> buildDirectPersistentHotkeyRuntimeWc3(args);
                case "build-deferred-persistent-hotkey-install-wc3" -> buildDeferredPersistentHotkeyInstallWc3(args);
                case "build-deferred-persistent-hotkey-runtime-wc3" -> buildDeferredPersistentHotkeyRuntimeWc3(args);
                case "build-persistent-field-hotkey-install-wc3" -> buildPersistentFieldHotkeyInstallWc3(args);
                case "build-persistent-field-hotkey-runtime-wc3" -> buildPersistentFieldHotkeyRuntimeWc3(args);
                case "build-persistent-gateway-hotkey-install-wc3" -> buildPersistentGatewayHotkeyInstallWc3(args);
                case "build-persistent-gateway-hotkey-runtime-wc3" -> buildPersistentGatewayHotkeyRuntimeWc3(args);
                case "build-shared-hotkey-smoke-install-wc3" -> buildSharedHotkeySmokeInstallWc3(args);
                case "build-shared-hotkey-smoke-runtime-wc3" -> buildSharedHotkeySmokeRuntimeWc3(args);
                case "build-shared-native-smoke-install-a-wc3" -> buildSharedNativeSmokeInstallAWc3(args);
                case "build-shared-native-smoke-install-b-wc3" -> buildSharedNativeSmokeInstallBWc3(args);
                case "build-shared-native-smoke-runtime-wc3" -> buildSharedNativeSmokeRuntimeWc3(args);
                case "build-shared-party-iv-smoke-install-a-wc3" -> buildSharedPartyIvSmokeInstallAWc3(args);
                case "build-shared-party-iv-smoke-install-b-wc3" -> buildSharedPartyIvSmokeInstallBWc3(args);
                case "build-shared-party-iv-smoke-install-c-wc3" -> buildSharedPartyIvSmokeInstallCWc3(args);
                case "build-shared-party-iv-smoke-runtime-wc3" -> buildSharedPartyIvSmokeRuntimeWc3(args);
                case "build-shared-native-install-a-wc3" -> buildSharedNativeInstallAWc3(args);
                case "build-shared-dual-native-smoke-install-a-wc3" -> buildSharedNativeInstallAWc3(args);
                case "build-shared-native-install-b-wc3" -> buildSharedNativeInstallBWc3(args);
                case "build-shared-dual-native-smoke-install-b-wc3" -> buildSharedNativeInstallBWc3(args);
                case "build-shared-native-install-c-wc3" -> buildSharedNativeInstallCWc3(args);
                case "build-shared-dual-native-smoke-install-c-wc3" -> buildSharedNativeInstallCWc3(args);
                case "build-shared-native-runtime-wc3" -> buildSharedNativeRuntimeWc3(args);
                case "build-shared-dual-native-smoke-runtime-wc3" -> buildSharedNativeRuntimeWc3(args);
                case "build-party-iv-staging-check-wc3" -> buildPartyIvStagingCheckWc3(args);
                case "build-party-iv-direct-call-check-wc3" -> buildPartyIvDirectCallCheckWc3(args);

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


    private static void planPresets(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: plan-presets <rom> <preset-id> [preset-id ...]");
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        java.util.List<String> ids = java.util.Arrays.asList(args).subList(2, args.length);
        System.out.print(PresetCompositionPlanner.plan(rom, ids).report());
    }

    private static void planInstallation(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: plan-installation <rom> <preset-id> [preset-id ...]");
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        List<String> ids = java.util.Arrays.asList(args).subList(2, args.length);
        PresetCompositionPlan composition = PresetCompositionPlanner.plan(rom, ids);
        InstallationPlan installation = CompositionInstallationPlanner.plan(composition);
        System.out.print(composition.report());
        System.out.println();
        System.out.print(installation.report());
    }


    private static void buildPlannedInstallationWc3(String[] args) throws Exception {
        if (args.length < 6) {
            throw new IllegalArgumentException("Usage: build-planned-installation-wc3 <rom> <seed-hex> <input.wc3> <output-prefix> <preset-id> [preset-id ...]");
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseUnsignedInt(args[2], 16);
        Path input = Path.of(args[3]);
        String prefix = args[4];
        List<String> ids = java.util.Arrays.asList(args).subList(5, args.length);
        PresetCompositionPlan composition = PresetCompositionPlanner.plan(rom, ids);
        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        InstallationEmitter.EmittedInstallation emitted = InstallationEmitter.emit(plan, seed);

        if (plan.localOnly()) {
            Path output = Path.of(prefix + "-local.wc3");
            buildIntoWc3(emitted.persistentStages().get(0).ramScript(), input, output);
            System.out.println("Generated: " + output);
        } else {
            for (InstallationEmitter.EmittedStage stage : emitted.persistentStages()) {
                Path output = Path.of(prefix + "-" + stage.name() + ".wc3");
                buildIntoWc3(stage.ramScript(), input, output);
                System.out.println("Generated: " + output);
            }
            if (emitted.runtime() != null) {
                Path output = Path.of(prefix + "-runtime.wc3");
                buildIntoWc3(emitted.runtime(), input, output);
                System.out.println("Generated: " + output);
            }
        }
        System.out.println();
        System.out.print(plan.report());
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









    /*
       Composes a raw Field Script payload supplied by the user.

       DELIVERYMAN:
         payload becomes the RamScript directly. No ROM-specific runtime is added.

       HOTKEY:
         payload is embedded byte-for-byte into HotkeyRuntimeV1 and is started
         later with the configured hotkey (default R+SELECT).

       The payload is expected to be a complete executable FR/LG Field Script.
       If it contains relocatable pointers, it should normally begin with
       setvaddress and use the v* control-flow/message commands, exactly like
       scripts emitted by RamScriptBuilder.
    */

    private static void buildSeedModifierBin(String[] args) throws Exception {
        if (args.length != 4 && args.length != 6) {
            throw new IllegalArgumentException(
                    "Usage: build-seed-modifier-bin <rom> <seed-hex> <output.bin> [--hotkey <held>-<pressed>]"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        Hotkey hotkey = parseOptionalHotkey(args, 4);
        TriggerBuildResult result = SeedModifierPreset.build(rom, seed, hotkey);
        buildBinary(result.ramScript(), Path.of(args[3]));
        printSeedModifier(result, seed, hotkey);
    }

    private static void buildSeedModifierWc3(String[] args) throws Exception {
        if (args.length != 5 && args.length != 7) {
            throw new IllegalArgumentException(
                    "Usage: build-seed-modifier-wc3 <rom> <seed-hex> <input.wc3> <output.wc3> [--hotkey <held>-<pressed>]"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        Hotkey hotkey = parseOptionalHotkey(args, 5);
        TriggerBuildResult result = SeedModifierPreset.build(rom, seed, hotkey);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        printSeedModifier(result, seed, hotkey);
    }

    private static Hotkey parseOptionalHotkey(String[] args, int baseLength) {
        if (args.length == baseLength) return Hotkey.DEFAULT;
        if (args.length != baseLength + 2 || !args[baseLength].equalsIgnoreCase("--hotkey")) {
            throw new IllegalArgumentException("Optional hotkey syntax: --hotkey <held>-<pressed>, for example --hotkey r-b");
        }
        return Hotkey.parse(args[baseLength + 1]);
    }

    private static int parseSeed(String value) {
        try {
            String digits = (value.startsWith("0x") || value.startsWith("0X"))
                    ? value.substring(2)
                    : value;
            int seed = Integer.parseUnsignedInt(digits, 16);
            if (seed > 0xFFFF) {
                throw new IllegalArgumentException("Initial seed must fit in u16");
            }
            return seed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid hexadecimal seed: " + value);
        }
    }

    private static void printSeedModifier(TriggerBuildResult result, int seed, Hotkey hotkey) {
        long predecessor = SeedModifierPreset.predecessor(seed);
        System.out.println();
        System.out.println("Seed Modifier preset:");
        System.out.println("  ROM:               " + result.rom().displayName());
        System.out.println("  trigger:           " + hotkey.displayName());
        System.out.println("  hotkey semantics:  hold " + hotkey.heldButton().displayName() + ", press " + hotkey.pressedButton().displayName());
        System.out.println("  prompt:            " + SeedModifierPreset.message(seed));
        System.out.printf("  desired seed:      0x%04X%n", seed);
        System.out.printf("  predecessor:       0x%08X%n", predecessor);
        System.out.printf("  after Random():    0x%08X%n", RngMath.nextState(predecessor));
        System.out.println("  payload bytes:     " + result.payloadBytes());
        System.out.println("  runtime overhead:  " + result.runtimeOverheadBytes());
        System.out.println("  total script:      " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + result.freeScriptBytes());
    }

    private static void buildPartyIvViewerBin(String[] args) throws Exception {
        if (args.length != 3 && args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: build-party-iv-viewer-bin <rom> <output.bin> [--hotkey <held>-<pressed>]"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Hotkey hotkey = parseOptionalHotkey(args, 3);
        TriggerBuildResult result = PartyIvViewerPreset.build(rom, hotkey);
        buildBinary(result.ramScript(), Path.of(args[2]));
        printPartyIvViewer(result, hotkey);
    }

    private static void buildPartyIvViewerWc3(String[] args) throws Exception {
        if (args.length != 4 && args.length != 6) {
            throw new IllegalArgumentException(
                    "Usage: build-party-iv-viewer-wc3 <rom> <input.wc3> <output.wc3> [--hotkey <held>-<pressed>]"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Hotkey hotkey = parseOptionalHotkey(args, 4);
        TriggerBuildResult result = PartyIvViewerPreset.build(rom, hotkey);
        buildIntoWc3(result.ramScript(), Path.of(args[2]), Path.of(args[3]));
        printPartyIvViewer(result, hotkey);
    }

    private static void printPartyIvViewer(TriggerBuildResult result, Hotkey hotkey) {
        System.out.println();
        System.out.println("Party IV Viewer preset:");
        System.out.println("  ROM:               " + result.rom().displayName());
        System.out.println("  trigger:           " + hotkey.displayName());
        System.out.println("  hotkey semantics:  hold " + hotkey.heldButton().displayName() + ", press " + hotkey.pressedButton().displayName());
        System.out.println("  display:           continuous prompted IV pages for the whole party");
        System.out.println("  native installer:  " + PartyIvViewerPreset.selectedInstallerMode(result.rom()) + " (AUTO)");
        System.out.println("  payload bytes:     " + result.payloadBytes());
        System.out.println("  runtime overhead:  " + result.runtimeOverheadBytes());
        System.out.println("  total script:      " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + result.freeScriptBytes());
    }


    private static void buildSeedRepelComboBin(String[] args) throws Exception {
        if (args.length != 4 && args.length != 8) {
            throw new IllegalArgumentException(
                    "Usage: build-seed-repel-combo-bin <rom> <seed-hex> <output.bin> [--seed-hotkey <held>-<pressed> --repel-hotkey <held>-<pressed>]"
            );
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        Hotkey seedHotkey = SeedRepelComboPreset.DEFAULT_SEED_HOTKEY;
        Hotkey repelHotkey = SeedRepelComboPreset.DEFAULT_REPEL_HOTKEY;
        if (args.length == 8) {
            if (!args[4].equalsIgnoreCase("--seed-hotkey") || !args[6].equalsIgnoreCase("--repel-hotkey")) {
                throw new IllegalArgumentException("Expected --seed-hotkey <hotkey> --repel-hotkey <hotkey>");
            }
            seedHotkey = Hotkey.parse(args[5]);
            repelHotkey = Hotkey.parse(args[7]);
        }
        TriggerBuildResult result = SeedRepelComboPreset.build(rom, seed, seedHotkey, repelHotkey);
        buildBinary(result.ramScript(), Path.of(args[3]));
        printSeedRepelCombo(result, seed, seedHotkey, repelHotkey);
    }

    private static void buildSeedRepelComboWc3(String[] args) throws Exception {
        if (args.length != 5 && args.length != 9) {
            throw new IllegalArgumentException(
                    "Usage: build-seed-repel-combo-wc3 <rom> <seed-hex> <input.wc3> <output.wc3> [--seed-hotkey <held>-<pressed> --repel-hotkey <held>-<pressed>]"
            );
        }
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        Hotkey seedHotkey = SeedRepelComboPreset.DEFAULT_SEED_HOTKEY;
        Hotkey repelHotkey = SeedRepelComboPreset.DEFAULT_REPEL_HOTKEY;
        if (args.length == 9) {
            if (!args[5].equalsIgnoreCase("--seed-hotkey") || !args[7].equalsIgnoreCase("--repel-hotkey")) {
                throw new IllegalArgumentException("Expected --seed-hotkey <hotkey> --repel-hotkey <hotkey>");
            }
            seedHotkey = Hotkey.parse(args[6]);
            repelHotkey = Hotkey.parse(args[8]);
        }
        TriggerBuildResult result = SeedRepelComboPreset.build(rom, seed, seedHotkey, repelHotkey);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        printSeedRepelCombo(result, seed, seedHotkey, repelHotkey);
    }

    private static void printSeedRepelCombo(TriggerBuildResult result, int seed, Hotkey seedHotkey, Hotkey repelHotkey) {
        int seedBytes = SeedModifierPreset.buildPayload(result.rom(), seed).length;
        int repelBytes = RepelHotkeyPreset.buildPayload().length;
        System.out.println();
        System.out.println("Seed + Repel multi-hotkey combo:");
        System.out.println("  ROM:               " + result.rom().displayName());
        System.out.println("  seed hotkey:       " + seedHotkey.displayName());
        System.out.println("  repel hotkey:      " + repelHotkey.displayName());
        System.out.printf("  desired seed:      0x%04X%n", seed);
        System.out.println("  seed payload:      " + seedBytes + " bytes @ +0x" + Integer.toHexString(MultiHotkeyRuntimeV1.firstPayloadOffset()).toUpperCase());
        System.out.println("  repel payload:     " + repelBytes + " bytes @ +0x" + Integer.toHexString(MultiHotkeyRuntimeV1.secondPayloadOffset(SeedModifierPreset.buildPayload(result.rom(), seed))).toUpperCase());
        System.out.println("  payload bytes:     " + result.payloadBytes());
        System.out.println("  shared overhead:   " + result.runtimeOverheadBytes());
        System.out.println("  total script:      " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + result.freeScriptBytes());
        System.out.println("  status:            EXPERIMENTAL - requires in-game validation");
    }

    private static void buildRepelHotkeyBin(String[] args) throws Exception {
        if (args.length != 3 && args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: build-repel-hotkey-bin <rom> <output.bin> [--hotkey <held>-<pressed>]"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Hotkey hotkey = parseOptionalHotkey(args, 3);
        TriggerBuildResult result = RepelHotkeyPreset.build(rom, hotkey);
        buildBinary(result.ramScript(), Path.of(args[2]));
        printRepelHotkey(result, hotkey);
    }

    private static void buildRepelHotkeyWc3(String[] args) throws Exception {
        if (args.length != 4 && args.length != 6) {
            throw new IllegalArgumentException(
                    "Usage: build-repel-hotkey-wc3 <rom> <input.wc3> <output.wc3> [--hotkey <held>-<pressed>]"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        Hotkey hotkey = parseOptionalHotkey(args, 4);
        TriggerBuildResult result = RepelHotkeyPreset.build(rom, hotkey);
        buildIntoWc3(result.ramScript(), Path.of(args[2]), Path.of(args[3]));
        printRepelHotkey(result, hotkey);
    }

    private static void printRepelHotkey(TriggerBuildResult result, Hotkey hotkey) {
        System.out.println();
        System.out.println("Repel Hotkey preset:");
        System.out.println("  ROM:               " + result.rom().displayName());
        System.out.println("  trigger:           " + hotkey.displayName());
        System.out.println("  hotkey semantics:  hold " + hotkey.heldButton().displayName() + ", press " + hotkey.pressedButton().displayName());
        System.out.println("  behavior:          Max Repel > Super Repel > Repel; no stacking while active");
        System.out.println("  payload bytes:     " + result.payloadBytes());
        System.out.println("  runtime overhead:  " + result.runtimeOverheadBytes());
        System.out.println("  total script:      " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + result.freeScriptBytes());
    }

    private static void buildShowSecretIdBin(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: build-show-secret-id-bin <rom> <output.bin>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = ShowSecretIdPreset.build(rom);
        buildBinary(script, Path.of(args[2]));
        printShowSecretId(rom, script);
    }

    private static void buildShowSecretIdWc3(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: build-show-secret-id-wc3 <rom> <input.wc3> <output.wc3>"
            );
        }

        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = ShowSecretIdPreset.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        printShowSecretId(rom, script);
    }

    private static void printShowSecretId(RomProfile rom, RamScript script) {
        NativeHelper helper = SecretIdNativeHelper.build(rom);
        int used = ShowSecretIdPreset.payloadSize(rom);

        System.out.println();
        System.out.println("Show Secret ID preset:");
        System.out.println("  ROM:               " + rom.displayName());
        System.out.println("  execution:         deliveryman");
        System.out.println("  hotkey runtime:    none");
        System.out.printf("  helper staging:    0x%08X%n", helper.stagingAddress());
        System.out.println("  native helper:     " + helper.size() + " bytes");
        System.out.println("  total script:      " + used + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  free bytes:        " + (RamScript.SCRIPT_SIZE - used));
        System.out.println("  output:            Your Secret ID is <value>.");
    }

    private static void buildCustomPayloadBin(String[] args) throws Exception {
        if (args.length != 5 && args.length != 7) {
            throw new IllegalArgumentException(
                    "Usage: build-custom-payload-bin <deliveryman|hotkey> <rom> <payload.bin> <output.bin> [--hotkey <held>-<pressed>]"
            );
        }

        EventTrigger trigger = EventTrigger.fromId(args[1]);
        RomProfile rom = RomProfile.fromId(args[2]);
        Path payloadPath = Path.of(args[3]);
        byte[] payload = Files.readAllBytes(payloadPath);
        Hotkey hotkey = parseOptionalHotkey(args, 5);

        TriggerBuildResult result = CustomPayloadComposer.compose(trigger, rom, payload, hotkey);
        buildBinary(result.ramScript(), Path.of(args[4]));

        System.out.println();
        System.out.println("Custom payload composed successfully.");
        System.out.println("Payload BIN:      " + payloadPath.toAbsolutePath());
        printTriggerBuild(result, trigger == EventTrigger.HOTKEY_RUNTIME ? hotkey : null);
    }

    private static void buildCustomPayloadWc3(String[] args) throws Exception {
        if (args.length != 6 && args.length != 8) {
            throw new IllegalArgumentException(
                    "Usage: build-custom-payload-wc3 <deliveryman|hotkey> <rom> <payload.bin> <input.wc3> <output.wc3> [--hotkey <held>-<pressed>]"
            );
        }

        EventTrigger trigger = EventTrigger.fromId(args[1]);
        RomProfile rom = RomProfile.fromId(args[2]);
        Path payloadPath = Path.of(args[3]);
        byte[] payload = Files.readAllBytes(payloadPath);
        Hotkey hotkey = parseOptionalHotkey(args, 6);

        TriggerBuildResult result = CustomPayloadComposer.compose(trigger, rom, payload, hotkey);
        buildIntoWc3(result.ramScript(), Path.of(args[4]), Path.of(args[5]));

        System.out.println();
        System.out.println("Custom payload composed successfully.");
        System.out.println("Payload BIN:      " + payloadPath.toAbsolutePath());
        printTriggerBuild(result, trigger == EventTrigger.HOTKEY_RUNTIME ? hotkey : null);
    }

    private static void buildTriggerTestBin(String[] args) throws Exception {
        if (args.length != 4 && args.length != 6) {
            throw new IllegalArgumentException(
                    "Usage: build-trigger-test-bin <deliveryman|hotkey> <rom> <output.bin> [--hotkey <held>-<pressed>]"
            );
        }
        EventTrigger trigger = EventTrigger.fromId(args[1]);
        RomProfile rom = RomProfile.fromId(args[2]);
        Hotkey hotkey = parseOptionalHotkey(args, 4);
        TriggerBuildResult result = TriggerComposer.compose(
                trigger, rom, TriggerTestPayloads.helloWonderCard(), hotkey
        );
        buildBinary(result.ramScript(), Path.of(args[3]));
        printTriggerBuild(result, trigger == EventTrigger.HOTKEY_RUNTIME ? hotkey : null);
    }

    private static void buildTriggerTestWc3(String[] args) throws Exception {
        if (args.length != 5 && args.length != 7) {
            throw new IllegalArgumentException(
                    "Usage: build-trigger-test-wc3 <deliveryman|hotkey> <rom> <input.wc3> <output.wc3> [--hotkey <held>-<pressed>]"
            );
        }
        EventTrigger trigger = EventTrigger.fromId(args[1]);
        RomProfile rom = RomProfile.fromId(args[2]);
        Hotkey hotkey = parseOptionalHotkey(args, 5);
        TriggerBuildResult result = TriggerComposer.compose(
                trigger, rom, TriggerTestPayloads.helloWonderCard(), hotkey
        );
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        printTriggerBuild(result, trigger == EventTrigger.HOTKEY_RUNTIME ? hotkey : null);
    }

    private static void printTriggerBuild(TriggerBuildResult result) {
        printTriggerBuild(result, result.trigger() == EventTrigger.HOTKEY_RUNTIME ? Hotkey.DEFAULT : null);
    }

    private static void printTriggerBuild(TriggerBuildResult result, Hotkey hotkey) {
        System.out.println();
        System.out.println("Trigger composition:");
        System.out.println("  trigger:          " + result.trigger());
        if (hotkey != null) {
            System.out.println("  hotkey:           " + hotkey.displayName());
            System.out.println("  semantics:        hold " + hotkey.heldButton().displayName() + ", press " + hotkey.pressedButton().displayName());
        }
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
        System.out.println(
                "  build-show-secret-id-*         deliveryman preset that displays the Secret ID"
        );
        System.out.println(
                "  build-seed-modifier-*          configurable-hotkey RNG seed modifier with A confirmation"
        );
        System.out.println(
                "  build-party-iv-viewer-*        configurable-hotkey sequential party IV viewer"
        );
        System.out.println(
                "  build-repel-hotkey-*            configurable-hotkey best-available Repel shortcut"
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



    private static void buildShowSecretIdPersistentInstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-show-secret-id-persistent-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentShowSecretIdPreset.buildInstaller(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Persistent Show Secret ID installer built.");
        System.out.println("Legacy/simple build-show-secret-id-wc3 remains unchanged and is still the default path.");
    }

    private static void buildShowSecretIdPersistentLaunchWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-show-secret-id-persistent-launch-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentShowSecretIdPreset.buildLauncher(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Persistent Show Secret ID launcher built.");
    }

    private static void buildPersistenceProbeInstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistence-probe-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = PersistenceProbePreset.buildInstaller(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        System.out.println("Persistence probe installer built: SaveBlock1 + 0x348C, 8 bytes only.");
    }

    private static void buildPersistenceProbeCheckWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistence-probe-check-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = PersistenceProbePreset.buildChecker(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        System.out.println("Persistence probe checker built: reads SaveBlock1 + 0x348C.");
    }

    private static void buildPersistence400InstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistence-400-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistenceFullRegionProbePreset.buildInstaller(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("400-byte persistence installer built: SaveBlock1 + 0x348C..0x361B.");
    }

    private static void buildPersistence400CheckWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistence-400-check-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistenceFullRegionProbePreset.buildChecker(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("400-byte persistence checker built: verifies all 400 bytes.");
    }

    private static void buildPersistence1024InstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistence-1024-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistenceSaveBlock2ProbePreset.buildInstaller(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("1024-byte persistence installer built: SaveBlock2 + 0xB20..0xF1F.");
    }

    private static void buildPersistence1024CheckWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistence-1024-check-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistenceSaveBlock2ProbePreset.buildChecker(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("1024-byte persistence checker built: verifies all 1024 bytes.");
    }



    private static void buildPersistentStorageV1InstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-storage-v1-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentToolkitStoragePreset.buildInstaller(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("PersistentToolkitStorage V1 installer built: stores executable proof payload in SaveBlock2 + 0xB20.");
    }

    private static void buildPersistentStorageV1LaunchWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-storage-v1-launch-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentToolkitStoragePreset.buildLauncher(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("PersistentToolkitStorage V1 launcher built: validates and executes persistent Thumb payload.");
    }

    private static void buildPersistentStorageV2InstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-storage-v2-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentToolkitStorageV2Preset.buildInstaller(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("PersistentToolkitStorage V2 installer built: module table + two Thumb proof modules.");
    }

    private static void buildPersistentStorageV2LaunchWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-persistent-storage-v2-launch-wc3 <rom> <module-id> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int moduleId = Integer.decode(args[2]);
        buildIntoWc3(PersistentToolkitStorageV2Preset.buildLauncher(rom, moduleId), Path.of(args[3]), Path.of(args[4]));
        System.out.println("PersistentToolkitStorage V2 launcher built for module " + moduleId + ".");
    }

    private static void buildPersistentStorageV3InstallAWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-storage-v3-install-a-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentToolkitStorageV3Preset.buildInstallerA(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("V3 installer A built: initializes storage with module 1.");
    }

    private static void buildPersistentStorageV3InstallBWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-storage-v3-install-b-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentToolkitStorageV3Preset.buildInstallerB(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("V3 installer B built: sparse-adds module 2 without rewriting module 1.");
    }

    private static void buildPersistentStorageV3LaunchWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-persistent-storage-v3-launch-wc3 <rom> <module-id> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int moduleId = Integer.decode(args[2]);
        buildIntoWc3(PersistentToolkitStorageV3Preset.buildLauncher(rom, moduleId), Path.of(args[3]), Path.of(args[4]));
        System.out.println("V3 launcher built for module " + moduleId + ".");
    }

    private static void buildCrossAreaModulesInstallWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-cross-area-modules-install-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentCrossAreaDispatcherPreset.buildInstaller(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 9 cross-area installer built: module 1 in SaveBlock1, module 2 in SaveBlock2.");
    }

    private static void buildCrossAreaModulesLaunchWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-cross-area-modules-launch-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        buildIntoWc3(PersistentCrossAreaDispatcherPreset.buildLauncher(rom), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 9 single dispatcher launcher built: tests both SaveBlock locations in one WC.");
    }

    private static void buildRealModulesInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-real-modules-install-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(PersistentRealPresetDispatcherPreset.buildInstaller(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 10 real-module installer built: SID in SaveBlock1, Seed Modifier core in SaveBlock2.");
    }

    private static void buildRealModulesLaunchWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-real-modules-launch-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(PersistentRealPresetDispatcherPreset.buildLauncher(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 10 single dispatcher launcher built for SID + persistent Seed Modifier core.");
    }

    private static void buildPersistentHotkeyInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-persistent-hotkey-install-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(PersistentHotkeyRuntimePrototype.buildInstaller(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 11 persistent modules + dispatcher installed for hotkey prototype.");
    }

    private static void buildPersistentHotkeyRuntimeWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-persistent-hotkey-runtime-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        TriggerBuildResult result = PersistentHotkeyRuntimePrototype.buildRuntime(rom, seed);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 11 persistent hotkey runtime built.");
        System.out.println("  IWRAM listener path: validated MultiHotkeyRuntimeV1 (unchanged)");
        System.out.println("  RamScript total:     " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  RamScript free:      " + result.freeScriptBytes());
        System.out.println("  hotkeys:             R+B -> SID, R+SELECT -> Seed Modifier");
    }

    private static void buildDeferredPersistentHotkeyInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-deferred-persistent-hotkey-install-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(PersistentDeferredHotkeyRuntime.buildInstaller(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 13 deferred persistent modules installed.");
    }

    private static void buildDeferredPersistentHotkeyRuntimeWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-deferred-persistent-hotkey-runtime-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        TriggerBuildResult result = PersistentDeferredHotkeyRuntime.buildRuntime(rom, seed);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 13 deferred persistent hotkey runtime built.");
        System.out.println("  callback model: validated MultiHotkeyRuntimeV1 deferred SetupScript");
        System.out.println("  new IWRAM claimed: none");
        System.out.println("  deferred resolver: temporary reuse of validated STAGE2 (14 B)");
        System.out.println("  RamScript total: " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  RamScript free:  " + result.freeScriptBytes());
    }

    private static void buildPersistentFieldHotkeyInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-persistent-field-hotkey-install-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        RamScript script = PersistentFieldScriptHotkeyRuntime.buildInstaller(rom, seed);
        buildIntoWc3(script, Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 14a persistent SB1 Field Script installer built.");
        System.out.printf("  Repel: SB1+0x%04X (distance -0x%02X from RamScript)%n", PersistentFieldScriptHotkeyRuntime.REPEL_SB1_OFFSET, PersistentFieldScriptHotkeyRuntime.REPEL_DISTANCE);
        System.out.printf("  Seed:  SB1+0x%04X (distance -0x%02X from RamScript)%n", PersistentFieldScriptHotkeyRuntime.SEED_SB1_OFFSET, PersistentFieldScriptHotkeyRuntime.SEED_DISTANCE);
    }

    private static void buildPersistentFieldHotkeyRuntimeWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-field-hotkey-runtime-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        TriggerBuildResult result = PersistentFieldScriptHotkeyRuntime.buildRuntime(rom);
        buildIntoWc3(result.ramScript(), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 14a deferred persistent SB1 Field Script runtime built.");
        System.out.println("  callback: validated SetupScript model");
        System.out.println("  runtime native dispatcher: none");
        System.out.println("  live IWRAM rewriting: none");
        System.out.printf("  RamScript total: %d / %d%n", result.totalScriptBytes(), RamScript.SCRIPT_SIZE);
        System.out.printf("  RamScript free:  %d%n", result.freeScriptBytes());
        System.out.println("  selected module: validated u8 backward-distance table");
    }

    private static void buildPersistentGatewayHotkeyInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-persistent-gateway-hotkey-install-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        RamScript script = PersistentFieldScriptGatewayRuntime.buildInstaller(rom, seed);
        buildIntoWc3(script, Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 15a SB1 gateway -> SB2 Field Script installer built.");
        System.out.printf("  Repel gateway: SB1+0x%04X -> payload SB2+0x%04X%n", PersistentFieldScriptGatewayRuntime.REPEL_ENTRY_SB1_OFFSET, PersistentFieldScriptGatewayRuntime.REPEL_SB2_OFFSET);
        System.out.printf("  Seed gateway:  SB1+0x%04X -> payload SB2+0x%04X%n", PersistentFieldScriptGatewayRuntime.SEED_ENTRY_SB1_OFFSET, PersistentFieldScriptGatewayRuntime.SEED_SB2_OFFSET);
        System.out.println("  gateway size: 10 bytes each (setvaddress + vgoto)");
    }

    private static void buildPersistentGatewayHotkeyRuntimeWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-persistent-gateway-hotkey-runtime-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        TriggerBuildResult result = PersistentFieldScriptGatewayRuntime.buildRuntime(rom);
        buildIntoWc3(result.ramScript(), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 15a deferred SB1-gateway persistent hotkey runtime built.");
        System.out.println("  callback: validated SetupScript model");
        System.out.println("  IWRAM resolver: none");
        System.out.println("  SB1 gateway: setvaddress + vgoto only");
        System.out.println("  payload storage: validated 1024-byte SB2 region");
        System.out.printf("  RamScript total: %d / %d%n", result.totalScriptBytes(), RamScript.SCRIPT_SIZE);
        System.out.printf("  RamScript free:  %d%n", result.freeScriptBytes());
    }



    private static void buildSharedNativeSmokeInstallAWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-smoke-install-a-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(SharedHotkeyNativeSmokeTestPreset.buildInstallerA(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 20 LAB native smoke installer A built.");
        System.out.print(SharedHotkeyNativeSmokeTestPreset.report(rom, seed));
    }

    private static void buildSharedNativeSmokeInstallBWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-smoke-install-b-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(SharedHotkeyNativeSmokeTestPreset.buildInstallerB(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 20 LAB native smoke installer B built.");
    }

    private static void buildSharedNativeSmokeRuntimeWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-smoke-runtime-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        TriggerBuildResult result = SharedHotkeyNativeSmokeTestPreset.buildRuntime(rom, seed);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 20 LAB shared native runtime built.");
        System.out.printf("RamScript total/free: %d / %d B%n", result.totalScriptBytes(), result.freeScriptBytes());
    }
    private static void buildSharedHotkeySmokeInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-hotkey-smoke-install-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        RamScript script = SharedHotkeySmokeTestPreset.buildInstaller(rom, seed);
        buildIntoWc3(script, Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 19 LAB shared-hotkey smoke installer built.");
        System.out.print(SharedHotkeySmokeTestPreset.placementReport(rom, seed));
        System.out.println("  bindings after runtime install: R+SELECT=Seed, R+B=Repel, R+A=Probe");
    }

    private static void buildSharedHotkeySmokeRuntimeWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-hotkey-smoke-runtime-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = parseSeed(args[2]);
        TriggerBuildResult result = SharedHotkeySmokeTestPreset.buildRuntime(rom, seed);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 19 LAB shared-hotkey runtime built.");
        System.out.println("  bindings: R+SELECT -> Seed, R+B -> Repel, R+A -> Probe");
        System.out.println("  dispatcher: Field Script, deferred after callback1");
        System.out.printf("  RamScript total: %d / %d%n", result.totalScriptBytes(), RamScript.SCRIPT_SIZE);
        System.out.printf("  RamScript free:  %d%n", result.freeScriptBytes());
    }

    private static void buildDirectPersistentHotkeyInstallWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-direct-persistent-hotkey-install-wc3 <rom> <seed> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        int seed = Integer.parseInt(args[2], 16);
        buildIntoWc3(PersistentDirectHotkeyRuntime.buildInstaller(rom, seed), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Build 12 direct persistent modules + r1 dispatcher installed.");
    }

    private static void buildDirectPersistentHotkeyRuntimeWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-direct-persistent-hotkey-runtime-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        TriggerBuildResult result = PersistentDirectHotkeyRuntime.buildRuntime(rom);
        buildIntoWc3(result.ramScript(), Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 12 direct persistent hotkey runtime built.");
        System.out.println("  temporary executable resolver: none");
        System.out.println("  Field Script hotkey bridges:    none");
        System.out.println("  RamScript total:                " + result.totalScriptBytes() + " / " + RamScript.SCRIPT_SIZE);
        System.out.println("  RamScript free:                 " + result.freeScriptBytes());
        System.out.println("  R+B -> SID module (check VAR_8004 at 020370C0)");
        System.out.println("  R+SELECT -> Seed Modifier core");
    }


    private static void buildSharedPartyIvSmokeInstallAWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-party-iv-smoke-install-a-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        RamScript script = SharedHotkeyPartyIvSmokeTestPreset.buildInstallerA(rom,seed);
        buildIntoWc3(script,Path.of(args[3]),Path.of(args[4]));
        System.out.print(SharedHotkeyPartyIvSmokeTestPreset.report(rom,seed));
    }

    private static void buildSharedPartyIvSmokeInstallBWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-party-iv-smoke-install-b-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        RamScript script = SharedHotkeyPartyIvSmokeTestPreset.buildInstallerB(rom,seed);
        buildIntoWc3(script,Path.of(args[3]),Path.of(args[4]));
        System.out.print(SharedHotkeyPartyIvSmokeTestPreset.report(rom,seed));
    }

    private static void buildSharedPartyIvSmokeInstallCWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-party-iv-smoke-install-c-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        RamScript script = SharedHotkeyPartyIvSmokeTestPreset.buildInstallerC(rom,seed);
        buildIntoWc3(script,Path.of(args[3]),Path.of(args[4]));
        System.out.print(SharedHotkeyPartyIvSmokeTestPreset.report(rom,seed));
    }

    private static void buildSharedPartyIvSmokeRuntimeWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-party-iv-smoke-runtime-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        TriggerBuildResult result = SharedHotkeyPartyIvSmokeTestPreset.buildRuntime(rom,seed);
        buildIntoWc3(result.ramScript(),Path.of(args[3]),Path.of(args[4]));
        System.out.println("Shared Party IV runtime: " + result.totalScriptBytes() + "/995 B; free " + result.freeScriptBytes() + " B");
        System.out.print(SharedHotkeyPartyIvSmokeTestPreset.report(rom,seed));
    }

    private static void buildSharedNativeInstallAWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-install-a-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        buildIntoWc3(SharedPersistentNativeComposition.buildInstallerA(rom,seed), Path.of(args[3]), Path.of(args[4]));
        System.out.print(SharedPersistentNativeComposition.report(rom,seed));
    }

    private static void buildSharedNativeInstallBWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-install-b-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        buildIntoWc3(SharedPersistentNativeComposition.buildInstallerB(rom,seed), Path.of(args[3]), Path.of(args[4]));
        System.out.print(SharedPersistentNativeComposition.report(rom,seed));
    }

    private static void buildSharedNativeInstallCWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-install-c-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        buildIntoWc3(SharedPersistentNativeComposition.buildInstallerC(rom,seed), Path.of(args[3]), Path.of(args[4]));
        System.out.print(SharedPersistentNativeComposition.report(rom,seed));
    }

    private static void buildSharedNativeRuntimeWc3(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: build-shared-native-runtime-wc3 <rom> <seed-hex> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]); int seed = Integer.parseUnsignedInt(args[2],16);
        TriggerBuildResult result = SharedPersistentNativeComposition.buildRuntime(rom,seed);
        buildIntoWc3(result.ramScript(), Path.of(args[3]), Path.of(args[4]));
        System.out.println("Shared dual-native runtime: " + result.totalScriptBytes() + "/995 B; free " + result.freeScriptBytes() + " B");
        System.out.println("  R+SELECT -> Seed | R+B -> Repel | R+A -> Party IV | R+START -> SID");
        System.out.print(SharedPersistentNativeComposition.report(rom,seed));
    }

    private static void buildPartyIvStagingCheckWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-party-iv-staging-check-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = PartyIvStagingDiagnostic.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 22E Party IV staging-copy diagnostic built (module is NOT executed).");
    }

    private static void buildPartyIvDirectCallCheckWc3(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: build-party-iv-direct-call-check-wc3 <rom> <input.wc3> <output.wc3>");
        RomProfile rom = RomProfile.fromId(args[1]);
        RamScript script = PartyIvDirectCallDiagnostic.build(rom);
        buildIntoWc3(script, Path.of(args[2]), Path.of(args[3]));
        System.out.println("Build 22F Party IV direct-call diagnostic built (persistent module staged, then called directly by Field Script).");
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
        System.out.println("Trigger composition:");
        System.out.println("  java -cp out Main build-trigger-test-bin hotkey fr10 output.bin");
        System.out.println("  java -cp out Main build-trigger-test-wc3 hotkey lg10 input.wc3 output.wc3");
        System.out.println("  trigger: deliveryman | hotkey");
        System.out.println("  ROM: fr10 | lg10 | fr11 | lg11");
        System.out.println();
        System.out.println("Persistence research:");
        System.out.println("  java -cp out Main build-persistence-probe-install-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistence-probe-check-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistence-400-install-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistence-400-check-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistence-1024-install-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistence-1024-check-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-storage-v1-install-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-storage-v1-launch-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-storage-v2-install-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-storage-v2-launch-wc3 fr10 1 input.wc3 output.wc3");
        System.out.println();
        System.out.println("Advanced presets:");
        System.out.println("  java -cp out Main build-show-secret-id-bin fr10 output.bin");
        System.out.println("  java -cp out Main build-show-secret-id-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-show-secret-id-persistent-install-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-show-secret-id-persistent-launch-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-seed-modifier-bin fr10 1234 output.bin [--hotkey r-select]");
        System.out.println("  java -cp out Main build-seed-modifier-wc3 fr10 1234 input.wc3 output.wc3 [--hotkey r-b]");
        System.out.println("  java -cp out Main build-party-iv-viewer-bin fr10 output.bin [--hotkey r-select]");
        System.out.println("  java -cp out Main build-party-iv-viewer-wc3 fr10 input.wc3 output.wc3 [--hotkey l-start]");
        System.out.println("  java -cp out Main build-repel-hotkey-bin fr10 output.bin [--hotkey r-select]");
        System.out.println("  java -cp out Main build-repel-hotkey-wc3 fr10 input.wc3 output.wc3 [--hotkey r-b]");
        System.out.println("  java -cp out Main build-seed-repel-combo-wc3 fr10 1234 input.wc3 output.wc3 [--seed-hotkey r-select --repel-hotkey r-b]");
        System.out.println("  java -cp out Main build-persistent-hotkey-install-wc3 fr10 1234 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-hotkey-runtime-wc3 fr10 1234 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-direct-persistent-hotkey-install-wc3 fr10 1234 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-direct-persistent-hotkey-runtime-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-field-hotkey-install-wc3 fr10 1234 input.wc3 output.wc3");
        System.out.println("  java -cp out Main build-persistent-field-hotkey-runtime-wc3 fr10 input.wc3 output.wc3");
        System.out.println("  hotkey syntax: <held>-<pressed>; first button is held, second is newly pressed");
        System.out.println("  buttons: a b select start right left up down r l");
        System.out.println("  default hotkey remains r-select");
        System.out.println();
        System.out.println("Legacy v5 build-* commands remain accepted for compatibility.");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
