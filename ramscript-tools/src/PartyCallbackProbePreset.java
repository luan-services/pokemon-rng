/*
   Build 45a diagnostic harness.

   Flow:
     Deliveryman -> install marker-only IWRAM callback -> open stock single-mon
     Party menu -> ReturnToField -> callback writes VAR_0x8005 = 0x45A1.

   The original RamScript remains stopped/stale intentionally. A black
   overworld after return is acceptable for this probe; a hardware reset is not.
*/
final class PartyCallbackProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_PROBE = 0x8005;
    private static final int PROBE_BEFORE = 0x4501;

    private PartyCallbackProbePreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45a callback probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        byte[] callback = PartyCallbackProbeRuntime.callback(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress()
         .vGoto("installer")
         .raw(new byte[] { 0x45, (byte)0xA1 });

        b.label("installer")
         .setVar(VAR_PROBE, PROBE_BEFORE)
         .writeBytes(PartyCallbackProbeRuntime.CALLBACK, callback)
         .writeBytes(launcherAddress, launcher)
         .fadeScreen(1)
         .callNative(launcherAddress | 1L)
         .waitState()
         .end();

        return b.buildScript();
    }
}
