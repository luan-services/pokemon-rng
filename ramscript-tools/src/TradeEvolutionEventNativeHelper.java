
/*
   Build 43g temporary helper.

   menu entry:
     ChooseMonForTradingBoard(PARTY_MENU_TYPE_FIELD,
         CB2_ReturnToFieldContinueScriptPlayMapMusic)
     then sets gPartyMenu.action = PARTY_ACTION_CHOOSE_AND_CLOSE.

   Why this path:
   - ChooseMonForTradingBoard is a public FireRed function with only 2 args;
   - it delegates all screen setup to stock InitPartyMenu;
   - the callback is the stock "return to field and continue script" path;
   - changing only action 0 -> 11 makes the stock selection handler write
     VAR_0x8004 and close immediately.

   evolution entry:
     existing stock-table trade evolution helper.

   Temporary EWRAM only. No resident IWRAM.
*/
final class TradeEvolutionEventNativeHelper {
    static final int MENU_ENTRY_OFFSET = 0x00;
    static final int EVO_ENTRY_OFFSET = 0x20;
    static final int CODE_SIZE = EVO_ENTRY_OFFSET + TradeEvolutionNativeHelper.CODE_SIZE;

    private TradeEvolutionEventNativeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 43g is FR1.0-only");

        byte[] code = new byte[CODE_SIZE];

        // 0000 push {r4,lr}
        // 0002 movs r0,#0                         ; PARTY_MENU_TYPE_FIELD
        // 0004 ldr  r1, =CB2_ReturnToFieldContinueScriptPlayMapMusic|1
        // 0006 ldr  r3, =ChooseMonForTradingBoard|1
        // 0008 blx  r3
        // 000A ldr  r0, =gPartyMenu
        // 000C movs r1,#11                        ; PARTY_ACTION_CHOOSE_AND_CLOSE
        // 000E strb r1,[r0,#11]                   ; gPartyMenu.action
        // 0010 pop  {r4,pc}
        // 0012 nop
        int[] ins = {
                0xB510,
                0x2000,
                0x4903, // PC@0x08 -> literal 0x14
                0x4B04, // PC@0x08 -> literal 0x18
                0x4798,
                0x4804, // PC@0x0C -> literal 0x1C
                0x210B,
                0x72C1,
                0xBD10,
                0x46C0
        };
        int o=0;
        for (int v: ins) {
            putU16(code,o,v);
            o += 2;
        }

        putU32(code,0x14,0x080568E1L); // ContinueScriptPlayMapMusic | 1
        putU32(code,0x18,0x081277F5L); // ChooseMonForTradingBoard | 1
        putU32(code,0x1C,0x0203B0A0L); // gPartyMenu

        byte[] evo = TradeEvolutionNativeHelper
                .buildAt(rom, stagingAddress + EVO_ENTRY_OFFSET)
                .codeCopy();
        System.arraycopy(evo,0,code,EVO_ENTRY_OFFSET,evo.length);

        return new NativeHelper(stagingAddress, code);
    }

    static long menuEntry(long stagingAddress) {
        return (stagingAddress + MENU_ENTRY_OFFSET) | 1L;
    }

    static long evolutionEntry(long stagingAddress) {
        return (stagingAddress + EVO_ENTRY_OFFSET) | 1L;
    }

    private static void putU16(byte[] d,int o,int v) {
        d[o]=(byte)v;
        d[o+1]=(byte)(v>>>8);
    }

    private static void putU32(byte[] d,int o,long v) {
        d[o]=(byte)v;
        d[o+1]=(byte)(v>>>8);
        d[o+2]=(byte)(v>>>16);
        d[o+3]=(byte)(v>>>24);
    }
}
