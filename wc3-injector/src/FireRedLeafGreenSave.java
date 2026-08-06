
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/* this the program main class, it stores constants relateds to the save file structure. it also include methods for loading / injection
on the save file. */

public final class FireRedLeafGreenSave {
    private static final int SAVE_SIZE = 0x20000;
    private static final int SECTOR_SIZE = 0x1000;
    private static final int SECTOR_DATA_SIZE = 0xF80;
    private static final int SECTORS_PER_SLOT = 14;
    private static final int SLOT_COUNT = 2;

    private static final int FOOTER_ID_OFFSET = 0xFF4;
    private static final int FOOTER_CHECKSUM_OFFSET = 0xFF6;
    private static final int FOOTER_SIGNATURE_OFFSET = 0xFF8;
    private static final int FOOTER_COUNTER_OFFSET = 0xFFC;
    private static final long SECTOR_SIGNATURE = 0x08012025L;

    // SaveBlock1 logical sector 4 is chunk 3 of SaveBlock1.
    private static final int TARGET_LOGICAL_SECTOR_ID = 4;
    private static final int TARGET_SECTOR_CHECKSUM_SIZE = 0xEE8;

    // Destination inside logical sector 4.
    private static final int WC3_CARD_DESTINATION = 0x460;
    private static final int RAM_SCRIPT_DESTINATION = 0x79C;

    // PKHeX preserves the save's four questionnaire words.
    private static final int QUESTIONNAIRE_START = 0x178;
    private static final int QUESTIONNAIRE_END = 0x180;

    // For this WC3 format, PKHeX clears WonderCardMetadata.iconSpecies.
    private static final int METADATA_ICON_SPECIES = 0x15A;

    private final byte[] data;

    private FireRedLeafGreenSave(byte[] data) {
        this.data = data;
    }

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

    public InjectionResult inject(Wc3File wc3) {
        SlotInfo activeSlot = findActiveSlot();
        int physicalSector = findPhysicalSector(activeSlot.slotIndex(), TARGET_LOGICAL_SECTOR_ID);
        int sectorOffset = physicalSector * SECTOR_SIZE;

        byte[] wc = wc3.data();

        // Copy the WC3 card/state block while preserving questionnaire words.
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

        // Copy the embedded event payload and RamScript area.
        int scriptLength = Wc3File.FILE_SIZE - Wc3File.RAM_SCRIPT_SOURCE_OFFSET;
        System.arraycopy(
                wc,
                Wc3File.RAM_SCRIPT_SOURCE_OFFSET,
                data,
                sectorOffset + RAM_SCRIPT_DESTINATION,
                scriptLength
        );

        int checksum = SectorChecksum.calculate(
                data,
                sectorOffset,
                TARGET_SECTOR_CHECKSUM_SIZE
        );
        Binary.putU16(data, sectorOffset + FOOTER_CHECKSUM_OFFSET, checksum);

        return new InjectionResult(
                activeSlot.slotIndex(),
                activeSlot.counter(),
                physicalSector,
                checksum,
                wc3.flagId()
        );
    }

    public void write(Path output) throws IOException {
        Files.write(output, data);
    }

    public byte[] copyBytes() {
        return Arrays.copyOf(data, data.length);
    }

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
