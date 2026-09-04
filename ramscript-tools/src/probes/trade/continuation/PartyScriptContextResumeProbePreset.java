/* Build 45c: isolate relocation-safe ScriptContext_SetupScript after Party return. */
final class PartyScriptContextResumeProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_PROBE = 0x8005;
    private static final int VAR_COPY = 0x8006;
    private static final int PROBE_BEFORE = 0x4501;
    private static final int PROBE_AFTER = 0x45C2;

    private PartyScriptContextResumeProbePreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45c resume probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        byte[] callback = PartyContinuationRuntime.callback(rom);
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress()
         .vGoto("installer")
         .raw(new byte[] { 0x45, (byte)0xC0 });

        if (b.position() != PartyContinuationRuntime.CONTINUATION_OFFSET)
            throw new IllegalStateException("Build 45c continuation must begin at RamScript+0x0C");

        // Minimal continuation. If this marker appears, SetupScript successfully
        // replaced the stale pre-relocation ScriptContext with the newly-resolved one.
        b.label("continuation")
         .setVar(VAR_PROBE, PROBE_AFTER)
         .copyVar(VAR_COPY, VAR_SELECTED)
         .fadeScreen(0)
         .releaseAll()
         .end();

        b.label("installer")
         .setVar(VAR_PROBE, PROBE_BEFORE)
         .writeBytes(PartyContinuationRuntime.CALLBACK, callback)
         .writeBytes(PartyContinuationRuntime.LITERAL_GET_RAM_SCRIPT, callbackLiterals)
         .writeBytes(launcherAddress, launcher)
         .fadeScreen(1)
         .callNative(launcherAddress | 1L)
         .waitState()
         .end();

        return b.buildScript();
    }
}
