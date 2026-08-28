/* Descriptor-only custom trainer battle using a previously persisted
   CustomTrainerBattleRuntimeV2. SB2 owns runtime/battle data; SB1 owns the
   variable-length overworld dialogue pool. */
final class CustomTrainerBattleSharedRuntimePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int SPECIAL_PLAY_SPECIAL_MAP_MUSIC = 153;

    private CustomTrainerBattleSharedRuntimePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom, CustomTrainerBattleSpec spec) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M, spec);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target, CustomTrainerBattleSpec spec) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        long textReader = CustomTrainerBattleRuntimeV2.fieldTextReaderAddress(rom);
        b.setVAddress().vGoto("start");

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setFlag(spec.completionFlag().eventFlag())
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.POST_VICTORY)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] descriptor = CustomTrainerBattleDescriptor.encode(spec, target.localId(), afterBattleOffset);
        byte[] fieldTexts = CustomTrainerFieldTextStorage.encode(spec);
        NativeHelper loader = CustomTrainerRuntimeTransportNative.descriptorLoader(rom, descriptor, fieldTexts);
        NativeHelperInstaller.Plan loaderPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, loader, rom.stringVar4,
                "custom_trainer_descriptor_loader", NativeHelperInstaller.Mode.AUTO);

        b.label("start").lockAll().facePlayer();
        loaderPlan.installAndCall(b);
        b.compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("runtime_missing")
         .checkFlag(spec.completionFlag().eventFlag()).vGotoIfEqual("already_done")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.PRE_BATTLE)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict().closeMessage()
         .callNative(CustomTrainerBattleRuntimeV2.stagingAddress(rom) | 1L)
         .compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("runtime_invalid")
         .waitState().end();

        b.label("already_done")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.ALREADY_COMPLETED)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.label("runtime_missing")
         .vMessage("runtime_missing_text").waitMessage().waitButtonPressStrict()
         .releaseAll().end();
        b.label("runtime_invalid")
         .vMessage("runtime_invalid_text").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.text("runtime_missing_text", "Runtime v2 missing.")
         .text("runtime_invalid_text", "Trainer data invalid.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }
    static TriggerBuildResult buildGymLeader(RomProfile rom, ObjectEventTarget target, CustomTrainerBattleSpec spec, long vanillaScript) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        long textReader = CustomTrainerBattleRuntimeV2.fieldTextReaderAddress(rom);
        b.setVAddress().vGoto("start");

        int afterBattleOffset = b.position();
        b.label("after_battle").setVAddressHere()
         .setFlag(spec.completionFlag().eventFlag())
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.POST_VICTORY)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] descriptor = CustomTrainerBattleDescriptor.encode(spec, target.localId(), afterBattleOffset);
        byte[] fieldTexts = CustomTrainerFieldTextStorage.encode(spec);
        NativeHelper loader = CustomTrainerRuntimeTransportNative.descriptorLoader(rom, descriptor, fieldTexts);
        NativeHelperInstaller.Plan loaderPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, loader, rom.stringVar4,
                "custom_trainer_descriptor_loader", NativeHelperInstaller.Mode.AUTO);

        b.label("start")
         // Before the first Hall of Fame clear, this object behaves exactly as stock.
         .checkFlag(0x082C).vGotoIfEqual("postgame")
         .gotoAddress(vanillaScript)
         .label("postgame")
         .lockAll().facePlayer();
        loaderPlan.installAndCall(b);
        b.compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("runtime_missing")
         .checkFlag(spec.completionFlag().eventFlag()).vGotoIfEqual("already_done")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.PRE_BATTLE)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict().closeMessage()
         .callNative(CustomTrainerBattleRuntimeV2.stagingAddress(rom) | 1L)
         .compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("runtime_invalid")
         .waitState().end();

        b.label("already_done")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.ALREADY_COMPLETED)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();
        b.label("runtime_missing").vMessage("runtime_missing_text").waitMessage().waitButtonPressStrict().releaseAll().end();
        b.label("runtime_invalid").vMessage("runtime_invalid_text").waitMessage().waitButtonPressStrict().releaseAll().end();
        b.text("runtime_missing_text", "Trainer battle runtime is not installed.")
         .text("runtime_invalid_text", "Trainer data invalid.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0, payload.length, RamScript.SCRIPT_SIZE-payload.length);
    }

}
