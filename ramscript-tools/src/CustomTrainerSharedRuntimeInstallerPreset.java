/* Research deployment that installs only CustomTrainerBattleRuntimeV2.
   It deliberately does not install a trainer descriptor. The user saves after
   running this card, then later cards prove that the resident runtime survives
   Wonder Card replacement. */
final class CustomTrainerSharedRuntimeInstallerPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;

    private CustomTrainerSharedRuntimeInstallerPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return build(rom, NoObjectRamScriptBinding.INSTANCE);
    }

    // Kept only for compatibility with the old persistence probe.
    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, new ObjectEventRamScriptBinding(ObjectEventCatalog.LAVENDER_TOWN_WORKER_M));
    }

    static TriggerBuildResult build(RomProfile rom, RamScriptBinding binding) {
        NativeHelper runtime = CustomTrainerBattleRuntimeV2.build(rom);
        byte[] runtimeImage = CustomTrainerRuntimeStorage.runtimeImage(runtime.codeCopy());
        NativeHelper transport = CustomTrainerRuntimeTransportNative.runtimeInstaller(rom, runtimeImage);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("start");

        long copierAddress = rom.stringVar4 + 0x100L;
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, transport, copierAddress,
                "custom_trainer_runtime_transport", NativeHelperInstaller.Mode.AUTO);

        b.label("start")
         .lockAll().facePlayer();
        plan.installAndCall(b);
        b.compareVarToValue(Vars.RESULT, 1).vGotoIfNotEqual("install_failed")
         .vMessage("installed").waitMessage().waitButtonPressStrict()
         .releaseAll().end()
         .label("install_failed")
         .vMessage("failed").waitMessage().waitButtonPressStrict()
         .releaseAll().end()
         .text("installed", "Trainer battle runtime installed.\\nPlease install a custom trainer wondercard.")
         .text("failed", "Runtime write failed.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }
}
