record PersistentNativeModuleSpec(int id, byte[] payload) {
    PersistentNativeModuleSpec {
        if (id < 0 || id > 0xFFFF) throw new IllegalArgumentException("module id must be u16");
        if (payload == null || payload.length == 0 || payload.length > 0xFFFF) {
            throw new IllegalArgumentException("native payload size must be 1..65535");
        }
        payload = payload.clone();
    }
    @Override public byte[] payload() { return payload.clone(); }
}
