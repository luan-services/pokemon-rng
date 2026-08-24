/* One cell in the preset x usage-mode x ROM validation matrix. */
record PresetValidationEntry(
        PresetUsageMode usageMode,
        RomProfile rom,
        PresetValidationStatus status,
        String notes
) {
    PresetValidationEntry {
        if (usageMode == null || rom == null || status == null) {
            throw new IllegalArgumentException("validation entry fields must not be null");
        }
        notes = notes == null ? "" : notes;
    }
}
