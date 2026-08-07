/* this class represents only the WonderCard structure inside a wc3 file. unlike Wc3File, it does not know the full wc3 layout,
it only knows where each WonderCard field is located relative to the start of the WonderCard structure. here we are able to edit
every detail (color, text, icon) on the wonder card structure */


final class WonderCard {
    static final int SIZE = 0x14C;
    static final int TEXT_LENGTH = 40;
    static final int BODY_LINE_COUNT = 4;

    /* all these offset attributes are based on the starting point of the wondercard structure, not the actual memory address:
    
    Wc3File - starts at 0x000
    WonderCard design structure - starts at 0x004
    when you call this class constructor you pass the base offset as 0x004, so for every memory access here the program will call
    baseOffset + structure_offset (FLAG_ID_OFFSET for example)
    */

    private static final int FLAG_ID_OFFSET = 0x000; 
    private static final int ICON_SPECIES_OFFSET = 0x002;
    private static final int ID_NUMBER_OFFSET = 0x004;
    private static final int PACKED_FLAGS_OFFSET = 0x008;
    private static final int MAX_STAMPS_OFFSET = 0x009;
    private static final int TITLE_OFFSET = 0x00A;
    private static final int SUBTITLE_OFFSET = 0x032;
    private static final int BODY_OFFSET = 0x05A;
    private static final int FOOTER_1_OFFSET = 0x0FA;
    private static final int FOOTER_2_OFFSET = 0x122;

    /* attributes to store both the full Wc3File bytes and the actual offset where the wonder card details address starts */
    private final byte[] data;
    private final int baseOffset; 

    WonderCard(byte[] data, int baseOffset) {
        this.data = data;
        this.baseOffset = baseOffset;
    }

    /* getters and setters for every wondercard card fields / mechanics */

    int flagId() {
        return Binary.u16(data, baseOffset + FLAG_ID_OFFSET);
    }

    void setFlagId(int value) {
        requireU16("flagId", value);
        Binary.putU16(data, baseOffset + FLAG_ID_OFFSET, value);
    }

    int iconSpecies() {
        return Binary.u16(data, baseOffset + ICON_SPECIES_OFFSET);
    }

    /* low-level setter for the WonderCard struct only. use Wc3File.setIconSpecies() when editing a WC3 file, because the game also 
    mirrors this value into cardMetadata.iconSpecies. */
    void setIconSpecies(int value) {
        requireU16("iconSpecies", value);
        Binary.putU16(data, baseOffset + ICON_SPECIES_OFFSET, value);
    }

    long idNumber() {
        return Binary.u32(data, baseOffset + ID_NUMBER_OFFSET);
    }

    void setIdNumber(long value) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("idNumber must fit in u32");
        }
        Binary.putU32(data, baseOffset + ID_NUMBER_OFFSET, value);
    }

    int type() {
        return packedFlags() & 0b11;
    }

    void setType(int value) {
        if (value < 0 || value > 2) {
            throw new IllegalArgumentException("type must be 0 (GIFT), 1 (STAMP), or 2 (LINK_STAT)");
        }
        int packed = packedFlags();
        packed = (packed & ~0b11) | value;
        data[baseOffset + PACKED_FLAGS_OFFSET] = (byte) packed;
    }

    int backgroundType() {
        return (packedFlags() >>> 2) & 0b1111;
    }

    void setBackgroundType(int value) {
        if (value < 0 || value > 7) {
            throw new IllegalArgumentException("background type must be between 0 and 7");
        }
        int packed = packedFlags();
        packed = (packed & ~(0b1111 << 2)) | (value << 2);
        data[baseOffset + PACKED_FLAGS_OFFSET] = (byte) packed;
    }

    int sendType() {
        return (packedFlags() >>> 6) & 0b11;
    }

    void setSendType(int value) {
        if (value < 0 || value > 2) {
            throw new IllegalArgumentException(
                    "send type must be 0 (DISALLOWED), 1 (ALLOWED), or 2 (ALLOWED_ALWAYS)"
            );
        }
        int packed = packedFlags();
        packed = (packed & ~(0b11 << 6)) | (value << 6);
        data[baseOffset + PACKED_FLAGS_OFFSET] = (byte) packed;
    }

    int maxStamps() {
        return Binary.u8(data, baseOffset + MAX_STAMPS_OFFSET);
    }

    void setMaxStamps(int value) {
        if (value < 0 || value > 7) {
            throw new IllegalArgumentException("maxStamps must be between 0 and 7");
        }
        data[baseOffset + MAX_STAMPS_OFFSET] = (byte) value;
    }

    String title() {
        return textAt(TITLE_OFFSET);
    }

    void setTitle(String value) {
        setTextAt(TITLE_OFFSET, value);
    }

    String subtitle() {
        return textAt(SUBTITLE_OFFSET);
    }

    void setSubtitle(String value) {
        setTextAt(SUBTITLE_OFFSET, value);
    }

    String bodyLine(int index) {
        requireBodyIndex(index);
        return textAt(BODY_OFFSET + index * TEXT_LENGTH);
    }

    void setBodyLine(int index, String value) {
        requireBodyIndex(index);
        setTextAt(BODY_OFFSET + index * TEXT_LENGTH, value);
    }

    String footerLine1() {
        return textAt(FOOTER_1_OFFSET);
    }

    void setFooterLine1(String value) {
        setTextAt(FOOTER_1_OFFSET, value);
    }

    String footerLine2() {
        return textAt(FOOTER_2_OFFSET);
    }

    void setFooterLine2(String value) {
        setTextAt(FOOTER_2_OFFSET, value);
    }

    String typeName() {
        return switch (type()) {
            case 0 -> "GIFT";
            case 1 -> "STAMP";
            case 2 -> "LINK_STAT";
            default -> "UNKNOWN";
        };
    }

    String sendTypeName() {
        return switch (sendType()) {
            case 0 -> "DISALLOWED";
            case 1 -> "ALLOWED";
            case 2 -> "ALLOWED_ALWAYS";
            default -> "UNKNOWN";
        };
    }

    private int packedFlags() {
        return Binary.u8(data, baseOffset + PACKED_FLAGS_OFFSET);
    }

    private String textAt(int relativeOffset) {
        return Gen3TextCodec.decode(data, baseOffset + relativeOffset, TEXT_LENGTH);
    }

    private void setTextAt(int relativeOffset, String value) {
        Gen3TextCodec.encodeInto(value, data, baseOffset + relativeOffset, TEXT_LENGTH);
    }

    private static void requireU16(String name, int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException(name + " must fit in u16");
        }
    }

    private static void requireBodyIndex(int index) {
        if (index < 0 || index >= BODY_LINE_COUNT) {
            throw new IllegalArgumentException("body line index must be between 0 and 3");
        }
    }
}
