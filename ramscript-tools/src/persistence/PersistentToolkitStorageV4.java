/* Build 8: first real preset module stored in PersistentToolkitStorage.

   V4 is deliberately tiny: one catalog entry and one real THUMB module. Its
   purpose is to validate the dual-path preset architecture before generalizing
   the catalog allocator/dispatcher further.
*/
final class PersistentToolkitStorageV4 {
    static final long MAGIC = 0x54504843L; // CHPT
    static final int VERSION = 4;
    static final int HEADER_SIZE = 0x10;
    static final int ENTRY_OFFSET = 0x10;
    static final int ENTRY_SIZE = 0x10;
    static final int PAYLOAD_OFFSET = 0x40;

    private PersistentToolkitStorageV4() {}

    static PersistentModule showSecretIdModule(RomProfile rom) {
        PresetPayload payload = PersistentShowSecretIdModule.payload(rom);
        PlacedPayload placed = PresetDeploymentPlanner.place(payload, PresetDeploymentMode.PERSISTENT_MODULE, 0);
        // Build 8 keeps the first real module in SaveBlock2 because the already
        // validated V2/V3 launcher executes from that area. Cross-area dispatch
        // is the next generalization; the planner abstraction is kept separate.
        if (placed.area() != PayloadStorageArea.SAVE_BLOCK1) {
            throw new IllegalStateException("unexpected planner result for 32-byte SID payload");
        }
        return new PersistentModule(PersistentShowSecretIdModule.MODULE_ID, PersistentModule.KIND_THUMB,
                PayloadStorageArea.SAVE_BLOCK2, payload.bytes());
    }

    static byte[] buildImage(RomProfile rom) {
        PersistentModule module = showSecretIdModule(rom);
        byte[] payload = module.payload();
        byte[] image = new byte[PAYLOAD_OFFSET + payload.length];
        PersistentToolkitStorageV2.putU32(image, 0, MAGIC);
        image[4] = VERSION;
        image[5] = 1; // moduleCount
        PersistentToolkitStorageV2.putU16(image, 6, HEADER_SIZE);
        PersistentToolkitStorageV2.putU16(image, 8, image.length);

        int e = ENTRY_OFFSET;
        PersistentToolkitStorageV2.putU16(image, e, module.id());
        image[e + 2] = (byte)module.kind();
        image[e + 3] = 2; // SaveBlock2 physical location
        PersistentToolkitStorageV2.putU16(image, e + 4, PAYLOAD_OFFSET);
        PersistentToolkitStorageV2.putU16(image, e + 6, payload.length);
        PersistentToolkitStorageV2.putU16(image, e + 8, PersistentToolkitStorageV2.checksum16(payload));
        PersistentToolkitStorageV2.putU16(image, e + 0x0A, 1); // enabled
        System.arraycopy(payload, 0, image, PAYLOAD_OFFSET, payload.length);
        return image;
    }
}
