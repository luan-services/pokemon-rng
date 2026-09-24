import java.io.ByteArrayOutputStream;
import java.util.*;

/* Probe 21: Probe-19 loss-safe launcher plus preserved MYSTRY reward.
   The key difference is the end callback: Balls are restored for every outcome;
   on player defeat gFieldCallback2 is cleared before stock CB2_WhiteOut. */
final class MewBossBattleTransactionNative {
    static final int BALL_POCKET_OFFSET=0x0430, BALL_SLOT_COUNT=13;
    static final int BACKUP_OFFSET=0x0EC0, BACKUP_BYTES=BALL_SLOT_COUNT*4;
    static final int MAGIC_OFFSET=BACKUP_OFFSET+BACKUP_BYTES;
    static final long MAGIC=0x314C4142L;
    private static final int KEY_OFFSET=0x0F20;
    private static final long G_ENEMY_PARTY=0x0202402CL;
    private static final long G_BATTLE_TYPE_FLAGS=0x02022B4CL;
    private static final long G_MAIN_SAVED_CALLBACK=0x030030F8L;
    private static final long G_BATTLE_OUTCOME=0x02023E8AL;
    private static final long G_FIELD_CALLBACK2=0x03005024L;
    private static final int BATTLE_TYPE_WILD_SCRIPTED=1<<17;
    private static final int[] MOVES={94,105,85,58};
    private MewBossBattleTransactionNative(){}

    record Layout(NativeHelper helper,int restoreOffset,int launchOffset,int rewardOffset){}

    static Layout buildAt(RomProfile rom,long addr){
        if(rom!=RomProfile.FIRE_RED_EN_10) throw new IllegalArgumentException("Probe 21 is FR1.0-only");
        Thumb t=new Thumb();
        // Entry 0: Probe-19 validated Ball backup/clear. Boss move override is omitted for space.
        t.u16(0xB5F0);
        t.ldr(0,"sb1");t.u16(0x6804);t.ldr(0,"pocket");t.u16(0x1824);
        t.ldr(0,"sb2");t.u16(0x6805);t.ldr(0,"backup");t.u16(0x182D);
        t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"key");t.u16(0x1840);t.u16(0x8806);
        t.u16(0x270D);t.label("backup_loop");
        t.u16(0x8820);t.u16(0x8028); t.u16(0x8860);t.u16(0x4070);t.u16(0x8068);
        t.u16(0x2000);t.u16(0x8020);t.u16(0x8066);
        t.u16(0x3404);t.u16(0x3504);t.u16(0x3F01);t.bc(1,"backup_loop");
        t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"magicOff");t.u16(0x1840);t.ldr(1,"magic");t.u16(0x6001);
        t.u16(0xBDF0);

        t.align(); int restore=t.p(); t.label("restore_entry");
        t.u16(0xB5F0);
        t.ldr(0,"sb2");t.u16(0x6805); t.ldr(1,"magicOff");t.u16(0x1868);t.u16(0x6801);t.ldr(2,"magic");t.u16(0x4291);t.bc(1,"restore_done");
        t.ldr(1,"key");t.u16(0x1868);t.u16(0x8806); t.ldr(1,"backup");t.u16(0x186D);
        t.ldr(0,"sb1");t.u16(0x6804);t.ldr(1,"pocket");t.u16(0x1864);
        t.u16(0x270D);t.label("restore_loop");
        t.u16(0x8828);t.u16(0x8020); t.u16(0x8868);t.u16(0x4070);t.u16(0x8060);
        t.u16(0x3404);t.u16(0x3504);t.u16(0x3F01);t.bc(1,"restore_loop");
        t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"magicOff");t.u16(0x1840);t.u16(0x2100);t.u16(0x6001);
        t.label("restore_done");t.u16(0xBDF0);

        // Launch entry. Equivalent battle type to StartScriptedWildBattle, but
        // the transition task installs our loss-safe savedCallback.
        t.align(); int launch=t.p(); t.label("launch_entry");
        t.u16(0xB510); // push r4,lr
        t.ldr(0,"battleFlags");t.ldr(1,"wildScripted");t.u16(0x6001);
        t.adr(0,"wait_task");t.u16(0x3001);t.u16(0x2101);t.ldr(3,"createTask");t.bl("thunk_r3");
        t.u16(0x2000);t.ldr(3,"playBgm");t.bl("thunk_r3");
        t.u16(0x2000);t.ldr(3,"startTransition");t.bl("thunk_r3");
        t.u16(0xBD10);

        t.align();t.label("wait_task");
        t.u16(0xB510);t.u16(0x1C04);t.ldr(3,"transitionDone");t.bl("thunk_r3");t.u16(0x2801);t.bc(1,"wait_return");
        t.ldr(0,"savedCallback");t.adr(1,"end_callback");t.u16(0x3101);t.u16(0x6001);
        t.ldr(3,"cleanup");t.bl("thunk_r3");t.ldr(0,"cb2InitBattle");t.ldr(3,"setMainCallback2");t.bl("thunk_r3");
        t.u16(0x1C20);t.ldr(3,"destroyTask");t.bl("thunk_r3");
        t.label("wait_return");t.u16(0xBD10);

        // Runs after battle resources are torn down. Restore Balls first. If the
        // player lost, remove our relocation bridge before the stock whiteout.
        t.align();t.label("end_callback");t.u16(0xB500);t.bl("restore_entry");
        t.ldr(0,"battleOutcome");t.u16(0x7800);t.u16(0x2802);t.bc(0,"lost");
        t.ldr(0,"returnToFieldContinue");t.ldr(3,"setMainCallback2");t.bl("thunk_r3");t.u16(0xBD00);
        t.label("lost");t.ldr(0,"fieldCallback2");t.u16(0x2100);t.u16(0x6001);
        t.ldr(0,"cb2WhiteOut");t.ldr(3,"setMainCallback2");t.bl("thunk_r3");t.u16(0xBD00);

        t.align();t.label("thunk_r3");t.u16(0x4718);t.align();
        t.lit("sb1",rom.saveBlock1Ptr);t.lit("sb2",rom.saveBlock2Ptr);t.lit("pocket",BALL_POCKET_OFFSET);t.lit("backup",BACKUP_OFFSET);t.lit("magicOff",MAGIC_OFFSET);t.lit("key",KEY_OFFSET);t.lit("magic",MAGIC);
        t.lit("battleFlags",G_BATTLE_TYPE_FLAGS);t.lit("wildScripted",BATTLE_TYPE_WILD_SCRIPTED);t.lit("savedCallback",G_MAIN_SAVED_CALLBACK);
        t.lit("createTask",0x0807741DL);t.lit("destroyTask",0x08077509L);t.lit("playBgm",0x08044091L);t.lit("startTransition",0x080D08B9L);t.lit("transitionDone",0x080D08F9L);
        t.lit("cleanup",0x080563F1L);t.lit("setMainCallback2",0x08000545L);t.lit("cb2InitBattle",0x0800FD9DL);
        t.lit("battleOutcome",G_BATTLE_OUTCOME);t.lit("fieldCallback2",G_FIELD_CALLBACK2);t.lit("cb2WhiteOut",0x080566A5L);t.lit("returnToFieldContinue",0x080568E1L);
        // Probe 23: after the stock givemon has safely selected a PC slot, replace
        // that temporary gift with the exact encrypted/shuffled MYSTRY #001
        // BoxPokemon bytes. This first preservation probe intentionally targets
        // MON_GIVEN_TO_PC only; party insertion is a later branch.
        t.align(); int reward=t.p(); t.label("reward_entry");
        t.u16(0xB500);
        // PC-only diagnostic: caller guarantees a full party, so givemon has
        // returned MON_GIVEN_TO_PC and populated these two globals.
        t.ldr(0,"monBoxId"); t.u16(0x8800);
        t.ldr(1,"monBoxPos"); t.u16(0x8809);
        t.adr(2,"reward_data");
        t.ldr(3,"setBoxMonAt"); t.bl("thunk_r3");
        t.u16(0xBD00);
        t.align(); t.label("reward_data");
        // Exact in-memory BoxPokemon representation of preserved MYSTRY Mew #001.
        t.raw(Thumb.hex("aea47dd1121b0000c7bfd1ff0870013401310202c7d3cdceccd3ff00dd9c0000bdbfedd1bcbf7dd19fb57dd12bbf7dd18cbd7dd1bcdb7dd1bcbf7dd1bcbf7dd1bcbf7dd1bc4077701e69cbe5bcbf7d51"));
        t.lit("monBoxId",rom.specialVarResult + 6L);
        t.lit("monBoxPos",rom.specialVarResult + 8L);
        t.lit("setBoxMonAt",0x0808BBB5L);
        return new Layout(new NativeHelper(addr,t.finish()),restore,launch,reward);
    }

    private static final class Thumb{
        ByteArrayOutputStream o=new ByteArrayOutputStream();Map<String,Integer>lab=new HashMap<>(),lp=new HashMap<>();List<B>bs=new ArrayList<>();List<BL>bls=new ArrayList<>();List<L>ls=new ArrayList<>(),ads=new ArrayList<>();Map<String,Long>lv=new LinkedHashMap<>();
        int p(){return o.size();}void u16(int v){o.write(v&255);o.write(v>>>8&255);}void label(String s){lab.put(s,p());}void align(){while((p()&3)!=0)u16(0x46C0);}void bc(int c,String s){int p=p();u16(0xD000|c<<8);bs.add(new B(p,s));}void bl(String s){int p=p();u16(0);u16(0);bls.add(new BL(p,s));}void ldr(int r,String s){int p=p();u16(0x4800|r<<8);ls.add(new L(p,r,s));}void adr(int r,String s){int p=p();u16(0xA000|r<<8);ads.add(new L(p,r,s));}void mov(int r,int v){u16(0x2000|r<<8|v);}void raw(byte[]x){o.writeBytes(x);}void lit(String s,long v){lv.put(s,v);}
        static byte[] hex(String s){byte[]b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}byte[]finish(){align();for(var e:lv.entrySet()){lp.put(e.getKey(),p());long v=e.getValue();o.write((int)v&255);o.write((int)(v>>>8)&255);o.write((int)(v>>>16)&255);o.write((int)(v>>>24)&255);}byte[]a=o.toByteArray();for(B f:bs){int h=(lab.get(f.s)-(f.p+4))/2;if(h< -128||h>127)throw new IllegalStateException("branch "+f.s);int x=(a[f.p]&255)|(a[f.p+1]&255)<<8;put(a,f.p,(x&0xFF00)|(h&255));}for(BL f:bls){int d=lab.get(f.s)-(f.p+4);put(a,f.p,0xF000|((d>>12)&0x7FF));put(a,f.p+2,0xF800|((d>>1)&0x7FF));}for(L f:ads){int pc=(f.p+4)&~3,d=lab.get(f.s)-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("adr "+f.s);put(a,f.p,0xA000|f.r<<8|d/4);}for(L f:ls){int pc=(f.p+4)&~3,d=lp.get(f.s)-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("literal "+f.s);put(a,f.p,0x4800|f.r<<8|d/4);}return a;}static void put(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);}record B(int p,String s){}record BL(int p,String s){}record L(int p,int r,String s){}
    }
}
