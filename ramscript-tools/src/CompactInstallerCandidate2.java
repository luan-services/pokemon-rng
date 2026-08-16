import java.io.ByteArrayOutputStream;
import java.util.List;

final class CompactInstallerCandidate2 {
    static final long BOOTSTRAP_ADDRESS = 0x03005310L;
    static final int HEADER_SIZE = 0x0A;
    static final long VIRTUAL_BASE = 0x08010000L;

    // Native installer layout:
    //   0x00..0x27 code      = 40 bytes
    //   0x28..0x57 table     = 48 bytes (12 records x 4)
    //   0x58..     block data = 123 bytes
    static final int NATIVE_CODE_SIZE = 40;
    static final int TABLE_SIZE = 48;
    static final int RESIDENT_DATA_SIZE = 123;
    static final int NATIVE_BLOB_SIZE =
            NATIVE_CODE_SIZE + TABLE_SIZE + RESIDENT_DATA_SIZE;

    private CompactInstallerCandidate2() {}

    static RamScript build(RomProfile rom) {
        byte[] nativeBlob = nativeInstallerBlob(rom);
        byte[] bootstrap = bootstrapBytes(rom);

        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int nativeBlobOffset = HEADER_SIZE;
        int installerOffset = nativeBlobOffset + nativeBlob.length;

        byte[] script = new byte[installerOffset + fieldInstaller.length];
        int p = 0;

        script[p++] = (byte)0xB8; // setvaddress
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        script[p++] = (byte)0xB9; // vgoto
        putU32(script, p, VIRTUAL_BASE + installerOffset);
        p += 4;

        if (p != nativeBlobOffset) {
            throw new IllegalStateException("compact C2 native blob offset mismatch");
        }

        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length);
        p += nativeBlob.length;

        if (p != installerOffset) {
            throw new IllegalStateException("compact C2 installer offset mismatch");
        }

        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        if (script.length > RamScript.SCRIPT_SIZE) {
            throw new IllegalStateException("compact C2 exceeds RamScript capacity");
        }

        return RamScript.createWonderCard(script);
    }

    static byte[] bootstrapBytes(RomProfile rom) {
        // Independent ARM7TDMI/Thumb assembly:
        //
        //   push {lr}
        //   ldr  r3, get_saved
        //   bl   call_r3
        //   cmp  r0,#0
        //   beq  fail
        //   ldrh r1,[r0,#6]     ; low halfword of vgoto target = installerOffset
        //   adds r0,r0,r1       ; current script + installerOffset
        //   subs r0,#211        ; back to native blob
        //   adds r0,#1          ; Thumb bit
        //   bx   r0
        // fail:
        //   pop {pc}
        // call_r3:
        //   bx r3
        //   nop
        // get_saved:
        //   .word GetSavedRamScriptIfValid|1
        //
        // The native blob executes from the current RamScript in EWRAM, so it
        // is free to overwrite BOOTSTRAP_ADDRESS with the final 32-byte wrapper.
        byte[] out = new byte[] {
                0x00,(byte)0xB5,
                0x06,0x4B,
                0x00,(byte)0xF0, 0x08,(byte)0xF8,
                0x00,0x28,
                0x04,(byte)0xD0,
                (byte)0xC1,(byte)0x88,
                0x40,0x18,
                (byte)0xD3,0x38,
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
            throw new IllegalStateException("compact C2 expects exactly 12 resident blocks");
        }
        if (RuntimeV1ResidentBlocks.totalResidentBytes(rom) != RESIDENT_DATA_SIZE) {
            throw new IllegalStateException("compact C2 resident data size mismatch");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Position-independent Thumb copier, independently assembled for ARM7TDMI.
        //
        // r4 = table
        // r6 = sequential data blob
        // r7 = 0x03000000 IWRAM base
        // r5 = 12 block records
        //
        // Each table record is:
        //   u16 destination low halfword
        //   u16 size
        //
        // All destinations are 0x0300xxxx, so this keeps metadata at only
        // four bytes per block.
        byte[] code = new byte[] {
                (byte)0xF0,(byte)0xB4,
                0x09,(byte)0xA4,
                0x14,(byte)0xA6,
                0x03,0x27,
                0x3F,0x06,
                0x0C,0x25,
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
                (byte)0xF0,(byte)0xBC,
                0x00,(byte)0xBD
        };
        out.writeBytes(code);

        // 12 x 4-byte records.
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            long address = block.address();
            if ((address & 0xFFFF0000L) != 0x03000000L) {
                throw new IllegalStateException(
                        "compact C2 block is outside expected IWRAM: 0x"
                                + Long.toHexString(address)
                );
            }
            u16(out, (int)(address & 0xFFFF));
            u16(out, block.data().length);
        }

        // Sequential compact data. Wrapper is last, so the temporary bootstrap
        // at 03005310 is overwritten only after every other resident block.
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            out.writeBytes(block.data());
        }

        byte[] blob = out.toByteArray();
        if (blob.length != NATIVE_BLOB_SIZE) {
            throw new IllegalStateException(
                    "compact C2 native blob expected " + NATIVE_BLOB_SIZE
                            + " bytes, got " + blob.length
            );
        }
        return blob;
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
        return HEADER_SIZE + NATIVE_BLOB_SIZE + fieldInstallerSize(rom);
    }

    static int freeBytes(RomProfile rom) {
        return RamScript.SCRIPT_SIZE - scriptSize(rom);
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
