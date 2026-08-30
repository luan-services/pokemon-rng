import java.io.ByteArrayOutputStream;
import java.util.List;

/*
   One-shot object-bound deployment adapter for the frozen SharedHotkeyRuntime.

   It intentionally does not modify the resident shared runtime. The object host
   only changes the transient installation path:
     1. copy the exact production SharedHotkeyRuntime resident blocks;
     2. rewrite this RamScript binding to FF/FF/FF;
     3. refresh the stock RamScript CRC;
     4. enable the existing validated VBlank supervisor;
     5. returnram to the host object's vanilla script.

   After the first interaction the normal SharedHotkeyRuntime locator can resolve
   the same RamScript through GetSavedRamScriptIfValid(). If the changed
   FF/FF/FF state is saved, the stock Mystery Gift deliveryman behavior is the
   same known limitation as the single-hotkey early installer.
*/
final class EarlyObjectBoundSharedHotkeyInstaller {
    private static final int RAM_SCRIPT_OFFSET_IN_SB1 = 0x361C;
    private static final int EARLY_CODE_AND_LITERALS_SIZE = 92;

    private EarlyObjectBoundSharedHotkeyInstaller() {}

    static TriggerBuildResult compose(
            RomProfile rom,
            HotkeyButton modifier,
            List<SharedHotkeyDispatcher.Entry> entries,
            byte[] sharedSupport,
            int sharedSupportAlignment,
            ObjectEventTarget target
    ) {
        if (rom == null || modifier == null || entries == null || entries.isEmpty()
                || sharedSupport == null || target == null) {
            throw new IllegalArgumentException("early shared hotkey installer arguments must not be null/empty");
        }
        if (sharedSupportAlignment < 1 || (sharedSupportAlignment & (sharedSupportAlignment - 1)) != 0) {
            throw new IllegalArgumentException("sharedSupportAlignment must be a positive power of two");
        }

        byte[] dispatcher = SharedHotkeyDispatcher.build(entries);
        byte[] earlyNative = earlyNativeInstallerBlob(rom, modifier);

        int afterDispatcher = SharedHotkeyRuntime.PAYLOAD_OFFSET + dispatcher.length;
        int sharedSupportOffset = align(afterDispatcher, sharedSupportAlignment);
        int afterSupport = sharedSupportOffset + sharedSupport.length;
        int nativeBlobOffset = align4(afterSupport);
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + earlyNative.length;
        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "Early object-bound SharedHotkeyRuntime requires " + total
                            + " bytes; maximum is " + RamScript.SCRIPT_SIZE
            );
        }

        byte[] body = new byte[total];
        int p = 0;
        body[p++] = (byte) 0xB8; // setvaddress
        putU32(body, p, HotkeyRuntimeV1.VIRTUAL_BASE);
        p += 4;
        body[p++] = (byte) 0xB9; // vgoto transient installer
        putU32(body, p, HotkeyRuntimeV1.VIRTUAL_BASE + fieldInstallerOffset);
        p += 4;
        body[p++] = (byte) (HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        body[p++] = (byte) ((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);

        if (p != SharedHotkeyRuntime.PAYLOAD_OFFSET) {
            throw new IllegalStateException("early shared dispatcher offset mismatch");
        }
        System.arraycopy(dispatcher, 0, body, p, dispatcher.length);
        p += dispatcher.length;

        while (p < sharedSupportOffset) body[p++] = 0;
        System.arraycopy(sharedSupport, 0, body, p, sharedSupport.length);
        p += sharedSupport.length;

        while (p < nativeBlobOffset) body[p++] = 0;
        System.arraycopy(earlyNative, 0, body, p, earlyNative.length);
        p += earlyNative.length;

        if (p != fieldInstallerOffset) throw new IllegalStateException("early shared field installer offset mismatch");
        System.arraycopy(fieldInstaller, 0, body, p, fieldInstaller.length);

        RamScript script = RamScript.createObjectBound(body, target.mapGroup(), target.mapNum(), target.localId());
        return new TriggerBuildResult(
                script,
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                dispatcher.length,
                total - dispatcher.length,
                total,
                RamScript.SCRIPT_SIZE - total
        );
    }

    static byte[] earlyNativeInstallerBlob(RomProfile rom, HotkeyButton modifier) {
        List<RuntimeV1ResidentBlocks.Block> blocks = SharedHotkeyRuntime.frozenResidentBlocks(rom, modifier);
        if (blocks.isEmpty() || blocks.size() > 255) throw new IllegalStateException("invalid shared resident block count");

        int tableSize = blocks.size() * 4;
        int residentSize = 0;
        for (RuntimeV1ResidentBlocks.Block block : blocks) residentSize += block.data().length;
        int dataOffset = EARLY_CODE_AND_LITERALS_SIZE + tableSize;

        byte[] code = new byte[] {
                (byte)0xF0,(byte)0xB5,       // push {r4-r7,lr}
                0,0,                         // adr r4, table (patched)
                0,0,                         // adr r6, data  (patched)
                0x03,0x27,                   // movs r7,#3
                0x3F,0x06,                   // lsls r7,#24
                0,0,                         // movs r5,#blockCount (patched)
                0x21,(byte)0x88,             // ldrh r1,[r4]
                0x62,(byte)0x88,             // ldrh r2,[r4,#2]
                0x04,0x34,                   // adds r4,#4
                (byte)0xC9,0x19,             // adds r1,r1,r7
                0x33,0x78,                   // ldrb r3,[r6]
                0x0B,0x70,                   // strb r3,[r1]
                0x01,0x36,                   // adds r6,#1
                0x01,0x31,                   // adds r1,#1
                0x01,0x3A,                   // subs r2,#1
                (byte)0xF9,(byte)0xD1,       // bne copy_loop
                0x01,0x3D,                   // subs r5,#1
                (byte)0xF3,(byte)0xD1,       // bne block_loop
                0x08,0x4C,                   // ldr r4,[pc,#32] sb1 ptr address
                0x24,0x68,                   // ldr r4,[r4]
                0x08,0x4D,                   // ldr r5,[pc,#32] 0x361C
                0x64,0x19,                   // adds r4,r4,r5
                (byte)0xFF,0x20,             // movs r0,#255
                0x60,0x71,                   // strb r0,[r4,#5]
                (byte)0xA0,0x71,             // strb r0,[r4,#6]
                (byte)0xE0,0x71,             // strb r0,[r4,#7]
                0x06,0x4B,                   // ldr r3,[pc,#24] checksum fn
                0x00,(byte)0xF0,0x05,(byte)0xF8, // bl local bx-r3 thunk
                0x20,0x60,                   // str r0,[r4] checksum
                0x05,0x48,                   // ldr r0,[pc,#20] vblank slot
                0x06,0x49,                   // ldr r1,[pc,#24] supervisor|1
                0x01,0x60,                   // str r1,[r0]
                (byte)0xF0,(byte)0xBD,       // pop {r4-r7,pc}
                0x18,0x47,                   // thunk: bx r3
                (byte)0xC0,0x46,             // nop/alignment
                0,0,0,0,                     // +0x48 gSaveBlock1Ptr address
                0,0,0,0,                     // +0x4C RamScript offset
                0,0,0,0,                     // +0x50 CalculateRamScriptChecksum|1
                0,0,0,0,                     // +0x54 VBlank slot
                0,0,0,0                      // +0x58 supervisor|1
        };
        if (code.length != EARLY_CODE_AND_LITERALS_SIZE) throw new IllegalStateException("early shared code size mismatch");

        putU16(code, 0x02, thumbAdr(4, 0x02, EARLY_CODE_AND_LITERALS_SIZE));
        putU16(code, 0x04, thumbAdr(6, 0x04, dataOffset));
        putU16(code, 0x0A, 0x2500 | blocks.size());
        putU32(code, 0x48, rom.saveBlock1Ptr);
        putU32(code, 0x4C, RAM_SCRIPT_OFFSET_IN_SB1);
        putU32(code, 0x50, EarlyObjectBoundHotkeyInstaller.calculateRamScriptChecksumThumb(rom));
        putU32(code, 0x54, rom.vblankSlot);
        putU32(code, 0x58, RuntimeV1ResidentBlocks.SUPERVISOR | 1L);

        ByteArrayOutputStream out = new ByteArrayOutputStream(EARLY_CODE_AND_LITERALS_SIZE + tableSize + residentSize);
        out.writeBytes(code);
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            if ((block.address() & 0xFFFF0000L) != 0x03000000L) throw new IllegalStateException("shared block outside IWRAM");
            u16(out, (int) (block.address() & 0xFFFF));
            u16(out, block.data().length);
        }
        for (RuntimeV1ResidentBlocks.Block block : blocks) out.writeBytes(block.data());
        return out.toByteArray();
    }

    static boolean residentBytesMatchFrozenShared(RomProfile rom, HotkeyButton modifier) {
        byte[] early = earlyNativeInstallerBlob(rom, modifier);
        int tableSize = SharedHotkeyRuntime.frozenResidentBlocks(rom, modifier).size() * 4;
        int offset = EARLY_CODE_AND_LITERALS_SIZE;
        byte[] earlyTail = java.util.Arrays.copyOfRange(early, offset, early.length);

        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        for (RuntimeV1ResidentBlocks.Block block : SharedHotkeyRuntime.frozenResidentBlocks(rom, modifier)) {
            u16(expected, (int)(block.address() & 0xFFFF));
            u16(expected, block.data().length);
        }
        for (RuntimeV1ResidentBlocks.Block block : SharedHotkeyRuntime.frozenResidentBlocks(rom, modifier)) {
            expected.writeBytes(block.data());
        }
        return tableSize > 0 && java.util.Arrays.equals(earlyTail, expected.toByteArray());
    }

    private static int thumbAdr(int rd, int instructionOffset, int targetOffset) {
        int pcBase = (instructionOffset + 4) & ~3;
        int delta = targetOffset - pcBase;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) {
            throw new IllegalArgumentException("early shared ADR target out of range");
        }
        return 0xA000 | (rd << 8) | (delta / 4);
    }

    private static int align4(int n) { return (n + 3) & ~3; }
    private static int align(int n, int a) { return a <= 1 ? n : (n + a - 1) & ~(a - 1); }
    private static void u16(ByteArrayOutputStream out, int v) { out.write(v & 0xFF); out.write((v >>> 8) & 0xFF); }
    private static void putU16(byte[] b, int o, int v) { b[o]=(byte)v; b[o+1]=(byte)(v>>>8); }
    private static void putU32(byte[] b, int o, long v) { b[o]=(byte)v; b[o+1]=(byte)(v>>>8); b[o+2]=(byte)(v>>>16); b[o+3]=(byte)(v>>>24); }
}
