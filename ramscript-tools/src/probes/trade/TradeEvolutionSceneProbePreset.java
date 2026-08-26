/*
   Build 47: launch the stock standalone evolution scene after the validated
   relocation-safe Party selector. This probe is intentionally for a Pokemon
   already proven by Build 46 to have a nonzero EVO_MODE_TRADE target.
*/
final class TradeEvolutionSceneProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_PROBE = 0x8005;
    private static final int VAR_SLOT_COPY = 0x8006;
    private static final int PROBE_BEFORE_PARTY = 0x4701;
    private static final int PROBE_BEFORE_EVO = 0x4702;
    private static final int PROBE_AFTER_EVO = 0x4703;

    private TradeEvolutionSceneProbePreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 47 evolution-scene probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        long evoHelperAddress = rom.stringVar4 + 0x180L;
        byte[] callback = PartyContinuationRuntime.callback(rom);
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);
        NativeHelper evoHelper = TradeEvolutionSceneProbeHelper.buildAt(rom, evoHelperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("installer").raw(new byte[] {0x46,(byte)0xC0});
        if (b.position() != PartyContinuationRuntime.CONTINUATION_OFFSET)
            throw new IllegalStateException("Build 47 continuation must begin at RamScript+0x0C");

        b.label("continuation")
         .copyVar(VAR_SLOT_COPY, VAR_SELECTED)
         .setVar(VAR_PROBE, PROBE_BEFORE_EVO);
        evoHelper.installAndCall(b);
        b.waitState()
         .setVar(VAR_PROBE, PROBE_AFTER_EVO)
         .releaseAll()
         .end();

        b.label("installer")
         .setVar(VAR_PROBE, PROBE_BEFORE_PARTY)
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
