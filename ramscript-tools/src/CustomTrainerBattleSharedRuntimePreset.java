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
         .label("post_dialogue")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.POST_VICTORY)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] compactDescriptor = CustomTrainerCompactTransport.encode(spec, target.localId(), afterBattleOffset);
        byte[] fieldTexts = CustomTrainerFieldTextStorage.encode(spec);
        NativeHelper loader = CustomTrainerRuntimeTransportNative.compactDescriptorLoader(rom, compactDescriptor, fieldTexts);
        NativeHelperInstaller.Plan loaderPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, loader, rom.stringVar4,
                "custom_trainer_descriptor_loader", NativeHelperInstaller.Mode.AUTO);

        b.label("start").lockAll().facePlayer();
        loaderPlan.installAndCall(b);
        b.compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("trainer_unavailable")
         .checkFlag(spec.completionFlag().eventFlag()).vGotoIfEqual("post_dialogue")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.PRE_BATTLE)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict().closeMessage()
         .callNative(CustomTrainerBattleRuntimeV2.stagingAddress(rom) | 1L)
         .compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("trainer_unavailable")
         .waitState().end();

        b.label("trainer_unavailable")
         .vMessage("trainer_unavailable_text").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.text("trainer_unavailable_text", "Unavailable.");

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
         .label("post_dialogue")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.POST_VICTORY)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] compactDescriptor = CustomTrainerCompactTransport.encode(spec, target.localId(), afterBattleOffset);
        byte[] fieldTexts = CustomTrainerFieldTextStorage.encode(spec);
        NativeHelper loader = CustomTrainerRuntimeTransportNative.compactDescriptorLoader(rom, compactDescriptor, fieldTexts);
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
        b.compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("trainer_unavailable")
         .checkFlag(spec.completionFlag().eventFlag()).vGotoIfEqual("post_dialogue")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.PRE_BATTLE)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict().closeMessage()
         .callNative(CustomTrainerBattleRuntimeV2.stagingAddress(rom) | 1L)
         .compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("trainer_unavailable")
         .waitState().end();
        b.label("trainer_unavailable").vMessage("trainer_unavailable_text").waitMessage().waitButtonPressStrict().releaseAll().end();
        b.text("trainer_unavailable_text", "Unavailable.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0, payload.length, RamScript.SCRIPT_SIZE-payload.length);
    }

    /* Five Island Giovanni production binding. Five Island is postgame-only, so
       unlike the seven vanilla Gym hosts it needs no Hall-of-Fame fallback. The
       permanent Fisherman is used as the safe object/script slot; while the screen
       is black, this card's transient loader changes the live object to Giovanni. */
    static TriggerBuildResult buildGiovanniFiveIsland(RomProfile rom, ObjectEventTarget target, CustomTrainerBattleSpec spec) {
        final int OBJ_EVENT_GFX_GIOVANNI = 87;
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        long textReader = CustomTrainerBattleRuntimeV2.fieldTextReaderAddress(rom);
        b.setVAddress().vGoto("start");

        int afterBattleOffset = b.position();
        b.label("after_battle").setVAddressHere()
         .setFlag(spec.completionFlag().eventFlag())
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .label("post_dialogue")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.POST_VICTORY)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] compactDescriptor = CustomTrainerCompactTransport.encode(spec, target.localId(), afterBattleOffset);
        byte[] fieldTexts = CustomTrainerFieldTextStorage.encode(spec);
        NativeHelper loader = CustomTrainerRuntimeTransportNative.compactDescriptorLoaderWithObjectGraphics(
                rom, compactDescriptor, fieldTexts, target, OBJ_EVENT_GFX_GIOVANNI);
        NativeHelperInstaller.Plan loaderPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, loader, rom.stringVar4,
                "custom_trainer_giovanni_descriptor_loader", NativeHelperInstaller.Mode.AUTO);

        b.label("start")
         .lockAll()
         .applyMovement(target.localId(), commonMovementExclamationMark(rom))
         .waitMovement(target.localId())
         .fadeScreen(1);
        loaderPlan.installAndCall(b);
        b.compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("trainer_unavailable_black")
         .facePlayer()
         .fadeScreen(0)
         .checkFlag(spec.completionFlag().eventFlag()).vGotoIfEqual("post_dialogue")
         .setVar(Vars.VAR_8004, CustomTrainerFieldTextStorage.PRE_BATTLE)
         .callNative(textReader)
         .message(rom.stringVar4).waitMessage().waitButtonPressStrict().closeMessage()
         .callNative(CustomTrainerBattleRuntimeV2.stagingAddress(rom) | 1L)
         .compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("trainer_unavailable")
         .waitState().end();

        b.label("trainer_unavailable_black")
         .fadeScreen(0)
         .vGoto("trainer_unavailable");
        b.label("trainer_unavailable")
         .vMessage("trainer_unavailable_text").waitMessage().waitButtonPressStrict()
         .releaseAll().end();
        b.text("trainer_unavailable_text", "Unavailable.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0, payload.length, RamScript.SCRIPT_SIZE-payload.length);
    }

    private static long commonMovementExclamationMark(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x081A75DBL;
            case FIRE_RED_EN_11 -> 0x081A7653L;
            case LEAF_GREEN_EN_10 -> 0x081A75B7L;
            case LEAF_GREEN_EN_11 -> 0x081A762FL;
        };
    }

}
