import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/* this class act as a constructor for the wondercard file (either .wc3 or .bin), it can read, validate the file and store its bytes.
it includes all the wc3 file data, but for card details related data, we use the wondercard class */

public final class Wc3File {
    public static final int FILE_SIZE = 0x58C; /* a wonder card file must always be 1420 bytes (0x58C) */
    static final int CARD_BLOCK_SIZE = 0x1A0; /* this is the size of the wonder card details block (title, message, colors, icon) 416 bytes */
    static final int CARD_CRC_OFFSET = 0x000; /* the first 4 bytes store the card CRC value */
    static final int WONDER_CARD_OFFSET = 0x004; /* cardCrc occupies the first 4 bytes, so the actual WonderCard structure starts at 0x04 */
    static final int RAM_SCRIPT_OFFSET = 0x1A0; /* this is the start point of the actual wc script (what it does), starts at 416 bytes and can go up to FILE_SIZE */
    static final int CARD_METADATA_ICON_SPECIES_OFFSET = 0x15A; /* offset of WonderCardMetadata.iconSpecies inside the wc3 card block */

    static final int RAM_SCRIPT_SIZE = FILE_SIZE - RAM_SCRIPT_OFFSET;
    static final int RAM_SCRIPT_CHECKSUM_OFFSET = RAM_SCRIPT_OFFSET;
    static final int RAM_SCRIPT_DATA_OFFSET = RAM_SCRIPT_OFFSET + 0x004;
    static final int RAM_SCRIPT_DATA_SIZE = RAM_SCRIPT_SIZE - 0x004;

    private final byte[] data;
    private final WonderCard wonderCard;

    private Wc3File(byte[] data) { /* private constructor, must call load method */
        this.data = data;
        this.wonderCard = new WonderCard(data, WONDER_CARD_OFFSET); /* WonderCard uses the same byte array, it does not create another copy */
    }

    public static Wc3File load(Path path) throws IOException { /* reads a .wc3 and load its content to an insnace of W3File */
        return fromBytes(Files.readAllBytes(path));
    }

    /* creates a Wc3File from an already available byte array.
    a defensive copy is used so callers cannot modify the file behind this object's back. */
    static Wc3File fromBytes(byte[] bytes) {
        if (bytes.length != FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Unsupported WC3 size: " + bytes.length +
                    " bytes; expected " + FILE_SIZE + " (0x58C)"
            );
        }

        return new Wc3File(Arrays.copyOf(bytes, bytes.length));
    }

    byte[] data() {
        return data;
    }

    byte[] copyBytes() {
        return Arrays.copyOf(data, data.length);
    }

    /* returns the WonderCard structure stored inside this wc3 file */
    public WonderCard wonderCard() {
        return wonderCard;
    }

    /* returns the CRC stored in the first four bytes. only the low 16 bits are meaningful. */
    public int storedCardCrc() {
        return (int) (Binary.u32(data, CARD_CRC_OFFSET) & 0xFFFF);
    }

    /* calculates the CRC over only the WonderCard structure, matching the game behavior. */
    public int calculatedCardCrc() {
        return Crc16.calculate(data, WONDER_CARD_OFFSET, WonderCard.SIZE);
    }

    public boolean isCardCrcValid() {
        return storedCardCrc() == calculatedCardCrc();
    }

    /* returns the checksum stored at the beginning of the 0x3EC-byte RamScript block. */
    public int storedRamScriptChecksum() {
        return (int) (Binary.u32(data, RAM_SCRIPT_CHECKSUM_OFFSET) & 0xFFFF);
    }

    public int calculatedRamScriptChecksum() {
        return Crc16.calculate(data, RAM_SCRIPT_DATA_OFFSET, RAM_SCRIPT_DATA_SIZE);
    }

    public boolean isRamScriptChecksumValid() {
        return storedRamScriptChecksum() == calculatedRamScriptChecksum();
    }

    /* content validation is intentionally permissive.
    this tool is also used for custom/research Wonder Cards, so unusual flag IDs,
    icon species values, CRCs or RamScript checksums are reported as warnings
    instead of blocking injection/extraction. */
    public String[] validationWarnings() {
        java.util.ArrayList<String> warnings = new java.util.ArrayList<>();

        if (wonderCard.flagId() == 0) {
            warnings.add("flagId is 0");
        }

        if (!isCardCrcValid()) {
            warnings.add(String.format(
                    "Wonder Card CRC mismatch (stored 0x%04X, calculated 0x%04X)",
                    storedCardCrc(),
                    calculatedCardCrc()
            ));
        }

        if (!isRamScriptChecksumValid()) {
            warnings.add(String.format(
                    "RamScript checksum mismatch (stored 0x%04X, calculated 0x%04X)",
                    storedRamScriptChecksum(),
                    calculatedRamScriptChecksum()
            ));
        }

        return warnings.toArray(String[]::new);
    }

    public void write(Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(output, data);
    }
}
