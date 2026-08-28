import java.io.ByteArrayOutputStream;
import java.util.*;

/* Shared native shell for custom trainer battles.

   The shell is persisted once in the validated SB2 auxiliary region, then a
   small per-card loader stages it to the same stable EWRAM scratch used by the
   validated trainer probes. Card-specific data is read from
   CustomTrainerBattleDescriptor.

   The e-Reader flag exists only across CB2_InitBattle so the stock initializer
   preserves gEnemyParty. The wrapper then removes it and the actual battle runs
   as BATTLE_TYPE_TRAINER.
*/
final class CustomTrainerBattleRuntimeV1 {
    static final int VERSION = 1;
    private static final long G_ENEMY_PARTY = 0x0202402CL;
    private static final long G_BATTLE_TYPE_FLAGS = 0x02022B4CL;
    private static final long G_TRAINER_BATTLE_OPPONENT_A = 0x020386AEL;
    private static final long S_SPECIAL_VAR_8004_COPY = 0x0203AAB8L;
    private static final long G_MAIN_SAVED_CALLBACK = 0x030030F8L;
    private static final long S_TRAINER_BATTLE_MODE = 0x020386ACL;
    private static final long S_TRAINER_A_DEFEAT_SPEECH = 0x020386B8L;
    private static final long G_BATTLE_OUTCOME = 0x02023E8AL;

    private static final int BATTLE_TYPE_TRAINER = 1 << 3;
    private static final int BATTLE_TYPE_EREADER_TRAINER = 1 << 11;

    private CustomTrainerBattleRuntimeV1() {}

    static long stagingAddress(RomProfile rom) { return rom.stringVar4 + 0x140L; }

    static NativeHelper build(RomProfile rom) {
        long stagingAddress = stagingAddress(rom);
        Thumb t = new Thumb(stagingAddress);

        t.label("entry");
        t.u16(0xB5F0); // push {r4-r7,lr}

        // r4 = live descriptor pointer.
        t.ldrLit(4, "saveBlock2Ptr");
        t.u16(ldrWord(4,4,0));
        t.ldrLit(1, "descriptorOffset");
        t.u16(addReg(4,4,1));

        // Validate descriptor identity/version/party count before touching battle state.
        t.u16(ldrWord(0,4,CustomTrainerBattleDescriptor.OFF_MAGIC));
        t.ldrLit(1, "descriptorMagic");
        t.u16(cmpReg(0,1));
        t.bcond(1, "entry_fail"); // bne
        t.u16(ldrHalf(0,4,CustomTrainerBattleDescriptor.OFF_VERSION));
        t.u16(0x2800 | CustomTrainerBattleDescriptor.VERSION);
        t.bcond(1, "entry_fail");
        t.u16(ldrByte(7,4,CustomTrainerBattleDescriptor.OFF_PARTY_COUNT));
        t.u16(0x2F00); // cmp r7,#0
        t.bcond(0, "entry_fail");
        t.u16(0x2F06); // cmp r7,#6
        t.bcond(8, "entry_fail"); // bhi

        // Snapshot relocation-sensitive descriptor values into this staged image.
        t.u16(ldrHalf(0,4,CustomTrainerBattleDescriptor.OFF_TRAINER_ID));
        t.adr(1, "state_trainer_id"); t.u16(strHalf(0,1,0));
        t.u16(ldrHalf(0,4,CustomTrainerBattleDescriptor.OFF_BGM));
        t.adr(1, "state_bgm"); t.u16(strHalf(0,1,0));
        t.u16(ldrByte(0,4,CustomTrainerBattleDescriptor.OFF_LOCAL_ID));
        t.adr(1, "state_local_id"); t.u16(strByte(0,1,0));
        t.u16(ldrHalf(0,4,CustomTrainerBattleDescriptor.OFF_AFTER_BATTLE));
        t.adr(1, "state_after_offset"); t.u16(strHalf(0,1,0));

        // Copy defeat speech to stable staged EWRAM; SaveBlock2 will relocate in CB2_InitBattle.
        t.u16(movReg(6,4));
        t.u16(0x3620); // adds r6,#DEFEAT_TEXT_OFFSET (0x20)
        t.adr(5, "defeat_buffer");
        t.u16(0x2240); // movs r2,#64
        t.label("copy_defeat");
        t.u16(ldrByte(0,6,0));
        t.u16(strByte(0,5,0));
        t.u16(0x3601); t.u16(0x3501); t.u16(0x3A01);
        t.bcond(1, "copy_defeat");

        // Build 1..6 custom enemy mons directly into gEnemyParty.
        t.ldrLit(3, "zeroEnemyParty");
        t.bl("thunk_r3");
        t.ldrLit(5, "enemyParty");
        t.u16(movReg(6,4));
        t.u16(0x3660); // descriptor + PARTY_OFFSET (0x60)
        t.u16(ldrByte(7,4,CustomTrainerBattleDescriptor.OFF_PARTY_COUNT));
        t.label("mon_loop");
        t.u16(movReg(0,5));
        t.u16(movReg(1,6));
        t.ldrLit(3, "createBattleTowerMon");
        t.bl("thunk_r3");
        t.u16(0x3564); // sizeof(struct Pokemon) = 0x64
        t.u16(0x362C); // sizeof(BattleTowerPokemon) = 0x2C
        t.u16(0x3F01); // subs r7,#1
        t.bcond(1, "mon_loop");

        t.ldrLit(0, "specialVarCopy");
        t.u16(0x2100); t.u16(0x8001); // no eReader farewell
        t.ldrLit(0, "battleFlags");
        t.ldrLit(1, "bootstrapFlags"); t.u16(0x6001);
        t.ldrLit(0, "opponent"); t.u16(0x2100); t.u16(0x8001);

        // Arm a continuation that lives inside this staged shared runtime.
        t.ldrLit(0, "fieldCallback2");
        t.adr(1, "continuation_callback"); t.u16(0x3101); t.u16(0x6001);

        t.adr(0, "wait_task"); t.u16(0x3001); t.u16(0x2101);
        t.ldrLit(3, "createTask"); t.bl("thunk_r3");

        t.adr(0, "state_bgm"); t.u16(ldrHalf(0,0,0));
        t.ldrLit(3, "playBattleBgm"); t.bl("thunk_r3");
        t.ldrLit(3, "getTransition"); t.bl("thunk_r3");
        t.ldrLit(3, "startTransition"); t.bl("thunk_r3");

        t.ldrLit(0, "specialVarResult"); t.u16(0x2101); t.u16(0x8001);
        t.u16(0xBDF0);

        t.label("entry_fail");
        t.ldrLit(0, "specialVarResult"); t.u16(0x2100); t.u16(0x8001);
        t.u16(0xBDF0);

        // Clone of stock Task_WaitBT, but schedules our init wrapper.
        t.align4();
        t.label("wait_task");
        t.u16(0xB510); t.u16(0x1C04);
        t.ldrLit(3, "isTransitionDone"); t.bl("thunk_r3");
        t.u16(0x2801); t.bcond(1, "wait_return");
        t.ldrLit(0, "savedCallback");
        t.adr(1, "custom_end_callback"); t.u16(0x3101); t.u16(0x6001);
        t.ldrLit(3, "cleanupOverworld"); t.bl("thunk_r3");
        t.adr(0, "init_wrapper"); t.u16(0x3001);
        t.ldrLit(3, "setMainCallback2"); t.bl("thunk_r3");
        t.u16(0x1C20); t.ldrLit(3, "destroyTask"); t.bl("thunk_r3");
        t.label("wait_return"); t.u16(0xBD10);

        // Let CB2_InitBattle see EREADER, then immediately become a normal trainer battle.
        t.align4();
        t.label("init_wrapper");
        t.u16(0xB500); t.ldrLit(3, "cb2InitBattle"); t.bl("thunk_r3");
        t.ldrLit(0, "battleFlags"); t.u16(0x2108); t.u16(0x6001);
        t.ldrLit(0, "opponent"); t.adr(1, "state_trainer_id"); t.u16(ldrHalf(1,1,0)); t.u16(0x8001);
        t.ldrLit(0, "trainerBattleMode"); t.u16(0x2100); t.u16(0x8001);
        t.ldrLit(0, "trainerDefeatSpeech"); t.adr(1, "defeat_buffer"); t.u16(0x6001);
        t.u16(0xBD00);

        // Victory returns to this card's RamScript. Loss clears our callback and uses stock whiteout.
        t.align4();
        t.label("custom_end_callback");
        t.u16(0xB500); t.ldrLit(0, "battleOutcome"); t.u16(0x7800); t.u16(0x2801);
        t.bcond(0, "end_win");
        t.ldrLit(0, "fieldCallback2"); t.u16(0x2100); t.u16(0x6001);
        t.ldrLit(0, "cb2WhiteOut"); t.ldrLit(3, "setMainCallback2"); t.bl("thunk_r3"); t.u16(0xBD00);
        t.label("end_win");
        t.ldrLit(0, "returnToFieldContinue"); t.ldrLit(3, "setMainCallback2"); t.bl("thunk_r3"); t.u16(0xBD00);

        // Relocation-safe object-bound continuation. localId/offset are runtime state, not hardcoded bytes.
        t.align4();
        t.label("continuation_callback");
        t.u16(0xB500);
        t.adr(2, "state_local_id"); t.u16(ldrByte(0,2,0));
        t.u16(0x2100);
        t.ldrLit(3, "getRamScript"); t.bl("thunk_r3");
        t.u16(0x2800); t.bcond(0, "continuation_done");
        t.adr(2, "state_after_offset"); t.u16(ldrHalf(1,2,0)); t.u16(addReg(0,0,1));
        t.ldrLit(3, "setupScript"); t.bl("thunk_r3");
        t.label("continuation_done"); t.u16(0x2001); t.u16(0xBD00);

        t.align4();
        t.label("thunk_r3"); t.u16(0x4718);

        t.align4();
        t.label("state_trainer_id"); t.u16(0); t.u16(0);
        t.label("state_bgm"); t.u16(0); t.u16(0);
        t.label("state_after_offset"); t.u16(0); t.u16(0);
        t.label("state_local_id"); t.raw(new byte[]{0,0,0,0});
        t.label("defeat_buffer"); t.raw(new byte[CustomTrainerBattleDescriptor.DEFEAT_TEXT_CAPACITY]);
        t.align4();

        t.literal("saveBlock2Ptr", rom.saveBlock2Ptr);
        t.literal("descriptorOffset", CustomTrainerRuntimeStorage.DESCRIPTOR_OFFSET);
        t.literal("descriptorMagic", Integer.toUnsignedLong(CustomTrainerBattleDescriptor.MAGIC));
        t.literal("enemyParty", G_ENEMY_PARTY);
        t.literal("specialVarCopy", S_SPECIAL_VAR_8004_COPY);
        t.literal("battleFlags", G_BATTLE_TYPE_FLAGS);
        t.literal("bootstrapFlags", BATTLE_TYPE_TRAINER | BATTLE_TYPE_EREADER_TRAINER);
        t.literal("opponent", G_TRAINER_BATTLE_OPPONENT_A);
        t.literal("savedCallback", G_MAIN_SAVED_CALLBACK);
        t.literal("trainerBattleMode", S_TRAINER_BATTLE_MODE);
        t.literal("trainerDefeatSpeech", S_TRAINER_A_DEFEAT_SPEECH);
        t.literal("zeroEnemyParty", zeroEnemyPartyMonsThumb(rom));
        t.literal("createBattleTowerMon", createBattleTowerMonThumb(rom));
        t.literal("createTask", createTaskThumb(rom));
        t.literal("playBattleBgm", playMapChosenOrBattleBgmThumb(rom));
        t.literal("getTransition", battleTowerTransitionThumb(rom));
        t.literal("startTransition", battleTransitionStartThumb(rom));
        t.literal("isTransitionDone", isBattleTransitionDoneThumb(rom));
        t.literal("battleOutcome", G_BATTLE_OUTCOME);
        t.literal("fieldCallback2", rom.fieldCallback2);
        t.literal("cb2WhiteOut", cb2WhiteOutThumb(rom));
        t.literal("returnToFieldContinue", returnToFieldContinueThumb(rom));
        t.literal("cleanupOverworld", cleanupOverworldThumb(rom));
        t.literal("setMainCallback2", setMainCallback2Thumb(rom));
        t.literal("destroyTask", destroyTaskThumb(rom));
        t.literal("cb2InitBattle", cb2InitBattleThumb(rom));
        t.literal("getRamScript", rom.getSavedRamScriptThumb - 0xBCL);
        t.literal("setupScript", rom.scriptContextSetupThumb);
        t.literal("specialVarResult", rom.specialVarResult);

        byte[] code = t.finish();
        if (code.length > CustomTrainerRuntimeStorage.RUNTIME_CAPACITY)
            throw new IllegalStateException("CustomTrainerBattleRuntimeV1 too large: " + code.length + "/" + CustomTrainerRuntimeStorage.RUNTIME_CAPACITY);
        return new NativeHelper(stagingAddress, code);
    }

    private static long zeroEnemyPartyMonsThumb(RomProfile rom) { return rev(rom) ? 0x0803DA49L : 0x0803DA35L; }
    private static long cb2WhiteOutThumb(RomProfile rom) { return rev(rom) ? 0x080566B9L : 0x080566A5L; }
    private static long returnToFieldContinueThumb(RomProfile rom) { return rev(rom) ? 0x080568F5L : 0x080568E1L; }
    private static long createBattleTowerMonThumb(RomProfile rom) { return rev(rom) ? 0x0803E0B9L : 0x0803E0A5L; }
    private static long createTaskThumb(RomProfile rom) { return rev(rom) ? 0x08077431L : 0x0807741DL; }
    private static long destroyTaskThumb(RomProfile rom) { return rev(rom) ? 0x0807751DL : 0x08077509L; }
    private static long playMapChosenOrBattleBgmThumb(RomProfile rom) { return rev(rom) ? 0x080440A5L : 0x08044091L; }
    private static long cleanupOverworldThumb(RomProfile rom) { return rev(rom) ? 0x08056405L : 0x080563F1L; }
    private static long setMainCallback2Thumb(RomProfile rom) { return rev(rom) ? 0x08000559L : 0x08000545L; }
    private static long cb2InitBattleThumb(RomProfile rom) { return rev(rom) ? 0x0800FDB1L : 0x0800FD9DL; }
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
    private static boolean rev(RomProfile rom) { return rom == RomProfile.FIRE_RED_EN_11 || rom == RomProfile.LEAF_GREEN_EN_11; }

    private static int movReg(int rd,int rs){ return 0x1C00 | (rs<<3) | rd; }
    private static int addReg(int rd,int rn,int rm){ return 0x1800 | (rm<<6) | (rn<<3) | rd; }
    private static int cmpReg(int rn,int rm){ return 0x4280 | (rm<<3) | rn; }
    private static int ldrWord(int rt,int rn,int byteOff){ if((byteOff&3)!=0||byteOff/4>31)throw new IllegalArgumentException(); return 0x6800 | ((byteOff/4)<<6) | (rn<<3) | rt; }
    private static int ldrHalf(int rt,int rn,int byteOff){ if((byteOff&1)!=0||byteOff/2>31)throw new IllegalArgumentException(); return 0x8800 | ((byteOff/2)<<6) | (rn<<3) | rt; }
    private static int strHalf(int rt,int rn,int byteOff){ if((byteOff&1)!=0||byteOff/2>31)throw new IllegalArgumentException(); return 0x8000 | ((byteOff/2)<<6) | (rn<<3) | rt; }
    private static int ldrByte(int rt,int rn,int byteOff){ if(byteOff<0||byteOff>31)throw new IllegalArgumentException(); return 0x7800 | (byteOff<<6) | (rn<<3) | rt; }
    private static int strByte(int rt,int rn,int byteOff){ if(byteOff<0||byteOff>31)throw new IllegalArgumentException(); return 0x7000 | (byteOff<<6) | (rn<<3) | rt; }

    private static final class Thumb {
        private final long baseAddress;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final Map<String,Integer> labels = new HashMap<>();
        private final List<BranchFix> branches = new ArrayList<>();
        private final List<BlFix> bls = new ArrayList<>();
        private final List<LiteralFix> literalLoads = new ArrayList<>();
        private final List<AdrFix> adrs = new ArrayList<>();
        private final Map<String,Long> literals = new LinkedHashMap<>();
        Thumb(long baseAddress){this.baseAddress=baseAddress;}
        int pos(){return out.size();}
        void u16(int v){out.write(v&0xFF);out.write((v>>>8)&0xFF);}
        void raw(byte[]b){out.writeBytes(b);} void label(String s){if(labels.put(s,pos())!=null)throw new IllegalStateException("duplicate label "+s);} void align4(){while((pos()&3)!=0)u16(0x46C0);}
        void bcond(int cond,String label){int p=pos();u16(0xD000|(cond<<8));branches.add(new BranchFix(p,label));}
        void bl(String label){int p=pos();u16(0);u16(0);bls.add(new BlFix(p,label));}
        void ldrLit(int rt,String name){int p=pos();u16(0x4800|(rt<<8));literalLoads.add(new LiteralFix(p,rt,name));}
        void adr(int rd,String label){int p=pos();u16(0xA000|(rd<<8));adrs.add(new AdrFix(p,rd,label));}
        void literal(String name,long value){literals.put(name,value);}
        byte[] finish(){align4();Map<String,Integer> lp=new HashMap<>();for(var e:literals.entrySet()){lp.put(e.getKey(),pos());long v=e.getValue();out.write((int)v&255);out.write((int)(v>>>8)&255);out.write((int)(v>>>16)&255);out.write((int)(v>>>24)&255);}byte[]b=out.toByteArray();for(var f:branches){int target=req(f.label),d=target-(f.pos+4),hw=d/2;if((d&1)!=0||hw< -128||hw>127)throw new IllegalStateException("branch range "+f.label+" d="+d);int ins=(b[f.pos]&255)|((b[f.pos+1]&255)<<8);put16(b,f.pos,(ins&0xFF00)|(hw&255));}for(var f:bls){int target=req(f.label),d=target-(f.pos+4);if((d&1)!=0||d<-(1<<22)||d>=(1<<22))throw new IllegalStateException("BL range "+f.label);put16(b,f.pos,0xF000|((d>>12)&0x7FF));put16(b,f.pos+2,0xF800|((d>>1)&0x7FF));}for(var f:literalLoads){Integer p=lp.get(f.name);if(p==null)throw new IllegalStateException("missing literal "+f.name);int pc=(f.pos+4)&~3,d=p-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("literal range "+f.name+" d="+d);put16(b,f.pos,0x4800|(f.rt<<8)|(d/4));}for(var f:adrs){int target=req(f.label),pc=(f.pos+4)&~3,d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("ADR range "+f.label+" d="+d);put16(b,f.pos,0xA000|(f.rd<<8)|(d/4));}return b;}
        private int req(String n){Integer v=labels.get(n);if(v==null)throw new IllegalStateException("missing label "+n);return v;} private static void put16(byte[]b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);} record BranchFix(int pos,String label){} record BlFix(int pos,String label){} record LiteralFix(int pos,int rt,String name){} record AdrFix(int pos,int rd,String label){}
    }
}
