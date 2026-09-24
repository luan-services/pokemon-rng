/*
   Mew Boss Probe — Probe 24: loss-safe battle lifecycle + preserved MYSTRY reward.

   Scope is deliberately narrow: object-bound trigger -> stock scripted wild
   encounter -> customize the stock Mew through SetMonMoveSlot -> battle ->
   stock return to the same RamScript -> report gBattleOutcome.

   Capture blocking uses the Probe-19 validated Ball transaction. Reward delivery
   is intentionally FR1.0-only and still requires in-game validation.
*/
final class MewBossProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int SPECIES_MEW = 151;
    private static final int VAR_RESULT = 0x800D;
    private static final int OUTCOME_WON = 1;
    private static final int OUTCOME_RAN = 4;
    private static final int OUTCOME_CAUGHT = 7;
    private static final long G_BATTLE_OUTCOME = 0x02023E8AL;

    private MewBossProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        return buildForObjectEvent(rom, ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
    }

    static TriggerBuildResult buildForObjectEvent(RomProfile rom, ObjectEventTarget target) {
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(target);
        byte[] payload = buildPayload(rom);
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10) throw new IllegalArgumentException("Probe 24 is intentionally FR1.0-only");

        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
        long copierAddress = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        MewBossBattleTransactionNative.Layout tx = MewBossBattleTransactionNative.buildAt(rom, helperAddress);
        NativeHelper helper = tx.helper();
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);

        // Keep the relocation callback target tiny and below 0x100: the existing
        // object-bound continuation bridge encodes its resume offset as Thumb ADD #imm8.
        b.setVAddress().vGoto("start");
        int afterBattleOffset = b.position();
        b.label("after_battle")
         .setVAddressHere()
         .vGoto("after_battle_body");

        // prepare() emits unreachable raw installer blocks here. The entry vgoto
        // skips them, while the post-battle trampoline can jump over them safely.
        NativeHelperInstaller.Plan txPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress, "mew_boss_tx", NativeHelperInstaller.Mode.AUTO);

        b.label("after_battle_body")
         .special(180) // GetBattleOutcome -> VAR_RESULT
         .compareVarToValue(VAR_RESULT, OUTCOME_WON).vGotoIfEqual("reward")
         .releaseAll().end();

        byte[] callback = binding.continuationCallback(rom, afterBattleOffset);
        byte[] literals = binding.continuationLiterals(rom);

        b.label("start")
         .lockAll().facePlayer()
         .setWildBattle(SPECIES_MEW, 100, 0);

        // One compact helper performs the Probe-4 move customization and the
        // logical Ball backup/clear in a single staging pass.
        txPlan.installAndCall(b);

        // Arm the same relocation-safe
        // fieldCallback2 bridge used by the validated object-bound trainer/
        // Trade Evolution infrastructure BEFORE the stock scripted wild battle.
        b.writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, callback)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, literals)
         .writeBytes(rom.fieldCallback2, le32(TradeEvolutionContinuationRuntime.CALLBACK | 1L))
         .callNative((helper.stagingAddress() + tx.launchOffset()) | 1L)
         .waitState().end(); // custom end callback chooses continuation vs whiteout

        b.label("reward")
         // Keep the Probe-22 GAME-VALIDATED placement path. For this first
         // preservation probe the party must be full, so givemon chooses a PC
         // slot and exposes its box/position globals; then replace that exact
         // slot with preserved MYSTRY #001 bytes.
         .giveMon(SPECIES_MEW, 10, 0, 0, 0, 0)
         .callNative((helper.stagingAddress() + tx.rewardOffset()) | 1L)
         .releaseAll().end();

        return b.buildScript();
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
