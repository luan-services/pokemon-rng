final class LeadEvViewerPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private LeadEvViewerPreset() {}
    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }
    static TriggerBuildResult build(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(EventTrigger.HOTKEY_RUNTIME, rom, buildPayload(rom), hotkey);
    }
    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0xC0L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = LeadEvNativeHelper.buildAt(rom, helperAddress);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress, "lead_ev", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        plan.installAndCall(b);
        b.message(LeadEvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end();
        return b.buildScript();
    }
    static int payloadSize(RomProfile rom) { return buildPayload(rom).length; }
}
