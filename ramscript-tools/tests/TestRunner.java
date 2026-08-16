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
        testCompactInstallerCandidate1();
        testCompactInstallerCandidate2();
        testCompactInstallerCandidate2a();
        testCompactRuntimeCandidate3();
        testCompactRuntimeCandidate4();
        testCompactRuntimeCandidate5a();

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


    private static void testCompactInstallerCandidate1() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] blob = CompactInstallerCandidate1.supervisorBlob();
            byte[] copier = CompactInstallerCandidate1.copierBytes(rom);

            assertTrue(blob.length == 14, "compact C1 supervisor blob size " + rom.id());
            assertTrue(copier.length == 40, "compact C1 copier size " + rom.id());

            int push = ((copier[1] & 0xFF) << 8) | (copier[0] & 0xFF);
            assertTrue(push == 0xB500, "compact C1 push lr " + rom.id());

            assertTrue(Binary.u32(copier, 0x20) == rom.getSavedRamScriptThumb,
                    "compact C1 GetSaved literal " + rom.id());
            assertTrue(Binary.u32(copier, 0x24) == 0x03003F42L,
                    "compact C1 destination literal " + rom.id());

            byte[] expected = new byte[] {
                    0x18,(byte)0xA3, 0x07,(byte)0xCB, 0x03,0x68, (byte)0x8B,0x42,
                    (byte)0xB3,(byte)0xD1, 0x02,0x60, (byte)0xB1,(byte)0xE7
            };
            assertTrue(java.util.Arrays.equals(expected, blob),
                    "compact C1 supervisor blob equality " + rom.id());

            RamScript script = CompactInstallerCandidate1.build(rom);
            assertTrue(script.isChecksumValid(), "compact C1 checksum " + rom.id());
            assertTrue(CompactInstallerCandidate1.scriptSize(rom) < 400,
                    "compact C1 should stay below 400 bytes " + rom.id());
        }
    }


    private static void testCompactInstallerCandidate2() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] bootstrap = CompactInstallerCandidate2.bootstrapBytes(rom);
            byte[] blob = CompactInstallerCandidate2.nativeInstallerBlob(rom);
            java.util.List<RuntimeV1ResidentBlocks.Block> blocks =
                    RuntimeV1ResidentBlocks.build(rom);

            assertTrue(bootstrap.length == 32,
                    "compact C2 bootstrap size " + rom.id());
            assertTrue(blob.length == 211,
                    "compact C2 native blob size " + rom.id());
            assertTrue(blocks.size() == 12,
                    "compact C2 resident block count " + rom.id());
            assertTrue(RuntimeV1ResidentBlocks.totalResidentBytes(rom) == 123,
                    "compact C2 resident data bytes " + rom.id());

            // Bootstrap literal must be profile-specific GetSavedRamScriptIfValid|1.
            assertTrue(Binary.u32(bootstrap, 0x1C) == rom.getSavedRamScriptThumb,
                    "compact C2 GetSaved literal " + rom.id());

            // Independently assembled bootstrap opcodes for:
            // ldrh r1,[r0,#6], add r0,r0,r1, sub #211, bx r0.
            assertTrue((((bootstrap[13] & 0xFF) << 8) | (bootstrap[12] & 0xFF)) == 0x88C1,
                    "compact C2 installer-offset LDRH " + rom.id());
            assertTrue((((bootstrap[15] & 0xFF) << 8) | (bootstrap[14] & 0xFF)) == 0x1840,
                    "compact C2 script+offset ADD " + rom.id());
            assertTrue((((bootstrap[17] & 0xFF) << 8) | (bootstrap[16] & 0xFF)) == 0x38D3,
                    "compact C2 subtract blob size " + rom.id());

            // Native blob code/table boundary and record metadata.
            assertTrue((((blob[1] & 0xFF) << 8) | (blob[0] & 0xFF)) == 0xB4F0,
                    "compact C2 native push " + rom.id());

            int table = CompactInstallerCandidate2.NATIVE_CODE_SIZE;
            int data = CompactInstallerCandidate2.NATIVE_CODE_SIZE
                    + CompactInstallerCandidate2.TABLE_SIZE;

            int cursor = data;
            for (int i = 0; i < blocks.size(); i++) {
                RuntimeV1ResidentBlocks.Block block = blocks.get(i);
                int record = table + i * 4;
                int destinationLow = (blob[record] & 0xFF)
                        | ((blob[record + 1] & 0xFF) << 8);
                int size = (blob[record + 2] & 0xFF)
                        | ((blob[record + 3] & 0xFF) << 8);

                assertTrue(destinationLow == (int)(block.address() & 0xFFFF),
                        "compact C2 table destination " + i + " " + rom.id());
                assertTrue(size == block.data().length,
                        "compact C2 table size " + i + " " + rom.id());

                for (int j = 0; j < size; j++) {
                    assertTrue(blob[cursor + j] == block.data()[j],
                            "compact C2 block data " + i + ":" + j + " " + rom.id());
                }
                cursor += size;
            }
            assertTrue(cursor == blob.length,
                    "compact C2 data consumes entire blob " + rom.id());

            // Wrapper must be last, so bootstrap self-overwrite happens only
            // after the copier has moved execution into the RamScript blob.
            RuntimeV1ResidentBlocks.Block last = blocks.get(blocks.size() - 1);
            assertTrue(last.address() == 0x03005310L && last.data().length == 32,
                    "compact C2 wrapper-last invariant " + rom.id());

            RamScript script = CompactInstallerCandidate2.build(rom);
            assertTrue(script.isChecksumValid(),
                    "compact C2 checksum " + rom.id());
            assertTrue(CompactInstallerCandidate2.scriptSize(rom) < 500,
                    "compact C2 full copy should fit below 500 bytes " + rom.id());
        }
    }


    private static void testCompactInstallerCandidate2a() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] blob = CompactInstallerCandidate2a.nativeInstallerBlob(rom);
            byte[] bootstrap = CompactInstallerCandidate2a.bootstrapBytes(rom);

            assertTrue(CompactInstallerCandidate2a.NATIVE_BLOB_OFFSET == 0x0C,
                    "compact C2a blob offset must be 4-byte aligned relative to script " + rom.id());
            assertTrue((0x3624 + CompactInstallerCandidate2a.NATIVE_BLOB_OFFSET) % 4 == 0,
                    "compact C2a physical SaveBlock-relative blob alignment " + rom.id());

            // With blob base B aligned to 4:
            // ADR r4 at B+2 uses Align(B+6,4)+36 = B+40 (table).
            // ADR r6 at B+4 uses Align(B+8,4)+80 = B+88 (data).
            long syntheticBase = 0x0200000CL;
            long tableTarget = (syntheticBase + 4) + 36;
            long dataTarget = (syntheticBase + 8) + 80;
            assertTrue(tableTarget == syntheticBase + CompactInstallerCandidate2a.NATIVE_CODE_SIZE,
                    "compact C2a ADR table target " + rom.id());
            assertTrue(dataTarget == syntheticBase
                            + CompactInstallerCandidate2a.NATIVE_CODE_SIZE
                            + CompactInstallerCandidate2a.TABLE_SIZE,
                    "compact C2a ADR data target " + rom.id());

            assertTrue(blob.length == 211, "compact C2a blob size " + rom.id());
            assertTrue(bootstrap.length == 32, "compact C2a bootstrap size " + rom.id());

            RamScript script = CompactInstallerCandidate2a.build(rom);
            assertTrue(script.isChecksumValid(), "compact C2a checksum " + rom.id());
            assertTrue(CompactInstallerCandidate2a.scriptSize(rom) == 421,
                    "compact C2a exact script size " + rom.id());
            assertTrue(CompactInstallerCandidate2a.freeBytes(rom) == 574,
                    "compact C2a free bytes " + rom.id());
        }
    }


    private static void testCompactRuntimeCandidate3() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] bootstrap = CompactRuntimeCandidate3.bootstrapBytes(rom);
            byte[] blob = CompactRuntimeCandidate3.nativeInstallerBlob(rom);
            byte[] payload = TriggerTestPayloads.helloWonderCard();

            assertTrue(payload.length == 43,
                    "compact C3 hello payload size " + rom.id());
            assertTrue(CompactRuntimeCandidate3.PAYLOAD_OFFSET == 0x0C,
                    "compact C3 runtime payload offset " + rom.id());
            assertTrue(CompactRuntimeCandidate3.nativeBlobOffset() == 0x38,
                    "compact C3 aligned native blob offset " + rom.id());
            assertTrue(CompactRuntimeCandidate3.alignmentPadding() == 1,
                    "compact C3 hello alignment padding " + rom.id());
            assertTrue((0x3624 + CompactRuntimeCandidate3.nativeBlobOffset()) % 4 == 0,
                    "compact C3 physical native blob alignment " + rom.id());

            assertTrue(bootstrap.length == 32,
                    "compact C3 bootstrap size " + rom.id());
            assertTrue(Binary.u32(bootstrap, 0x1C) == rom.getSavedRamScriptThumb,
                    "compact C3 GetSaved literal " + rom.id());
            assertTrue((((bootstrap[17] & 0xFF) << 8) | (bootstrap[16] & 0xFF)) == 0x38E3,
                    "compact C3 subtract 227 " + rom.id());

            assertTrue(blob.length == 227,
                    "compact C3 native blob size " + rom.id());

            // Independently assembled activation sequence at blob + 0x24.
            assertTrue((((blob[0x25] & 0xFF) << 8) | (blob[0x24] & 0xFF)) == 0x4802,
                    "compact C3 ldr VBlank slot " + rom.id());
            assertTrue((((blob[0x27] & 0xFF) << 8) | (blob[0x26] & 0xFF)) == 0x4903,
                    "compact C3 ldr supervisor ptr " + rom.id());
            assertTrue((((blob[0x29] & 0xFF) << 8) | (blob[0x28] & 0xFF)) == 0x6001,
                    "compact C3 atomic VBlank str " + rom.id());
            assertTrue(Binary.u32(blob, 0x30) == 0x03003550L,
                    "compact C3 VBlank slot literal " + rom.id());
            assertTrue(Binary.u32(blob, 0x34) == 0x03003F43L,
                    "compact C3 supervisor literal " + rom.id());

            // The compact payload data must still be the exact C2a resident image.
            int data = CompactRuntimeCandidate3.NATIVE_CODE_AND_LITERALS_SIZE
                    + CompactRuntimeCandidate3.TABLE_SIZE;
            int cursor = data;
            java.util.List<RuntimeV1ResidentBlocks.Block> blocks =
                    RuntimeV1ResidentBlocks.build(rom);
            for (RuntimeV1ResidentBlocks.Block block : blocks) {
                for (int j = 0; j < block.data().length; j++) {
                    assertTrue(blob[cursor + j] == block.data()[j],
                            "compact C3 resident byte mismatch " + rom.id());
                }
                cursor += block.data().length;
            }
            assertTrue(cursor == blob.length,
                    "compact C3 resident data consumes blob " + rom.id());

            RamScript script = CompactRuntimeCandidate3.build(rom);
            assertTrue(script.isChecksumValid(),
                    "compact C3 checksum " + rom.id());
            assertTrue(CompactRuntimeCandidate3.scriptSize(rom) == 481,
                    "compact C3 exact script size " + rom.id());
            assertTrue(CompactRuntimeCandidate3.runtimeOverhead(rom) == 438,
                    "compact C3 runtime overhead " + rom.id());
            assertTrue(CompactRuntimeCandidate3.freeBytes(rom) == 514,
                    "compact C3 free bytes " + rom.id());
        }
    }


    private static void testCompactRuntimeCandidate4() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] bootstrap = CompactRuntimeCandidate4.bootstrapBytes(rom);
            byte[] blob = CompactRuntimeCandidate4.nativeInstallerBlob(rom);

            assertTrue(bootstrap.length == 20,
                    "compact C4 bootstrap size " + rom.id());
            assertTrue(Binary.u32(bootstrap, 0x0C) == 0x020370A8L,
                    "compact C4 sAddressOffset literal " + rom.id());

            long expectedVirtualThumb =
                    0x08010000L + CompactRuntimeCandidate4.nativeBlobOffset() + 1L;
            assertTrue(Binary.u32(bootstrap, 0x10) == expectedVirtualThumb,
                    "compact C4 virtual blob literal " + rom.id());

            // Exact independently assembled opcodes.
            assertTrue((((bootstrap[1] & 0xFF) << 8) | (bootstrap[0] & 0xFF)) == 0x4802,
                    "compact C4 ldr offset ptr " + rom.id());
            assertTrue((((bootstrap[3] & 0xFF) << 8) | (bootstrap[2] & 0xFF)) == 0x6800,
                    "compact C4 ldr offset " + rom.id());
            assertTrue((((bootstrap[5] & 0xFF) << 8) | (bootstrap[4] & 0xFF)) == 0x4902,
                    "compact C4 ldr virtual target " + rom.id());
            assertTrue((((bootstrap[7] & 0xFF) << 8) | (bootstrap[6] & 0xFF)) == 0x1A09,
                    "compact C4 subtract relocation " + rom.id());
            assertTrue((((bootstrap[9] & 0xFF) << 8) | (bootstrap[8] & 0xFF)) == 0x4708,
                    "compact C4 bx target " + rom.id());

            // C4 keeps the same 227-byte compact runtime blob as C3 except
            // the return instruction: pop {pc} -> bx lr.
            assertTrue(blob.length == CompactRuntimeCandidate3.NATIVE_BLOB_SIZE,
                    "compact C4 blob size " + rom.id());

            // Find the native return tail inside the 56-byte code/literal area.
            int tail = -1;
            for (int i = 0; i <= 52; i++) {
                if ((blob[i] & 0xFF) == 0xF0
                        && (blob[i + 1] & 0xFF) == 0xBC
                        && (blob[i + 2] & 0xFF) == 0x70
                        && (blob[i + 3] & 0xFF) == 0x47) {
                    tail = i;
                    break;
                }
            }
            assertTrue(tail >= 0, "compact C4 native tail bx lr " + rom.id());

            // C3 and C4 native blobs must otherwise be byte-identical,
            // except for the two-byte return instruction at tail+2.
            byte[] c3Blob = CompactRuntimeCandidate3.nativeInstallerBlob(rom);
            assertTrue(c3Blob.length == blob.length,
                    "compact C4/C3 blob length equality " + rom.id());
            for (int i = 0; i < blob.length; i++) {
                if (i == tail + 2 || i == tail + 3) {
                    continue;
                }
                assertTrue(blob[i] == c3Blob[i],
                        "compact C4/C3 blob unchanged at " + i + " " + rom.id());
            }
            assertTrue((c3Blob[tail + 2] & 0xFF) == 0x00
                            && (c3Blob[tail + 3] & 0xFF) == 0xBD,
                    "compact C3 old pop pc tail " + rom.id());

            RamScript script = CompactRuntimeCandidate4.build(rom);
            assertTrue(script.isChecksumValid(),
                    "compact C4 checksum " + rom.id());

            assertTrue(CompactRuntimeCandidate4.fieldInstallerSize(rom) == 126,
                    "compact C4 field installer exact size " + rom.id());
            assertTrue(CompactRuntimeCandidate4.scriptSize(rom) == 409,
                    "compact C4 exact script size " + rom.id());
            assertTrue(RamScript.SCRIPT_SIZE - CompactRuntimeCandidate4.scriptSize(rom) == 586,
                    "compact C4 free bytes " + rom.id());
        }
    }

    private static void testCompactRuntimeCandidate5a() {
        for (RomProfile rom : RomProfile.values()) {
            RamScript c4 = CompactRuntimeCandidate4.build(rom);
            RamScript c5a = CompactRuntimeCandidate5a.build(rom);
            assertTrue(c4.isChecksumValid(), "compact C4 reference checksum " + rom.id());
            assertTrue(c5a.isChecksumValid(), "compact C5a checksum " + rom.id());

            byte[] a = c4.bytesCopy();
            byte[] b = c5a.bytesCopy();
            assertTrue(a.length == b.length, "compact C5a/C4 total length " + rom.id());
            for (int i = 0; i < a.length; i++) {
                assertTrue(a[i] == b[i], "compact C5a Hello byte-identical to C4 at " + i + " " + rom.id());
            }

            byte[] shortPayload = new byte[] {(byte)0xB8,0,0,1,8,0x02};
            int nativeOffset = ((CompactRuntimeCandidate5a.PAYLOAD_OFFSET + shortPayload.length + 3) / 4) * 4;
            byte[] boot = CompactRuntimeCandidate5a.bootstrapBytes(rom, nativeOffset);
            long target = Binary.u32(boot, 0x10);
            assertTrue(target == CompactRuntimeCandidate5a.VIRTUAL_BASE + nativeOffset + 1L,
                    "compact C5a generic payload target " + rom.id());
            RamScript generic = CompactRuntimeCandidate5a.build(rom, shortPayload);
            assertTrue(generic.isChecksumValid(), "compact C5a generic checksum " + rom.id());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
