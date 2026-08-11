/* CRC16 implementation used by FireRed/LeafGreen. This is the bit-by-bit equivalent of CalcCRC16/CalcCRC16WithTable from util.c. */
final class Crc16 {
    private Crc16() {}

    static int calculate(byte[] data, int offset, int length) {
        int crc = 0x1121;

        for (int i = 0; i < length; i++) {
            crc ^= Binary.u8(data, offset + i);

            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0x8408;
                } else {
                    crc >>>= 1;
                }
            }
        }

        return (~crc) & 0xFFFF;
    }
}
