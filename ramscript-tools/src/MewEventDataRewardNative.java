import java.io.ByteArrayOutputStream;
import java.util.*;

/* Exact Probe-24 PC reward writer, separated from the battle module.
   The SetBoxMonAt entry and special-var convention are unchanged from the
   GAME-VALIDATED Probe 24 path. */
final class MewEventDataRewardNative {
    private MewEventDataRewardNative() {}
    static NativeHelper buildAt(RomProfile rom,long addr){
        if(rom!=RomProfile.FIRE_RED_EN_10) throw new IllegalArgumentException("Mew Event/Data Core is FR1.0-only");
        Thumb t=new Thumb();
        t.u16(0xB500);
        t.ldr(0,"monBoxId"); t.u16(0x8800);
        t.ldr(1,"monBoxPos"); t.u16(0x8809);
        t.adr(2,"reward_data");
        t.ldr(3,"setBoxMonAt"); t.bl("thunk_r3");
        t.u16(0xBD00);
        t.align(); t.label("thunk_r3"); t.u16(0x4718); t.align();
        t.label("reward_data");
        t.raw(Thumb.hex("aea47dd1121b0000c7bfd1ff0870013401310202c7d3cdceccd3ff00dd9c0000bdbfedd1bcbf7dd19fb57dd12bbf7dd18cbd7dd1bcdb7dd1bcbf7dd1bcbf7dd1bcbf7dd1bc4077701e69cbe5bcbf7d51"));
        t.lit("monBoxId",rom.specialVarResult + 6L);
        t.lit("monBoxPos",rom.specialVarResult + 8L);
        t.lit("setBoxMonAt",0x0808BBB5L);
        return new NativeHelper(addr,t.finish());
    }
    private static final class Thumb{
        ByteArrayOutputStream o=new ByteArrayOutputStream(); Map<String,Integer> lab=new HashMap<>(),lp=new HashMap<>(); List<BL> bls=new ArrayList<>(); List<L> ls=new ArrayList<>(),ads=new ArrayList<>(); Map<String,Long> lv=new LinkedHashMap<>();
        int p(){return o.size();} void u16(int v){o.write(v&255);o.write(v>>>8&255);} void label(String s){lab.put(s,p());} void align(){while((p()&3)!=0)u16(0x46C0);} void bl(String s){int p=p();u16(0);u16(0);bls.add(new BL(p,s));} void ldr(int r,String s){int p=p();u16(0x4800|r<<8);ls.add(new L(p,r,s));} void adr(int r,String s){int p=p();u16(0xA000|r<<8);ads.add(new L(p,r,s));} void raw(byte[]x){o.writeBytes(x);} void lit(String s,long v){lv.put(s,v);} static byte[] hex(String s){byte[]b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
        byte[] finish(){align();for(var e:lv.entrySet()){lp.put(e.getKey(),p());long v=e.getValue();o.write((int)v&255);o.write((int)(v>>>8)&255);o.write((int)(v>>>16)&255);o.write((int)(v>>>24)&255);}byte[]a=o.toByteArray();for(BL f:bls){int d=lab.get(f.s)-(f.p+4);put(a,f.p,0xF000|((d>>12)&0x7FF));put(a,f.p+2,0xF800|((d>>1)&0x7FF));}for(L f:ads){int pc=(f.p+4)&~3,d=lab.get(f.s)-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("adr "+f.s);put(a,f.p,0xA000|f.r<<8|d/4);}for(L f:ls){int pc=(f.p+4)&~3,d=lp.get(f.s)-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("literal "+f.s);put(a,f.p,0x4800|f.r<<8|d/4);}return a;} static void put(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);} record BL(int p,String s){} record L(int p,int r,String s){}
    }
}
