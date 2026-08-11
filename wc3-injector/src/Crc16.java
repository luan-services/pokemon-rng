/* this class is used for calculating the CRC used by Wonder Card and RamScript data.
the algorithm is the same one used by the other wc3/ramscript tools in this project family. */

final class Crc16 {
    private static final int[] TABLE = new int[256];

    static {
        for (int i = 0; i < TABLE.length; i++) {
            int value = i;
            for (int bit = 0; bit < 8; bit++) {
                value = ((value & 1) != 0)
                        ? ((value >>> 1) ^ 0x8408)
                        : (value >>> 1);
            }
            TABLE[i] = value & 0xFFFF;
        }
    }

    private Crc16() {}

    static int calculate(byte[] data, int offset, int length) {
        int crc = 0x1121;

        for (int i = 0; i < length; i++) {
            int current = Byte.toUnsignedInt(data[offset + i]);
            crc = ((crc >>> 8) ^ TABLE[(crc ^ current) & 0xFF]) & 0xFFFF;
        }

        return (~crc) & 0xFFFF;
    }
}
