record PresetSelection(String presetId, PresetActivation activation) {
    PresetSelection {
        if (presetId == null || presetId.isBlank()) throw new IllegalArgumentException("preset id must not be blank");
        if (activation == null) throw new IllegalArgumentException("activation must not be null");
    }

    static PresetSelection deliveryman(String presetId) {
        return new PresetSelection(presetId, PresetActivation.DELIVERYMAN);
    }

    static PresetSelection hotkey(String presetId) {
        return new PresetSelection(presetId, PresetActivation.HOTKEY);
    }
}
