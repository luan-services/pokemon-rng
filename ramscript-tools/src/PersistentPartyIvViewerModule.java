/* Persistent-native form of the Party IV Viewer helper.

   The original PartyIvViewerPreset remains the simple standalone path. This
   class only exposes the already validated 296-byte Thumb helper as a module
   payload that can live in the SB2 persistent native catalog.
*/
final class PersistentPartyIvViewerModule {
    static final int MODULE_ID = 0x20;

    private PersistentPartyIvViewerModule() {}

    static byte[] payload(RomProfile rom) {
        // The generated code does not embed its own staging address; it only
        // embeds stock ROM/RAM globals and writes the dynamic message to
        // gStringVar4 + 0x280. Use a harmless synthetic address here only to
        // satisfy the legacy builder's overlap validation.
        return PartyMonDataNativeHelper.buildAt(rom, 0x02000000L).codeCopy();
    }
}
