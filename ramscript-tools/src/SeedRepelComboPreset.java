/** @deprecated Legacy validated one-WC Seed+Repel build using MultiHotkeyRuntimeV1. */
@Deprecated
final class SeedRepelComboPreset {
    static final Hotkey DEFAULT_SEED_HOTKEY = Hotkey.DEFAULT;
    static final Hotkey DEFAULT_REPEL_HOTKEY = new Hotkey(HotkeyButton.R, HotkeyButton.B);

    private SeedRepelComboPreset() {}

    static TriggerBuildResult build(RomProfile rom, int seed) {
        return build(rom, seed, DEFAULT_SEED_HOTKEY, DEFAULT_REPEL_HOTKEY);
    }

    static TriggerBuildResult build(RomProfile rom, int seed, Hotkey seedHotkey, Hotkey repelHotkey) {
        byte[] seedPayload = SeedModifierPreset.buildPayload(rom, seed);
        byte[] repelPayload = RepelHotkeyPreset.buildPayload();
        return MultiHotkeyRuntimeV1.compose(
                rom,
                new HotkeyPayload(seedHotkey, seedPayload),
                new HotkeyPayload(repelHotkey, repelPayload)
        );
    }
}
