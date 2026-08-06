
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/* this the program main class, it stores constants relateds to the save file structure. it also include methods for loading / injection
on the save file. */

public final class FireRedLeafGreenSave {
    private static final int SAVE_SIZE = 0x20000; /* full save file size, expected to be exactly 131072 bytes (128kb) */
    private static final int SECTOR_SIZE = 0x1000; /* save file is splitted into sectors, each having 4096 bytes */
    private static final int SECTOR_DATA_SIZE = 0xF80; /* sector mains data section has 3968 bytes, after that comes the section footer (which display important shared data about save file) */
    private static final int SECTORS_PER_SLOT = 14; /* sector count per save slot */
    private static final int SLOT_COUNT = 2; /* save is split into to slots (sectors 0-13 are the main sectors, 14-27 are the backup save file) */

    private static final int FOOTER_ID_OFFSET = 0xFF4; /* footer id starts on byte 4084 of every sector */
    private static final int FOOTER_CHECKSUM_OFFSET = 0xFF6; /* checksum starts at byte 4086 */
    private static final int FOOTER_SIGNATURE_OFFSET = 0xFF8; /* signature (the value below) starts at byte 4088 */
    private static final int FOOTER_COUNTER_OFFSET = 0xFFC; /* this offset is for counting which is the active save slot */
    private static final long SECTOR_SIGNATURE = 0x08012025L; /* signature is value that must exist in each footer of each section to declare the save is valid */

    /* our sector target and checksum size (where wondercard data is stored on the save) */
    private static final int TARGET_LOGICAL_SECTOR_ID = 4;
    private static final int TARGET_SECTOR_CHECKSUM_SIZE = 0xEE8;

    // Destination inside logical sector 4.
    private static final int WC3_CARD_DESTINATION = 0x460;
    private static final int RAM_SCRIPT_DESTINATION = 0x79C;

    /* must set where the questionnaire words data starts and ends to prevent it from being overwritten */
    private static final int QUESTIONNAIRE_START = 0x178;
    private static final int QUESTIONNAIRE_END = 0x180;

    private static final int METADATA_ICON_SPECIES = 0x15A; /* necessary because pkhex clear this data after injection (it is an unused feature from the game) */

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
            throw new IllegalArgumentException("No valid FireRed/LeafGreen save slot was found");
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
                return new SlotInfo(slot, 0, false);
            }
            ids[id] = true;

            if (expectedCounter == null) {
                expectedCounter = counter;
            } else if (expectedCounter != counter) {
                return new SlotInfo(slot, 0, false);
            }
        }

        return new SlotInfo(slot, expectedCounter == null ? 0 : expectedCounter, true);
    }

    /* method to inject the wonder card on the save file */
    public InjectionResult inject(Wc3File wc3) {
        SlotInfo activeSlot = findActiveSlot(); /* first find active slot */
        int physicalSector = findPhysicalSector(activeSlot.slotIndex(), TARGET_LOGICAL_SECTOR_ID); /* run through all slot sectors to find the physical slot 4 (wc sector) */
        int sectorOffset = physicalSector * SECTOR_SIZE; /* stores the wc sector position in an attribute */

        byte[] wc = wc3.data();

        /* first copy the wonder card data to the save file, we preserve the bytes where the save file questionary words are stored, wc data would overwrite if we didn't preserve 
        because of our code nature, the decompiled game versions have functions to write wc without overwriting the questionnaire. */
        for (int i = 0; i < Wc3File.CARD_BLOCK_SIZE; i++) {
            if (i >= QUESTIONNAIRE_START && i < QUESTIONNAIRE_END) {
                continue;
            }
            data[sectorOffset + WC3_CARD_DESTINATION + i] = wc[i];
        }

        /* not sure why but pkhex sets METADATA_ICONS_SPECIES to 0, the decompiled game always copy the value from the 
        oldest wonder card */
        data[sectorOffset + WC3_CARD_DESTINATION + METADATA_ICON_SPECIES] = 0;
        data[sectorOffset + WC3_CARD_DESTINATION + METADATA_ICON_SPECIES + 1] = 0;

        /* this piece of code copies METADATA_ICON_SPECIES from the wc to the user save, as the decompiled game does 
        int iconSpecies = wc3.iconSpecies();

        Binary.putU16(
            data,
            sectorOffset
                + WC3_CARD_DESTINATION
                + METADATA_ICON_SPECIES,
            iconSpecies
        ); */

        /* copy the wc script to the actual savedata wc script area (the actual code for delivering the gift) */
        int scriptLength = Wc3File.FILE_SIZE - Wc3File.RAM_SCRIPT_SOURCE_OFFSET;
        System.arraycopy(
                wc,
                Wc3File.RAM_SCRIPT_SOURCE_OFFSET,
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
                wc3.flagId()
        );
    }

    /* auxiliar function to write directly to the .sav file */
    public void write(Path output) throws IOException {
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

    private record SlotInfo(int slotIndex, int counter, boolean valid) {}

    public record InjectionResult(
            int slotIndex,
            int saveCounter,
            int physicalSector,
            int sectorChecksum,
            int wonderCardFlagId
    ) {}
}
