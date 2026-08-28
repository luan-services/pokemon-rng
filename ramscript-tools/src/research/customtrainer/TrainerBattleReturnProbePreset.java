/*
   First trainer-battle boundary probe.

   Goal: prove only this transition on FireRed/LeafGreen:

       object-bound RamScript -> stock trainer battle -> ReturnToField
       -> stable IWRAM callback -> relocated object-bound RamScript

   It deliberately reuses a vanilla trainer (Youngster Ben, id 89) and ROM
   text. Custom trainer data is a later phase. The trainer flag is cleared on
   each interaction so the probe remains repeatable.
*/
final class TrainerBattleReturnProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int TRAINER_YOUNGSTER_BEN = 89;
    private static final int VAR_MARKER = 0x8005;

    private TrainerBattleReturnProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return build(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult build(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);

        b.setVAddress().vGoto("start").raw(new byte[] {0x54, 0x42}); // "TB"

        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .setVar(VAR_MARKER, 0x5102)
         .fadeScreen(0)
         .vMessage("returned").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        byte[] callback = binding.continuationCallback(rom, afterBattleOffset);
        byte[] literals = binding.continuationLiterals(rom);

        b.label("start")
         .lockAll()
         .facePlayer()
         .setVar(VAR_MARKER, 0x5101)
         .clearTrainerFlag(TRAINER_YOUNGSTER_BEN)
         .writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, callback)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, literals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .trainerBattleSingle(
                 TRAINER_YOUNGSTER_BEN,
                 target.localId(),
                 rom.trainerProbeIntroText,
                 rom.trainerProbeDefeatText)
         .end(); // not expected to execute; trainerbattle switches into ROM trainer script

        b.text("returned", "Trainer battle returned safely.");

        byte[] payload = b.buildScript();
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    private static byte[] le32(long value) {
        return new byte[] {
                (byte)value,
                (byte)(value >>> 8),
                (byte)(value >>> 16),
                (byte)(value >>> 24)
        };
    }
}
