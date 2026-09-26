import java.io.ByteArrayOutputStream;
import java.util.*;

/* Tiny field-script bridge for the final Mew event. It never caches a physical
   SaveBlock address: every call rereads *gSaveBlock1Ptr / *gSaveBlock2Ptr. */
final class MewPersistentCallNative {
    static final int MODE_PREPARE=0, MODE_LAUNCH=1, MODE_TRANSFORM=2, MODE_ENSURE=3, MODE_REWARD=4, MODE_SCENE_STAN=5, MODE_SCENE_SHANE=6;
    private MewPersistentCallNative(){}
    static NativeHelper buildAt(RomProfile rom,long addr){
        MewBattleCore.Image a=MewBattleCore.build(rom); MewEventDataCore.Image b=MewEventDataCore.build(rom);
        T t=new T(); t.u16(0xB500); t.ldr(0,"mode"); t.u16(0x8800);
        t.u16(0x2800); t.bc(0,"prepare"); t.u16(0x2801);t.bc(0,"launch"); t.u16(0x2802);t.bc(0,"transform"); t.u16(0x2803);t.bc(0,"ensure"); t.u16(0x2804);t.bc(0,"reward"); t.u16(0x2805);t.bc(0,"sceneStan"); t.b("sceneShane");
        t.label("prepare"); t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"prepareOff");t.u16(0x1843);t.b("call");
        t.label("launch"); t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"launchOff");t.u16(0x1843);t.b("call");
        t.label("transform"); t.ldr(0,"sb1");t.u16(0x6800);t.ldr(1,"transformOff");t.u16(0x1843);t.b("call");
        t.label("ensure"); t.ldr(0,"sb1");t.u16(0x6800);t.ldr(1,"ensureOff");t.u16(0x1843);t.b("call");
        t.label("reward"); t.ldr(0,"sb1");t.u16(0x6800);t.ldr(1,"rewardOff");t.u16(0x1843);t.b("call");
        t.label("sceneStan");t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"sceneStanOff");t.u16(0x1843);t.b("call");
        t.label("sceneShane");t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"sceneShaneOff");t.u16(0x1843);
        t.label("call"); t.u16(0x3301);t.bl("thunk");t.u16(0xBD00);t.label("thunk");t.u16(0x4718);t.align();
        t.lit("mode",rom.specialVarResult); // gSpecialVar_Result / 0x800D
        t.lit("sb1",rom.saveBlock1Ptr);t.lit("sb2",rom.saveBlock2Ptr);
        t.lit("prepareOff",MewBattleCore.STORAGE_OFFSET+a.prepareOffset()); t.lit("launchOff",MewBattleCore.STORAGE_OFFSET+a.launchOffset());
        t.lit("transformOff",MewEventDataCore.STORAGE_OFFSET+b.transformRebindOffset()); t.lit("ensureOff",MewEventDataCore.STORAGE_OFFSET+b.ensureMewOffset()); t.lit("rewardOff",MewEventDataCore.STORAGE_OFFSET+b.rewardOffset()); t.lit("sceneStanOff",MewBattleCore.STORAGE_OFFSET+a.sceneStanOffset()); t.lit("sceneShaneOff",MewBattleCore.STORAGE_OFFSET+a.sceneShaneOffset());
        return new NativeHelper(addr,t.finish());
    }
    private static final class T{
        ByteArrayOutputStream o=new ByteArrayOutputStream();Map<String,Integer>lab=new HashMap<>(),lp=new HashMap<>();List<B>bs=new ArrayList<>();List<BL>bls=new ArrayList<>();List<L>ls=new ArrayList<>();Map<String,Long>lv=new LinkedHashMap<>();
        int p(){return o.size();}void u16(int v){o.write(v&255);o.write(v>>>8&255);}void label(String s){lab.put(s,p());}void align(){while((p()&3)!=0)u16(0x46C0);}void bc(int c,String s){int p=p();u16(0xD000|c<<8);bs.add(new B(p,s,false));}void b(String s){int p=p();u16(0xE000);bs.add(new B(p,s,true));}void bl(String s){int p=p();u16(0);u16(0);bls.add(new BL(p,s));}void ldr(int r,String s){int p=p();u16(0x4800|r<<8);ls.add(new L(p,r,s));}void lit(String s,long v){lv.put(s,v);}
        byte[]finish(){align();for(var e:lv.entrySet()){lp.put(e.getKey(),p());long v=e.getValue();o.write((int)v&255);o.write((int)(v>>>8)&255);o.write((int)(v>>>16)&255);o.write((int)(v>>>24)&255);}byte[]a=o.toByteArray();for(B f:bs){int h=(lab.get(f.s)-(f.p+4))/2;if(f.un){if(h< -1024||h>1023)throw new IllegalStateException("b");put(a,f.p,0xE000|(h&0x7FF));}else{if(h< -128||h>127)throw new IllegalStateException("bc");int x=(a[f.p]&255)|(a[f.p+1]&255)<<8;put(a,f.p,(x&0xFF00)|(h&255));}}for(BL f:bls){int d=lab.get(f.s)-(f.p+4);put(a,f.p,0xF000|((d>>12)&0x7FF));put(a,f.p+2,0xF800|((d>>1)&0x7FF));}for(L f:ls){int pc=(f.p+4)&~3,d=lp.get(f.s)-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("ldr "+f.s);put(a,f.p,0x4800|f.r<<8|d/4);}return a;}static void put(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);}record B(int p,String s,boolean un){}record BL(int p,String s){}record L(int p,int r,String s){}
    }
}
