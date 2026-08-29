/* Audited stock leader-script entrypoints by supported ROM profile.
   Object-bound custom trainer cards delegate here before Hall of Fame. */
final class CustomTrainerVanillaScripts {
    private CustomTrainerVanillaScripts() {}

    static long brock(RomProfile r){return pick(r,0x0816A593L,0x0816A60BL,0x0816A56FL,0x0816A5E7L);}
    static long misty(RomProfile r){return pick(r,0x0816AAA1L,0x0816AB19L,0x0816AA7DL,0x0816AAF5L);}
    static long ltSurge(RomProfile r){return pick(r,0x0816B94AL,0x0816B9C2L,0x0816B926L,0x0816B99EL);}
    static long erika(RomProfile r){return pick(r,0x0816D061L,0x0816D0D9L,0x0816D03DL,0x0816D0B5L);}
    static long koga(RomProfile r){return pick(r,0x0816D54EL,0x0816D5C6L,0x0816D52AL,0x0816D5A2L);}
    static long sabrina(RomProfile r){return pick(r,0x0816EDD8L,0x0816EE50L,0x0816EDB4L,0x0816EE2CL);}
    static long blaine(RomProfile r){return pick(r,0x0816D9D4L,0x0816DA4CL,0x0816D9B0L,0x0816DA28L);}
    static long giovanni(RomProfile r){return pick(r,0x08169ECAL,0x08169F42L,0x08169EA6L,0x08169F1EL);}
    static long fiveIslandFisher(RomProfile r){return pick(r,0x08167EAAL,0x08167F22L,0x08167E86L,0x08167EFEL);}

    private static long pick(RomProfile r,long fr10,long fr11,long lg10,long lg11){
        return switch(r){
            case FIRE_RED_EN_10 -> fr10; case FIRE_RED_EN_11 -> fr11;
            case LEAF_GREEN_EN_10 -> lg10; case LEAF_GREEN_EN_11 -> lg11;
        };
    }
}
