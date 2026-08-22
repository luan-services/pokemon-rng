import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class TestRunner {
    private TestRunner() {}

    public static void main(String[] args) throws Exception {
        testOfficialScripts();
        testRareCandyPreset();
        testGenericItemGift();
        testRepeatableItemGift();
        testClearFlagUtility();
        testBinaryRoundTrip();
        testWc3ReplacementPreservesDesign();
        testScriptSizeLimit();
        testCatalog();
        testTriggerComposition();
        testCustomPayloadComposition();
        testShowSecretIdPreset();
        testSeedModifierPreset();
        testPartyIvViewerPreset();
        testRepelHotkeyPreset();
        testHotkeyConfiguration();
        testMultiHotkeyRuntimeV1();
        testPersistenceProbeHelpers();

        testHotkeyRuntimeV1();

        testPayloadPlacementPlanner();
        testDeploymentCompatibility();
        testPersistentShowSecretIdModule();
        testCrossAreaDispatcherBuild();
        testRealPersistentModulesBuild();
        testPersistentStorageAreaModel();
        PersistentToolkitStorageV2Test.run();
        PersistentToolkitStorageV3Test.run();
        System.out.println("All tests passed.");
    }


    private static void testDeploymentCompatibility() throws Exception {
        PresetPayload field = new PresetPayload("field", PresetPayloadKind.FIELD_SCRIPT, new byte[] {1,2,3});
        PlacedPayload simple = PresetDeploymentPlanner.place(field, PresetDeploymentMode.SIMPLE_RAMSCRIPT, 16);
        assertTrue(simple.area() == PayloadStorageArea.RAMSCRIPT, "simple deployment must remain in RamScript");

        boolean rejected = false;
        try {
            PresetDeploymentPlanner.place(field, PresetDeploymentMode.PERSISTENT_MODULE, 16);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected, "field scripts must not silently become persistent native modules");

        // Strong compatibility guard: these are full 1004-byte RamScript binaries
        // from Build 7a. Discovering persistent storage must not alter them.
        assertSha256(ShowSecretIdPreset.build(RomProfile.FIRE_RED_EN_10).bytesCopy(),
                "3c4901bb60250a61d85a904afe7d6b732dc1bf831ce7a100f708726cfc6899b5", "simple Show SID");
        assertSha256(SeedModifierPreset.build(RomProfile.FIRE_RED_EN_10, 0x1234).ramScript().bytesCopy(),
                "5324a548bab6a51a8eb7b035a17ecb1c7c847ada127f64fd05d3f6c79199f43d", "simple Seed Modifier");
        assertSha256(RepelHotkeyPreset.build(RomProfile.FIRE_RED_EN_10).ramScript().bytesCopy(),
                "d23e89e25cf38c1d7dfd17d1fe3f6ae78cdd7d38561370d79b8fa63fb4b44315", "simple Repel");
        assertSha256(PartyIvViewerPreset.build(RomProfile.FIRE_RED_EN_10).ramScript().bytesCopy(),
                "966d5c6a03875febd3a9c6d3216b2f6027b150a09158848b76db695d4f269036", "simple IV Viewer");
    }

    private static void testPersistentShowSecretIdModule() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        PresetPayload payload = PersistentShowSecretIdModule.payload(rom);
        assertTrue(payload.kind() == PresetPayloadKind.THUMB, "persistent SID payload kind");
        assertTrue(payload.bytes().length == 0x20, "persistent SID payload size");
        byte[] image = PersistentToolkitStorageV4.buildImage(rom);
        assertTrue(Byte.toUnsignedInt(image[4]) == PersistentToolkitStorageV4.VERSION, "V4 version");
        assertTrue(Byte.toUnsignedInt(image[5]) == 1, "V4 module count");
        assertTrue((image[PersistentToolkitStorageV4.ENTRY_OFFSET] & 0xFF) == PersistentShowSecretIdModule.MODULE_ID, "V4 SID module id");
        assertTrue((image[PersistentToolkitStorageV4.ENTRY_OFFSET + 3] & 0xFF) == 2, "V4 SID physical area");
        RamScript installer = PersistentShowSecretIdPreset.buildInstaller(rom);
        RamScript launcher = PersistentShowSecretIdPreset.buildLauncher(rom);
        assertTrue(installer.isChecksumValid(), "persistent SID installer checksum");
        assertTrue(launcher.isChecksumValid(), "persistent SID launcher checksum");
    }


    private static void testCrossAreaDispatcherBuild() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] image = PersistentToolkitStorageV5.buildCatalogImage(rom);
        assertTrue(Byte.toUnsignedInt(image[4]) == 5, "V5 version");
        assertTrue(Byte.toUnsignedInt(image[5]) == 2, "V5 module count");
        assertTrue(Byte.toUnsignedInt(image[PersistentToolkitStorageV5.ENTRY_1 + 3]) == 1, "V5 module 1 is SaveBlock1");
        assertTrue(Byte.toUnsignedInt(image[PersistentToolkitStorageV5.ENTRY_2 + 3]) == 2, "V5 module 2 is SaveBlock2");
        assertTrue(PersistentToolkitStorageV5.buildSaveBlock1Payload(rom).length == 0x20, "V5 SB1 payload length");
        NativeHelper dispatcher = PersistentToolkitStorageV5NativeHelper.buildDispatcherAt(rom, 0x02030000L);
        assertTrue(dispatcher.size() < 256, "cross-area dispatcher should stay compact");
        RamScript installer = PersistentCrossAreaDispatcherPreset.buildInstaller(rom);
        RamScript launcher = PersistentCrossAreaDispatcherPreset.buildLauncher(rom);
        assertTrue(installer.isChecksumValid(), "cross-area installer checksum");
        assertTrue(launcher.isChecksumValid(), "cross-area launcher checksum");
        assertTrue(installer.scriptCopy().length <= RamScript.SCRIPT_SIZE, "cross-area installer fits RamScript");
        assertTrue(launcher.scriptCopy().length <= RamScript.SCRIPT_SIZE, "cross-area launcher fits RamScript");
    }

    private static void testRealPersistentModulesBuild() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        int seed = 0x1234;
        PresetPayload seedPayload = PersistentSeedModifierModule.payload(rom, seed);
        assertTrue(seedPayload.kind() == PresetPayloadKind.THUMB, "persistent seed payload kind");
        assertTrue(seedPayload.bytes().length == 0x1C, "persistent seed payload size");
        byte[] seedBytes = seedPayload.bytes();
        long predecessor = PersistentSeedModifierModule.predecessor(seed);
        assertTrue(((seedBytes[0x0C] & 0xFF) | ((seedBytes[0x0D] & 0xFF) << 8)) == 0x4770, "persistent seed payload returns before literal pool");
        assertTrue(readU32(seedBytes, 0x10) == rom.specialVarResult, "persistent seed result literal");
        assertTrue(readU32(seedBytes, 0x14) == rom.rngValue, "persistent seed rng literal");
        assertTrue(readU32(seedBytes, 0x18) == predecessor, "persistent seed predecessor literal");

        byte[] image = PersistentToolkitStorageV6.buildCatalogImage(rom, seed);
        assertTrue(Byte.toUnsignedInt(image[4]) == PersistentToolkitStorageV6.VERSION, "V6 version");
        assertTrue(Byte.toUnsignedInt(image[5]) == 2, "V6 module count");
        assertTrue(Byte.toUnsignedInt(image[PersistentToolkitStorageV6.ENTRY_1 + 3]) == 1, "V6 SID in SaveBlock1");
        assertTrue(Byte.toUnsignedInt(image[PersistentToolkitStorageV6.ENTRY_2 + 3]) == 2, "V6 seed in SaveBlock2");
        assertTrue((image[PersistentToolkitStorageV6.ENTRY_1] & 0xFF) == PersistentShowSecretIdModule.MODULE_ID, "V6 SID id");
        assertTrue((image[PersistentToolkitStorageV6.ENTRY_2] & 0xFF) == PersistentSeedModifierModule.MODULE_ID, "V6 seed id");

        NativeHelper dispatcher = PersistentToolkitStorageV6NativeHelper.buildDispatcherAt(rom, 0x02030000L);
        assertTrue(dispatcher.size() < 256, "V6 dispatcher should stay compact");
        RamScript installer = PersistentRealPresetDispatcherPreset.buildInstaller(rom, seed);
        RamScript launcher = PersistentRealPresetDispatcherPreset.buildLauncher(rom, seed);
        assertTrue(installer.isChecksumValid(), "real-module installer checksum");
        assertTrue(launcher.isChecksumValid(), "real-module launcher checksum");
        assertTrue(installer.scriptCopy().length <= RamScript.SCRIPT_SIZE, "real-module installer fits RamScript");
        assertTrue(launcher.scriptCopy().length <= RamScript.SCRIPT_SIZE, "real-module launcher fits RamScript");
    }

    private static void assertSha256(byte[] bytes, String expected, String label) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        assertTrue(hex.toString().equals(expected), label + " regression hash");
    }

    private static void testOfficialScripts() {
        RamScript aurora = OfficialGiftScripts.buildAuroraTicket();
        RamScript mystic = OfficialGiftScripts.buildMysticTicket();

        assertTrue(aurora.isChecksumValid(), "Aurora checksum");
        assertTrue(aurora.hasWonderCardHeader(), "Aurora WC header");
        assertTrue(mystic.isChecksumValid(), "Mystic checksum");
        assertTrue(mystic.hasWonderCardHeader(), "Mystic WC header");
    }

    private static void testRareCandyPreset() {
        RamScript script = CustomGiftScripts.buildRareCandyTest();
        assertTrue(script.isChecksumValid(), "Rare Candy checksum");
        assertTrue(script.hasWonderCardHeader(), "Rare Candy header");
    }

    private static void testGenericItemGift() {
        ItemGiftPreset preset = new ItemGiftPreset(
                0x44,
                2,
                0x2AA,
                "Hello, {PLAYER}!\\nTake two RARE CANDY.",
                "Enjoy!",
                "Already received.",
                "Your BAG is full."
        );

        RamScript script = preset.build();
        assertTrue(script.isChecksumValid(), "generic item gift checksum");
        assertTrue(script.hasWonderCardHeader(), "generic item gift header");

        byte[] bytes = script.scriptCopy();
        byte[] encoded = Gen3TextCodec.encodeString("Take two RARE CANDY.");
        assertTrue(indexOf(bytes, encoded) >= 0, "custom item-gift text should be embedded");
    }


    private static void testRepeatableItemGift() {
        RamScript script = RepeatableItemGiftPreset.defaults(0x44, 1).build();

        assertTrue(script.isChecksumValid(), "repeatable gift checksum");
        assertTrue(script.hasWonderCardHeader(), "repeatable gift header");

        byte[] bytes = script.scriptCopy();

        // 0x2B = checkflag, 0x29 = setflag in FR/LG field-script bytecode.
        assertTrue(indexOfByte(bytes, 0x2B) < 0, "repeatable gift must not emit checkflag");
        assertTrue(indexOfByte(bytes, 0x29) < 0, "repeatable gift must not emit setflag");

        // 0x46 = checkitemspace. The giveItem macro itself expands to normal
        // variable setup + callstd, so checking the full disassembly is preferable.
        assertTrue(indexOfByte(bytes, 0x46) >= 0, "repeatable gift should check bag space");
    }

    private static void testClearFlagUtility() {
        RamScript script = SimpleGiftScripts.buildClearFlag(
                0x08010000L,
                0x2AA,
                "Flag cleared."
        );

        assertTrue(script.isChecksumValid(), "clear-flag checksum");
        assertTrue(script.hasWonderCardHeader(), "clear-flag header");

        byte[] bytes = script.scriptCopy();
        assertTrue(indexOfByte(bytes, 0x2A) >= 0, "clear-flag must emit clearflag");
    }

    private static void testBinaryRoundTrip() throws Exception {
        RamScript original = CustomGiftScripts.buildRareCandyTest();

        Path file = Files.createTempFile("ramscript", ".bin");
        original.writeBinary(file);
        RamScript loaded = RamScript.fromBinary(file);

        assertTrue(
                Arrays.equals(original.bytesCopy(), loaded.bytesCopy()),
                "BIN read/write round-trip"
        );
    }

    private static void testWc3ReplacementPreservesDesign() throws Exception {
        byte[] wc3 = new byte[RamScript.WC3_FILE_SIZE];

        for (int i = 0; i < RamScript.WC3_OFFSET; i++) {
            wc3[i] = (byte) (i * 7);
        }

        Path input = Files.createTempFile("design", ".wc3");
        Path output = Files.createTempFile("event", ".wc3");
        Files.write(input, wc3);

        RamScript script = ItemGiftPreset.defaults(0x44, 1, 0x2AA).build();
        script.replaceInWc3(input, output);

        byte[] replaced = Files.readAllBytes(output);

        for (int i = 0; i < RamScript.WC3_OFFSET; i++) {
            if (wc3[i] != replaced[i]) {
                throw new AssertionError("card/design byte changed at 0x" + Integer.toHexString(i));
            }
        }

        byte[] expectedScript = script.bytesCopy();
        byte[] actualScript = Arrays.copyOfRange(
                replaced,
                RamScript.WC3_OFFSET,
                RamScript.WC3_OFFSET + RamScript.SIZE
        );

        assertTrue(Arrays.equals(expectedScript, actualScript), "RamScript replacement bytes");
    }

    private static void testScriptSizeLimit() {
        RamScriptBuilder builder = new RamScriptBuilder(0x08010000L);
        builder.setVAddress();

        // 996 one-byte end opcodes after the 5-byte setvaddress must exceed the 995-byte field limit.
        for (int i = 0; i < 996; i++) {
            builder.end();
        }

        boolean rejected = false;
        try {
            builder.buildScript();
        } catch (IllegalStateException expected) {
            rejected = true;
        }

        assertTrue(rejected, "oversized script must be rejected");
    }

    private static void testCatalog() {
        assertTrue(!BuilderCatalog.entries().isEmpty(), "builder catalog should not be empty");
        assertTrue(
                BuilderCatalog.entries().stream().anyMatch(e -> e.name().equals("giveItem")),
                "catalog should expose giveItem"
        );
    }



    private static void testTriggerComposition() throws Exception {
        byte[] payload = TriggerTestPayloads.helloWonderCard();
        assertTrue(payload.length == 43, "hello payload should remain 43 bytes");

        TriggerBuildResult delivery = TriggerComposer.compose(
                EventTrigger.DELIVERYMAN, RomProfile.FIRE_RED_EN_10, payload
        );
        assertTrue(delivery.runtimeOverheadBytes() == 0, "deliveryman must have zero runtime overhead");
        assertTrue(delivery.totalScriptBytes() == payload.length, "deliveryman total size");
        assertTrue(Arrays.equals(
                Arrays.copyOf(delivery.ramScript().scriptCopy(), payload.length), payload
        ), "deliveryman script must start with the original payload bytes");

        TriggerBuildResult hotkeyFr = TriggerComposer.compose(
                EventTrigger.HOTKEY_RUNTIME, RomProfile.FIRE_RED_EN_10, payload
        );
        TriggerBuildResult hotkeyLg = TriggerComposer.compose(
                EventTrigger.HOTKEY_RUNTIME, RomProfile.LEAF_GREEN_EN_10, payload
        );
        assertTrue(hotkeyFr.runtimeOverheadBytes() > 0, "hotkey must report runtime overhead");
        assertTrue(hotkeyFr.freeScriptBytes() >= 0, "hotkey size must fit");
        assertTrue(Arrays.equals(hotkeyFr.ramScript().bytesCopy(), hotkeyLg.ramScript().bytesCopy()),
                "FR10/LG10 hotkey runtime must be byte-identical");

        byte[] script = hotkeyFr.ramScript().scriptCopy();
        assertTrue(Byte.toUnsignedInt(script[0x0A]) == 0xA7, "runtime format signature low byte");
        assertTrue(Byte.toUnsignedInt(script[0x0B]) == 0x00, "runtime format signature high byte");
        assertTrue(Arrays.equals(
                Arrays.copyOfRange(script, 0x0C, 0x0C + payload.length), payload
        ), "hotkey wrapper must preserve payload byte-for-byte");

        // Strong regression: composition must reproduce the already validated
        // standalone Runtime v1 binary exactly when using its original hello payload.
        Path validated = Path.of("../native-ramscript-runtime-v1-multiprofile/runtime-v1-fr10.bin");
        if (Files.exists(validated)) {
            byte[] expected = Files.readAllBytes(validated);
            assertTrue(Arrays.equals(expected, hotkeyFr.ramScript().bytesCopy()),
                    "integrated hotkey hello must match validated Runtime v1 FR10 binary");
        }
    }


    private static void testCustomPayloadComposition() {
        byte[] payload = TriggerTestPayloads.helloWonderCard();

        TriggerBuildResult delivery = CustomPayloadComposer.compose(
                EventTrigger.DELIVERYMAN,
                RomProfile.LEAF_GREEN_EN_10,
                payload
        );
        assertTrue(delivery.runtimeOverheadBytes() == 0,
                "custom deliveryman payload must not add runtime overhead");
        assertTrue(delivery.totalScriptBytes() == payload.length,
                "custom deliveryman payload size");
        assertTrue(java.util.Arrays.equals(
                        java.util.Arrays.copyOf(delivery.ramScript().scriptCopy(), payload.length),
                        payload
                ),
                "custom deliveryman must preserve payload byte-for-byte");

        TriggerBuildResult hotkey = CustomPayloadComposer.hotkey(
                RomProfile.FIRE_RED_EN_10,
                payload
        );
        assertTrue(hotkey.runtimeOverheadBytes() > 0,
                "custom hotkey payload must include runtime");
        byte[] wrapped = hotkey.ramScript().scriptCopy();
        assertTrue(java.util.Arrays.equals(
                        java.util.Arrays.copyOfRange(
                                wrapped,
                                HotkeyRuntimeV1.PAYLOAD_OFFSET,
                                HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length
                        ),
                        payload
                ),
                "custom hotkey must preserve payload byte-for-byte");

        TriggerBuildResult frDelivery = CustomPayloadComposer.compose(
                EventTrigger.DELIVERYMAN,
                RomProfile.FIRE_RED_EN_10,
                payload
        );
        assertTrue(java.util.Arrays.equals(
                        frDelivery.ramScript().bytesCopy(),
                        delivery.ramScript().bytesCopy()
                ),
                "deliveryman custom payload must be ROM-profile agnostic");
    }


    private static void testShowSecretIdPreset() {
        byte[] referenceHelper = null;
        byte[] referenceScript = null;

        for (RomProfile rom : RomProfile.values()) {
            NativeHelper helper = SecretIdNativeHelper.build(rom);
            byte[] code = helper.codeCopy();

            assertTrue(helper.stagingAddress() == 0x03005310L,
                    "SID helper staging " + rom.id());
            assertTrue(helper.thumbEntryAddress() == 0x03005311L,
                    "SID helper Thumb entry " + rom.id());
            assertTrue(code.length == 20,
                    "SID helper exact size " + rom.id());

            // Independently verified Thumb opcodes:
            // ldr r0,literal; ldr r0,[r0]; ldrh r0,[r0,#0x0C];
            // ldr r1,literal; strh r0,[r1]; bx lr.
            assertTrue((((code[1] & 0xFF) << 8) | (code[0] & 0xFF)) == 0x4802,
                    "SID helper first literal load " + rom.id());
            assertTrue((((code[3] & 0xFF) << 8) | (code[2] & 0xFF)) == 0x6800,
                    "SID helper SaveBlock2 dereference " + rom.id());
            assertTrue((((code[5] & 0xFF) << 8) | (code[4] & 0xFF)) == 0x8980,
                    "SID helper Secret ID halfword load " + rom.id());
            assertTrue((((code[7] & 0xFF) << 8) | (code[6] & 0xFF)) == 0x4902,
                    "SID helper result literal load " + rom.id());
            assertTrue((((code[9] & 0xFF) << 8) | (code[8] & 0xFF)) == 0x8008,
                    "SID helper result halfword store " + rom.id());
            assertTrue((((code[11] & 0xFF) << 8) | (code[10] & 0xFF)) == 0x4770,
                    "SID helper bx lr " + rom.id());

            assertTrue(Binary.u32(code, 0x0C) == rom.saveBlock2Ptr,
                    "SID helper gSaveBlock2Ptr literal " + rom.id());
            assertTrue(Binary.u32(code, 0x10) == rom.specialVarResult,
                    "SID helper gSpecialVar_Result literal " + rom.id());

            RamScript script = ShowSecretIdPreset.build(rom);
            assertTrue(script.isChecksumValid(),
                    "ShowSecretId checksum " + rom.id());
            assertTrue(script.hasWonderCardHeader(),
                    "ShowSecretId WC header " + rom.id());

            byte[] payload = ShowSecretIdPreset.buildScript(rom);
            assertTrue(payload.length < 200,
                    "ShowSecretId should remain compact " + rom.id());

            // The current four supported profiles expose the same RAM symbols,
            // so this helper/preset should be byte-identical across them.
            if (referenceHelper == null) {
                referenceHelper = code;
                referenceScript = payload;
            } else {
                assertTrue(java.util.Arrays.equals(referenceHelper, code),
                        "SID helper profile equality " + rom.id());
                assertTrue(java.util.Arrays.equals(referenceScript, payload),
                        "ShowSecretId profile equality " + rom.id());
            }

            // Confirm the script contains buffernumberstring 0, VAR_RESULT.
            byte[] needle = new byte[] {
                    (byte)0x83, 0x00, 0x0D, (byte)0x80
            };
            assertTrue(indexOfSequence(payload, needle) >= 0,
                    "ShowSecretId buffernumberstring VAR_RESULT " + rom.id());
        }
    }


    private static void testSeedModifierPreset() {
        int desiredSeed = 0x1234;
        long predecessor = SeedModifierPreset.predecessor(desiredSeed);

        assertTrue(RngMath.nextState(predecessor) == desiredSeed,
                "Seed Modifier predecessor must advance to desired seed");
        assertTrue(SeedModifierPreset.message(desiredSeed).equals("Press A to set 1234 as seed."),
                "Seed Modifier prompt must include 4-digit uppercase seed");
        assertTrue(SeedModifierPreset.message(0x00AF).equals("Press A to set 00AF as seed."),
                "Seed Modifier prompt must preserve leading zeroes");

        byte[] referencePayload = null;
        for (RomProfile rom : RomProfile.values()) {
            byte[] payload = SeedModifierPreset.buildPayload(rom, desiredSeed);
            TriggerBuildResult result = SeedModifierPreset.build(rom, desiredSeed);

            assertTrue(result.trigger() == EventTrigger.HOTKEY_RUNTIME,
                    "Seed Modifier must use hotkey runtime " + rom.id());
            assertTrue(result.ramScript().isChecksumValid(),
                    "Seed Modifier checksum " + rom.id());
            assertTrue(result.ramScript().hasWonderCardHeader(),
                    "Seed Modifier WC header " + rom.id());
            assertTrue(result.payloadBytes() == payload.length,
                    "Seed Modifier payload accounting " + rom.id());
            assertTrue(payload.length == 68,
                    "Seed Modifier compact payload size " + rom.id());

            byte[] predecessorBytes = new byte[] {
                    (byte) predecessor,
                    (byte) (predecessor >>> 8),
                    (byte) (predecessor >>> 16),
                    (byte) (predecessor >>> 24)
            };
            for (int i = 0; i < predecessorBytes.length; i++) {
                byte[] write = new byte[] {
                        0x11, predecessorBytes[i],
                        (byte) ((rom.rngValue + i) & 0xFF),
                        (byte) (((rom.rngValue + i) >>> 8) & 0xFF),
                        (byte) (((rom.rngValue + i) >>> 16) & 0xFF),
                        (byte) (((rom.rngValue + i) >>> 24) & 0xFF)
                };
                assertTrue(indexOfSequence(payload, write) >= 0,
                        "Seed Modifier writes predecessor byte " + i + " " + rom.id());
            }

            byte[] message = Gen3TextCodec.encodeString(SeedModifierPreset.message(desiredSeed));
            assertTrue(indexOfSequence(payload, message) >= 0,
                    "Seed Modifier message embedded " + rom.id());

            if (referencePayload == null) {
                referencePayload = payload;
            } else {
                assertTrue(java.util.Arrays.equals(referencePayload, payload),
                        "Seed Modifier profile equality " + rom.id());
            }
        }

        try {
            SeedModifierPreset.buildPayload(RomProfile.FIRE_RED_EN_10, 0x10000);
            throw new AssertionError("Seed Modifier must reject >u16 seed");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }


    private static int indexOfSequence(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int indexOfByte(byte[] data, int value) {
        for (int i = 0; i < data.length; i++) {
            if (Byte.toUnsignedInt(data[i]) == value) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }


    private static void testPartyIvViewerPreset() {
        for (RomProfile rom : RomProfile.values()) {
            TriggerBuildResult result = PartyIvViewerPreset.build(rom);
            assertTrue(result.payloadBytes() == 495, "party IV viewer payload size for " + rom.id());
            assertTrue(result.totalScriptBytes() <= RamScript.SCRIPT_SIZE, "party IV viewer must fit for " + rom.id());
            assertTrue(result.totalScriptBytes() == 861, "party IV viewer total for " + rom.id());
            assertTrue(result.freeScriptBytes() == 134, "party IV viewer free bytes for " + rom.id());
            assertTrue(result.ramScript().isChecksumValid(), "party IV viewer checksum for " + rom.id());
            assertTrue(PartyIvViewerPreset.selectedInstallerMode(rom) == NativeHelperInstaller.Mode.CPU_SET_BLOCK,
                    "party IV viewer should auto-select CpuSet for " + rom.id());
        }

        NativeHelper rev0 = PartyMonDataNativeHelper.build(RomProfile.FIRE_RED_EN_10);
        NativeHelper rev1 = PartyMonDataNativeHelper.build(RomProfile.FIRE_RED_EN_11);
        assertTrue(rev0.size() == 296, "party message helper size");
        assertTrue(rev0.stagingAddress() == 0x02021E58L, "party message helper staging");
        assertTrue(readU32(rev0.codeCopy(), 0x11C) == 0x02024284L, "player party literal");
        assertTrue(readU32(rev0.codeCopy(), 0x120) == 0x02021F98L, "dynamic message literal");
        assertTrue(readU32(rev0.codeCopy(), 0x124) == 0x0803FBE9L, "FR1.0 GetMonData3 literal");
        assertTrue(readU32(rev1.codeCopy(), 0x124) == 0x0803FBFDL, "FR1.1 GetMonData3 literal");
        assertTrue(PartyMonDataNativeHelper.DYNAMIC_MESSAGE_DELTA + PartyMonDataNativeHelper.MAX_MESSAGE_BYTES <= 1000,
                "worst-case party message must fit in gStringVar4");

        long copierAddress = 0x02021E18L;
        byte[] copier = CpuSetNativeHelperInstaller.copierBytes(
                copierAddress, 0x0801000CL, rev0.size()
        );
        assertTrue(copier.length == 28, "compact CpuSet copier size");
        assertTrue((copier[0x0E] & 0xFF) == 74 && (copier[0x0F] & 0xFF) == 0x32,
                "compact CpuSet copier word count");
        assertTrue(readU32(copier, 0x14) == 0x020370A8L, "compact copier sAddressOffset literal");
        assertTrue(readU32(copier, 0x18) == 0x0801000CL, "compact copier virtual source literal");

        // AUTO must keep tiny helpers on direct setptr but select CpuSet once
        // the raw helper is large enough to amortize the 28-byte copier.
        NativeHelper tiny = new NativeHelper(0x02022040L, new byte[20]);
        assertTrue(NativeHelperInstaller.chooseMode(5, tiny, 0x02022000L)
                        == NativeHelperInstaller.Mode.DIRECT_SET_PTR,
                "AUTO should choose direct setptr for a 20-byte helper");
        assertTrue(NativeHelperInstaller.chooseMode(5, rev0, copierAddress)
                        == NativeHelperInstaller.Mode.CPU_SET_BLOCK,
                "AUTO should choose CpuSet for the party message helper");
        assertTrue(NativeHelperInstaller.directEncodingCost(20) == 120,
                "direct helper encoding cost");

        byte[] compactPayload = PartyIvViewerPreset.buildPayload(RomProfile.FIRE_RED_EN_10);
        assertTrue((compactPayload[0] & 0xFF) == 0xB8, "IV payload begins with setvaddress");
        assertTrue((compactPayload[5] & 0xFF) == 0xB9, "IV payload skips raw helper with vgoto");
        assertTrue(readU32(compactPayload, 6) == 0x08010134L, "IV main label virtual pointer");
        assertTrue(java.util.Arrays.equals(
                        java.util.Arrays.copyOfRange(compactPayload, 0x0C, 0x0C + rev0.size()),
                        PartyMonDataNativeHelper.buildAt(RomProfile.FIRE_RED_EN_10, 0x02021E58L).codeCopy()),
                "IV payload embeds raw helper bytes");

        byte[] dynamicMessageCommand = new byte[] {
                0x67, (byte)0x98, 0x1F, 0x02, 0x02
        };
        assertTrue(indexOfSequence(compactPayload, dynamicMessageCommand) >= 0,
                "party IV viewer should display the continuous dynamic party message");

        // The helper embeds CHAR_PROMPT_CLEAR (0xFB), used by the stock text
        // printer to show the down-arrow and clear the existing message window.
        assertTrue(indexOfSequence(rev0.codeCopy(), new byte[] {(byte)0xFB, (byte)0xCD, (byte)0xCA, (byte)0xBB}) >= 0,
                "party helper should embed prompt-clear before SPA page");
    }



    private static void testRepelHotkeyPreset() {
        byte[] payload = RepelHotkeyPreset.buildPayload();

        for (RomProfile rom : RomProfile.values()) {
            TriggerBuildResult result = RepelHotkeyPreset.build(rom);
            assertTrue(result.trigger() == EventTrigger.HOTKEY_RUNTIME,
                    "Repel hotkey must use HotkeyRuntimeV1 " + rom.id());
            assertTrue(result.payloadBytes() == payload.length,
                    "Repel hotkey payload accounting " + rom.id());
            assertTrue(result.totalScriptBytes() <= RamScript.SCRIPT_SIZE,
                    "Repel hotkey must fit " + rom.id());
            assertTrue(result.ramScript().isChecksumValid(),
                    "Repel hotkey checksum " + rom.id());
        }

        // compare VAR_REPEL_STEP_COUNT, 0
        assertTrue(indexOfSequence(payload, new byte[] {
                0x21, 0x20, 0x40, 0x00, 0x00
        }) >= 0, "Repel hotkey must refuse stacking while active");

        // Prefer Max Repel (0x0054), then Super (0x0053), then normal Repel (0x0056).
        int maxCheck = indexOfSequence(payload, new byte[] {0x47, 0x54, 0x00, 0x01, 0x00});
        int superCheck = indexOfSequence(payload, new byte[] {0x47, 0x53, 0x00, 0x01, 0x00});
        int normalCheck = indexOfSequence(payload, new byte[] {0x47, 0x56, 0x00, 0x01, 0x00});
        assertTrue(maxCheck >= 0 && maxCheck < superCheck && superCheck < normalCheck,
                "Repel hotkey item priority");

        // setvar VAR_REPEL_STEP_COUNT with stock durations 250/200/100.
        assertTrue(indexOfSequence(payload, new byte[] {0x16, 0x20, 0x40, (byte)0xFA, 0x00}) >= 0,
                "Max Repel duration");
        assertTrue(indexOfSequence(payload, new byte[] {0x16, 0x20, 0x40, (byte)0xC8, 0x00}) >= 0,
                "Super Repel duration");
        assertTrue(indexOfSequence(payload, new byte[] {0x16, 0x20, 0x40, 0x64, 0x00}) >= 0,
                "Repel duration");

        assertTrue(indexOfSequence(payload, Gen3TextCodec.encodeString("No Repels.")) >= 0,
                "Repel hotkey no-item message");

        // Mirror vanilla Repel feedback: SE_REPEL (0x0029), waitse, and used-item message.
        assertTrue(indexOfSequence(payload, new byte[] {0x2F, 0x29, 0x00, 0x30}) >= 0,
                "Repel hotkey must play and wait for SE_REPEL");
        assertTrue(indexOfSequence(payload, Gen3TextCodec.encodeString("{PLAYER} used the\\n{STR_VAR_2}.")) >= 0,
                "Repel hotkey used-item message");

        // Every use branch must buffer its chosen item into STR_VAR_2 (index 1).
        assertTrue(indexOfSequence(payload, new byte[] {(byte)0x80, 0x01, 0x54, 0x00}) >= 0,
                "Max Repel name buffer");
        assertTrue(indexOfSequence(payload, new byte[] {(byte)0x80, 0x01, 0x53, 0x00}) >= 0,
                "Super Repel name buffer");
        assertTrue(indexOfSequence(payload, new byte[] {(byte)0x80, 0x01, 0x56, 0x00}) >= 0,
                "Repel name buffer");
    }

    private static void testHotkeyConfiguration() {
        assertTrue(Hotkey.parse("r-select").equals(Hotkey.DEFAULT),
                "r-select must parse as the default hotkey");
        assertTrue(Hotkey.parse("R+B").equals(new Hotkey(HotkeyButton.R, HotkeyButton.B)),
                "plus syntax should parse and preserve held/pressed order");
        assertTrue(Hotkey.parse("l-start").displayName().equals("L + START"),
                "hotkey display name");

        try {
            Hotkey.parse("r-r");
            throw new AssertionError("same-button hotkey must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] hello = TriggerTestPayloads.helloWonderCard();
        RamScript defaultExplicit = HotkeyRuntimeV1.build(rom, hello, Hotkey.DEFAULT);
        RamScript defaultLegacyApi = HotkeyRuntimeV1.build(rom, hello);
        assertTrue(Arrays.equals(defaultExplicit.bytesCopy(), defaultLegacyApi.bytesCopy()),
                "explicit default hotkey must preserve legacy build bytes");

        Hotkey rB = new Hotkey(HotkeyButton.R, HotkeyButton.B);
        RamScript custom = HotkeyRuntimeV1.build(rom, hello, rB);
        assertTrue(custom.isChecksumValid(), "custom hotkey checksum");
        assertTrue(custom.scriptCopy().length == defaultExplicit.scriptCopy().length,
                "custom hotkey must not increase runtime size");
        assertTrue(!Arrays.equals(custom.bytesCopy(), defaultExplicit.bytesCopy()),
                "custom hotkey must change runtime bytes");

        byte[] wrapper = RuntimeV1ResidentBlocks.build(rom, rB).stream()
                .filter(block -> block.address() == RuntimeV1ResidentBlocks.WRAPPER)
                .findFirst()
                .orElseThrow()
                .data();

        // Custom wrapper uses LSRS carry tests over the packed
        // heldKeysRaw | (newKeysRaw << 16) word.
        // R (bit 8 held) => LSRS #9 = 0x0A41.
        // B (bit 1 new)  => LSRS #18 = 0x0C81.
        assertTrue((((wrapper[5] & 0xFF) << 8) | (wrapper[4] & 0xFF)) == 0x0A41,
                "custom wrapper held R test");
        assertTrue((((wrapper[9] & 0xFF) << 8) | (wrapper[8] & 0xFF)) == 0x0C81,
                "custom wrapper pressed B test");

        TriggerBuildResult seed = SeedModifierPreset.build(rom, 0x1234, rB);
        TriggerBuildResult ivs = PartyIvViewerPreset.build(rom, rB);
        assertTrue(seed.totalScriptBytes() == SeedModifierPreset.build(rom, 0x1234).totalScriptBytes(),
                "custom Seed Modifier hotkey must be size-neutral");
        assertTrue(ivs.totalScriptBytes() == PartyIvViewerPreset.build(rom).totalScriptBytes(),
                "custom Party IV hotkey must be size-neutral");
    }

    private static void testMultiHotkeyRuntimeV1() {
        Hotkey seedHotkey = Hotkey.DEFAULT;
        Hotkey repelHotkey = new Hotkey(HotkeyButton.R, HotkeyButton.B);

        for (RomProfile rom : RomProfile.values()) {
            byte[] seedPayload = SeedModifierPreset.buildPayload(rom, 0x1234);
            byte[] repelPayload = RepelHotkeyPreset.buildPayload();
            TriggerBuildResult result = SeedRepelComboPreset.build(
                    rom, 0x1234, seedHotkey, repelHotkey
            );

            assertTrue(result.ramScript().isChecksumValid(),
                    "multi-hotkey checksum " + rom.id());
            assertTrue(result.totalScriptBytes() <= RamScript.SCRIPT_SIZE,
                    "multi-hotkey combo must fit " + rom.id());
            assertTrue(result.payloadBytes() == seedPayload.length + repelPayload.length,
                    "multi-hotkey payload accounting " + rom.id());

            byte[] script = result.ramScript().scriptCopy();
            int first = MultiHotkeyRuntimeV1.firstPayloadOffset();
            int second = MultiHotkeyRuntimeV1.secondPayloadOffset(seedPayload);
            assertTrue(Arrays.equals(
                            Arrays.copyOfRange(script, first, first + seedPayload.length),
                            seedPayload),
                    "first multi payload preserved byte-for-byte " + rom.id());
            assertTrue(Arrays.equals(
                            Arrays.copyOfRange(script, second, second + repelPayload.length),
                            repelPayload),
                    "second multi payload preserved byte-for-byte " + rom.id());
            assertTrue((script[0x0A] & 0xFF) == 0xA7 && (script[0x0B] & 0xFF) == 0,
                    "multi runtime keeps format signature " + rom.id());
        }

        TriggerBuildResult fr = SeedRepelComboPreset.build(
                RomProfile.FIRE_RED_EN_10, 0x1234, seedHotkey, repelHotkey
        );
        assertTrue(fr.totalScriptBytes() == 643,
                "FR10 Seed+Repel shared runtime size regression");
        assertTrue(fr.freeScriptBytes() == 352,
                "FR10 Seed+Repel shared runtime free-space regression");

        try {
            SeedRepelComboPreset.build(
                    RomProfile.FIRE_RED_EN_10, 0x1234, seedHotkey, seedHotkey
            );
            throw new AssertionError("duplicate multi-hotkey chord must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            SeedRepelComboPreset.build(
                    RomProfile.FIRE_RED_EN_10, 0x1234,
                    new Hotkey(HotkeyButton.R, HotkeyButton.SELECT),
                    new Hotkey(HotkeyButton.L, HotkeyButton.B)
            );
            throw new AssertionError("multi-hotkey V1 must reject different held buttons");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            SeedRepelComboPreset.build(
                    RomProfile.FIRE_RED_EN_10, 0x1234,
                    new Hotkey(HotkeyButton.R, HotkeyButton.SELECT),
                    new Hotkey(HotkeyButton.R, HotkeyButton.A)
            );
            throw new AssertionError("multi-hotkey V1 must reject non-adjacent pressed bits");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void testHotkeyRuntimeV1() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] hello = TriggerTestPayloads.helloWonderCard();
            RamScript direct = HotkeyRuntimeV1.build(rom, hello);
            TriggerBuildResult composed = TriggerComposer.compose(
                    EventTrigger.HOTKEY_RUNTIME, rom, hello
            );

            assertTrue(direct.isChecksumValid(), "hotkey runtime checksum " + rom.id());
            assertTrue(java.util.Arrays.equals(direct.bytesCopy(), composed.ramScript().bytesCopy()),
                    "trigger composer must use HotkeyRuntimeV1 " + rom.id());
            assertTrue(composed.payloadBytes() == hello.length,
                    "hotkey payload size " + rom.id());
            assertTrue(composed.totalScriptBytes() == 409,
                    "validated Hello total size " + rom.id());
            assertTrue(composed.freeScriptBytes() == 586,
                    "validated Hello free size " + rom.id());

            byte[][] payloads = {
                    new byte[] {(byte)0xB8, 0x00,0x00,0x01,0x08, 0x02},
                    new byte[64],
                    new byte[300]
            };
            payloads[1][0] = (byte)0xB8;
            payloads[1][1] = 0x00; payloads[1][2] = 0x00; payloads[1][3] = 0x01; payloads[1][4] = 0x08;
            payloads[1][63] = 0x02;
            payloads[2][0] = (byte)0xB8;
            payloads[2][1] = 0x00; payloads[2][2] = 0x00; payloads[2][3] = 0x01; payloads[2][4] = 0x08;
            payloads[2][299] = 0x02;

            for (byte[] payload : payloads) {
                int nativeOffset = (HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length + 3) & ~3;
                byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeOffset);
                long target = Binary.u32(bootstrap, 0x10);
                assertTrue(target == HotkeyRuntimeV1.VIRTUAL_BASE + nativeOffset + 1L,
                        "generic payload bootstrap target " + payload.length + " " + rom.id());
                RamScript script = HotkeyRuntimeV1.build(rom, payload);
                assertTrue(script.isChecksumValid(),
                        "generic payload checksum " + payload.length + " " + rom.id());
            }
        }
    }

    private static long readU32(byte[] data, int offset) {
        return Byte.toUnsignedLong(data[offset])
                | (Byte.toUnsignedLong(data[offset + 1]) << 8)
                | (Byte.toUnsignedLong(data[offset + 2]) << 16)
                | (Byte.toUnsignedLong(data[offset + 3]) << 24);
    }


    private static void testPersistenceProbeHelpers() {
        NativeHelper writer = PersistenceProbeNativeHelper.buildWriter(RomProfile.FIRE_RED_EN_10);
        byte[] writerCode = writer.codeCopy();
        assertTrue(Byte.toUnsignedInt(writerCode[0x0C]) == 0x04 && Byte.toUnsignedInt(writerCode[0x0D]) == 0x49,
                "persistence writer VERSION_MARKER literal load must target literal at 0x20");

        NativeHelper checker = PersistenceProbeNativeHelper.buildChecker(RomProfile.FIRE_RED_EN_10);
        byte[] checkerCode = checker.codeCopy();
        assertTrue(Byte.toUnsignedInt(checkerCode[0x10]) == 0x07 && Byte.toUnsignedInt(checkerCode[0x11]) == 0x49,
                "persistence checker VERSION_MARKER literal load must target literal at 0x30");
        assertTrue(Byte.toUnsignedInt(checkerCode[0x1E]) == 0x05 && Byte.toUnsignedInt(checkerCode[0x1F]) == 0x49,
                "persistence checker result literal load must target gSpecialVar_Result at 0x34");

        NativeHelper fullWriter = PersistenceProbeNativeHelper.buildFullWriterAt(
                RomProfile.FIRE_RED_EN_10, 0x02000100L);
        assertTrue(fullWriter.size() == 36, "400-byte persistence writer helper size");

        NativeHelper fullChecker = PersistenceProbeNativeHelper.buildFullCheckerAt(
                RomProfile.FIRE_RED_EN_10, 0x02000100L);
        byte[] fullCheckerCode = fullChecker.codeCopy();
        assertTrue(fullChecker.size() == 60, "400-byte persistence checker helper size");
        assertTrue(Binary.u32(fullCheckerCode, 0x2C) == RomProfile.FIRE_RED_EN_10.saveBlock1Ptr,
                "400-byte checker SaveBlock1 pointer literal");
        assertTrue(Binary.u32(fullCheckerCode, 0x30) == PersistenceProbeNativeHelper.STORAGE_OFFSET,
                "400-byte checker storage offset literal");
        assertTrue(Binary.u32(fullCheckerCode, 0x34) == PersistenceProbeNativeHelper.STORAGE_SIZE,
                "400-byte checker storage size literal");
        assertTrue(Binary.u32(fullCheckerCode, 0x38) == RomProfile.FIRE_RED_EN_10.specialVarResult,
                "400-byte checker result pointer literal");
        assertTrue(Byte.toUnsignedInt(fullCheckerCode[0x16]) == 0x09
                        && Byte.toUnsignedInt(fullCheckerCode[0x17]) == 0x06
                        && Byte.toUnsignedInt(fullCheckerCode[0x18]) == 0x09
                        && Byte.toUnsignedInt(fullCheckerCode[0x19]) == 0x0E,
                "400-byte checker must wrap expected pattern byte after 0xFF");

        NativeHelper sb2Writer = PersistenceSaveBlock2ProbeNativeHelper.buildWriterAt(RomProfile.FIRE_RED_EN_10, 0x02000100L);
        NativeHelper sb2Checker = PersistenceSaveBlock2ProbeNativeHelper.buildCheckerAt(RomProfile.FIRE_RED_EN_10, 0x02000100L);
        assertTrue(Binary.u32(sb2Writer.codeCopy(), 0x1C) == RomProfile.FIRE_RED_EN_10.saveBlock2Ptr, "1024-byte writer SaveBlock2 pointer literal");
        assertTrue(Binary.u32(sb2Writer.codeCopy(), 0x20) == 0x0B20, "1024-byte writer offset literal");
        assertTrue(Binary.u32(sb2Writer.codeCopy(), 0x24) == 0x400, "1024-byte writer size literal");
        assertTrue(Byte.toUnsignedInt(sb2Writer.codeCopy()[0x18]) == 0xF8
                && Byte.toUnsignedInt(sb2Writer.codeCopy()[0x19]) == 0xD1,
                "1024-byte writer loop must branch back to STRB at offset 0x0C");
        assertTrue(Binary.u32(sb2Checker.codeCopy(), 0x2C) == RomProfile.FIRE_RED_EN_10.saveBlock2Ptr, "1024-byte checker SaveBlock2 pointer literal");
        assertTrue(Binary.u32(sb2Checker.codeCopy(), 0x30) == 0x0B20, "1024-byte checker offset literal");
        assertTrue(Binary.u32(sb2Checker.codeCopy(), 0x34) == 0x400, "1024-byte checker size literal");
        assertTrue(PersistenceSaveBlock2ProbePreset.buildInstaller(RomProfile.FIRE_RED_EN_10).isChecksumValid(), "1024-byte installer checksum");
        assertTrue(PersistenceSaveBlock2ProbePreset.buildChecker(RomProfile.FIRE_RED_EN_10).isChecksumValid(), "1024-byte checker checksum");

        byte[] ptsImage = PersistentToolkitStorageV1.buildExecutionProofImage(RomProfile.FIRE_RED_EN_10);
        assertTrue(ptsImage.length == 28, "PersistentToolkitStorage V1 image size");
        assertTrue(Binary.u32(ptsImage, 0x00) == PersistentToolkitStorageV1.MAGIC, "PTS V1 magic");
        assertTrue(Byte.toUnsignedInt(ptsImage[0x04]) == 1, "PTS V1 version");
        assertTrue(Binary.u16(ptsImage, 0x08) == 0x10, "PTS V1 payload offset");
        assertTrue(Binary.u16(ptsImage, 0x0A) == 12, "PTS V1 payload size");
        byte[] ptsPayload = Arrays.copyOfRange(ptsImage, 0x10, 0x1C);
        assertTrue(Binary.u16(ptsImage, 0x0C) == PersistentToolkitStorageV1.checksum16(ptsPayload),
                "PTS V1 payload checksum");

        NativeHelper ptsInstallerHelper = PersistentToolkitStorageNativeHelper.buildInstallerAt(
                RomProfile.FIRE_RED_EN_10, 0x02000100L);
        assertTrue(ptsInstallerHelper.size() == 64, "PTS V1 installer helper size");
        assertTrue(Arrays.equals(Arrays.copyOfRange(ptsInstallerHelper.codeCopy(), 0x1C, 0x38), ptsImage),
                "PTS V1 installer embeds exact persistent image");
        assertTrue(Binary.u32(ptsInstallerHelper.codeCopy(), 0x38) == RomProfile.FIRE_RED_EN_10.saveBlock2Ptr,
                "PTS V1 installer SaveBlock2 pointer");

        NativeHelper ptsLauncherHelper = PersistentToolkitStorageNativeHelper.buildLauncherAt(
                RomProfile.FIRE_RED_EN_10, 0x02000100L);
        assertTrue(ptsLauncherHelper.size() == 120, "PTS V1 launcher helper size");
        assertTrue(Binary.u32(ptsLauncherHelper.codeCopy(), 0x68) == RomProfile.FIRE_RED_EN_10.saveBlock2Ptr,
                "PTS V1 launcher SaveBlock2 pointer");
        assertTrue(Binary.u32(ptsLauncherHelper.codeCopy(), 0x70) == PersistentToolkitStorageV1.MAGIC,
                "PTS V1 launcher magic literal");
        assertTrue(Binary.u32(ptsLauncherHelper.codeCopy(), 0x74) == RomProfile.FIRE_RED_EN_10.specialVarResult,
                "PTS V1 launcher result pointer");

        RamScript ptsInstall = PersistentToolkitStoragePreset.buildInstaller(RomProfile.FIRE_RED_EN_10);
        RamScript ptsLaunch = PersistentToolkitStoragePreset.buildLauncher(RomProfile.FIRE_RED_EN_10);
        assertTrue(ptsInstall.isChecksumValid(), "PTS V1 installer checksum");
        assertTrue(ptsLaunch.isChecksumValid(), "PTS V1 launcher checksum");

        RamScript fullInstallScript = PersistenceFullRegionProbePreset.buildInstaller(RomProfile.FIRE_RED_EN_10);
        RamScript fullCheckScript = PersistenceFullRegionProbePreset.buildChecker(RomProfile.FIRE_RED_EN_10);
        assertTrue(fullInstallScript.isChecksumValid(), "400-byte persistence installer checksum");
        assertTrue(fullCheckScript.isChecksumValid(), "400-byte persistence checker checksum");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void testPayloadPlacementPlanner() {
        byte[] small = new byte[32];
        byte[] medium = new byte[300];
        byte[] large = new byte[900];

        assertTrue(PayloadPlacementPlanner.place(small, PayloadPlacement.AUTO, 100).area() == PayloadStorageArea.RAMSCRIPT,
                "AUTO should prefer remaining RamScript space");
        assertTrue(PayloadPlacementPlanner.place(medium, PayloadPlacement.AUTO, 100).area() == PayloadStorageArea.SAVE_BLOCK1,
                "AUTO should use SaveBlock1 for a small persistent spill");
        assertTrue(PayloadPlacementPlanner.place(large, PayloadPlacement.AUTO, 100).area() == PayloadStorageArea.SAVE_BLOCK2,
                "AUTO should use SaveBlock2 for a larger persistent spill");

        assertTrue(PayloadPlacementPlanner.place(small, PayloadPlacement.SAVE_BLOCK2, 995).area() == PayloadStorageArea.SAVE_BLOCK2,
                "explicit placement should be honored");

        boolean failed = false;
        try {
            PayloadPlacementPlanner.place(new byte[401], PayloadPlacement.SAVE_BLOCK1, 995);
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        assertTrue(failed, "explicit SaveBlock1 overflow must fail");
    }

    private static void testPersistentStorageAreaModel() {
        assertTrue(PayloadStorageArea.SAVE_BLOCK1.capacity() == 400, "SaveBlock1 storage capacity");
        assertTrue(PayloadStorageArea.SAVE_BLOCK1.offset() == 0x348C, "SaveBlock1 storage offset");
        assertTrue(PayloadStorageArea.SAVE_BLOCK2.capacity() == 0x400, "SaveBlock2 storage capacity");
        assertTrue(PayloadStorageArea.SAVE_BLOCK2.offset() == 0x0B20, "SaveBlock2 storage offset");
        assertTrue(PayloadStorageArea.SAVE_BLOCK1.persistent(), "SaveBlock1 area should be persistent");
        assertTrue(PayloadStorageArea.SAVE_BLOCK2.persistent(), "SaveBlock2 area should be persistent");
        assertTrue(PayloadStorageArea.RAMSCRIPT.persistent(), "RamScript survives reset while its Wonder Card remains installed");
        assertTrue(PayloadStorageArea.SAVE_BLOCK1.pointerAddress(RomProfile.FIRE_RED_EN_10) == RomProfile.FIRE_RED_EN_10.saveBlock1Ptr,
                "SaveBlock1 pointer source");
        assertTrue(PayloadStorageArea.SAVE_BLOCK2.pointerAddress(RomProfile.FIRE_RED_EN_10) == RomProfile.FIRE_RED_EN_10.saveBlock2Ptr,
                "SaveBlock2 pointer source");
    }

}
