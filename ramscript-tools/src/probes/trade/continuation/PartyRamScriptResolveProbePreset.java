/* Build 45b: post-Party callback calls only GetSavedRamScriptIfValid(). */
final class PartyRamScriptResolveProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_PROBE = 0x8005;
    private static final int PROBE_BEFORE = 0x4501;

    private PartyRamScriptResolveProbePreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45b resolve probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        byte[] callback = PartyRamScriptResolveProbeRuntime.callback(rom);
        byte[] callbackLiterals = PartyRamScriptResolveProbeRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress()
         .vGoto("installer")
         .raw(new byte[] { 0x45, (byte)0xB0 });

        b.label("installer")
         .setVar(VAR_PROBE, PROBE_BEFORE)
         .writeBytes(PartyRamScriptResolveProbeRuntime.CALLBACK, callback)
         .writeBytes(PartyRamScriptResolveProbeRuntime.LITERAL_GET_RAM_SCRIPT, callbackLiterals)
         .writeBytes(launcherAddress, launcher)
         .fadeScreen(1)
         .callNative(launcherAddress | 1L)
         .waitState()
         .end();

        return b.buildScript();
    }
}
