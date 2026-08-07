
/* this class has auxiliar methods to deal with binaries in java. java directly doesn't support u16 and u32 integers, so
we made methods to read/write them. */

final class Binary {
    private Binary() {}

    static int u8(byte[] data, int offset) { /* unused for injection */
        return Byte.toUnsignedInt(data[offset]);
    }

    static int u16(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset])
                | (Byte.toUnsignedInt(data[offset + 1]) << 8);
    }

    static long u32(byte[] data, int offset) {
        return Integer.toUnsignedLong(
                Byte.toUnsignedInt(data[offset])
                        | (Byte.toUnsignedInt(data[offset + 1]) << 8)
                        | (Byte.toUnsignedInt(data[offset + 2]) << 16)
                        | (Byte.toUnsignedInt(data[offset + 3]) << 24)
        );
    }

    static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }

    static void putU32(byte[] data, int offset, long value) { /* unused for injection */
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
