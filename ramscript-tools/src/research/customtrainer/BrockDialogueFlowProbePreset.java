/* Probe 3F: custom dialogue around the validated hybrid trainer battle lifecycle. */
final class BrockDialogueFlowProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_MARKER = 0x8005;
    private static final int SPECIAL_PLAY_SPECIAL_MAP_MUSIC = 153;

    private BrockDialogueFlowProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("start");

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setVar(VAR_MARKER, 0x5803)
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .fadeScreen(0)
         .vMessage("win_overworld").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        long copierAddress = rom.stringVar4 + 0x100L;
        long stagingAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = BrockDialogueFlowProbeNative.buildAt(rom, stagingAddress);
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress,
                "brock_dialogue_flow_helper", NativeHelperInstaller.Mode.AUTO);

        byte[] continuation = binding.continuationCallback(rom, afterBattleOffset);
        byte[] continuationLiterals = binding.continuationLiterals(rom);

        b.label("start").lockAll().facePlayer()
         .vMessage("pre_battle").waitMessage().waitButtonPressStrict().closeMessage();
        plan.install(b); // stage only; launcher must run after return bridge is armed
        b.writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, continuation)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, continuationLiterals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .callNative(helper.thumbEntryAddress())
         .waitState().end();

        b.text("pre_battle", "I have been waiting for a rematch.\\nShow me how strong you have become!")
         .text("win_overworld", "That was a great battle.\\nYou have grown much stronger!");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    private static byte[] le32(long value) {
        return new byte[]{(byte)value,(byte)(value>>>8),(byte)(value>>>16),(byte)(value>>>24)};
    }
}
