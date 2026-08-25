final class PersistentPartyEvViewerModule {
    static final int MODULE_ID = 0x22;
    private PersistentPartyEvViewerModule() {}
    static byte[] payload(RomProfile rom) {
        return PartyEvNativeHelper.buildCompactAt(rom, 0x02000000L).codeCopy();
    }
}
