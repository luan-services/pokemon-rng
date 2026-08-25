/*
   Build 46: prove selected-slot -> stock GetEvolutionTargetSpecies(EVO_MODE_TRADE).

   Reuses the validated Build 45c relocation-safe Party continuation. The helper
   is staged only AFTER the Party return, so it is not expected to survive the
   ReturnToField heap/save-block relocation.
*/
final class TradeEvolutionTargetProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_PROBE = 0x8005;
    private static final int VAR_SLOT_COPY = 0x8006;
    private static final int VAR_TARGET_COPY = 0x8007;
    private static final int VAR_RESULT = 0x800D;
    private static final int PROBE_BEFORE = 0x4601;
    private static final int PROBE_AFTER = 0x4602;

    private TradeEvolutionTargetProbePreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 46 target probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        long targetHelperAddress = rom.stringVar4 + 0x180L;

        byte[] callback = PartyContinuationRuntime.callback(rom);
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);
        NativeHelper targetHelper = TradeEvolutionTargetProbeHelper.buildAt(rom, targetHelperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress()
         .vGoto("installer")
         .raw(new byte[] { 0x46, (byte)0xC0 });

        if (b.position() != PartyContinuationRuntime.CONTINUATION_OFFSET)
            throw new IllegalStateException("Build 46 continuation must begin at RamScript+0x0C");

        b.label("continuation")
         .copyVar(VAR_SLOT_COPY, VAR_SELECTED)
         .setVar(VAR_RESULT, 0);
        targetHelper.installAndCall(b);
        b.copyVar(VAR_TARGET_COPY, VAR_RESULT)
         .setVar(VAR_PROBE, PROBE_AFTER)
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
