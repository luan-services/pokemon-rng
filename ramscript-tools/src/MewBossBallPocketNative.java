import java.io.ByteArrayOutputStream;
import java.util.*;

/* Probe 15: logical Ball-pocket transaction. Backup is plaintext in SB2 filler,
   so SaveBlock relocation / encryption-key rotation cannot invalidate it. */
final class MewBossBallPocketNative {
    static final int BALL_POCKET_OFFSET=0x0430, BALL_SLOT_COUNT=13;
    static final int BACKUP_OFFSET=0x0EC0, BACKUP_BYTES=BALL_SLOT_COUNT*4;
    static final int MAGIC_OFFSET=BACKUP_OFFSET+BACKUP_BYTES;
    static final long MAGIC=0x314C4142L; // BAL1
    private static final int KEY_OFFSET=0x0F20;
    private MewBossBallPocketNative(){}

    static NativeHelper backupAndClearAt(RomProfile rom,long addr){requireFr10(rom);Thumb t=new Thumb();
        t.u16(0xB5F0); // push r4-r7,lr
        t.ldr(0,"sb1");t.u16(0x6804);t.ldr(0,"pocket");t.u16(0x1824); // r4=pocket
        t.ldr(0,"sb2");t.u16(0x6805);t.ldr(0,"backup");t.u16(0x182D); // r5=backup
        t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"key");t.u16(0x1840);t.u16(0x8806); // r6=key16
        t.u16(0x270D);t.label("loop");
        t.u16(0x8820);t.u16(0x8028); // item id -> backup
        t.u16(0x8860);t.u16(0x4070);t.u16(0x8068); // decrypted qty -> backup
        t.u16(0x2000);t.u16(0x8020);t.u16(0x8066); // live slot = none, encrypted zero
        t.u16(0x3404);t.u16(0x3504);t.u16(0x3F01);t.bc(1,"loop");
        t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"magicOff");t.u16(0x1840);t.ldr(1,"magic");t.u16(0x6001);
        t.u16(0xBDF0);t.align();lits(t,rom);return new NativeHelper(addr,t.finish());}

    static NativeHelper restoreAt(RomProfile rom,long addr){requireFr10(rom);Thumb t=new Thumb();
        t.u16(0xB5F0);
        t.ldr(0,"sb2");t.u16(0x6805); // r5=current SB2
        t.ldr(1,"magicOff");t.u16(0x1868);t.u16(0x6801);t.ldr(2,"magic");t.u16(0x4291);t.bc(1,"done");
        t.ldr(1,"key");t.u16(0x1868);t.u16(0x8806); // r6=current key16
        t.ldr(1,"backup");t.u16(0x186D); // r5=backup
        t.ldr(0,"sb1");t.u16(0x6804);t.ldr(1,"pocket");t.u16(0x1864); // r4=current pocket
        t.u16(0x270D);t.label("loop");
        t.u16(0x8828);t.u16(0x8020); // item id
        t.u16(0x8868);t.u16(0x4070);t.u16(0x8060); // qty encrypted with current key
        t.u16(0x3404);t.u16(0x3504);t.u16(0x3F01);t.bc(1,"loop");
        t.ldr(0,"sb2");t.u16(0x6800);t.ldr(1,"magicOff");t.u16(0x1840);t.u16(0x2100);t.u16(0x6001);
        t.label("done");t.u16(0xBDF0);t.align();lits(t,rom);return new NativeHelper(addr,t.finish());}

    private static void lits(Thumb t,RomProfile r){t.lit("sb1",r.saveBlock1Ptr);t.lit("sb2",r.saveBlock2Ptr);t.lit("pocket",BALL_POCKET_OFFSET);t.lit("backup",BACKUP_OFFSET);t.lit("magicOff",MAGIC_OFFSET);t.lit("key",KEY_OFFSET);t.lit("magic",MAGIC);}
    private static void requireFr10(RomProfile r){if(r!=RomProfile.FIRE_RED_EN_10)throw new IllegalArgumentException("Probe 15 is FR1.0-only");}

    private static final class Thumb{ByteArrayOutputStream o=new ByteArrayOutputStream();Map<String,Integer>lab=new HashMap<>(),lp=new HashMap<>();List<B>bs=new ArrayList<>();List<L>ls=new ArrayList<>();Map<String,Long>lv=new LinkedHashMap<>();int p(){return o.size();}void u16(int v){o.write(v&255);o.write(v>>>8&255);}void label(String s){lab.put(s,p());}void align(){while((p()&3)!=0)u16(0x46C0);}void bc(int c,String s){int p=p();u16(0xD000|c<<8);bs.add(new B(p,s));}void ldr(int r,String s){int p=p();u16(0x4800|r<<8);ls.add(new L(p,r,s));}void lit(String s,long v){lv.put(s,v);}byte[]finish(){align();for(var e:lv.entrySet()){lp.put(e.getKey(),p());long v=e.getValue();o.write((int)v&255);o.write((int)(v>>>8)&255);o.write((int)(v>>>16)&255);o.write((int)(v>>>24)&255);}byte[]a=o.toByteArray();for(B f:bs){int h=(lab.get(f.s)-(f.p+4))/2;if(h< -128||h>127)throw new IllegalStateException("branch");int x=(a[f.p]&255)|(a[f.p+1]&255)<<8;put(a,f.p,(x&0xFF00)|(h&255));}for(L f:ls){int pc=(f.p+4)&~3,d=lp.get(f.s)-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("literal "+f.s);put(a,f.p,0x4800|f.r<<8|d/4);}return a;}static void put(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);}record B(int p,String s){}record L(int p,int r,String s){}}
}
