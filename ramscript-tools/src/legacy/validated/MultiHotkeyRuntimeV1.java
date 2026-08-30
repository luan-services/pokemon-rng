import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/*
   Experimental two-payload shared runtime derived from HotkeyRuntimeV1.

   This is the replacement for the rejected first multi-hotkey experiment.
   The rejected build expanded the 32-byte wrapper at 03005310 across live
   gCanvas* IWRAM and caused immediate field corruption in FR1.0.

   Safety rule for this candidate:
   - never grow a resident block through a live symbol;
   - use only the same validated Runtime-v1 blocks plus linker padding that is
     identical in all four supplied FR/LG symbol maps;
   - use gSaveUnusedVar only as a 2-byte part of a 4-byte dispatch table. The
     source declares it but never reads or writes it anywhere else.

   Current V1 scope is intentionally narrow: two chords must share the same
   held button and their newly-pressed buttons must be adjacent GBA key bits.
   This covers the first real combo, R+SELECT and R+B, without inventing a
   larger resident dispatcher.

   Dispatch flow:
     wrapper extracts the two adjacent new-key bits -> r1 index
     4-byte table maps that index to the selected RamScript payload offset
     stage1 saves r1 on its existing stack frame
     GetSavedRamScriptIfValid resolves the CURRENT RamScript
     stage2 reloads the selected offset from the stack and adds it to r0
     ScriptContext_SetupScript starts that payload

   No persistent physical RamScript pointer is stored.
*/
/**
 * @deprecated Validated historical two-binding runtime. Superseded for all new
 * builds by SharedHotkeyRuntime. Retained byte-for-byte for reproducibility and
 * explicit legacy Seed+Repel builds; do not modify or auto-select it.
 */
@Deprecated
final class MultiHotkeyRuntimeV1 {
    static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    static final int HEADER_SIZE = HotkeyRuntimeV1.HEADER_SIZE;
    static final int SIGNATURE_OFFSET = HotkeyRuntimeV1.SIGNATURE_OFFSET;
    static final int FIRST_PAYLOAD_OFFSET = HotkeyRuntimeV1.PAYLOAD_OFFSET;
    static final int FORMAT_SIGNATURE = HotkeyRuntimeV1.FORMAT_SIGNATURE;

    // Verified identical gaps / unused storage in all four supplied .sym files.
    // 0300504C..0300504F: linker padding after VMap.
    // 03005356..0300535F: linker padding after gCanvasPaletteStart.
    // 0300539C..0300539D: gSaveUnusedVar; 0300539E..0300539F: linker padding.
    private static final long SETUP_SCRIPT_LITERAL = 0x0300504CL;
    private static final long MULTI_LITERAL_POOL = 0x03005356L;
    private static final long OFFSET_TABLE = 0x0300539CL;

    private static final int NATIVE_CODE_AND_LITERALS_SIZE = 56;
    private static final int BLOCK_COUNT = 15;
    private static final int TABLE_SIZE = BLOCK_COUNT * 4;

    private MultiHotkeyRuntimeV1() {}

    static TriggerBuildResult compose(RomProfile rom, HotkeyPayload first, HotkeyPayload second) {
        if (rom == null) throw new IllegalArgumentException("rom must not be null");
        if (first == null || second == null) throw new IllegalArgumentException("hotkey payloads must not be null");
        validateHotkeys(first.hotkey(), second.hotkey());

        byte[] firstPayload = first.payload();
        byte[] secondPayload = second.payload();
        int firstOffset = FIRST_PAYLOAD_OFFSET;
        int secondOffset = firstOffset + firstPayload.length;

        // The compact resident wrapper maps key pairs directly to u8 offsets.
        if (firstOffset > 0xFF || secondOffset > 0xFF) {
            throw new IllegalArgumentException(
                    "multi-hotkey V1 payload offsets must fit in u8; second payload starts at +0x"
                            + Integer.toHexString(secondOffset).toUpperCase()
            );
        }

        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(
                rom, first.hotkey(), second.hotkey(), firstOffset, secondOffset
        );
        int residentBytes = totalResidentBytes(blocks);
        byte[] nativeBlob = nativeInstallerBlob(blocks, residentBytes);

        int afterPayloads = secondOffset + secondPayload.length;
        int nativeBlobOffset = align4(afterPayloads);
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "Multi-hotkey Runtime v1 requires " + total + " bytes; maximum is "
                            + RamScript.SCRIPT_SIZE
            );
        }

        byte[] script = new byte[total];
        int p = 0;

        script[p++] = (byte)0xB8; // setvaddress
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        script[p++] = (byte)0xB9; // vgoto installer
        putU32(script, p, VIRTUAL_BASE + fieldInstallerOffset);
        p += 4;

        if (p != SIGNATURE_OFFSET) throw new IllegalStateException("multi runtime signature offset mismatch");
        script[p++] = (byte)(FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((FORMAT_SIGNATURE >>> 8) & 0xFF);

        if (p != firstOffset) throw new IllegalStateException("multi runtime first payload offset mismatch");
        System.arraycopy(firstPayload, 0, script, p, firstPayload.length);
        p += firstPayload.length;

        if (p != secondOffset) throw new IllegalStateException("multi runtime second payload offset mismatch");
        System.arraycopy(secondPayload, 0, script, p, secondPayload.length);
        p += secondPayload.length;

        while (p < nativeBlobOffset) script[p++] = 0;
        if ((nativeBlobOffset & 3) != 0) throw new IllegalStateException("multi runtime native blob alignment mismatch");

        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length);
        p += nativeBlob.length;

        if (p != fieldInstallerOffset) throw new IllegalStateException("multi runtime installer offset mismatch");
        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        RamScript ramScript = RamScript.createWonderCard(script);
        int payloadBytes = firstPayload.length + secondPayload.length;
        return new TriggerBuildResult(
                ramScript,
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                payloadBytes,
                total - payloadBytes,
                total,
                RamScript.SCRIPT_SIZE - total
        );
    }

    static int firstPayloadOffset() {
        return FIRST_PAYLOAD_OFFSET;
    }

    static int secondPayloadOffset(byte[] firstPayload) {
        if (firstPayload == null) throw new IllegalArgumentException("first payload must not be null");
        return FIRST_PAYLOAD_OFFSET + firstPayload.length;
    }

    private static void validateHotkeys(Hotkey first, Hotkey second) {
        if (first == null || second == null) throw new IllegalArgumentException("multi-hotkey bindings must not be null");
        if (first.equals(second)) throw new IllegalArgumentException("multi-hotkey bindings must use different hotkeys");
        if (first.heldButton() != second.heldButton()) {
            throw new IllegalArgumentException(
                    "multi-hotkey V1 requires both chords to share the same held button"
            );
        }
        int a = first.pressedButton().bit();
        int b = second.pressedButton().bit();
        if (Math.abs(a - b) != 1) {
            throw new IllegalArgumentException(
                    "multi-hotkey V1 requires adjacent newly-pressed button bits"
            );
        }
    }

    private static List<RuntimeV1ResidentBlocks.Block> residentBlocks(
            RomProfile rom,
            Hotkey firstHotkey,
            Hotkey secondHotkey,
            int firstOffset,
            int secondOffset
    ) {
        List<RuntimeV1ResidentBlocks.Block> blocks = new ArrayList<>();

        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_TAIL,
                new byte[] { 0x02, 0x4B, 0x18, 0x47 }
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_LITERAL,
                le32(rom.originalVBlankThumb)
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.SUPERVISOR,
                supervisor()
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS,
                supervisorLiterals(rom)
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.STAGE2,
                multiStage2()
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                SETUP_SCRIPT_LITERAL,
                le32(rom.scriptContextSetupThumb)
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.STAGE1,
                multiStage1()
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.PRIMARY_THUNK,
                new byte[] { 0x00, 0x4C, 0x20, 0x47 }
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.FUNCTION_LITERAL,
                le32(rom.getSavedRamScriptThumb)
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.MARKER,
                new byte[] { 0x00 }
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                MULTI_LITERAL_POOL,
                multiLiteralPool(rom)
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                OFFSET_TABLE,
                offsetTable(firstHotkey, secondHotkey, firstOffset, secondOffset)
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.SAFETY_GATE,
                multiSafetyGate()
        ));
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.FORMAT_VALIDATOR,
                validator()
        ));

        // Wrapper stays last because the temporary bootstrap initially occupies
        // exactly 03005310..03005323 and execution has left it before copying.
        blocks.add(new RuntimeV1ResidentBlocks.Block(
                RuntimeV1ResidentBlocks.WRAPPER,
                multiWrapper(rom, firstHotkey, secondHotkey)
        ));

        if (blocks.size() != BLOCK_COUNT) throw new IllegalStateException("two-hotkey runtime block count mismatch");
        return List.copyOf(blocks);
    }

    private static byte[] nativeInstallerBlob(List<RuntimeV1ResidentBlocks.Block> blocks, int residentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Same validated byte-copy / atomic-hook installer structure as Runtime v1.
        // Only the table-data ADR and block count change because this candidate
        // has three additional small resident blocks.
        byte[] codeAndLiterals = new byte[] {
                (byte)0xF0,(byte)0xB4,
                0x0D,(byte)0xA4,
                0x1B,(byte)0xA6,             // data starts at blob+116 (56 + 60)
                0x03,0x27,
                0x3F,0x06,
                0x0F,0x25,                   // 15 resident blocks
                0x21,(byte)0x88,
                0x62,(byte)0x88,
                0x04,0x34,
                (byte)0xC9,0x19,
                0x33,0x78,
                0x0B,0x70,
                0x01,0x36,
                0x01,0x31,
                0x01,0x3A,
                (byte)0xF9,(byte)0xD1,
                0x01,0x3D,
                (byte)0xF3,(byte)0xD1,
                0x02,0x48,
                0x03,0x49,
                0x01,0x60,
                (byte)0xF0,(byte)0xBC,
                0x70,0x47,
                (byte)0xC0,0x46,
                0x50,0x35,0x00,0x03,
                0x43,0x3F,0x00,0x03
        };

        if (codeAndLiterals.length != NATIVE_CODE_AND_LITERALS_SIZE) {
            throw new IllegalStateException("multi runtime native code size mismatch");
        }
        out.writeBytes(codeAndLiterals);

        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            long address = block.address();
            if ((address & 0xFFFF0000L) != 0x03000000L) {
                throw new IllegalStateException("multi runtime block outside IWRAM");
            }
            u16(out, (int)(address & 0xFFFF));
            u16(out, block.data().length);
        }

        for (RuntimeV1ResidentBlocks.Block block : blocks) out.writeBytes(block.data());

        byte[] blob = out.toByteArray();
        int expected = NATIVE_CODE_AND_LITERALS_SIZE + TABLE_SIZE + residentBytes;
        if (blob.length != expected) {
            throw new IllegalStateException("multi runtime native blob expected " + expected + ", got " + blob.length);
        }
        return blob;
    }

    private static byte[] multiWrapper(RomProfile rom, Hotkey first, Hotkey second) {
        int lowPressed = Math.min(first.pressedButton().bit(), second.pressedButton().bit());
        int highPressed = Math.max(first.pressedButton().bit(), second.pressedButton().bit());
        int extractLeftShift = 15 - highPressed;

        // Exactly the original validated 32-byte wrapper allocation.
        // Code is 26 bytes, followed by a 2-byte NOP and the heldKeysRaw literal.
        byte[] out = new byte[] {
                0x06,0x48,                   // ldr  r0, heldKeysRaw @ +0x1C
                0x00,0x68,                   // ldr  r0, [r0]
                0x00,0x00,                   // lsrs r2,r0,#heldBit+1
                0x06,(byte)0xD3,             // bcc  fail
                0x00,0x00,                   // lsls r1,r0,#extractLeftShift
                (byte)0x89,0x0F,             // lsrs r1,r1,#30 ; Z if neither pressed
                0x03,(byte)0xD0,             // beq  fail
                0x1F,(byte)0xA2,             // adr  r2,0300539C offset table
                0x51,0x5C,                   // ldrb r1,[r2,r1]
                0x0D,0x4A,                   // ldr  r2,03005358 lock flag ptr
                (byte)0x86,(byte)0xE0,       // b    03005434 safety gate
                0x0D,0x4B,                   // fail: ldr r3,0300535C cb1 ptr
                0x18,0x47,                   // bx   r3
                (byte)0xC0,0x46,             // nop / literal alignment
                0,0,0,0                      // heldKeysRaw
        };

        if (out.length != 32) throw new IllegalStateException("multi wrapper must remain exactly 32 bytes");
        putU16(out, 0x04, thumbLsrsImm(2, 0, first.heldButton().bit() + 1));
        putU16(out, 0x08, thumbLslsImm(1, 0, extractLeftShift));
        putU32(out, 0x1C, rom.heldKeysRaw);

        // Validation guarantees these are adjacent; keep the local variables to
        // make the bit-extraction assumption explicit to future readers.
        if (highPressed - lowPressed != 1) throw new IllegalStateException("pressed buttons stopped being adjacent");
        return out;
    }

    private static byte[] multiLiteralPool(RomProfile rom) {
        byte[] out = new byte[10];
        // 03005356..57 stays padding so both words are naturally aligned.
        putU32(out, 2, rom.lockFieldControls); // 03005358
        putU32(out, 6, rom.cb1OverworldThumb); // 0300535C
        return out;
    }

    private static byte[] offsetTable(
            Hotkey first,
            Hotkey second,
            int firstOffset,
            int secondOffset
    ) {
        byte[] table = new byte[4];
        int lowBit = Math.min(first.pressedButton().bit(), second.pressedButton().bit());
        int firstIndex = first.pressedButton().bit() == lowBit ? 1 : 2;
        int secondIndex = second.pressedButton().bit() == lowBit ? 1 : 2;
        table[0] = 0;
        table[firstIndex] = (byte)firstOffset;
        table[secondIndex] = (byte)secondOffset;
        table[3] = (byte)firstOffset; // simultaneous press: deterministic first-binding priority
        return table;
    }

    private static byte[] multiStage1() {
        // r1 is the selected payload offset. Save it in the existing stack frame
        // while GetSavedRamScriptIfValid is allowed to clobber caller-saved regs.
        return new byte[] {
                0x12,(byte)0xB5,             // push {r1,r4,lr}
                (byte)0xFE,(byte)0xF7,(byte)0x88,(byte)0xFF,
                0x00,0x28,
                0x00,(byte)0xD0,
                (byte)0x8C,(byte)0xE1,
                0x12,(byte)0xBD              // pop {r1,r4,pc}
        };
    }

    private static byte[] multiStage2() {
        // Same 14-byte linker-padding allocation as Runtime v1. The setup-script
        // literal moved into the verified 0300504C..4F gap, which saves enough
        // instructions to make dynamic payload-offset dispatch size-neutral here.
        return new byte[] {
                0x00,(byte)0x99,             // ldr  r1,[sp]
                0x40,0x18,                   // adds r0,r0,r1
                0x05,0x4C,                   // ldr  r4,0300504C ScriptContext_SetupScript
                (byte)0xFE,(byte)0xF7,(byte)0xAF,(byte)0xFF, // bl 03003F9A (bx r4)
                0x27,(byte)0xE0,             // b   0300508E cleanup
                (byte)0xC0,0x46              // nop
        };
    }

    static byte[] safetyGateBytesForTest() { return multiSafetyGate(); }

    private static byte[] multiSafetyGate() {
        // Multi-hotkey wrapper is 2 bytes longer in code than the original
        // single-hotkey wrapper, so its CB1_Overworld return thunk starts at
        // 03005326 (not 03005324). The locked-input branch must target 03005326;
        // targeting 03005324 loops back into the safety gate and freezes the game.
        return new byte[] {
                0x10,0x78,
                0x00,0x28,
                0x00,(byte)0xD0,
                0x74,(byte)0xE7,
                0x21,(byte)0xE6,
                0x12,(byte)0xBD              // validator-failure cleanup: pop {r1,r4,pc}
        };
    }

    private static byte[] validator() {
        return new byte[] {
                0x41,(byte)0x89,
                (byte)0xA7,0x29,
                0x47,(byte)0xD1,
                0x40,(byte)0xE6
        };
    }

    private static byte[] supervisor() {
        return new byte[] {
                0x18,(byte)0xA3, 0x07,(byte)0xCB, 0x03,0x68, (byte)0x8B,0x42,
                (byte)0xB3,(byte)0xD1, 0x02,0x60, (byte)0xB1,(byte)0xE7
        };
    }

    private static byte[] supervisorLiterals(RomProfile rom) {
        byte[] out = new byte[12];
        putU32(out, 0, 0x030030F0L);
        putU32(out, 4, rom.cb1OverworldThumb);
        putU32(out, 8, RuntimeV1ResidentBlocks.WRAPPER | 1L);
        return out;
    }

    private static int totalResidentBytes(List<RuntimeV1ResidentBlocks.Block> blocks) {
        int total = 0;
        for (RuntimeV1ResidentBlocks.Block block : blocks) total += block.data().length;
        return total;
    }

    private static int thumbLsrsImm(int rd, int rm, int shift) {
        if (shift < 1 || shift > 31) throw new IllegalArgumentException("Thumb LSRS immediate must be 1..31");
        return 0x0800 | (shift << 6) | (rm << 3) | rd;
    }

    private static int thumbLslsImm(int rd, int rm, int shift) {
        if (shift < 0 || shift > 31) throw new IllegalArgumentException("Thumb LSLS immediate must be 0..31");
        return (shift << 6) | (rm << 3) | rd;
    }

    private static int align4(int value) {
        return (value + 3) & ~3;
    }

    private static byte[] le32(long value) {
        byte[] out = new byte[4];
        putU32(out, 0, value);
        return out;
    }

    private static void u16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
