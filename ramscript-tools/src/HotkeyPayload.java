record HotkeyPayload(Hotkey hotkey, byte[] payload) {
    HotkeyPayload {
        if (hotkey == null) throw new IllegalArgumentException("hotkey must not be null");
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("payload must not be empty");
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
