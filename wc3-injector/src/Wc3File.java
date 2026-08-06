
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/* this class act as a constructor for the wondercard file (either .wc3 or .bin), it can read, validate the file, store its bytes and
return some fields as methods */

public final class Wc3File {
    public static final int FILE_SIZE = 0x58C; /* a wonder card file must always be 1420 bytes (0x58C) */
    static final int CARD_BLOCK_SIZE = 0x1A0; /* this is the size of the wonder card details block (title, message, colors, icon) 416 bytes */
    static final int RAM_SCRIPT_SOURCE_OFFSET = 0x1A0; /* this is the start point of the actual wc script (what it does), starts at 416 bytes and can go up to FILE_SIZE */

    private final byte[] data;

    private Wc3File(byte[] data) { /* private constructor, must call load method */
        this.data = data;
    }

    public static Wc3File load(Path path) throws IOException { /* reads a .wc3 and load its content to an insnace of W3File */
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

    /* reads two bytes starting at 0x04 (right after cardCrc), returns the flagId, which a value that tells the game which flag 
    they should check (aurora or mystic) too wether the event was already claimed or not. unused for now, later on with custom injections
    might need to try a workaround to prevent being blocked by the game. */
    public int flagId() { 
        return Binary.u16(data, 0x04);
    }

    /* reads two bytes starting at 0x06 (right after flagId), returns the iconSpecies, which a value that tells the game which icon
    they should display on the wonder card image. unused for injection */
    public int iconSpecies() {
        return Binary.u16(data, 0x06);
    }

    /* reads two bytes from the start (0x00) of the file, returns the CRC of the wc, unused for injection, game calculate these 
    bytes to check if a wc is valid */
    public int cardCrc() { 
        return Binary.u16(data, 0x00);
    }
}
