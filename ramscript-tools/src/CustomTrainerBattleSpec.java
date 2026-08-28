import java.util.List;

/* High-level, card-specific data. Runtime mechanics intentionally do not live here. */
record CustomTrainerBattleSpec(
        String id,
        TrainerIdentityHost identity,
        BattleMusic battleMusic,
        CustomTrainerCompletionFlag completionFlag,
        String preBattleText,
        String defeatText,
        String postVictoryText,
        String alreadyCompletedText,
        List<EReaderTrainerData.Mon> party) {
    CustomTrainerBattleSpec {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (identity == null || battleMusic == null || completionFlag == null) throw new IllegalArgumentException("identity/music/flag required");
        if (party == null || party.isEmpty() || party.size() > 6) throw new IllegalArgumentException("party must contain 1..6 mons");
        party = List.copyOf(party);
    }
}
