import java.util.ArrayList;
import java.util.List;

/*
   Production-oriented catalog for persistent THUMB modules stored in SB2.
   The binary format deliberately remains compatible with the Build-10 V6
   dispatcher that was validated in-game. SB1 is not used as module storage.
*/
final class PersistentNativeModuleCatalog {
    static final long MAGIC = PersistentNativeCatalogFormat.MAGIC;
    static final int VERSION = PersistentNativeCatalogFormat.VERSION;
    static final int HEADER_SIZE = PersistentNativeCatalogFormat.HEADER_SIZE;
    static final int ENTRY_SIZE = PersistentNativeCatalogFormat.ENTRY_SIZE;

    private PersistentNativeModuleCatalog() {}

    record Placement(int moduleId, int payloadOffset, int payloadSize, int checksum16) {}
    record Image(byte[] bytes, List<Placement> placements) {
        Image { bytes = bytes.clone(); placements = List.copyOf(placements); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }

    static Image build(int catalogBaseOffset, List<PersistentNativeModuleSpec> modules) {
        if (modules == null || modules.isEmpty()) throw new IllegalArgumentException("catalog needs modules");
        if (modules.size() > 0xFF) throw new IllegalArgumentException("catalog module count must fit u8");

        int tableEnd = HEADER_SIZE + ENTRY_SIZE * modules.size();
        int cursor = align4(tableEnd);
        List<Placement> placements = new ArrayList<>();
        java.util.HashSet<Integer> ids = new java.util.HashSet<>();

        for (PersistentNativeModuleSpec module : modules) {
            if (!ids.add(module.id())) throw new IllegalArgumentException("duplicate native module id: " + module.id());
            byte[] payload = module.payload();
            placements.add(new Placement(module.id(), cursor, payload.length,
                    PersistentNativeCatalogFormat.checksum16(payload)));
            cursor = align4(cursor + payload.length);
        }

        byte[] image = new byte[cursor];
        PersistentNativeCatalogFormat.putU32(image, 0, MAGIC);
        image[4] = (byte) VERSION;
        image[5] = (byte) modules.size();
        PersistentNativeCatalogFormat.putU16(image, 6, HEADER_SIZE);
        PersistentNativeCatalogFormat.putU16(image, 8, image.length);

        for (int i = 0; i < modules.size(); i++) {
            PersistentNativeModuleSpec module = modules.get(i);
            Placement placement = placements.get(i);
            int entry = HEADER_SIZE + i * ENTRY_SIZE;
            PersistentNativeCatalogFormat.putU16(image, entry, module.id());
            image[entry + 2] = (byte) PersistentNativeCatalogFormat.KIND_THUMB;
            image[entry + 3] = 2; // SaveBlock2 only in the production catalog.
            PersistentNativeCatalogFormat.putU16(image, entry + 4, placement.payloadOffset());
            PersistentNativeCatalogFormat.putU16(image, entry + 6, placement.payloadSize());
            PersistentNativeCatalogFormat.putU16(image, entry + 8, placement.checksum16());
            PersistentNativeCatalogFormat.putU16(image, entry + 0x0A, 1);
            byte[] payload = module.payload();
            System.arraycopy(payload, 0, image, placement.payloadOffset(), payload.length);
        }

        int sb2Start = PayloadStorageArea.SAVE_BLOCK2.offset();
        int sb2End = sb2Start + PayloadStorageArea.SAVE_BLOCK2.capacity();
        if (catalogBaseOffset < sb2Start || catalogBaseOffset + image.length > sb2End) {
            throw new IllegalArgumentException("native catalog does not fit SB2 persistent region");
        }
        return new Image(image, placements);
    }

    private static int align4(int n) { return (n + 3) & ~3; }
}
