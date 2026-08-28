final class CustomTrainerSharedRuntimeVerifierPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private CustomTrainerSharedRuntimeVerifierPreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
        NativeHelper checker = CustomTrainerRuntimeTransportNative.runtimeHeaderChecker(rom);
        long copier = rom.stringVar4 + 0x100L;
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("start");
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(b, VIRTUAL_BASE, checker, copier,
                "custom_trainer_runtime_verify", NativeHelperInstaller.Mode.AUTO);
        b.label("start").lockAll().facePlayer();
        plan.installAndCall(b);
        b.compareVarToValue(Vars.RESULT,1).vGotoIfEqual("ok")
         .vMessage("missing").waitMessage().waitButtonPressStrict().releaseAll().end()
         .label("ok").vMessage("found").waitMessage().waitButtonPressStrict().releaseAll().end()
         .text("missing","Trainer runtime header is missing after reload.")
         .text("found","Trainer runtime v2 header survived the save.");
        byte[] payload=b.buildScript();
        RamScript script=binding.createRamScript(payload);
        return new TriggerBuildResult(script,binding.trigger(),rom,payload.length,0,payload.length,RamScript.SCRIPT_SIZE-payload.length);
    }
}
