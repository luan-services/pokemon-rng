import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/* Builds an FR/LG Wireless Adapter distribution ROM from the matching
   untouched US Aurora Ticket distribution ROM and an arbitrary WC3.

   The transport behavior remains Nintendo's original FR/LG sender. This class
   only replaces the Wonder Card, relocates the 0x400-byte script source, and
   replaces the pre-rendered AURORA TICKET title with our MYSTERY GIFT asset. */
public final class DistributionRom {
    public static final int ROM_SIZE = 0x400000;
    public static final String REQUIRED_BASE_SHA1 = "94a21d133e3f1b1a2129f450a9bc753d12c13b09";

    private static final int CARD_ROM_OFFSET = 0x14FBC;
    private static final int CARD_SIZE = WonderCard.SIZE; // 0x14C
    private static final int SCRIPT_ROM_OFFSET = 0x0C0000;
    private static final int SCRIPT_SEND_SIZE = 0x400;
    private static final int SCRIPT_POINTER_ROM_OFFSET = 0x167E0;
    private static final int SCRIPT_GBA_ADDRESS = 0x080C0000;

    private static final int TITLE_TILES_OFFSET = 0x62AC0;
    private static final int TILE_SIZE = 32;
    private static final int[][] TITLE_TILE_RANGES = {
            {128, 148}, {160, 180}, {192, 212}
    };

    /* 60 4bpp tiles (20x3), generated from scratch for this project.
       They replace only the title tiles; Nintendo's palette/tilemaps/background
       remain untouched. Goppier/Project Wonder assets are not used. */
    private static final String MYSTERY_GIFT_TILES_GZIP_BASE64 =
            "H4sIAAp4qWoC/6WUz2sTQRTHvySTpNtk9d/wIuxRPDR795D8A9aANz00B0HBQ3LxJrRH8dKCIjIguYiIYJN/oCbnKGxPHoy4IUndnTTd8U02DZvd2ZaYB99lmDePeT8+O8D6JiqkfdKflANNydAjjQOmc3sm0GkAtpsSf2bauMj59bbt6Nx99zHan4NiBkcN/fVS1jgPaDnT+aU0J5wbE1pO9H4pP3DOt1PSMy0pDcvlsNpav3Tugfepfojif9YPqh/1duoI3qgqAX9X5/z9rYDjgZsj/44+3IkoaTbVbzuc2y19tDwUkj96x9OSo3DnV4u/7tzExuZPGfy/BvyPJvwnFfjskPSd5DGfjYz5gRPBrIZgzbJgnZJg4y3BvKxgPyAYhAGcErC1rgWcfwJBGfZODc9qn5bre2LrYM9BzX0ATGlg1ezy9mFp0aSnefq4z7PoS6eAh+fUGUvxJRaMJUTDn+XD9eQqKcYupZhT7EX3FmnOlScZxB4u2ZvzN7xBok6PS7BnOSl3ib2uSq1Ae9vX1d+8BbwgUp5RxB2SqjMTHYBFkB4QrD26YkhcjkrhlWq489+vTi8BXi3PB5Tfe67q9sNeOCu8ZWNSjC2lmFPsRfaixik5GfspVRpXaVPzWOe2x5otj1nSY8n3pEGMlUkmqaB4S1hkgNoHo1emHqqBTUlVTQrTLKpO98vFzsB9mXCyJHtxizO3YnH20tK/S6po37sh/TKjYghHb3/9+q8xxR/8+yGzeJs8UKX9n8HXQessOM4dxb1R1jKa926Fv9b6fGzK2z9Euvl/gAcAAA==";

    private DistributionRom() {}

    public static BuildResult build(Path baseRomPath, Wc3File wc3, Path output) throws IOException {
        byte[] base = Files.readAllBytes(baseRomPath);
        validateBase(base);
        byte[] rom = Arrays.copyOf(base, base.length);

        byte[] wc3Bytes = wc3.copyBytes();
        System.arraycopy(wc3Bytes, Wc3File.WONDER_CARD_OFFSET, rom, CARD_ROM_OFFSET, CARD_SIZE);

        // The sender expects exactly 0x400 bytes. WC3 executable body is 0x3E4
        // bytes (0x1A8..0x58B); the remaining 0x1C bytes are deterministic zero padding.
        Arrays.fill(rom, SCRIPT_ROM_OFFSET, SCRIPT_ROM_OFFSET + SCRIPT_SEND_SIZE, (byte) 0);
        int executableSize = Wc3File.FILE_SIZE - 0x1A8;
        System.arraycopy(wc3Bytes, 0x1A8, rom, SCRIPT_ROM_OFFSET, executableSize);
        Binary.putU32(rom, SCRIPT_POINTER_ROM_OFFSET, SCRIPT_GBA_ADDRESS);

        patchMysteryGiftTitle(rom);

        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(output, rom);
        return new BuildResult(REQUIRED_BASE_SHA1, sha1Hex(rom), rom.length, SCRIPT_ROM_OFFSET, SCRIPT_SEND_SIZE);
    }

    private static void validateBase(byte[] base) {
        if (base.length != ROM_SIZE) {
            throw new IllegalArgumentException("Unsupported distribution ROM size: " + base.length
                    + " bytes; expected " + ROM_SIZE + " (0x400000)");
        }
        String actual = sha1Hex(base);
        if (!actual.equals(REQUIRED_BASE_SHA1)) {
            throw new IllegalArgumentException("Unsupported base distribution ROM SHA-1: " + actual
                    + "; expected untouched Aurora Ticket (USA) " + REQUIRED_BASE_SHA1);
        }
    }

    private static void patchMysteryGiftTitle(byte[] rom) throws IOException {
        byte[] packed = Base64.getDecoder().decode(MYSTERY_GIFT_TILES_GZIP_BASE64);
        byte[] tiles;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(packed))) {
            tiles = gzip.readAllBytes();
        }
        if (tiles.length != 60 * TILE_SIZE) {
            throw new IOException("Internal MYSTERY GIFT title asset has invalid size: " + tiles.length);
        }

        int src = 0;
        for (int[] range : TITLE_TILE_RANGES) {
            int count = range[1] - range[0];
            int length = count * TILE_SIZE;
            int dst = TITLE_TILES_OFFSET + range[0] * TILE_SIZE;
            System.arraycopy(tiles, src, rom, dst, length);
            src += length;
        }
    }

    private static String sha1Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder result = new StringBuilder(40);
            for (byte value : digest) result.append(String.format("%02x", value & 0xFF));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 unavailable", exception);
        }
    }

    public record BuildResult(String baseSha1, String outputSha1, int sizeBytes,
                              int scriptRomOffset, int scriptSendSize) {}
}
