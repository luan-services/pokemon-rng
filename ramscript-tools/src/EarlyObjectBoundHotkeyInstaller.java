import java.util.Arrays;

/*
   One-shot object-bound installer for the validated HotkeyRuntimeV1.

   Why this exists:
     - GetRamScript(localId, originalScript) can enter a RamScript bound to an NPC.
     - the resident HotkeyRuntimeV1 later resolves its payload with
       GetSavedRamScriptIfValid(), which intentionally accepts only FF/FF/FF.

   The first NPC interaction therefore performs two operations atomically:
     1. install the exact validated resident RuntimeV1 blocks;
     2. rewrite this RamScript's binding bytes to FF/FF/FF and recompute its CRC.

   gRamScriptRetAddr was already captured by GetRamScript before this code runs,
   so returnram can still resume the NPC's original vanilla script. Future talks
   no longer hit this installer, while the resident hotkey can resolve the same
   RamScript through GetSavedRamScriptIfValid().

   The resident RuntimeV1 bytes are copied byte-for-byte from HotkeyRuntimeV1's
   validated native blob. Only the transient installer code differs.
*/
final class EarlyObjectBoundHotkeyInstaller {
    private static final int RAM_SCRIPT_OFFSET_IN_SB1 = 0x361C;
    private static final int EARLY_CODE_AND_LITERALS_SIZE = 92;
    private static final int EARLY_NATIVE_BLOB_SIZE =
            EARLY_CODE_AND_LITERALS_SIZE
                    + HotkeyRuntimeV1.TABLE_SIZE
                    + HotkeyRuntimeV1.RESIDENT_DATA_SIZE; // 263

    private EarlyObjectBoundHotkeyInstaller() {}

    static TriggerBuildResult compose(
            RomProfile rom,
            byte[] payload,
            Hotkey hotkey,
            ObjectEventTarget target
    ) {
        if (rom == null || payload == null || payload.length == 0 || hotkey == null || target == null) {
            throw new IllegalArgumentException("early object-bound hotkey installer arguments must not be null/empty");
        }

        RamScript script = build(rom, payload, hotkey, target);
        int total = scriptSize(rom, payload, hotkey);
        return new TriggerBuildResult(
                script,
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                payload.length,
                total - payload.length,
                total,
                RamScript.SCRIPT_SIZE - total
        );
    }

    static RamScript build(
            RomProfile rom,
            byte[] payload,
            Hotkey hotkey,
            ObjectEventTarget target
    ) {
        int afterPayload = HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length;
        int nativeBlobOffset = align4(afterPayload);
        byte[] nativeBlob = earlyNativeInstallerBlob(rom, hotkey);
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
                    "Early object-bound HotkeyRuntimeV1 requires " + total
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

        if (p != HotkeyRuntimeV1.PAYLOAD_OFFSET) {
            throw new IllegalStateException("early hotkey payload offset mismatch");
        }

        System.arraycopy(payload, 0, body, p, payload.length);
        p += payload.length;
        while (p < nativeBlobOffset) body[p++] = 0;
        System.arraycopy(nativeBlob, 0, body, p, nativeBlob.length);
        p += nativeBlob.length;
        System.arraycopy(fieldInstaller, 0, body, p, fieldInstaller.length);

        return RamScript.createObjectBound(
                body,
                target.mapGroup(),
                target.mapNum(),
                target.localId()
        );
    }

    static int scriptSize(RomProfile rom, byte[] payload, Hotkey hotkey) {
        int nativeBlobOffset = align4(HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length);
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerSize = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build()
                .length;
        return nativeBlobOffset + EARLY_NATIVE_BLOB_SIZE + fieldInstallerSize;
    }

    static byte[] earlyNativeInstallerBlob(RomProfile rom, Hotkey hotkey) {
        byte[] validated = HotkeyRuntimeV1.nativeInstallerBlob(rom, hotkey);
        byte[] out = new byte[EARLY_NATIVE_BLOB_SIZE];

        // Independently assembled for ARM7TDMI / Thumb-1. The first loop is
        // the same copy algorithm as HotkeyRuntimeV1, but table/data ADRs move
        // because this transient installer has an additional self-detach tail.
        //
        // After copying the validated resident blocks:
        //   r4 = *gSaveBlock1Ptr + 0x361C
        //   ramScript.data.{mapGroup,mapNum,objectId} = FF/FF/FF
        //   checksum = CalculateRamScriptChecksum()
        //   install VBlank supervisor only after the standard header+CRC exist
        //
        // The local BL -> bx r3 thunk is ARMv4T-safe and preserves a Thumb LR.
        byte[] code = new byte[] {
                (byte)0xF0,(byte)0xB5,       // push {r4-r7,lr}
                0x16,(byte)0xA4,             // adr r4, table @ +0x5C
                0x21,(byte)0xA6,             // adr r6, data  @ +0x8C
                0x03,0x27,                   // movs r7,#3
                0x3F,0x06,                   // lsls r7,#24
                0x0C,0x25,                   // movs r5,#12
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

        if (code.length != EARLY_CODE_AND_LITERALS_SIZE) {
            throw new IllegalStateException("early object-bound installer code size mismatch: " + code.length);
        }

        putU32(code, 0x48, rom.saveBlock1Ptr);
        putU32(code, 0x4C, RAM_SCRIPT_OFFSET_IN_SB1);
        putU32(code, 0x50, calculateRamScriptChecksumThumb(rom));
        putU32(code, 0x54, rom.vblankSlot);
        putU32(code, 0x58, RuntimeV1ResidentBlocks.SUPERVISOR | 1L);
        System.arraycopy(code, 0, out, 0, code.length);

        // Preserve the validated 12-entry resident table + all 123 resident
        // bytes exactly. Only their position inside the transient blob moves.
        System.arraycopy(
                validated,
                HotkeyRuntimeV1.NATIVE_CODE_AND_LITERALS_SIZE,
                out,
                EARLY_CODE_AND_LITERALS_SIZE,
                HotkeyRuntimeV1.TABLE_SIZE + HotkeyRuntimeV1.RESIDENT_DATA_SIZE
        );
        return out;
    }

    static long calculateRamScriptChecksumThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10, LEAF_GREEN_EN_10 -> 0x08069CB1L;
            case FIRE_RED_EN_11, LEAF_GREEN_EN_11 -> 0x08069CC5L;
        };
    }

    static boolean residentTableAndDataMatchValidated(RomProfile rom, Hotkey hotkey) {
        byte[] early = earlyNativeInstallerBlob(rom, hotkey);
        byte[] validated = HotkeyRuntimeV1.nativeInstallerBlob(rom, hotkey);
        return Arrays.equals(
                Arrays.copyOfRange(early, EARLY_CODE_AND_LITERALS_SIZE, early.length),
                Arrays.copyOfRange(validated, HotkeyRuntimeV1.NATIVE_CODE_AND_LITERALS_SIZE, validated.length)
        );
    }

    private static int align4(int n) {
        return (n + 3) & ~3;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
