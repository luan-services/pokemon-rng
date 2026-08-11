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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
