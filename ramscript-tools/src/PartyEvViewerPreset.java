final class PartyEvViewerPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private PartyEvViewerPreset() {}
    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }
    static TriggerBuildResult build(RomProfile rom) {
        return build(rom, new Hotkey(HotkeyButton.R, HotkeyButton.UP));
    }
    static TriggerBuildResult build(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(EventTrigger.HOTKEY_RUNTIME, rom, buildPayload(rom), hotkey);
    }
    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0xC0L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = PartyEvNativeHelper.buildAt(rom, helperAddress);
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(builder, VIRTUAL_BASE, helper, copierAddress, "party_ev", NativeHelperInstaller.Mode.AUTO);
        builder.lockAll();
        plan.installAndCall(builder);
        builder.message(PartyEvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end();
        return builder.buildScript();
    }
    static int payloadSize(RomProfile rom) { return buildPayload(rom).length; }
}
