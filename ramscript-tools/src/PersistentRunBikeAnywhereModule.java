/* Persistent-native form of the Run + Bike Anywhere toggle helper.
   The resident post-map sidecar is still installed separately at 02022B08;
   only the hotkey-time toggle helper lives in the SB2 native catalog. */
final class PersistentRunBikeAnywhereModule {
    static final int MODULE_ID = 0x26;

    private PersistentRunBikeAnywhereModule() {}

    static byte[] payload(RomProfile rom) {
        return RunBikeAnywhereHotkeyRuntimeV1.toggleHelper(rom);
    }
}
