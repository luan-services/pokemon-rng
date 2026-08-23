import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
   Validated shared runtime for up to eight hotkeys with one common modifier
   (R or L) and pressed buttons from the low byte of GBA key input.

   callback1 stays lightweight: recognize/select the binding, apply the safety
   gate and defer functional work to the normal Field Script engine. N-way
   dispatch is performed later from RamScript+0x0C.
*/
final class SharedHotkeyRuntime {
    static final int PAYLOAD_OFFSET = HotkeyRuntimeV1.PAYLOAD_OFFSET;
    static final long SELECTED_KEY_BYTE = SharedHotkeyDispatcher.SELECTED_KEY_BYTE;

    private static final long SHARED_LITERAL_POOL = 0x03005356L;
    private static final int NATIVE_CODE_AND_LITERALS_SIZE = 56;
    private static final int BLOCK_COUNT = 13;
    private static final int TABLE_SIZE = BLOCK_COUNT * 4;

    private SharedHotkeyRuntime() {}

    static TriggerBuildResult compose(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries
    ) {
        return compose(rom, modifier, entries, new byte[0], 1);
    }

    static TriggerBuildResult compose(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment
    ) {
        validate(rom, modifier, entries);
        if (sharedSupport == null) throw new IllegalArgumentException("sharedSupport must not be null");
        if (sharedSupportAlignment < 1 || (sharedSupportAlignment & (sharedSupportAlignment - 1)) != 0) {
            throw new IllegalArgumentException("sharedSupportAlignment must be a positive power of two");
        }
        byte[] dispatcher = SharedHotkeyDispatcher.build(entries);
        byte[] nativeBlob = nativeInstallerBlob(rom, modifier);

        int afterDispatcher = PAYLOAD_OFFSET + dispatcher.length;
        int sharedSupportOffset = align(afterDispatcher, sharedSupportAlignment);
        int afterSupport = sharedSupportOffset + sharedSupport.length;
        int nativeBlobOffset = align4(afterSupport);
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
                    "shared hotkey candidate requires " + total + " bytes; maximum is " + RamScript.SCRIPT_SIZE
            );
        }

        byte[] script = new byte[total];
        int p = 0;
        script[p++] = (byte)0xB8;
        putU32(script, p, HotkeyRuntimeV1.VIRTUAL_BASE);
        p += 4;
        script[p++] = (byte)0xB9;
        putU32(script, p, HotkeyRuntimeV1.VIRTUAL_BASE + fieldInstallerOffset);
        p += 4;

        if (p != HotkeyRuntimeV1.SIGNATURE_OFFSET) throw new IllegalStateException("signature offset mismatch");
        script[p++] = (byte)(HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);

        if (p != PAYLOAD_OFFSET) throw new IllegalStateException("dispatcher offset mismatch");
        System.arraycopy(dispatcher, 0, script, p, dispatcher.length);
        p += dispatcher.length;

        while (p < sharedSupportOffset) script[p++] = 0;
        System.arraycopy(sharedSupport, 0, script, p, sharedSupport.length);
        p += sharedSupport.length;

        while (p < nativeBlobOffset) script[p++] = 0;
        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length);
        p += nativeBlob.length;

        if (p != fieldInstallerOffset) throw new IllegalStateException("field installer offset mismatch");
        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        RamScript ramScript = RamScript.createWonderCard(script);
        return new TriggerBuildResult(
                ramScript,
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                dispatcher.length,
                total - dispatcher.length,
                total,
                RamScript.SCRIPT_SIZE - total
        );
    }

    static byte[] wrapperBytesForTest(RomProfile rom, HotkeyButton modifier) {
        validateModifier(modifier);
        byte[] out = new byte[] {
                0x06,0x48,                   // ldr  r0, heldKeysRaw literal @ +0x1C
                0x00,0x68,                   // ldr  r0,[r0] => heldKeysRaw | newKeysRaw<<16
                0x00,0x00,                   // lsrs r2,r0,#modifierBit+1 (modifier in carry)
                0x07,(byte)0xD3,             // bcc  fail @ +0x18
                0x01,0x0C,                   // lsrs r1,r0,#16
                0x09,0x06,                   // lsls r1,r1,#24
                0x09,0x0E,                   // lsrs r1,r1,#24 => low byte only
                0x03,(byte)0xD0,             // beq  fail
                0x1E,(byte)0xA2,             // adr  r2,0300539C
                0x11,0x70,                   // strb r1,[r2]
                0x0C,0x4A,                   // ldr  r2,03005358 lock flag ptr
                (byte)0x85,(byte)0xE0,       // b    03005434 safety gate
                0x0C,0x4B,                   // fail: ldr r3,0300535C CB1_Overworld
                0x18,0x47,                   // bx   r3
                0,0,0,0                      // heldKeysRaw
        };
        if (out.length != 32) throw new IllegalStateException("shared wrapper must be exactly 32 bytes");
        putU16(out, 0x04, thumbLsrsImm(2, 0, modifier.bit() + 1));
        putU32(out, 0x1C, rom.heldKeysRaw);
        return out;
    }

    static byte[] safetyGateBytesForTest() {
        // The wrapper return thunk moved from 03005326 to 03005328 because the
        // generic low-byte decoder uses all 28 code bytes before its literal.
        // Only this locked-input branch changes. Validator cleanup at +0x0A is
        // deliberately preserved.
        return new byte[] {
                0x10,0x78,
                0x00,0x28,
                0x00,(byte)0xD0,
                0x75,(byte)0xE7,             // locked -> 03005328
                0x21,(byte)0xE6,             // unlocked -> existing stage1
                0x10,(byte)0xBD              // validator-failure cleanup
        };
    }

    static int nativeBlobSize(RomProfile rom, HotkeyButton modifier) {
        return nativeInstallerBlob(rom, modifier).length;
    }

    static int dispatcherSize(int bindings) {
        return SharedHotkeyDispatcher.sizeForBindings(bindings);
    }

    private static byte[] nativeInstallerBlob(RomProfile rom, HotkeyButton modifier) {
        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(rom, modifier);
        int residentBytes = totalResidentBytes(blocks);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Same byte copier / atomic VBlank hook as validated Runtime v1.
        // 13 records => table is 52 B and resident data starts at blob+108.
        byte[] codeAndLiterals = new byte[] {
                (byte)0xF0,(byte)0xB4,
                0x0D,(byte)0xA4,
                0x19,(byte)0xA6,             // data at blob+108
                0x03,0x27,
                0x3F,0x06,
                0x0D,0x25,                   // 13 resident blocks
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
            throw new IllegalStateException("shared native copier size mismatch");
        }
        out.writeBytes(codeAndLiterals);

        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            if ((block.address() & 0xFFFF0000L) != 0x03000000L) {
                throw new IllegalStateException("shared runtime block outside IWRAM");
            }
            u16(out, (int)(block.address() & 0xFFFF));
            u16(out, block.data().length);
        }
        for (RuntimeV1ResidentBlocks.Block block : blocks) out.writeBytes(block.data());

        byte[] blob = out.toByteArray();
        int expected = NATIVE_CODE_AND_LITERALS_SIZE + TABLE_SIZE + residentBytes;
        if (blob.length != expected) throw new IllegalStateException("shared native blob size mismatch");
        return blob;
    }

    private static List<RuntimeV1ResidentBlocks.Block> residentBlocks(RomProfile rom, HotkeyButton modifier) {
        // Start from the exact single-hotkey V1 blocks so stage1/stage2,
        // validator, supervisor, thunks and their addresses stay unchanged.
        List<RuntimeV1ResidentBlocks.Block> base = RuntimeV1ResidentBlocks.build(rom, Hotkey.DEFAULT);
        List<RuntimeV1ResidentBlocks.Block> out = new ArrayList<>();
        for (RuntimeV1ResidentBlocks.Block block : base) {
            if (block.address() == RuntimeV1ResidentBlocks.WRAPPER) {
                out.add(new RuntimeV1ResidentBlocks.Block(block.address(), wrapperBytesForTest(rom, modifier)));
            } else if (block.address() == RuntimeV1ResidentBlocks.SAFETY_GATE) {
                out.add(new RuntimeV1ResidentBlocks.Block(block.address(), safetyGateBytesForTest()));
            } else {
                out.add(block);
            }
        }

        // External literals used by the full 28-byte wrapper. This exact gap
        // was already used by the validated two-hotkey baseline.
        byte[] pool = new byte[10];
        putU32(pool, 2, rom.lockFieldControls);   // 03005358
        putU32(pool, 6, rom.cb1OverworldThumb); // 0300535C
        out.add(new RuntimeV1ResidentBlocks.Block(SHARED_LITERAL_POOL, pool));

        if (out.size() != BLOCK_COUNT) throw new IllegalStateException("shared runtime block count mismatch");
        return List.copyOf(out);
    }

    private static int totalResidentBytes(List<RuntimeV1ResidentBlocks.Block> blocks) {
        int total = 0;
        for (RuntimeV1ResidentBlocks.Block block : blocks) total += block.data().length;
        return total;
    }

    private static void validate(RomProfile rom, HotkeyButton modifier, List<SharedHotkeyDispatcher.Entry> entries) {
        if (rom == null) throw new IllegalArgumentException("rom must not be null");
        validateModifier(modifier);
        if (entries == null || entries.isEmpty()) throw new IllegalArgumentException("entries must not be empty");
        if (entries.size() > 8) throw new IllegalArgumentException("shared candidate supports at most 8 bindings");
        Set<HotkeyButton> seen = new HashSet<>();
        for (SharedHotkeyDispatcher.Entry entry : entries) {
            if (!seen.add(entry.pressedButton())) {
                throw new IllegalArgumentException("duplicate pressed button: " + entry.pressedButton());
            }
        }
    }

    private static void validateModifier(HotkeyButton modifier) {
        if (modifier != HotkeyButton.R && modifier != HotkeyButton.L) {
            throw new IllegalArgumentException("shared runtime candidate modifier must be R or L");
        }
    }

    private static int align4(int value) {
        return (value + 3) & ~3;
    }

    private static int align(int value, int alignment) {
        if (alignment <= 1) return value;
        return (value + alignment - 1) & ~(alignment - 1);
    }

    private static int thumbLsrsImm(int rd, int rm, int shift) {
        if (shift < 1 || shift > 31) throw new IllegalArgumentException("Thumb LSRS immediate must be 1..31");
        return 0x0800 | (shift << 6) | (rm << 3) | rd;
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
