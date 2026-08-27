/* Probe 3B: keep the validated e-Reader lifecycle and add only slots 3..5. */
final class BrockSixMonBattleProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_MARKER = 0x8005;
    private static final int VAR_DIAGNOSTIC = 0x8006;
    private static final int VAR_SPECIAL_MODE = 0x8004;
    private static final int SPECIAL_START_SPECIAL_BATTLE = 236;
    private static final int SPECIAL_VALIDATE_EREADER_TRAINER = 246;
    private static final int SPECIAL_PLAY_SPECIAL_MAP_MUSIC = 153;

    private BrockSixMonBattleProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("start").raw(new byte[]{0x54,0x33});

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setVar(VAR_MARKER, 0x5403)
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .fadeScreen(0)
         .vMessage("returned").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        long copierAddress = rom.stringVar4 + 0x100L;
        long stagingAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = BrockSixMonEReaderNative.buildAt(rom, stagingAddress);
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress,
                "brock_six_mon_helper", NativeHelperInstaller.Mode.AUTO);

        byte[] continuation = binding.continuationCallback(rom, afterBattleOffset);
        byte[] continuationLiterals = binding.continuationLiterals(rom);

        b.label("start").lockAll().facePlayer().setVar(VAR_MARKER, 0x5401);
        plan.installAndCall(b); // install helper + entry 0: install e-reader descriptor
        b.setVar(VAR_MARKER, 0x5402)
         .special(SPECIAL_VALIDATE_EREADER_TRAINER)
         .copyVar(VAR_DIAGNOSTIC, 0x800D)
         .writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, continuation)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, continuationLiterals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .setVar(VAR_SPECIAL_MODE, 2)
         .special(SPECIAL_START_SPECIAL_BATTLE)
         // StartSpecialBattle(2) has now zeroed enemy party and created slots 0..2.
         // Fill 3..5 in the same field-script tick, before Task_WaitBT reaches CB2_InitBattle.
         .callNative((stagingAddress + BrockSixMonEReaderNative.EXTRA_ENTRY_OFFSET) | 1L)
         .waitState().end();

        b.text("returned", "Six-Pokemon Brock probe returned safely.");
        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    private static byte[] le32(long value) {
        return new byte[]{(byte)value,(byte)(value>>>8),(byte)(value>>>16),(byte)(value>>>24)};
    }
}
