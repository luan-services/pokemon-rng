import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/* this class act as a constructor for the wondercard file (either .wc3 or .bin), it can read, validate the file and store its bytes.
it includes all the wc3 file data, but for card details related data, we use the wondercard class */

public final class Wc3File {
    public static final int FILE_SIZE = 0x58C; /* a wonder card file must always be 1420 bytes (0x58C) */
    static final int CARD_BLOCK_SIZE = 0x1A0; /* this is the size of the wonder card details block (title, message, colors, icon) 416 bytes */
    static final int WONDER_CARD_OFFSET = 0x004; /* cardCrc occupies the first 4 bytes, so the actual WonderCard structure starts at 0x04 */
    static final int RAM_SCRIPT_OFFSET = 0x1A0; /* this is the start point of the actual wc script (what it does), starts at 416 bytes and can go up to FILE_SIZE */
    static final int CARD_METADATA_ICON_SPECIES_OFFSET = 0x15A; /* offset of WonderCardMetadata.iconSpecies inside the wc3 card block */

    private final byte[] data;
    private final WonderCard wonderCard;

    private Wc3File(byte[] data) { /* private constructor, must call load method */
        this.data = data;
        this.wonderCard = new WonderCard(data, WONDER_CARD_OFFSET); /* WonderCard uses the same byte array, it does not create another copy */
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

    /* returns the WonderCard structure stored inside this wc3 file */
    public WonderCard wonderCard() {
        return wonderCard;
    }
}
