/*
   Trainer Battle Probe 3A.

   Goal:
     keep the already validated stock e-Reader trainer battle lifecycle,
     but replace the generic Youngster identity with FR/LG's facility-class
     mapping for a male Gym Leader and the name BROCK.

   This deliberately does NOT try six Pokemon yet. It isolates only the next
   question: can our custom-party path present a Kanto Gym Leader identity
   without any ROM patching or new battle callback machinery?
*/
final class BrockIdentityBattleProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_MARKER = 0x8005;
    private static final int VAR_DIAGNOSTIC = 0x8006;
    private static final int VAR_SPECIAL_MODE = 0x8004;
    private static final int SPECIAL_START_SPECIAL_BATTLE = 236;
    private static final int SPECIAL_VALIDATE_EREADER_TRAINER = 246;
    private static final int SPECIAL_PLAY_SPECIAL_MAP_MUSIC = 153;

    private BrockIdentityBattleProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);

        b.setVAddress().vGoto("start").raw(new byte[] {0x54, 0x33}); // "T3"

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setVar(VAR_MARKER, 0x5303)
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .fadeScreen(0)
         .vMessage("returned").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] trainerData = EReaderTrainerData.brockIdentityProbeTrainer();
        long copierAddress = rom.stringVar4 + 0x100L;
        long installerAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper installer = EReaderTrainerInstallerNative.buildAt(rom, installerAddress, trainerData);
        NativeHelperInstaller.Plan installerPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, installer, copierAddress,
                "brock_identity_trainer_installer", NativeHelperInstaller.Mode.AUTO);

        byte[] continuation = binding.continuationCallback(rom, afterBattleOffset);
        byte[] continuationLiterals = binding.continuationLiterals(rom);

        b.label("start")
         .lockAll()
         .facePlayer()
         .setVar(VAR_MARKER, 0x5301);
        installerPlan.installAndCall(b);
        b.setVar(VAR_MARKER, 0x5302)
         .special(SPECIAL_VALIDATE_EREADER_TRAINER)
         .copyVar(VAR_DIAGNOSTIC, 0x800D)
         .writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, continuation)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, continuationLiterals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .setVar(VAR_SPECIAL_MODE, 2)
         .special(SPECIAL_START_SPECIAL_BATTLE)
         .waitState()
         .end();

        b.text("returned", "Brock identity probe returned safely.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    private static byte[] le32(long value) {
        return new byte[] {(byte)value,(byte)(value>>>8),(byte)(value>>>16),(byte)(value>>>24)};
    }
}
