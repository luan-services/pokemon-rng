import java.io.ByteArrayOutputStream;
import java.util.*;

/*
   Probe 3C staged launcher.

   Goal: use BATTLE_TYPE_EREADER_TRAINER only during CB2_InitBattle so
   CreateNPCTrainerParty preserves the six prebuilt enemy mons. Immediately
   after CB2_InitBattle returns, switch to a normal trainer battle and select
   Brock (TRAINER_LEADER_BROCK = 414) as the ROM identity host.

   No ROM data is modified. The helper lives only in staged EWRAM and is no
   longer needed once the battle init wrapper has returned.
*/
final class BrockNormalFlagsSixMonNative {
    private static final long G_ENEMY_PARTY = 0x0202402CL;
    private static final long G_BATTLE_TYPE_FLAGS = 0x02022B4CL;
    private static final long G_TRAINER_BATTLE_OPPONENT_A = 0x020386AEL;
    private static final long S_SPECIAL_VAR_8004_COPY = 0x0203AAB8L;
    private static final long G_MAIN_SAVED_CALLBACK = 0x030030F8L;

    private static final int BATTLE_TYPE_TRAINER = 1 << 3;
    private static final int BATTLE_TYPE_EREADER_TRAINER = 1 << 11;
    private static final int TRAINER_LEADER_BROCK = 414;

    private BrockNormalFlagsSixMonNative() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        Thumb t = new Thumb(stagingAddress);

        // Entry 0: build all six mons, start a stock Battle Tower-style
        // transition, but use our wait task so CB2_InitBattle can be wrapped.
        t.label("entry");
        t.u16(0xB5F0);                    // push {r4-r7,lr}
        t.ldrLit(4, "enemyParty");
        t.adr(5, "mons");
        t.ldrLit(7, "createBattleTowerMon");
        t.u16(0x2606);                    // movs r6,#6
        t.label("mon_loop");
        t.u16(0x1C20);                    // adds r0,r4,#0
        t.u16(0x1C29);                    // adds r1,r5,#0
        t.bl("thunk_r7");
        t.u16(0x3464);                    // adds r4,#0x64
        t.u16(0x352C);                    // adds r5,#0x2C
        t.u16(0x3E01);                    // subs r6,#1
        t.bcond(1, "mon_loop");          // bne

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
        t.ldrLit(1, "finishEReaderBattle");
        t.u16(0x6001);                    // gMain.savedCallback = finish
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
        t.literal("brockId", TRAINER_LEADER_BROCK);
        t.literal("createBattleTowerMon", createBattleTowerMonThumb(rom));
        t.literal("createTask", createTaskThumb(rom));
        t.literal("playBattleBgm", playMapChosenOrBattleBgmThumb(rom));
        t.literal("getTransition", battleTowerTransitionThumb(rom));
        t.literal("startTransition", battleTransitionStartThumb(rom));
        t.literal("isTransitionDone", isBattleTransitionDoneThumb(rom));
        t.literal("finishEReaderBattle", finishEReaderBattleThumb(rom));
        t.literal("cleanupOverworld", cleanupOverworldThumb(rom));
        t.literal("setMainCallback2", setMainCallback2Thumb(rom));
        t.literal("destroyTask", destroyTaskThumb(rom));
        t.literal("cb2InitBattle", cb2InitBattleThumb(rom));

        t.label("mons");
        t.raw(EReaderTrainerData.brockSixProbeMons());

        byte[] code = t.finish();
        return new NativeHelper(stagingAddress, code);
    }

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
    private static long finishEReaderBattleThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x080E6855L;
            case LEAF_GREEN_EN_10 -> 0x080E682DL;
            case FIRE_RED_EN_11 -> 0x080E6869L;
            case LEAF_GREEN_EN_11 -> 0x080E6841L;
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
