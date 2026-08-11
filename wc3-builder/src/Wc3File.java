import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/* this class act as a constructor for the wondercard file (either .wc3 or .bin), it can read, validate the file and store its bytes.
it includes all the wc3 file data, but for card details related data, we use a child wondercard class */

final class Wc3File {
    static final int FILE_SIZE = 0x58C; /* a wonder card file must always be 1420 bytes (0x58C) */
    static final int CARD_CRC_OFFSET = 0x000; /* set the start point of the card's crc value */
    static final int WONDER_CARD_OFFSET = 0x004; /* cardCrc occupies the first 4 bytes, so the actual WonderCard structure starts at 0x04 */
    static final int RAM_SCRIPT_OFFSET = 0x1A0; /* this is the start point of the actual wc script (what it does), starts at 416 bytes and can go up to FILE_SIZE */
    static final int RAM_SCRIPT_SIZE = FILE_SIZE - RAM_SCRIPT_OFFSET; /* complete RamScript size: 0x3EC bytes */
    static final int RAM_SCRIPT_DATA_OFFSET = RAM_SCRIPT_OFFSET + 0x004;
    static final int RAM_SCRIPT_DATA_SIZE = RAM_SCRIPT_SIZE - 0x004;
    private static final int CARD_METADATA_ICON_SPECIES_OFFSET = 0x15A; /* offset of WonderCardMetadata.iconSpecies inside the wc3 card 
        block, the original game copies WonderCard.iconSpecies to this field */

    private final byte[] data;
    private final WonderCard wonderCard;

    private Wc3File(byte[] data) { /* private constructor, must call load method */
        this.data = data;
        this.wonderCard = new WonderCard(data, WONDER_CARD_OFFSET); /* WonderCard uses the same byte array, it does not create another copy */
    }

    /* creates a new blank WC3 container. the card details and the default RamScript are filled by Wc3Factory. */
    static Wc3File createEmpty() {
        return new Wc3File(new byte[FILE_SIZE]);
    }

    static Wc3File load(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);

        if (data.length != FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Expected a 0x58C-byte WC3 file, but received "
                            + data.length + " bytes"
            );
        }

        return new Wc3File(data);
    }

    WonderCard wonderCard() { /* getter for the WonderCard object instance on this file */
        return wonderCard;
    }

    /* specific methods for wc3-builder */

    int storedCardCrc() { /* getter for the card CRC */
        return (int) (Binary.u32(data, CARD_CRC_OFFSET) & 0xFFFF);
    }

    int calculatedCardCrc() { /* method to calculates the actual card CRC */
        return Crc16.calculate(
                data,
                WONDER_CARD_OFFSET,
                WonderCard.SIZE
        );
    }

    boolean isCardCrcValid() { /* compare both to see if they are valid */
        return storedCardCrc() == calculatedCardCrc();
    }

    void updateCardCrc() {
        Binary.putU32(
                data,
                CARD_CRC_OFFSET,
                calculatedCardCrc()
        );
    }

    int metadataIconSpecies() {
        return Binary.u16(
                data,
                CARD_METADATA_ICON_SPECIES_OFFSET
        );
    }

    void setIconSpecies(int species) {
        wonderCard.setIconSpecies(species);
        Binary.putU16(
                data,
                CARD_METADATA_ICON_SPECIES_OFFSET,
                species
        );
    }

    byte[] ramScriptCopy() {
        return Arrays.copyOfRange(
                data,
                RAM_SCRIPT_OFFSET,
                data.length
        );
    }

    void setRamScript(byte[] ramScript) {
        if (ramScript.length != RAM_SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "RamScript must be exactly 0x3EC bytes, got " + ramScript.length
            );
        }

        System.arraycopy(
                ramScript,
                0,
                data,
                RAM_SCRIPT_OFFSET,
                ramScript.length
        );
    }

    int storedRamScriptChecksum() {
        return (int) (Binary.u32(data, RAM_SCRIPT_OFFSET) & 0xFFFF);
    }

    int calculatedRamScriptChecksum() {
        return Crc16.calculate(
                data,
                RAM_SCRIPT_DATA_OFFSET,
                RAM_SCRIPT_DATA_SIZE
        );
    }

    boolean isRamScriptChecksumValid() {
        return storedRamScriptChecksum() == calculatedRamScriptChecksum();
    }

    byte[] copyBytes() {
        return Arrays.copyOf(data, data.length);
    }

    void write(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(path, data);
    }
}
