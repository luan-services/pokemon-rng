import java.util.List;

/* Production authoring data for the eight Kanto postgame Gym Leader rematches.
   Teams are ADV-inspired but adapted for the FR/LG AI: no Hidden Power dependency,
   no prediction-heavy Baton Pass/Trick plans, and six Pokemon per leader. */
final class CustomTrainerGymLeaderPresets {
    private static final int LEFTOVERS = 0x00C8;
    private static final int CHOICE_BAND = 0x00BA;
    private static final int LUM_BERRY = 0x008D;

    private CustomTrainerGymLeaderPresets() {}

    static CustomTrainerBattleSpec brock() {
        return spec("brock-postgame-rematch", TrainerIdentityHost.BROCK, CustomTrainerCompletionFlag.BROCK,
                "Ready for a rematch?\\nMy defense is much\\lharder now!",
                "You broke through it...\\nAgain.",
                List.of(
                        mon(76,72,LEFTOVERS, mv(89,157,153,280), 252,252,0,4,0,0, 0, 0xB1000001L,"GOLEM"),
                        mon(142,74,CHOICE_BAND,mv(157,89,38,332), 4,252,0,252,0,0, 0,0xB1000002L,"AERODACTYL"),
                        mon(139,73,LEFTOVERS,mv(57,58,191,92), 252,0,252,4,0,0, 0,0xB1000003L,"OMASTAR"),
                        mon(219,74,LEFTOVERS,mv(53,157,92,120), 252,0,0,4,252,0, 1,0xB1000004L,"MAGCARGO"),
                        mon(248,76,LUM_BERRY,mv(349,157,89,280), 4,252,0,252,0,0, 0,0xB1000005L,"TYRANITAR"),
                        mon(208,77,LEFTOVERS,mv(89,92,157,153), 252,36,0,0,0,220, 0,0xB1000006L,"STEELIX")
                ));
    }

    static CustomTrainerBattleSpec misty() {
        return spec("misty-postgame-rematch", TrainerIdentityHost.MISTY, CustomTrainerCompletionFlag.MISTY,
                "Glad to see you again!\\nYou already know the rules,\\lright?",
                "C'mon, I lost again.",
                List.of(
                        mon(121,74,LEFTOVERS,mv(57,94,85,105), 4,0,0,252,252,0,0,0xB2000001L,"STARMIE"),
                        mon(130,75,LEFTOVERS,mv(349,89,332,216), 4,252,0,252,0,0,0,0xB2000002L,"GYARADOS"),
                        mon(131,75,LEFTOVERS,mv(57,58,85,156), 252,0,0,0,4,252,0,0xB2000003L,"LAPRAS"),
                        mon(230,76,LEFTOVERS,mv(240,56,58,225), 4,0,0,252,252,0,0,0xB2000004L,"KINGDRA"),
                        mon(134,77,LEFTOVERS,mv(57,58,273,182), 252,0,252,0,4,0,0,0xB2000005L,"VAPOREON"),
                        mon(184,78,CHOICE_BAND,mv(38,280,216,231), 172,252,0,84,0,0,1,0xB2000006L,"AZUMARILL")
                ));
    }

    static CustomTrainerBattleSpec ltSurge() {
        return spec("lt-surge-postgame-rematch", TrainerIdentityHost.LT_SURGE, CustomTrainerCompletionFlag.LT_SURGE,
                "Welcome back, kid.\\nYou gonna get shocked.",
                "I can see why you became\\nthe CHAMPION, kid.",
                List.of(
                        mon(101,75,LEFTOVERS,mv(85,86,113,153), 4,0,0,252,252,0,0,0xB3000001L,"ELECTRODE"),
                        mon(26,76,LEFTOVERS,mv(85,280,86,164), 4,252,0,252,0,0,0,0xB3000002L,"RAICHU"),
                        mon(125,77,LEFTOVERS,mv(85,8,7,280), 4,172,0,240,94,0,0,0xB3000003L,"ELECTABUZZ"),
                        mon(171,77,LEFTOVERS,mv(85,57,109,86), 40,0,0,0,252,218,0,0xB3000004L,"LANTURN"),
                        mon(135,78,LEFTOVERS,mv(85,86,44,164), 4,0,0,252,252,0,0,0xB3000005L,"JOLTEON"),
                        mon(338,79,LEFTOVERS,mv(85,53,86,164), 4,0,0,252,252,0,0,0xB3000006L,"MANECTRIC")
                ));
    }

    static CustomTrainerBattleSpec erika() {
        return spec("erika-postgame-rematch", TrainerIdentityHost.ERIKA, CustomTrainerCompletionFlag.ERIKA,
                "What a nostalgic scent.\\nI'm glad to see you\\lagain.",
                "You've grown so much.",
                List.of(
                        mon(45,76,LEFTOVERS,mv(202,188,79,236), 252,0,0,0,4,252,0,0xB4000001L,"VILEPLUME"),
                        mon(154,77,LEFTOVERS,mv(202,115,73,235), 252,0,252,0,4,0,0,0xB4000002L,"MEGANIUM"),
                        mon(297,78,LEFTOVERS,mv(240,57,58,202), 152,0,0,104,252,0,0,0xB4000003L,"LUDICOLO"),
                        mon(369,78,LEFTOVERS,mv(332,89,73,235), 252,48,128,84,0,0,0,0xB4000004L,"TROPIUS"),
                        mon(103,79,LEFTOVERS,mv(94,202,79,153), 4,0,0,252,252,0,0,0xB4000005L,"EXEGGUTOR"),
                        mon(71,80,LEFTOVERS,mv(188,202,79,14), 4,252,0,252,0,0,0,0xB4000006L,"VICTREEBEL")
                ));
    }

    static CustomTrainerBattleSpec koga() {
        return spec("koga-postgame-rematch", TrainerIdentityHost.KOGA, CustomTrainerCompletionFlag.KOGA,
                "Fwahahaha! Watch out,\\nI won't take it easy\\lthis time!",
                "Humph! You are really\\nhonorable.",
                List.of(
                        mon(89,77,LEFTOVERS,mv(188,280,247,153), 252,252,0,4,0,0,0,0xB5000001L,"MUK"),
                        mon(110,78,LEFTOVERS,mv(188,261,85,153), 252,0,252,0,4,0,0,0xB5000002L,"WEEZING"),
                        mon(169,79,CHOICE_BAND,mv(188,332,247,216), 4,252,0,252,0,0,0,0xB5000003L,"CROBAT"),
                        mon(211,79,LEFTOVERS,mv(188,57,191,153), 4,252,0,252,0,0,0,0xB5000004L,"QWILFISH"),
                        mon(49,80,LEFTOVERS,mv(79,94,202,188), 4,0,0,252,252,0,0,0xB5000005L,"VENOMOTH"),
                        mon(168,81,LEFTOVERS,mv(14,188,324,332), 4,252,0,252,0,0,0,0xB5000006L,"ARIADOS")
                ));
    }

    static CustomTrainerBattleSpec sabrina() {
        return spec("sabrina-postgame-rematch", TrainerIdentityHost.SABRINA, CustomTrainerCompletionFlag.SABRINA,
                "I knew you would surpass\\nthe CHAMPION. Show me\\lyour toughness.",
                "...",
                List.of(
                        mon(65,78,LUM_BERRY,mv(94,347,7,9), 4,0,0,252,252,0,0,0xB6000001L,"ALAKAZAM"),
                        mon(196,79,LEFTOVERS,mv(94,347,234,44), 4,0,0,252,252,0,0,0xB6000002L,"ESPEON"),
                        mon(394,80,LEFTOVERS,mv(94,347,85,95), 4,0,0,252,252,0,1,0xB6000003L,"GARDEVOIR"),
                        mon(199,80,LEFTOVERS,mv(57,94,347,156), 252,0,252,0,4,0,0,0xB6000004L,"SLOWKING"),
                        mon(348,81,LEFTOVERS,mv(94,58,347,153), 252,0,0,0,252,4,0,0xB6000005L,"LUNATONE"),
                        mon(124,82,LEFTOVERS,mv(58,94,142,347), 4,0,0,252,252,0,0,0xB6000006L,"JYNX")
                ));
    }

    static CustomTrainerBattleSpec blaine() {
        return spec("blaine-postgame-rematch", TrainerIdentityHost.BLAINE, CustomTrainerCompletionFlag.BLAINE,
                "Your burning heart led\\nyou to victory! Let's do\\lit one more time!",
                "You're burning as always.",
                List.of(
                        mon(59,79,LEFTOVERS,mv(53,245,231,242), 4,252,0,252,0,0,0,0xB7000001L,"ARCANINE"),
                        mon(38,80,LEFTOVERS,mv(53,261,109,241), 252,0,0,4,252,0,0,0xB7000002L,"NINETALES"),
                        mon(321,80,LEFTOVERS,mv(53,34,92,153), 252,252,0,0,4,0,0,0xB7000003L,"TORKOAL"),
                        mon(126,81,LEFTOVERS,mv(53,9,94,280), 4,0,0,252,252,0,0,0xB7000004L,"MAGMAR"),
                        mon(78,82,LEFTOVERS,mv(126,38,231,332), 4,252,0,252,0,0,0,0xB7000005L,"RAPIDASH"),
                        mon(340,83,LEFTOVERS,mv(89,53,157,153), 252,252,0,0,4,0,0,0xB7000006L,"CAMERUPT")
                ));
    }

    static CustomTrainerBattleSpec giovanni() {
        return new CustomTrainerBattleSpec(
                "giovanni-postgame-rematch", TrainerIdentityHost.GIOVANNI, BattleMusic.CHAMPION, CustomTrainerCompletionFlag.GIOVANNI,
                "...\\nGive me one last dance,\\lkid.",
                "...",
                "You deserve that title...",
                "You deserve that title...",
                List.of(
                        mon(112,80,CHOICE_BAND,mv(89,157,224,38), 4,252,0,252,0,0,0,0xB8000001L,"RHYDON"),
                        mon(31,81,LEFTOVERS,mv(89,188,58,85), 252,0,252,0,4,0,0,0xB8000002L,"NIDOQUEEN"),
                        mon(53,82,CHOICE_BAND,mv(216,247,332,231), 4,252,0,252,0,0,0,0xB8000003L,"PERSIAN"),
                        mon(51,82,CHOICE_BAND,mv(89,157,332,188), 4,252,0,252,0,0,1,0xB8000004L,"DUGTRIO"),
                        mon(34,83,LEFTOVERS,mv(89,224,247,58), 4,252,0,252,0,0,0,0xB8000005L,"NIDOKING"),
                        mon(150,85,LUM_BERRY,mv(94,347,58,105), 4,0,0,252,252,0,0,0xB8000006L,"MEWTWO")
                ));
    }

    static List<CustomTrainerBattleSpec> all() {
        return List.of(brock(), misty(), ltSurge(), erika(), koga(), sabrina(), blaine(), giovanni());
    }

    private static CustomTrainerBattleSpec spec(String id, TrainerIdentityHost host, CustomTrainerCompletionFlag flag,
                                                 String pre, String post, List<EReaderTrainerData.Mon> party) {
        return new CustomTrainerBattleSpec(id, host, BattleMusic.GYM_LEADER, flag, pre, "...", post, post, party);
    }

    private static int[] mv(int a, int b, int c, int d) { return new int[]{a,b,c,d}; }

    private static EReaderTrainerData.Mon mon(int species, int level, int item, int[] moves,
                                               int hp, int atk, int def, int spe, int spa, int spd,
                                               int abilityNum, long personality, String nickname) {
        return new EReaderTrainerData.Mon(species, level, item, moves, 0, hp, atk,def,spe,spa,spd,
                0x43545231L, 31,31,31,31,31,31, abilityNum, personality, nickname, 255);
    }
}
