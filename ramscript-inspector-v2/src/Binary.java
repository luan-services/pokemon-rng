/* this class has auxiliar methods to deal with binaries in java. java directly doesn't support u16 and u32 integers, so
we made methods to read/write them. */

final class Binary {
    private Binary() {}

    static int u8(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset]);
    }

    static int u16(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset])
                | (Byte.toUnsignedInt(data[offset + 1]) << 8);
    }

    static long u32(byte[] data, int offset) {
        return Byte.toUnsignedLong(data[offset])
                | (Byte.toUnsignedLong(data[offset + 1]) << 8)
                | (Byte.toUnsignedLong(data[offset + 2]) << 16)
                | (Byte.toUnsignedLong(data[offset + 3]) << 24);
    }
}
