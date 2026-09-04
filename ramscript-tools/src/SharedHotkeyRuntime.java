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

    record ResidentMaintenance(byte[] code, int offset) {
        ResidentMaintenance {
            if (code == null || code.length == 0) throw new IllegalArgumentException("maintenance code must not be empty");
            if (offset < PAYLOAD_OFFSET) throw new IllegalArgumentException("maintenance offset must be inside RamScript payload area");
            code = code.clone();
        }
    }

    /* Resident-sidecar extension hook. Ordinary production compose paths never pass
       this record, so their resident bytes and installer layout stay unchanged. */
    record ResidentSidecar(long address, long callbackThumb, byte[] code) {
        ResidentSidecar {
            if ((address & 1L) != 0) throw new IllegalArgumentException("sidecar address must be even");
            if (callbackThumb != (address | 1L)) throw new IllegalArgumentException("sidecar callback must be address|1");
            if (code == null || code.length == 0) throw new IllegalArgumentException("sidecar code must not be empty");
            code = code.clone();
        }
        @Override public byte[] code() { return code.clone(); }
    }

    private SharedHotkeyRuntime() {}

    static TriggerBuildResult compose(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries
    ) {
        return composeInternal(rom, modifier, entries, new byte[0], 1, null, null, false);
    }

    static TriggerBuildResult compose(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment
    ) {
        return composeInternal(rom, modifier, entries, sharedSupport, sharedSupportAlignment, null, null, false);
    }

    static TriggerBuildResult compose(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment,
            ResidentMaintenance maintenance
    ) {
        return composeInternal(rom, modifier, entries, sharedSupport, sharedSupportAlignment, maintenance, null, false);
    }

    static TriggerBuildResult composeWithResidentSidecar(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment,
            ResidentSidecar sidecar
    ) {
        if (sidecar == null) throw new IllegalArgumentException("sidecar must not be null");
        return composeInternal(rom, modifier, entries, sharedSupport, sharedSupportAlignment, null, sidecar, false);
    }

    /* Catalog integration variant. Unlike the already game-validated probe path
       above, this packs the EWRAM sidecar into the temporary native installer
       and copies it with a compact Thumb loop. Default/probe output remains
       byte-for-byte unchanged. */
    static TriggerBuildResult composeWithResidentSidecarNativeCopy(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment,
            ResidentSidecar sidecar
    ) {
        if (sidecar == null) throw new IllegalArgumentException("sidecar must not be null");
        return composeInternal(rom, modifier, entries, sharedSupport, sharedSupportAlignment, null, sidecar, true);
    }

    private static TriggerBuildResult composeInternal(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment,
            ResidentMaintenance maintenance,
            ResidentSidecar sidecar,
            boolean nativeCopySidecar
    ) {
        validate(rom, modifier, entries);
        if (sharedSupport == null) throw new IllegalArgumentException("sharedSupport must not be null");
        if (sharedSupportAlignment < 1 || (sharedSupportAlignment & (sharedSupportAlignment - 1)) != 0) {
            throw new IllegalArgumentException("sharedSupportAlignment must be a positive power of two");
        }
        byte[] dispatcher = SharedHotkeyDispatcher.build(entries);
        byte[] nativeBlob = nativeCopySidecar
                ? nativeInstallerBlobWithSidecarCopy(rom, modifier, sidecar)
                : nativeInstallerBlob(rom, modifier, maintenance != null, sidecar == null ? null : sidecar.callbackThumb());

        int afterDispatcher = PAYLOAD_OFFSET + dispatcher.length;
        int sharedSupportOffset = align(afterDispatcher, sharedSupportAlignment);
        int afterSupport = sharedSupportOffset + sharedSupport.length;
        int nativeBlobOffset = align4(afterSupport);
        byte[] bootstrap = maintenance == null
                ? HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset)
                : maintenanceBootstrapBytes(nativeBlobOffset, maintenance.offset());
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        FieldScriptWriter installer = new FieldScriptWriter();
        if (sidecar != null && !nativeCopySidecar) installer.writeBytes(sidecar.address(), sidecar.code());
        byte[] fieldInstaller = installer
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
        return nativeInstallerBlob(rom, modifier, false, null).length;
    }

    /*
       Deployment adapters may need to install the already-validated resident
       shared runtime from a different transient host (for example an
       object-bound one-shot installer). Keep the resident block construction in
       this class so adapters cannot silently fork wrapper/safety-gate bytes.

       This does not change the SharedHotkeyRuntime output. Callers receive the
       exact production resident blocks used by nativeInstallerBlob(..., false).
    */
    static List<RuntimeV1ResidentBlocks.Block> frozenResidentBlocks(
            RomProfile rom,
            HotkeyButton modifier
    ) {
        return residentBlocks(rom, modifier, false, null);
    }

    static int dispatcherSize(int bindings) {
        return SharedHotkeyDispatcher.sizeForBindings(bindings);
    }

    private static byte[] nativeInstallerBlob(RomProfile rom, HotkeyButton modifier, boolean maintenance, Long postMapCallbackThumb) {
        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(rom, modifier, maintenance, postMapCallbackThumb);
        int residentBytes = totalResidentBytes(blocks);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Same byte copier / atomic VBlank hook as validated Runtime v1.
        // Maintenance reuses the existing VBlank tail literal, so that literal is
        // dynamically patched by the temporary bootstrap and omitted from the
        // resident copy table. No new IWRAM block is reserved.
        int blockCount = blocks.size();
        int residentDataOffset = NATIVE_CODE_AND_LITERALS_SIZE + blockCount * 4;
        byte[] codeAndLiterals = new byte[] {
                (byte)0xF0,(byte)0xB4,
                0x0D,(byte)0xA4,
                0x00,(byte)0xA6,             // adr r6,data (patched below)
                0x03,0x27,
                0x3F,0x06,
                0x00,0x25,                   // resident block count (patched below)
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
        int adrImm = (residentDataOffset - 8) / 4;
        if (residentDataOffset < 8 || ((residentDataOffset - 8) & 3) != 0 || adrImm > 255) {
            throw new IllegalStateException("shared resident data offset cannot be encoded by Thumb ADR");
        }
        putU16(codeAndLiterals, 0x04, 0xA600 | adrImm);
        codeAndLiterals[0x0A] = (byte)blockCount;
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
        int expected = NATIVE_CODE_AND_LITERALS_SIZE + blockCount * 4 + residentBytes;
        if (blob.length != expected) throw new IllegalStateException("shared native blob size mismatch");
        return blob;
    }

    private static byte[] nativeInstallerBlobWithSidecarCopy(
            RomProfile rom,
            HotkeyButton modifier,
            ResidentSidecar sidecar
    ) {
        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(
                rom, modifier, false, sidecar.callbackThumb());
        int blockCount = blocks.size();
        int residentBytes = totalResidentBytes(blocks);
        final int codeAndLiteralsSize = 0x4C;
        int descriptorOffset = codeAndLiteralsSize;
        int residentDataOffset = descriptorOffset + blockCount * 4;

        byte[] head = new byte[codeAndLiteralsSize];
        int[] insn = {
                0xB4F0,       // push {r4-r7}
                0,            // adr r4, descriptors
                0,            // adr r6, resident data
                0x2703,       // movs r7,#3
                0x063F,       // lsls r7,r7,#24 => 03000000
                0x2500,       // movs r5,#count (patched)
                0x8821, 0x8862, 0x3404, 0x19C9,
                0x7833, 0x700B, 0x3601, 0x3101, 0x3A01, 0xD1F9,
                0x3D01, 0xD1F3,
                0,            // ldr r0, sidecar destination
                0x2200 | sidecar.code().length,
                0x7833,       // sidecar copy: ldrb r3,[r6]
                0x7003,       // strb r3,[r0]
                0x3601,       // adds r6,#1
                0x3001,       // adds r0,#1
                0x3A01,       // subs r2,#1
                0xD1F9,       // bne sidecar copy loop
                0,            // ldr r0, VBlank hook
                0,            // ldr r1, supervisor|1
                0x6001,
                0xBCF0,
                0x4770,
                0x46C0
        };
        for (int i = 0; i < insn.length; i++) putU16(head, i * 2, insn[i]);
        putU16(head, 0x02, thumbAdr(4, 0x02, descriptorOffset));
        putU16(head, 0x04, thumbAdr(6, 0x04, residentDataOffset));
        head[0x0A] = (byte) blockCount;
        putU16(head, 0x24, thumbLdrLiteral(0, 0x24, 0x40));
        putU16(head, 0x34, thumbLdrLiteral(0, 0x34, 0x44));
        putU16(head, 0x36, thumbLdrLiteral(1, 0x36, 0x48));
        putU32(head, 0x40, sidecar.address());
        putU32(head, 0x44, 0x03003550L);
        putU32(head, 0x48, 0x03003F43L);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(head);
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            if ((block.address() & 0xFFFF0000L) != 0x03000000L) {
                throw new IllegalStateException("shared runtime block outside IWRAM");
            }
            u16(out, (int) (block.address() & 0xFFFF));
            u16(out, block.data().length);
        }
        for (RuntimeV1ResidentBlocks.Block block : blocks) out.writeBytes(block.data());
        out.writeBytes(sidecar.code());

        byte[] blob = out.toByteArray();
        int expected = codeAndLiteralsSize + blockCount * 4 + residentBytes + sidecar.code().length;
        if (blob.length != expected) throw new IllegalStateException("shared sidecar native blob size mismatch");
        return blob;
    }

    private static int thumbAdr(int rd, int insnOffset, int targetOffset) {
        int base = (insnOffset + 4) & ~3;
        int delta = targetOffset - base;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) throw new IllegalArgumentException("ADR target out of range");
        return 0xA000 | (rd << 8) | (delta / 4);
    }

    private static int thumbLdrLiteral(int rt, int insnOffset, int literalOffset) {
        int base = (insnOffset + 4) & ~3;
        int delta = literalOffset - base;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) throw new IllegalArgumentException("literal target out of range");
        return 0x4800 | (rt << 8) | (delta / 4);
    }

    private static List<RuntimeV1ResidentBlocks.Block> residentBlocks(RomProfile rom, HotkeyButton modifier, boolean maintenance, Long postMapCallbackThumb) {
        // Start from the exact single-hotkey V1 blocks so stage1/stage2,
        // validator, supervisor, thunks and their addresses stay unchanged.
        List<RuntimeV1ResidentBlocks.Block> base = RuntimeV1ResidentBlocks.build(rom, Hotkey.DEFAULT);
        List<RuntimeV1ResidentBlocks.Block> out = new ArrayList<>();
        for (RuntimeV1ResidentBlocks.Block block : base) {
            if (maintenance && block.address() == RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_LITERAL) {
                continue;
            }
            if (block.address() == RuntimeV1ResidentBlocks.WRAPPER) {
                out.add(new RuntimeV1ResidentBlocks.Block(block.address(), wrapperBytesForTest(rom, modifier)));
            } else if (block.address() == RuntimeV1ResidentBlocks.SAFETY_GATE) {
                out.add(new RuntimeV1ResidentBlocks.Block(block.address(), safetyGateBytesForTest()));
            } else if (postMapCallbackThumb != null && block.address() == RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS) {
                byte[] literals = block.data().clone();
                putU32(literals, 8, postMapCallbackThumb);
                out.add(new RuntimeV1ResidentBlocks.Block(block.address(), literals));
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

        int expectedBlocks = maintenance ? BLOCK_COUNT - 1 : BLOCK_COUNT;
        if (out.size() != expectedBlocks) throw new IllegalStateException("shared runtime block count mismatch");
        return List.copyOf(out);
    }

    private static byte[] maintenanceBootstrapBytes(
            int nativeBlobOffset,
            int maintenanceOffset
    ) {
        int delta = nativeBlobOffset - maintenanceOffset;
        if (delta < 0 || delta > 255) {
            throw new IllegalArgumentException("maintenance code must be within 255 bytes before native blob");
        }
        byte[] out = new byte[] {
                0x03,0x48,
                0x00,0x68,
                0x03,0x49,
                0x00,0x22,
                0x00,0x00,
                0x03,0x4B,
                0x00,0x00,
                0x08,0x47,
                0,0,0,0,
                0,0,0,0,
                0,0,0,0
        };
        out[0x06] = (byte)(delta & 0xFF);
        out[0x07] = 0x22;                    // movs r2,#delta
        putU16(out, 0x08, 0x1A8A);          // subs r2,r1,r2
        putU16(out, 0x0C, 0x601A);          // str  r2,[r3]
        putU32(out, 0x10, HotkeyRuntimeV1.S_ADDRESS_OFFSET);
        putU32(out, 0x14, HotkeyRuntimeV1.VIRTUAL_BASE + nativeBlobOffset + 1L);
        putU32(out, 0x18, RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_LITERAL);
        if (out.length > 32) throw new IllegalStateException("maintenance bootstrap exceeds WRAPPER staging slot");
        return out;
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

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
    }

    private static void u16(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
