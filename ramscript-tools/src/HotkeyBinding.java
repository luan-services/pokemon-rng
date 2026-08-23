record HotkeyBinding(String presetId, Hotkey hotkey) {
    HotkeyBinding {
        if (presetId == null || presetId.isBlank()) {
            throw new IllegalArgumentException("preset id must not be blank");
        }
        if (hotkey == null) throw new IllegalArgumentException("hotkey must not be null");
    }
}
