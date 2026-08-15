final class Binary {
    private Binary() {}

    static long u32(byte[] data, int offset) {
        return Byte.toUnsignedLong(data[offset])
                | (Byte.toUnsignedLong(data[offset + 1]) << 8)
                | (Byte.toUnsignedLong(data[offset + 2]) << 16)
                | (Byte.toUnsignedLong(data[offset + 3]) << 24);
    }

    static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
