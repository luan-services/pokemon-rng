final class PersistentLeadEvViewerModule {
    static final int MODULE_ID = 0x24;
    private PersistentLeadEvViewerModule() {}
    static byte[] payload(RomProfile rom) { return LeadEvNativeHelper.buildAt(rom, 0x02000000L).codeCopy(); }
}
