/* Small shared-service bridge for the persistent Run + Bike Anywhere toggle. */
final class PersistentRunBikeAnywhereBridge {
    private PersistentRunBikeAnywhereBridge() {}

    static byte[] build(RomProfile rom, long sharedServiceVirtualTarget) {
        return PersistentNativeCallBridge.buildViaSharedStagingService(
                rom,
                PersistentRunBikeAnywhereModule.MODULE_ID,
                sharedServiceVirtualTarget,
                rom.stringVar4 + 0x140L,
                b -> {},
                b -> b.playSe(RunBikeAnywhereHotkeyRuntimeV1.SE_TOGGLE_CLICK).waitSe().releaseAll().end(),
                b -> b.releaseAll().end()
        ).fieldScript();
    }

    static int fieldScriptSize(RomProfile rom) {
        return build(rom, HotkeyRuntimeV1.VIRTUAL_BASE + 0x100).length;
    }
}
