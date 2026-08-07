/* this class represents only the WonderCard structure inside a wc3 file. unlike Wc3File, it does not know the full wc3 layout,
it only knows where each WonderCard field is located relative to the start of the WonderCard structure. */

public final class WonderCard {
    private static final int FLAG_ID_OFFSET = 0x000; /* flagId is the first u16 field inside the WonderCard structure */
    private static final int ICON_SPECIES_OFFSET = 0x002; /* iconSpecies comes right after flagId and tells the game which icon should be displayed */

    private final byte[] data;
    private final int baseOffset;

    WonderCard(byte[] data, int baseOffset) {
        this.data = data; /* this is the same byte array stored by Wc3File, it is not a copy */
        this.baseOffset = baseOffset; /* WonderCard starts at 0x04 inside a wc3 file */
    }

    /* returns the flagId stored inside the WonderCard structure */
    public int flagId() {
        return Binary.u16(data, baseOffset + FLAG_ID_OFFSET);
    }

    /* returns the iconSpecies stored inside the WonderCard structure */
    public int iconSpecies() {
        return Binary.u16(data, baseOffset + ICON_SPECIES_OFFSET);
    }
}
