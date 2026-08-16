final class CompactInstallerCandidate1 {
    static final long COPIER_ADDRESS = 0x03005310L;
    static final long DESTINATION = 0x03003F42L;
    static final int BLOB_OFFSET = 0x0A;
    static final int BLOB_SIZE = 14;
    static final long VIRTUAL_BASE = 0x08010000L;

    private CompactInstallerCandidate1() {}

    static RamScript build(RomProfile rom) {
        byte[] blob = supervisorBlob();
        byte[] copier = copierBytes(rom);

        byte[] installer = new FieldScriptWriter()
                .writeBytes(COPIER_ADDRESS, copier)
                .callNative(COPIER_ADDRESS | 1L)
                .returnRam()
                .build();

        int installerOffset = BLOB_OFFSET + blob.length;
        byte[] script = new byte[installerOffset + installer.length];

        int p = 0;
        script[p++] = (byte)0xB8;
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        script[p++] = (byte)0xBA;
        putU32(script, p, VIRTUAL_BASE + installerOffset);
        p += 4;

        if (p != BLOB_OFFSET) throw new IllegalStateException("compact C1 blob offset mismatch");

        System.arraycopy(blob, 0, script, p, blob.length);
        p += blob.length;

        if (p != installerOffset) throw new IllegalStateException("compact C1 installer offset mismatch");

        System.arraycopy(installer, 0, script, p, installer.length);

        if (script.length > RamScript.SCRIPT_SIZE) {
            throw new IllegalStateException("compact C1 exceeds RamScript capacity");
        }

        return RamScript.createWonderCard(script);
    }

    static byte[] supervisorBlob() {
        return new byte[] {
                0x18,(byte)0xA3, 0x07,(byte)0xCB, 0x03,0x68, (byte)0x8B,0x42,
                (byte)0xB3,(byte)0xD1, 0x02,0x60, (byte)0xB1,(byte)0xE7
        };
    }

    static byte[] copierBytes(RomProfile rom) {
        byte[] out = new byte[] {
                0x00,(byte)0xB5,
                0x07,0x4B,
                0x00,(byte)0xF0, 0x0A,(byte)0xF8,
                0x0A,0x30,
                0x06,0x49,
                0x0E,0x22,
                0x03,0x78,
                0x0B,0x70,
                0x01,0x30,
                0x01,0x31,
                0x01,0x3A,
                (byte)0xF9,(byte)0xD1,
                0x00,(byte)0xBD,
                0x18,0x47,
                (byte)0xC0,0x46,
                0,0,0,0,
                0,0,0,0
        };

        putU32(out, 0x20, rom.getSavedRamScriptThumb);
        putU32(out, 0x24, DESTINATION);
        return out;
    }

    static int installerSize(RomProfile rom) {
        return new FieldScriptWriter()
                .writeBytes(COPIER_ADDRESS, copierBytes(rom))
                .callNative(COPIER_ADDRESS | 1L)
                .returnRam()
                .build()
                .length;
    }

    static int scriptSize(RomProfile rom) {
        return BLOB_OFFSET + BLOB_SIZE + installerSize(rom);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
