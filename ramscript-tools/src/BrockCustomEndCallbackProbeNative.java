import java.io.ByteArrayOutputStream;
import java.util.*;

/*
   Probe 3E staged launcher.

   Goal: use BATTLE_TYPE_EREADER_TRAINER only during CB2_InitBattle so
   CreateNPCTrainerParty preserves the prebuilt enemy party. Immediately
   after CB2_InitBattle returns, switch to a normal trainer battle and select
   Brock (TRAINER_LEADER_BROCK = 414) as the ROM identity host.

   Probe 3E deliberately uses only one enemy mon. Six custom enemy slots were
   already validated in Probe 3B/3C; reducing the party isolates end-flow work.

   No ROM data is modified. The helper lives only in staged EWRAM and is no
   longer needed once the battle init wrapper has returned.
*/
final class BrockCustomEndCallbackProbeNative {
    private static final long G_ENEMY_PARTY = 0x0202402CL;
    private static final long G_BATTLE_TYPE_FLAGS = 0x02022B4CL;
    private static final long G_TRAINER_BATTLE_OPPONENT_A = 0x020386AEL;
    private static final long S_SPECIAL_VAR_8004_COPY = 0x0203AAB8L;
    private static final long G_MAIN_SAVED_CALLBACK = 0x030030F8L;
    private static final long S_TRAINER_BATTLE_MODE = 0x020386ACL;
    private static final long S_TRAINER_A_DEFEAT_SPEECH = 0x020386B8L;

    private static final int BATTLE_TYPE_TRAINER = 1 << 3;
    private static final int BATTLE_TYPE_EREADER_TRAINER = 1 << 11;
    private static final int TRAINER_LEADER_BROCK = 414;

    private BrockCustomEndCallbackProbeNative() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        Thumb t = new Thumb(stagingAddress);

        // Entry 0: build one mon, start a stock Battle Tower-style
        // transition, but use our wait task so CB2_InitBattle can be wrapped.
        t.label("entry");
        t.u16(0xB5F0);                    // push {r4-r7,lr}
        // Start from a clean six-slot enemy party, then materialize only one
        // mon. Six custom slots were already proven by Probe 3B/3C; one slot
        // keeps this end-callback probe small and isolated.
        t.ldrLit(3, "zeroEnemyParty");
        t.bl("thunk_r3");
        t.ldrLit(0, "enemyParty");
        t.adr(1, "mons");
        t.ldrLit(3, "createBattleTowerMon");
        t.bl("thunk_r3");

        t.ldrLit(0, "specialVarCopy");
        t.u16(0x2100);                    // movs r1,#0 (no eReader farewell)
        t.u16(0x8001);                    // strh r1,[r0]

        t.ldrLit(0, "battleFlags");
        t.ldrLit(1, "bootstrapFlags");
        t.u16(0x6001);                    // str r1,[r0]
        t.ldrLit(0, "opponent");
        t.u16(0x2100);
        t.u16(0x8001);                    // opponent=0 during CB2_InitBattle

        t.adr(0, "wait_task");            // r0 = custom TaskFunc
        t.u16(0x3001);                    // Thumb bit
        t.u16(0x2101);                    // priority 1
        t.ldrLit(3, "createTask");
        t.bl("thunk_r3");

        t.u16(0x2000);                    // PlayMapChosenOrBattleBGM(0)
        t.ldrLit(3, "playBattleBgm");
        t.bl("thunk_r3");

        t.ldrLit(3, "getTransition");
        t.bl("thunk_r3");                // r0 = transition
        t.ldrLit(3, "startTransition");
        t.bl("thunk_r3");
        t.u16(0xBDF0);                    // pop {r4-r7,pc}

        // Custom clone of Task_WaitBT. The only semantic difference from
        // stock is that it schedules our CB2_InitBattle wrapper.
        t.align4();
        t.label("wait_task");
        t.u16(0xB510);                    // push {r4,lr}
        t.u16(0x1C04);                    // adds r4,r0,#0 (taskId)
        t.ldrLit(3, "isTransitionDone");
        t.bl("thunk_r3");
        t.u16(0x2801);                    // cmp r0,#1
        t.bcond(1, "wait_return");

        t.ldrLit(0, "savedCallback");
        t.adr(1, "custom_end_callback");
        t.u16(0x3101);                    // Thumb bit
        t.u16(0x6001);                    // gMain.savedCallback = custom finish
        t.ldrLit(3, "cleanupOverworld");
        t.bl("thunk_r3");
        t.adr(0, "init_wrapper");
        t.u16(0x3001);                    // Thumb bit
        t.ldrLit(3, "setMainCallback2");
        t.bl("thunk_r3");
        t.u16(0x1C20);                    // r0 = taskId
        t.ldrLit(3, "destroyTask");
        t.bl("thunk_r3");
        t.label("wait_return");
        t.u16(0xBD10);                    // pop {r4,pc}

        // Called once after the transition. CB2_InitBattle sees the eReader bit
        // and therefore preserves gEnemyParty. Once it returns, switch to a
        // completely normal trainer type and Brock as the ROM identity host.
        t.align4();
        t.label("init_wrapper");
        t.u16(0xB500);                    // push {lr}
        t.ldrLit(3, "cb2InitBattle");
        t.bl("thunk_r3");
        t.ldrLit(0, "battleFlags");
        t.u16(0x2108);                    // BATTLE_TYPE_TRAINER only
        t.u16(0x6001);                    // str r1,[r0]
        t.ldrLit(0, "opponent");
        t.ldrLit(1, "brockId");
        t.u16(0x8001);                    // strh r1,[r0]

        // Configure the stock ordinary-trainer end flow enough for this probe.
        t.ldrLit(0, "trainerBattleMode");
        t.u16(0x2100);                    // ordinary trainer mode
        t.u16(0x8001);                    // strh r1,[r0]
        t.ldrLit(0, "trainerDefeatSpeech");
        t.ldrLit(1, "brockDefeatText");
        t.u16(0x6001);                    // sTrainerADefeatSpeech = Brock ROM text
        t.u16(0xBD00);                    // pop {pc}

        // Custom battle end callback. Victory keeps the relocation-safe
        // gFieldCallback2 bridge and returns to the RamScript. Any non-win
        // outcome clears gFieldCallback2 first, allowing CB2_WhiteOut to
        // install/run FieldCB_RushInjuredPokemonToCenter normally.
        t.align4();
        t.label("custom_end_callback");
        t.u16(0xB500);                    // push {lr}
        t.ldrLit(0, "battleOutcome");
        t.u16(0x7800);                    // ldrb r0,[r0]
        t.u16(0x2801);                    // cmp r0,#B_OUTCOME_WON (1)
        t.bcond(0, "end_win");            // beq

        t.ldrLit(0, "fieldCallback2");
        t.u16(0x2100);                    // movs r1,#0
        t.u16(0x6001);                    // gFieldCallback2 = NULL
        t.ldrLit(0, "cb2WhiteOut");
        t.ldrLit(3, "setMainCallback2");
        t.bl("thunk_r3");
        t.u16(0xBD00);                    // pop {pc}

        t.label("end_win");
        t.ldrLit(0, "returnToFieldContinue");
        t.ldrLit(3, "setMainCallback2");
        t.bl("thunk_r3");
        t.u16(0xBD00);                    // pop {pc}

        // ARMv4T-safe register-call thunks.
        t.align4();
        t.label("thunk_r3"); t.u16(0x4718); // bx r3
        t.label("thunk_r7"); t.u16(0x4738); // bx r7
        t.align4();

        t.literal("enemyParty", G_ENEMY_PARTY);
        t.literal("specialVarCopy", S_SPECIAL_VAR_8004_COPY);
        t.literal("battleFlags", G_BATTLE_TYPE_FLAGS);
        t.literal("bootstrapFlags", BATTLE_TYPE_TRAINER | BATTLE_TYPE_EREADER_TRAINER);
        t.literal("opponent", G_TRAINER_BATTLE_OPPONENT_A);
        t.literal("savedCallback", G_MAIN_SAVED_CALLBACK);
        t.literal("trainerBattleMode", S_TRAINER_BATTLE_MODE);
        t.literal("trainerDefeatSpeech", S_TRAINER_A_DEFEAT_SPEECH);
        t.literal("brockDefeatText", brockDefeatText(rom));
        t.literal("brockId", TRAINER_LEADER_BROCK);
        t.literal("zeroEnemyParty", zeroEnemyPartyMonsThumb(rom));
        t.literal("createBattleTowerMon", createBattleTowerMonThumb(rom));
        t.literal("createTask", createTaskThumb(rom));
        t.literal("playBattleBgm", playMapChosenOrBattleBgmThumb(rom));
        t.literal("getTransition", battleTowerTransitionThumb(rom));
        t.literal("startTransition", battleTransitionStartThumb(rom));
        t.literal("isTransitionDone", isBattleTransitionDoneThumb(rom));
        t.literal("battleOutcome", 0x02023E8AL);
        t.literal("fieldCallback2", 0x03005024L);
        t.literal("cb2WhiteOut", cb2WhiteOutThumb(rom));
        t.literal("returnToFieldContinue", returnToFieldContinueThumb(rom));
        t.literal("cleanupOverworld", cleanupOverworldThumb(rom));
        t.literal("setMainCallback2", setMainCallback2Thumb(rom));
        t.literal("destroyTask", destroyTaskThumb(rom));
        t.literal("cb2InitBattle", cb2InitBattleThumb(rom));

        t.label("mons");
        t.raw(EReaderTrainerData.brockOneProbeMon());

        byte[] code = t.finish();
        return new NativeHelper(stagingAddress, code);
    }

    private static long zeroEnemyPartyMonsThumb(RomProfile rom) { return rev(rom) ? 0x0803DA49L : 0x0803DA35L; }
    private static long cb2WhiteOutThumb(RomProfile rom) { return rev(rom) ? 0x080566B9L : 0x080566A5L; }
    private static long returnToFieldContinueThumb(RomProfile rom) { return rev(rom) ? 0x080568F5L : 0x080568E1L; }

    private static long createBattleTowerMonThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10, LEAF_GREEN_EN_10 -> 0x0803E0A5L;
            case FIRE_RED_EN_11, LEAF_GREEN_EN_11 -> 0x0803E0B9L;
        };
    }
    private static long createTaskThumb(RomProfile rom) { return rev(rom) ? 0x08077431L : 0x0807741DL; }
    private static long destroyTaskThumb(RomProfile rom) { return rev(rom) ? 0x0807751DL : 0x08077509L; }
    private static long playMapChosenOrBattleBgmThumb(RomProfile rom) { return rev(rom) ? 0x080440A5L : 0x08044091L; }
    private static long cleanupOverworldThumb(RomProfile rom) { return rev(rom) ? 0x08056405L : 0x080563F1L; }
    private static long setMainCallback2Thumb(RomProfile rom) { return rev(rom) ? 0x08000559L : 0x08000545L; }
    private static long cb2InitBattleThumb(RomProfile rom) { return rev(rom) ? 0x0800FDB1L : 0x0800FD9DL; }
    private static long endTrainerBattleThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x080804ADL;
            case LEAF_GREEN_EN_10 -> 0x08080481L;
            case FIRE_RED_EN_11 -> 0x080804C1L;
            case LEAF_GREEN_EN_11 -> 0x08080495L;
        };
    }
    private static long brockDefeatText(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x08190E4FL;
            case LEAF_GREEN_EN_10 -> 0x08190E2BL;
            case FIRE_RED_EN_11 -> 0x08190EC7L;
            case LEAF_GREEN_EN_11 -> 0x08190EA3L;
        };
    }
    private static long battleTowerTransitionThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x08080061L;
            case LEAF_GREEN_EN_10 -> 0x08080035L;
            case FIRE_RED_EN_11 -> 0x08080075L;
            case LEAF_GREEN_EN_11 -> 0x08080049L;
        };
    }
    private static long battleTransitionStartThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x080D08B9L;
            case LEAF_GREEN_EN_10 -> 0x080D088DL;
            case FIRE_RED_EN_11 -> 0x080D08CDL;
            case LEAF_GREEN_EN_11 -> 0x080D08A1L;
        };
    }
    private static long isBattleTransitionDoneThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x080D08F9L;
            case LEAF_GREEN_EN_10 -> 0x080D08CDL;
            case FIRE_RED_EN_11 -> 0x080D090DL;
            case LEAF_GREEN_EN_11 -> 0x080D08E1L;
        };
    }
    private static boolean rev(RomProfile rom) {
        return rom == RomProfile.FIRE_RED_EN_11 || rom == RomProfile.LEAF_GREEN_EN_11;
    }

    private static final class Thumb {
        private final long baseAddress;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final Map<String,Integer> labels = new HashMap<>();
        private final List<BranchFix> branches = new ArrayList<>();
        private final List<BlFix> bls = new ArrayList<>();
        private final List<LiteralFix> literalLoads = new ArrayList<>();
        private final List<AdrFix> adrs = new ArrayList<>();
        private final Map<String,Long> literals = new LinkedHashMap<>();

        Thumb(long baseAddress) { this.baseAddress = baseAddress; }
        int pos(){ return out.size(); }
        void u16(int v){ out.write(v & 0xFF); out.write((v >>> 8) & 0xFF); }
        void raw(byte[] b){ out.writeBytes(b); }
        void label(String s){ if(labels.put(s,pos()) != null) throw new IllegalStateException("duplicate label "+s); }
        void align4(){ while((pos() & 3) != 0) u16(0x46C0); }
        void bcond(int cond,String label){ int p=pos(); u16(0xD000 | (cond << 8)); branches.add(new BranchFix(p,label)); }
        void bl(String label){ int p=pos(); u16(0);u16(0); bls.add(new BlFix(p,label)); }
        void ldrLit(int rt,String name){ int p=pos();u16(0x4800 | (rt << 8));literalLoads.add(new LiteralFix(p,rt,name)); }
        void adr(int rd,String label){ int p=pos();u16(0xA000 | (rd << 8));adrs.add(new AdrFix(p,rd,label)); }
        void literal(String name,long value){ literals.put(name,value); }

        byte[] finish(){
            align4();
            Map<String,Integer> literalPos = new HashMap<>();
            for (var e : literals.entrySet()) {
                literalPos.put(e.getKey(), pos());
                long v=e.getValue();
                out.write((int)v&0xFF);out.write((int)(v>>>8)&0xFF);out.write((int)(v>>>16)&0xFF);out.write((int)(v>>>24)&0xFF);
            }
            byte[] b=out.toByteArray();
            for (BranchFix f: branches) {
                int target=reqLabel(f.label); int delta=target-(f.pos+4); int hw=delta/2;
                if((delta&1)!=0||hw< -128||hw>127) throw new IllegalStateException("branch range "+f.label);
                int insn=(b[f.pos]&0xFF)|((b[f.pos+1]&0xFF)<<8);
                put16(b,f.pos,(insn&0xFF00)|(hw&0xFF));
            }
            for (BlFix f: bls) {
                int target=reqLabel(f.label); int delta=target-(f.pos+4);
                if((delta&1)!=0||delta<-(1<<22)||delta>=(1<<22)) throw new IllegalStateException("BL range "+f.label);
                put16(b,f.pos,0xF000|((delta>>12)&0x7FF));
                put16(b,f.pos+2,0xF800|((delta>>1)&0x7FF));
            }
            for (LiteralFix f: literalLoads) {
                Integer lp=literalPos.get(f.name); if(lp==null) throw new IllegalStateException("missing literal "+f.name);
                int pc=(f.pos+4)&~3; int d=lp-pc;
                if(d<0||(d&3)!=0||d/4>255) throw new IllegalStateException("literal range "+f.name+" d="+d);
                put16(b,f.pos,0x4800|(f.rt<<8)|(d/4));
            }
            for (AdrFix f: adrs) {
                int target=reqLabel(f.label); int pc=(f.pos+4)&~3; int d=target-pc;
                if(d<0||(d&3)!=0||d/4>255) throw new IllegalStateException("ADR range "+f.label+" d="+d);
                put16(b,f.pos,0xA000|(f.rd<<8)|(d/4));
            }
            return b;
        }
        private int reqLabel(String n){ Integer v=labels.get(n); if(v==null) throw new IllegalStateException("missing label "+n); return v; }
        private static void put16(byte[]b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);}
        record BranchFix(int pos,String label){}
        record BlFix(int pos,String label){}
        record LiteralFix(int pos,int rt,String name){}
        record AdrFix(int pos,int rd,String label){}
    }
}
