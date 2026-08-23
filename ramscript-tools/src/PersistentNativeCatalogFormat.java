/*
   Binary contract for the persistent native-module catalog used by the
   production SB2 path.

   The values intentionally remain compatible with the Build-10 V6 catalog,
   but production code no longer depends on the archived Build-10 classes.
*/
final class PersistentNativeCatalogFormat {
    static final long MAGIC = 0x54504843L; // CHPT
    static final int VERSION = 6;
    static final int HEADER_SIZE = 0x10;
    static final int ENTRY_SIZE = 0x10;
    static final int KIND_THUMB = 1;
    static final int LOCATION_SB1 = 1;
    static final int LOCATION_SB2 = 2;

    private PersistentNativeCatalogFormat() {}

    static int checksum16(byte[] data) {
        int sum = 0;
        for (byte b : data) sum = (sum + Byte.toUnsignedInt(b)) & 0xFFFF;
        return sum;
    }

    static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }

    static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
