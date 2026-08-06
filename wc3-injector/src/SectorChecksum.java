
final class SectorChecksum {
    private SectorChecksum() {}

    static int calculate(byte[] save, int offset, int size) {
        if ((size & 3) != 0) {
            throw new IllegalArgumentException("Checksum size must be divisible by 4");
        }

        long sum = 0;
        for (int i = 0; i < size; i += 4) {
            sum = (sum + Binary.u32(save, offset + i)) & 0xFFFF_FFFFL;
        }
        return (int) (((sum >>> 16) + sum) & 0xFFFF);
    }
}
