import java.util.List;

/* Editable production placeholders for the first authored Gym Leader rematches.
   These use strong Gen III-style sets while leaving final event balance/text open. */
final class CustomTrainerBattleExamples {
    private static final int LEFTOVERS = 0x00C8;
    private static final int CHOICE_BAND = 0x00BA;

    private CustomTrainerBattleExamples() {}

    static CustomTrainerBattleSpec brock() {
        return new CustomTrainerBattleSpec(
                "brock-postgame-rematch", TrainerIdentityHost.BROCK, BattleMusic.GYM_LEADER,
                CustomTrainerCompletionFlag.BROCK,
                "A Champion, huh?\\nShow me your strength!",
                "Your strength is rock solid!",
                "That was a solid battle.",
                "That was a solid battle.",
                List.of(
                        mon(76, 72, LEFTOVERS, new int[]{89,157,153,38}, 252,252,4,0,0,0, 0xB0010001L, "GOLEM"),
                        mon(139,73, LEFTOVERS, new int[]{191,57,58,92},40,0,0,216,252,0, 0xB0010002L, "OMASTAR"),
                        mon(142,75, CHOICE_BAND,new int[]{157,89,38,332},4,252,0,252,0,0, 0xB0010003L, "AERODACTYL")
                )
        );
    }

    static CustomTrainerBattleSpec misty() {
        return new CustomTrainerBattleSpec(
                "misty-postgame-rematch", TrainerIdentityHost.MISTY, BattleMusic.GYM_LEADER,
                CustomTrainerCompletionFlag.MISTY,
                "A Champion? Great!\\nTry to keep up!",
                "Wow! You really are something!",
                "That was fun! Nice battle.",
                "That was fun! Nice battle.",
                List.of(
                        mon(121,75, LEFTOVERS,new int[]{56,58,85,105},4,0,0,252,252,0, 0xA1150001L, "STARMIE"),
                        mon(131,73, LEFTOVERS,new int[]{57,58,85,215},120,0,252,136,0,0, 0xA1150002L, "LAPRAS"),
                        mon(130,74, LEFTOVERS,new int[]{349,89,1,38},68,252,0,188,0,0, 0xA1150003L, "GYARADOS")
                )
        );
    }

    static CustomTrainerBattleSpec brockSixMonBoundary() {
        return new CustomTrainerBattleSpec(
                "brock-six-mon-boundary", TrainerIdentityHost.BROCK, BattleMusic.GYM_LEADER,
                CustomTrainerCompletionFlag.BROCK,
                "A Champion, huh?\\nShow me your strength!",
                "Your strength is rock solid!",
                "That was a solid battle.",
                "That was a solid battle.",
                List.of(
                        mon(76, 72, LEFTOVERS, new int[]{89,157,153,38}, 252,252,4,0,0,0, 0xB0010001L, "GOLEM"),
                        mon(139,73, LEFTOVERS, new int[]{191,57,58,92},40,0,0,216,252,0, 0xB0010002L, "OMASTAR"),
                        mon(142,75, CHOICE_BAND,new int[]{157,89,38,332},4,252,0,252,0,0, 0xB0010003L, "AERODACTYL"),
                        mon(95, 74, LEFTOVERS, new int[]{89,157,38,153}, 252,252,4,0,0,0, 0xB0010004L, "ONIX"),
                        mon(112,76, LEFTOVERS, new int[]{89,157,38,58}, 252,252,4,0,0,0, 0xB0010005L, "RHYDON"),
                        mon(141,77, LEFTOVERS, new int[]{57,157,332,14}, 4,252,0,252,0,0, 0xB0010006L, "KABUTOPS")
                )
        );
    }

    private static EReaderTrainerData.Mon mon(int species, int level, int item, int[] moves,
                                               int hp, int atk, int def, int spe, int spa, int spd,
                                               long personality, String nickname) {
        return new EReaderTrainerData.Mon(species, level, item, moves, 0, hp, atk, def, spe, spa, spd,
                0x43545231L, 31,31,31,31,31,31, 0, personality, nickname, 255);
    }
}
