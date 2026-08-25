/*
   Build 47a: same scene probe as Build 47, with the BeginEvolutionScene
   thunk fixed so r3/partyId survives the branch trampoline.

   This build intentionally still targets Pokemon already proven by Build 46
   to have a nonzero EVO_MODE_TRADE target. No-target/cancel handling remains
   outside this probe so we change only one diagnostic variable at a time.
*/
final class TradeEvolutionSceneProbe47aPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_PROBE = 0x8005;
    private static final int VAR_SLOT_COPY = 0x8006;
    private static final int PROBE_BEFORE_PARTY = 0x47A1;
    private static final int PROBE_BEFORE_EVO = 0x47A2;
    private static final int PROBE_AFTER_EVO = 0x47A3;

    private TradeEvolutionSceneProbe47aPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 47a evolution-scene probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        long evoHelperAddress = rom.stringVar4 + 0x180L;
        byte[] callback = PartyContinuationRuntime.callback(rom);
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);
        NativeHelper evoHelper = TradeEvolutionSceneProbe47aHelper.buildAt(rom, evoHelperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("installer").raw(new byte[] {0x47, (byte)0xA0});
        if (b.position() != PartyContinuationRuntime.CONTINUATION_OFFSET)
            throw new IllegalStateException("Build 47a continuation must begin at RamScript+0x0C");

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
