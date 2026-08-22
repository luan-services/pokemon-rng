/* Build 10: first catalog made entirely from real preset modules.

   Module 0x10: Show Secret ID native module in SaveBlock1.
   Module 0x11: Seed Modifier native core in SaveBlock2.

   The cross-area choice is explicit in this validation build. Build 9 already
   proved that the dispatcher can honor the location field; this build proves
   that the second area can contain a real preset rather than a probe.
*/
final class PersistentToolkitStorageV6 {
    static final long MAGIC = 0x54504843L; // CHPT
    static final int VERSION = 6;
    static final int HEADER_SIZE = 0x10;
    static final int ENTRY_SIZE = 0x10;
    static final int ENTRY_1 = 0x10;
    static final int ENTRY_2 = 0x20;

    static final int SID_SB1_OFFSET = 0x20;
    static final int SEED_SB2_OFFSET = 0x80;

    private PersistentToolkitStorageV6() {}

    static PersistentModule sidModule(RomProfile rom) {
        return new PersistentModule(PersistentShowSecretIdModule.MODULE_ID,
                PersistentModule.KIND_THUMB, PayloadStorageArea.SAVE_BLOCK1,
                PersistentShowSecretIdModule.payload(rom).bytes());
    }

    static PersistentModule seedModule(RomProfile rom, int desiredSeed) {
        return new PersistentModule(PersistentSeedModifierModule.MODULE_ID,
                PersistentModule.KIND_THUMB, PayloadStorageArea.SAVE_BLOCK2,
                PersistentSeedModifierModule.payload(rom, desiredSeed).bytes());
    }

    static byte[] buildCatalogImage(RomProfile rom, int desiredSeed) {
        PersistentModule sid = sidModule(rom);
        PersistentModule seed = seedModule(rom, desiredSeed);
        byte[] seedBytes = seed.payload();
        byte[] image = new byte[SEED_SB2_OFFSET + seedBytes.length];

        PersistentToolkitStorageV2.putU32(image, 0, MAGIC);
        image[4] = VERSION;
        image[5] = 2;
        PersistentToolkitStorageV2.putU16(image, 6, HEADER_SIZE);
        PersistentToolkitStorageV2.putU16(image, 8, image.length);

        writeEntry(image, ENTRY_1, sid, SID_SB1_OFFSET);
        writeEntry(image, ENTRY_2, seed, SEED_SB2_OFFSET);
        System.arraycopy(seedBytes, 0, image, SEED_SB2_OFFSET, seedBytes.length);
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
        PersistentToolkitStorageV2.putU16(image, offset + 0x0A, 1);
    }
}
