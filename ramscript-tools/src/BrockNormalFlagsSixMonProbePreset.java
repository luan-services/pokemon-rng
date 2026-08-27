/* Probe 3C: six custom mons survive CB2_InitBattle, then battle flags become normal trainer. */
final class BrockNormalFlagsSixMonProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_MARKER = 0x8005;
    private static final int SPECIAL_PLAY_SPECIAL_MAP_MUSIC = 153;

    private BrockNormalFlagsSixMonProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("start").raw(new byte[]{0x55,0x33});

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setVar(VAR_MARKER, 0x5503)
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .fadeScreen(0)
         .releaseAll().end();

        long copierAddress = rom.stringVar4 + 0x100L;
        long stagingAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = BrockNormalFlagsSixMonNative.buildAt(rom, stagingAddress);
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress,
                "brock_normal_flags_six_mon_helper", NativeHelperInstaller.Mode.AUTO);

        byte[] continuation = binding.continuationCallback(rom, afterBattleOffset);
        byte[] continuationLiterals = binding.continuationLiterals(rom);

        b.label("start").lockAll().facePlayer().setVar(VAR_MARKER, 0x5501);
        plan.install(b); // stage only; launcher must run after return bridge is armed
        b.writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, continuation)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, continuationLiterals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .setVar(VAR_MARKER, 0x5502)
         .callNative(helper.thumbEntryAddress())
         .waitState().end();

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    private static byte[] le32(long value) {
        return new byte[]{(byte)value,(byte)(value>>>8),(byte)(value>>>16),(byte)(value>>>24)};
    }
}
