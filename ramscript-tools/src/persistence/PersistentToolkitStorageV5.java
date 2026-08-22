/* Build 9: cross-area persistent module catalog.

   The catalog itself lives at the beginning of the validated SaveBlock2
   toolkit region. Individual module entries may point at either validated
   SaveBlock area. This is intentionally not a replacement for simple
   RamScript presets; it is the persistent-module path only.
*/
final class PersistentToolkitStorageV5 {
    static final long MAGIC = 0x54504843L; // CHPT
    static final int VERSION = 5;
    static final int HEADER_SIZE = 0x10;
    static final int ENTRY_SIZE = 0x10;
    static final int ENTRY_1 = 0x10;
    static final int ENTRY_2 = 0x20;

    static final int SID_SB1_OFFSET = 0x20;
    static final int PROOF_SB2_OFFSET = 0x80;

    private PersistentToolkitStorageV5() {}

    static PersistentModule sidModule(RomProfile rom) {
        byte[] payload = PersistentShowSecretIdModule.payload(rom).bytes();
        return new PersistentModule(PersistentShowSecretIdModule.MODULE_ID,
                PersistentModule.KIND_THUMB, PayloadStorageArea.SAVE_BLOCK1, payload);
    }

    static PersistentModule proofModule(RomProfile rom) {
        return new PersistentModule(PersistentDispatcherProofModule.MODULE_ID,
                PersistentModule.KIND_THUMB, PayloadStorageArea.SAVE_BLOCK2,
                PersistentDispatcherProofModule.build(rom));
    }

    /* Image written at SaveBlock2 + 0xB20. Module 1's bytes are deliberately
       absent because they physically live in SaveBlock1. */
    static byte[] buildCatalogImage(RomProfile rom) {
        PersistentModule sid = sidModule(rom);
        PersistentModule proof = proofModule(rom);
        byte[] proofBytes = proof.payload();
        byte[] image = new byte[PROOF_SB2_OFFSET + proofBytes.length];

        PersistentToolkitStorageV2.putU32(image, 0, MAGIC);
        image[4] = VERSION;
        image[5] = 2;
        PersistentToolkitStorageV2.putU16(image, 6, HEADER_SIZE);
        PersistentToolkitStorageV2.putU16(image, 8, image.length);

        writeEntry(image, ENTRY_1, sid, SID_SB1_OFFSET);
        writeEntry(image, ENTRY_2, proof, PROOF_SB2_OFFSET);
        System.arraycopy(proofBytes, 0, image, PROOF_SB2_OFFSET, proofBytes.length);
        return image;
    }

    static byte[] buildSaveBlock1Payload(RomProfile rom) {
        return sidModule(rom).payload();
    }

    private static void writeEntry(byte[] image, int offset, PersistentModule module, int payloadOffset) {
        PersistentToolkitStorageV2.putU16(image, offset, module.id());
        image[offset + 2] = (byte)module.kind();
        image[offset + 3] = (byte)switch (module.area()) {
            case SAVE_BLOCK1 -> 1;
            case SAVE_BLOCK2 -> 2;
            default -> throw new IllegalArgumentException("persistent entry cannot use " + module.area());
        };
        byte[] payload = module.payload();
        PersistentToolkitStorageV2.putU16(image, offset + 4, payloadOffset);
        PersistentToolkitStorageV2.putU16(image, offset + 6, payload.length);
        PersistentToolkitStorageV2.putU16(image, offset + 8, PersistentToolkitStorageV2.checksum16(payload));
        PersistentToolkitStorageV2.putU16(image, offset + 0x0A, 1); // enabled
    }
}
