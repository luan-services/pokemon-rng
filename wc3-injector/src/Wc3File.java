
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Wc3File {
    public static final int FILE_SIZE = 0x58C;
    static final int CARD_BLOCK_SIZE = 0x1A0;
    static final int RAM_SCRIPT_SOURCE_OFFSET = 0x1A0;

    private final byte[] data;

    private Wc3File(byte[] data) {
        this.data = data;
    }

    public static Wc3File load(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        if (data.length != FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Unsupported WC3 size: " + data.length +
                    " bytes; expected " + FILE_SIZE + " (0x58C)"
            );
        }
        return new Wc3File(data);
    }

    byte[] data() {
        return data;
    }

    public int flagId() {
        return Binary.u16(data, 0x04);
    }

    public int iconSpecies() {
        return Binary.u16(data, 0x06);
    }

    public int cardCrc() {
        return Binary.u16(data, 0x00);
    }
}
