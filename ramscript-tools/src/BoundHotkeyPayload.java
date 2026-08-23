record BoundHotkeyPayload(HotkeyBinding binding, byte[] entryPayload) {
    BoundHotkeyPayload {
        if (binding == null) throw new IllegalArgumentException("binding must not be null");
        if (entryPayload == null || entryPayload.length == 0) {
            throw new IllegalArgumentException("entry payload must not be empty");
        }
        entryPayload = entryPayload.clone();
    }

    @Override
    public byte[] entryPayload() {
        return entryPayload.clone();
    }
}
