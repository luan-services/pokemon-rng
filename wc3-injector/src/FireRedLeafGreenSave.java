import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/* this the program main class, it stores constants relateds to the save file structure. it also include methods for loading / injection
on the save file. */

public final class FireRedLeafGreenSave {
    private static final int SAVE_SIZE = 0x20000; /* full save file size, expected to be exactly 131072 bytes (128kb) */
    private static final int SECTOR_SIZE = 0x1000; /* save file is splitted into sectors, each having 4096 bytes */
    /* private static final int SECTOR_DATA_SIZE = 0xF80; sector mains data section has 3968 bytes, after that comes the section footer (which display important shared data about save file) */
    private static final int SECTORS_PER_SLOT = 14; /* sector count per save slot */
    private static final int SLOT_COUNT = 2; /* save is split into to slots (sectors 0-13 are the main sectors, 14-27 are the backup save file) */

    private static final int FOOTER_ID_OFFSET = 0xFF4; /* footer id starts on byte 4084 of every sector */
    private static final int FOOTER_CHECKSUM_OFFSET = 0xFF6; /* checksum starts at byte 4086 */
    private static final int FOOTER_SIGNATURE_OFFSET = 0xFF8; /* signature (the value below) starts at byte 4088 */
    private static final int FOOTER_COUNTER_OFFSET = 0xFFC; /* this offset is for counting which is the active save slot */
    private static final long SECTOR_SIGNATURE = 0x08012025L; /* signature is value that must exist in each footer of each section to declare the save is valid */

    /* Each logical sector is checksummed only over the amount of structure data actually stored in it.
    The layout follows sSaveSlotLayout in the decompiled game:
      0       = SaveBlock2
      1..4    = SaveBlock1
      5..13   = PokemonStorage
    Most chunks are 0xF80 bytes; the final chunk of each structure can be smaller. */
    private static final int[] LOGICAL_SECTOR_CHECKSUM_SIZES = {
            0xF24, // 0: SaveBlock2
            0xF80, // 1: SaveBlock1 chunk 0
            0xF80, // 2: SaveBlock1 chunk 1
            0xF80, // 3: SaveBlock1 chunk 2
            0xEE8, // 4: SaveBlock1 chunk 3
            0xF80, // 5: PokemonStorage chunk 0
            0xF80, // 6
            0xF80, // 7
            0xF80, // 8
            0xF80, // 9
            0xF80, // 10
            0xF80, // 11
            0xF80, // 12
            0x7D0  // 13: PokemonStorage final chunk
    };

    /* our sector target and checksum size (where wondercard data is stored on the save) */
    private static final int TARGET_LOGICAL_SECTOR_ID = 4;
    private static final int TARGET_SECTOR_CHECKSUM_SIZE = 0xEE8;

    // Destination inside logical sector 4.
    private static final int WC3_CARD_DESTINATION = 0x460;
    private static final int RAM_SCRIPT_DESTINATION = 0x79C;

    /* must set where the questionnaire words data starts and ends to prevent it from being overwritten */
    private static final int QUESTIONNAIRE_START = 0x178;
    private static final int QUESTIONNAIRE_END = 0x180;

    private final byte[] data;

    private FireRedLeafGreenSave(byte[] data) {
        this.data = data;
    }

    /* loads the save file (if valid size) in memory (byte array) */
    public static FireRedLeafGreenSave load(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        if (data.length != SAVE_SIZE) {
            throw new IllegalArgumentException(
                    "Unsupported save size: " + data.length +
                    " bytes; expected " + SAVE_SIZE + " (128 KiB)"
            );
        }
        return new FireRedLeafGreenSave(data);
    }

    /* this method find which save slot is the active one */
    private SlotInfo findActiveSlot() {
        SlotInfo best = null;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            SlotInfo current = inspectSlot(slot);
            if (!current.valid()) {
                continue;
            }
            if (best == null || Integer.compareUnsigned(current.counter(), best.counter()) > 0) {
                best = current;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException(
                    "No valid FireRed/LeafGreen save slot was found. "
                    + "Both slots failed signature/ID/counter/checksum validation."
            );
        }
        return best;
    }

    /* method to check wether a save slot is valid or not (corrupted) */
    private SlotInfo inspectSlot(int slot) {
        Integer expectedCounter = null;
        boolean[] ids = new boolean[SECTORS_PER_SLOT];

        for (int i = 0; i < SECTORS_PER_SLOT; i++) {
            int physicalSector = slot * SECTORS_PER_SLOT + i;
            int offset = physicalSector * SECTOR_SIZE;
            int id = Binary.u16(data, offset + FOOTER_ID_OFFSET);
            long signature = Binary.u32(data, offset + FOOTER_SIGNATURE_OFFSET);
            int counter = (int) Binary.u32(data, offset + FOOTER_COUNTER_OFFSET);

            if (signature != SECTOR_SIGNATURE || id < 0 || id >= SECTORS_PER_SLOT || ids[id]) {
                return new SlotInfo(slot, 0, false, "invalid signature or logical sector IDs");
            }
            ids[id] = true;

            if (expectedCounter == null) {
                expectedCounter = counter;
            } else if (expectedCounter != counter) {
                return new SlotInfo(slot, 0, false, "sector counters do not match");
            }

            int storedChecksum = Binary.u16(data, offset + FOOTER_CHECKSUM_OFFSET);
            int calculatedChecksum = SectorChecksum.calculate(
                    data,
                    offset,
                    LOGICAL_SECTOR_CHECKSUM_SIZES[id]
            );

            if (storedChecksum != calculatedChecksum) {
                return new SlotInfo(
                        slot,
                        0,
                        false,
                        String.format(
                                "logical sector %d checksum mismatch (stored 0x%04X, calculated 0x%04X)",
                                id,
                                storedChecksum,
                                calculatedChecksum
                        )
                );
            }
        }

        return new SlotInfo(
                slot,
                expectedCounter == null ? 0 : expectedCounter,
                true,
                "ok"
        );
    }

    /* method to inject the wonder card on the save file */
    public InjectionResult inject(Wc3File wc3) {
        SlotInfo activeSlot = findActiveSlot(); /* first find active slot */
        int physicalSector = findPhysicalSector(activeSlot.slotIndex(), TARGET_LOGICAL_SECTOR_ID); /* run through all slot sectors to find the physical slot 4 (wc sector) */
        int sectorOffset = physicalSector * SECTOR_SIZE; /* stores the wc sector position in an attribute */

        byte[] wc = wc3.data();
        WonderCard card = wc3.wonderCard(); /* gets the WonderCard structure stored inside the wc3 file */

        /* first copy the wonder card data to the save file, we preserve the bytes where the save file questionary words are stored, wc data would overwrite if we didn't preserve
        because of our code nature, the decompiled game versions have functions to write wc without overwriting the questionnaire. */
        for (int i = 0; i < Wc3File.CARD_BLOCK_SIZE; i++) {
            if (i >= QUESTIONNAIRE_START && i < QUESTIONNAIRE_END) {
                continue;
            }
            data[sectorOffset + WC3_CARD_DESTINATION + i] = wc[i];
        }

        /* this piece of code copies METADATA_ICON_SPECIES from the wc to the user save, as the decompiled game does */
        Binary.putU16(
            data,
            sectorOffset
                + WC3_CARD_DESTINATION
                + Wc3File.CARD_METADATA_ICON_SPECIES_OFFSET,
            card.iconSpecies()
        );

        /* copy the wc script to the actual savedata wc script area (the actual code for delivering the gift) */
        int scriptLength = Wc3File.FILE_SIZE - Wc3File.RAM_SCRIPT_OFFSET;
        System.arraycopy(
                wc,
                Wc3File.RAM_SCRIPT_OFFSET,
                data,
                sectorOffset + RAM_SCRIPT_DESTINATION,
                scriptLength
        );

        /* calculate the checksum after editing the file (SectorChecksum method) and turn into u16 */
        int checksum = SectorChecksum.calculate(
                data,
                sectorOffset,
                TARGET_SECTOR_CHECKSUM_SIZE
        );
        Binary.putU16(data, sectorOffset + FOOTER_CHECKSUM_OFFSET, checksum);

        /* after that we create a simple object to store all the save data changes and return it */
        return new InjectionResult(
                activeSlot.slotIndex(),
                activeSlot.counter(),
                physicalSector,
                checksum,
                card.flagId()
        );
    }

    /* extracts the currently active Wonder Card + RamScript from the save and reconstructs a standard 0x58C-byte WC3 file.
    Unlike injection, extraction copies the questionnaire bytes exactly as they currently exist in the save. */
    public ExtractionResult extractWc3() {
        SlotInfo activeSlot = findActiveSlot();
        int physicalSector = findPhysicalSector(activeSlot.slotIndex(), TARGET_LOGICAL_SECTOR_ID);
        int sectorOffset = physicalSector * SECTOR_SIZE;

        byte[] wc = new byte[Wc3File.FILE_SIZE];

        System.arraycopy(
                data,
                sectorOffset + WC3_CARD_DESTINATION,
                wc,
                0,
                Wc3File.CARD_BLOCK_SIZE
        );

        System.arraycopy(
                data,
                sectorOffset + RAM_SCRIPT_DESTINATION,
                wc,
                Wc3File.RAM_SCRIPT_OFFSET,
                Wc3File.RAM_SCRIPT_SIZE
        );

        Wc3File wc3 = Wc3File.fromBytes(wc);

        /* a normal extraction should return a usable WC3, not a blind dump from an empty/corrupted save region.
        if forensic/raw extraction is ever needed, it can be added later as a separate explicit command. */
        return new ExtractionResult(
                activeSlot.slotIndex(),
                activeSlot.counter(),
                physicalSector,
                wc3
        );
    }

    /* returns a summary of both slots without mutating the save. */
    public SaveInspection inspect() {
        SlotInfo slot1 = inspectSlot(0);
        SlotInfo slot2 = inspectSlot(1);

        SlotInfo active = null;
        if (slot1.valid()) {
            active = slot1;
        }
        if (slot2.valid() && (active == null
                || Integer.compareUnsigned(slot2.counter(), active.counter()) > 0)) {
            active = slot2;
        }

        return new SaveInspection(slot1, slot2, active);
    }

    /* auxiliar function to write directly to the .sav file */
    public void write(Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(output, data);
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(data, data.length);
    }

    private int findPhysicalSector(int slot, int logicalId) {
        for (int i = 0; i < SECTORS_PER_SLOT; i++) {
            int physicalSector = slot * SECTORS_PER_SLOT + i;
            int offset = physicalSector * SECTOR_SIZE;
            if (Binary.u16(data, offset + FOOTER_ID_OFFSET) == logicalId) {
                return physicalSector;
            }
        }
        throw new IllegalArgumentException("Logical sector " + logicalId + " was not found");
    }

    public record SlotInfo(int slotIndex, int counter, boolean valid, String status) {}

    public record SaveInspection(
            SlotInfo slot1,
            SlotInfo slot2,
            SlotInfo activeSlot
    ) {}

    public record InjectionResult(
            int slotIndex,
            int saveCounter,
            int physicalSector,
            int sectorChecksum,
            int wonderCardFlagId
    ) {}

    public record ExtractionResult(
            int slotIndex,
            int saveCounter,
            int physicalSector,
            Wc3File wc3
    ) {}
}
