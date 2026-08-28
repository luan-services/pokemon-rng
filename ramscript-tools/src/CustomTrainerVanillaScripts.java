/* Audited stock leader-script entrypoints by supported ROM profile.
   Object-bound custom trainer cards delegate here before Hall of Fame. */
final class CustomTrainerVanillaScripts {
    private CustomTrainerVanillaScripts() {}

    static long brock(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x0816A593L;
            case FIRE_RED_EN_11 -> 0x0816A60BL;
            case LEAF_GREEN_EN_10 -> 0x0816A56FL;
            case LEAF_GREEN_EN_11 -> 0x0816A5E7L;
        };
    }

    static long misty(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x0816AAA1L;
            case FIRE_RED_EN_11 -> 0x0816AB19L;
            case LEAF_GREEN_EN_10 -> 0x0816AA7DL;
            case LEAF_GREEN_EN_11 -> 0x0816AAF5L;
        };
    }
}
