import java.io.ByteArrayOutputStream;
import java.util.List;

/*
   Compact Runtime Candidate 3.

   C3 is the first full functional compact runtime candidate:

     deliveryman
       -> installs 32-byte bootstrap with Field Script
       -> bootstrap resolves current RamScript
       -> jumps into aligned native installer stored compactly in RamScript
       -> native installer copies the validated 123-byte resident Runtime v1
       -> native installer atomically hooks VBlank to the validated supervisor
       -> supervisor later rearms callback1 when CB1_Overworld is present

     R + SELECT
       -> validated safety gate / format guard
       -> ScriptContext_SetupScript(script + 0x0C)
       -> Hello from the Wonder Card!

   The resident Runtime v1 bytes are identical to C2a / RC4a. Only the
   transport/activation mechanism is changed.
*/
final class CompactRuntimeCandidate3 {
    static final long BOOTSTRAP_ADDRESS = 0x03005310L;
    static final long VIRTUAL_BASE = 0x08010000L;

    static final int HEADER_SIZE = 0x0A;
    static final int SIGNATURE_OFFSET = 0x0A;
    static final int PAYLOAD_OFFSET = 0x0C;
    static final int FORMAT_SIGNATURE = 0x00A7;

    // Aligned native blob format:
    //   56 bytes native copier/hook code + literals
    //   48 bytes table
    //  123 bytes validated resident Runtime v1 data
    static final int NATIVE_CODE_AND_LITERALS_SIZE = 56;
    static final int TABLE_SIZE = 48;
    static final int RESIDENT_DATA_SIZE = 123;
    static final int NATIVE_BLOB_SIZE =
            NATIVE_CODE_AND_LITERALS_SIZE + TABLE_SIZE + RESIDENT_DATA_SIZE; // 227

    private CompactRuntimeCandidate3() {}

    static RamScript build(RomProfile rom) {
        byte[] payload = TriggerTestPayloads.helloWonderCard();
        return build(rom, payload);
    }

    static RamScript build(RomProfile rom, byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("compact C3 payload must not be empty");
        }

        byte[] bootstrap = bootstrapBytes(rom);
        byte[] nativeBlob = nativeInstallerBlob(rom);

        int afterPayload = PAYLOAD_OFFSET + payload.length;
        int nativeBlobOffset = align4(afterPayload);
        int alignmentPadding = nativeBlobOffset - afterPayload;
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        // The bootstrap reads fieldInstallerOffset from the vgoto target and
        // subtracts NATIVE_BLOB_SIZE, therefore the native blob must be
        // immediately before the Field Script installer.
        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "Compact C3 requires " + total + " bytes; maximum is "
                            + RamScript.SCRIPT_SIZE
            );
        }

        byte[] script = new byte[total];
        int p = 0;

        script[p++] = (byte)0xB8; // setvaddress
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        script[p++] = (byte)0xB9; // vgoto Field Script installer
        putU32(script, p, VIRTUAL_BASE + fieldInstallerOffset);
        p += 4;

        if (p != SIGNATURE_OFFSET) {
            throw new IllegalStateException("compact C3 signature offset mismatch");
        }

        script[p++] = (byte)(FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((FORMAT_SIGNATURE >>> 8) & 0xFF);

        if (p != PAYLOAD_OFFSET) {
            throw new IllegalStateException("compact C3 payload offset mismatch");
        }

        System.arraycopy(payload, 0, script, p, payload.length);
        p += payload.length;

        // Inert bytes skipped by the deliveryman vgoto and never reached by
        // the hotkey payload. Their only purpose is to make the native blob
        // physically 4-byte aligned for Thumb ADR semantics.
        for (int i = 0; i < alignmentPadding; i++) {
            script[p++] = 0;
        }

        if (p != nativeBlobOffset || (nativeBlobOffset & 3) != 0) {
            throw new IllegalStateException("compact C3 native blob alignment mismatch");
        }

        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length);
        p += nativeBlob.length;

        if (p != fieldInstallerOffset) {
            throw new IllegalStateException("compact C3 field installer offset mismatch");
        }

        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        return RamScript.createWonderCard(script);
    }

    static byte[] bootstrapBytes(RomProfile rom) {
        // Independently assembled for ARM7TDMI Thumb-1:
        //
        // push {lr}
        // ldr  r3, get_saved
        // bl   call_r3
        // cmp  r0,#0
        // beq  fail
        // ldrh r1,[r0,#6]      ; low halfword of vgoto target = installer offset
        // adds r0,r0,r1        ; current script + installer offset
        // subs r0,#227         ; immediately preceding native blob
        // adds r0,#1           ; Thumb state
        // bx   r0              ; tail-enter native installer in EWRAM
        // fail: pop {pc}
        // call_r3: bx r3
        //          nop
        // get_saved: .word GetSavedRamScriptIfValid|1
        byte[] out = new byte[] {
                0x00,(byte)0xB5,
                0x06,0x4B,
                0x00,(byte)0xF0, 0x08,(byte)0xF8,
                0x00,0x28,
                0x04,(byte)0xD0,
                (byte)0xC1,(byte)0x88,
                0x40,0x18,
                (byte)0xE3,0x38,
                0x01,0x30,
                0x00,0x47,
                0x00,(byte)0xBD,
                0x18,0x47,
                (byte)0xC0,0x46,
                0,0,0,0
        };
        putU32(out, 0x1C, rom.getSavedRamScriptThumb);
        return out;
    }

    static byte[] nativeInstallerBlob(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = RuntimeV1ResidentBlocks.build(rom);
        if (blocks.size() != 12) {
            throw new IllegalStateException("compact C3 expects 12 resident blocks");
        }
        if (RuntimeV1ResidentBlocks.totalResidentBytes(rom) != RESIDENT_DATA_SIZE) {
            throw new IllegalStateException("compact C3 resident byte count mismatch");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Assembled with clang --target=armv4t-none-eabi and independently
        // disassembled before embedding.
        //
        // r4 = table, r6 = sequential resident data, r7 = 03000000 base,
        // r5 = block count. After all blocks are copied, the wrapper is fully
        // present, so VBlank is atomically switched to supervisor|1.
        byte[] codeAndLiterals = new byte[] {
                (byte)0xF0,(byte)0xB4,       // push {r4-r7}
                0x0D,(byte)0xA4,             // adr r4, table (blob+56)
                0x18,(byte)0xA6,             // adr r6, data  (blob+104)
                0x03,0x27,                   // movs r7,#3
                0x3F,0x06,                   // lsls r7,#24 = 03000000
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
                0x02,0x48,                   // ldr r0, [pc,#8]  -> vblank slot
                0x03,0x49,                   // ldr r1, [pc,#12] -> supervisor|1
                0x01,0x60,                   // str r1,[r0]      atomic hook
                (byte)0xF0,(byte)0xBC,       // pop {r4-r7}
                0x00,(byte)0xBD,             // pop {pc}
                (byte)0xC0,0x46,             // alignment nop
                0x50,0x35,0x00,0x03,         // 03003550
                0x43,0x3F,0x00,0x03          // 03003F43
        };

        if (codeAndLiterals.length != NATIVE_CODE_AND_LITERALS_SIZE) {
            throw new IllegalStateException("compact C3 native code size mismatch");
        }
        out.writeBytes(codeAndLiterals);

        // 12 compact records: low 16 bits of 0300xxxx destination + u16 size.
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            long address = block.address();
            if ((address & 0xFFFF0000L) != 0x03000000L) {
                throw new IllegalStateException("compact C3 block outside IWRAM");
            }
            u16(out, (int)(address & 0xFFFF));
            u16(out, block.data().length);
        }

        // Exact resident bytes validated by C2a. Wrapper remains last.
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            out.writeBytes(block.data());
        }

        byte[] blob = out.toByteArray();
        if (blob.length != NATIVE_BLOB_SIZE) {
            throw new IllegalStateException(
                    "compact C3 expected native blob " + NATIVE_BLOB_SIZE
                            + ", got " + blob.length
            );
        }
        return blob;
    }

    static int payloadSize() {
        return TriggerTestPayloads.helloWonderCard().length;
    }

    static int nativeBlobOffset() {
        return align4(PAYLOAD_OFFSET + payloadSize());
    }

    static int alignmentPadding() {
        return nativeBlobOffset() - (PAYLOAD_OFFSET + payloadSize());
    }

    static int fieldInstallerSize(RomProfile rom) {
        return new FieldScriptWriter()
                .writeBytes(BOOTSTRAP_ADDRESS, bootstrapBytes(rom))
                .callNative(BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build()
                .length;
    }

    static int scriptSize(RomProfile rom) {
        return nativeBlobOffset() + NATIVE_BLOB_SIZE + fieldInstallerSize(rom);
    }

    static int runtimeOverhead(RomProfile rom) {
        return scriptSize(rom) - payloadSize();
    }

    static int freeBytes(RomProfile rom) {
        return RamScript.SCRIPT_SIZE - scriptSize(rom);
    }

    private static int align4(int value) {
        return (value + 3) & ~3;
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
