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

        testHotkeyRuntimeV1();

        System.out.println("All tests passed.");
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
