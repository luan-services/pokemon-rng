final class LeadIvViewerPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private LeadIvViewerPreset() {}
    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }
    static TriggerBuildResult build(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(EventTrigger.HOTKEY_RUNTIME, rom, buildPayload(rom), hotkey);
    }
    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = LeadIvNativeHelper.buildAt(rom, helperAddress);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress, "lead_iv", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        plan.installAndCall(b);
        b.message(LeadIvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end();
        return b.buildScript();
    }
    static int payloadSize(RomProfile rom) { return buildPayload(rom).length; }
}
