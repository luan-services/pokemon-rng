/*
   Trainer Battle Probe 2.

   Goal:
     object-bound RamScript
       -> install a stock BattleTowerEReaderTrainer into SaveBlock2
       -> StartSpecialBattle(mode 2)
       -> engine materializes gEnemyParty from save data
       -> battle
       -> object-bound continuation after SaveBlock relocation

   This intentionally uses the stock e-Reader battle path as a capability
   probe. It is not yet the final CustomTrainerBattlePreset.
*/
final class EReaderTrainerBattleProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_MARKER = 0x8005;
    private static final int VAR_DIAGNOSTIC = 0x8006;
    private static final int VAR_SPECIAL_MODE = 0x8004;
    private static final int SPECIAL_START_SPECIAL_BATTLE = 236;
    private static final int SPECIAL_VALIDATE_EREADER_TRAINER = 246;
    private static final int SPECIAL_PLAY_SPECIAL_MAP_MUSIC = 153;

    private EReaderTrainerBattleProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);

        b.setVAddress().vGoto("start").raw(new byte[] {0x54, 0x32}); // "T2"

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setVar(VAR_MARKER, 0x5203)
         // gFieldCallback2 has priority over the stock e-Reader return callback.
         // When our continuation completes, RunFieldCallback clears gFieldCallback,
         // so FieldCB_ContinueScriptHandleMusic never gets to restore the map BGM.
         // Re-run the same stock music operation explicitly before our own fade-in.
         .special(SPECIAL_PLAY_SPECIAL_MAP_MUSIC)
         .fadeScreen(0)
         .vMessage("returned").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] trainerData = EReaderTrainerData.probeTrainer();
        long copierAddress = rom.stringVar4 + 0x100L;
        long installerAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper installer = EReaderTrainerInstallerNative.buildAt(rom, installerAddress, trainerData);
        NativeHelperInstaller.Plan installerPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, installer, copierAddress,
                "ereader_trainer_installer", NativeHelperInstaller.Mode.AUTO);

        byte[] continuation = binding.continuationCallback(rom, afterBattleOffset);
        byte[] continuationLiterals = binding.continuationLiterals(rom);

        b.label("start")
         .lockAll()
         .facePlayer()
         .setVar(VAR_MARKER, 0x5201)
         // Reuse the already validated staged-native installation path used
         // elsewhere in the toolkit. The installer is copied to stable EWRAM
         // and entered through stock callnative; no new IWRAM trampoline is used.
         ;
        installerPlan.installAndCall(b);
        b.setVar(VAR_MARKER, 0x5202)
         // Stock checksum validation: VAR_RESULT -> diagnostic variable.
         .special(SPECIAL_VALIDATE_EREADER_TRAINER)
         .copyVar(VAR_DIAGNOSTIC, 0x800D) // VAR_RESULT
         // Replace the transient installer trampoline with the proven
         // ReturnToField continuation bridge before entering battle.
         .writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, continuation)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, continuationLiterals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .setVar(VAR_SPECIAL_MODE, 2)
         .special(SPECIAL_START_SPECIAL_BATTLE)
         .waitState()
         .end();

        b.text("returned", "Custom trainer battle returned safely.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    private static byte[] le32(long value) {
        return new byte[] {(byte)value,(byte)(value>>>8),(byte)(value>>>16),(byte)(value>>>24)};
    }
}
