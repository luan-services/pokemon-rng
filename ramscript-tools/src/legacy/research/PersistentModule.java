record PersistentModule(int id, int kind, PayloadStorageArea area, byte[] payload) {
    static final int KIND_THUMB = 1;

    PersistentModule {
        if (id <= 0 || id > 0xFF) throw new IllegalArgumentException("module id must be 1..255");
        if (kind != KIND_THUMB) throw new IllegalArgumentException("unsupported module kind: " + kind);
        if (area != PayloadStorageArea.SAVE_BLOCK1 && area != PayloadStorageArea.SAVE_BLOCK2) {
            throw new IllegalArgumentException("persistent modules must live in a SaveBlock area");
        }
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("payload must not be empty");
        payload = payload.clone();
    }

    @Override public byte[] payload() { return payload.clone(); }
}
