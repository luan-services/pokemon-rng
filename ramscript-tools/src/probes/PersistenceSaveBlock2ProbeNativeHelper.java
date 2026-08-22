/* Experimental full-region probe for SaveBlock2::filler_B20[0x400].
   Build 3a fixes the writer loop branch so every byte is written. */
final class PersistenceSaveBlock2ProbeNativeHelper {
    static final int STORAGE_OFFSET = 0x0B20;
    static final int STORAGE_SIZE = 0x400;

    private PersistenceSaveBlock2ProbeNativeHelper() {}

    static NativeHelper buildWriterAt(RomProfile rom, long address) {
        byte[] code = new byte[] {
                0x06,0x48, 0x00,0x68, 0x06,0x4B, (byte)0xC0,0x18,
                0x00,0x21, 0x06,0x4A, 0x01,0x70, 0x01,0x30,
                0x01,0x31, 0x09,0x06, 0x09,0x0E, 0x01,0x3A,
                (byte)0xF8,(byte)0xD1, 0x70,0x47,
                0,0,0,0, 0x20,0x0B,0,0, 0x00,0x04,0,0
        };
        putU32(code, 0x1C, rom.saveBlock2Ptr);
        return new NativeHelper(address, code);
    }

    static NativeHelper buildCheckerAt(RomProfile rom, long address) {
        byte[] code = new byte[] {
                0x0A,0x48, 0x00,0x68, 0x0A,0x4B, (byte)0xC0,0x18,
                0x00,0x21, 0x0A,0x4A, 0x03,0x78, (byte)0x8B,0x42,
                0x07,(byte)0xD1, 0x01,0x30, 0x01,0x31, 0x09,0x06,
                0x09,0x0E, 0x01,0x3A, (byte)0xF6,(byte)0xD1, 0x01,0x20,
                0x00,(byte)0xE0, 0x00,0x20, 0x04,0x49, 0x08,(byte)0x80,
                0x70,0x47, (byte)0xC0,0x46,
                0,0,0,0, 0x20,0x0B,0,0, 0x00,0x04,0,0, 0,0,0,0
        };
        putU32(code, 0x2C, rom.saveBlock2Ptr);
        putU32(code, 0x38, rom.specialVarResult);
        return new NativeHelper(address, code);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
