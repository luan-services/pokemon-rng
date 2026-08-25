final class PersistentLeadIvViewerModule {
    static final int MODULE_ID = 0x23;
    private PersistentLeadIvViewerModule() {}
    static byte[] payload(RomProfile rom) { return LeadIvNativeHelper.buildAt(rom, 0x02000000L).codeCopy(); }
}
