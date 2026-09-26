final class MewCompositionProbe {
    static final int SB2_BASE=0x0B20, SB1_BASE=0x348C, MANIFEST=0x0F00;
    static final int SCRATCH=MewBossBattleTransactionNative.BACKUP_OFFSET;
    static final long MAGIC=0x5045574DL; // MWEP
    static final int VERSION=1, A_OFFSET=SB2_BASE+0x20, B_OFFSET=SB1_BASE, READY_OFFSET=MANIFEST+5;
    private MewCompositionProbe(){}
    static byte[] moduleA(RomProfile rom){
        byte[] c=new byte[0x40];
        put16(c,0x00,0xB500); // push lr
        put16(c,0x02,0);      // ldr r0,sb1ptr
        put16(c,0x04,0x6800); // ldr r0,[r0]
        put16(c,0x06,0);      // ldr r1,Boff
        put16(c,0x08,0x1843); // adds r3,r0,r1
        put16(c,0x0A,0x3301); // +Thumb
        // BL thunk at 0x20
        patchBl(c,0x0C,0x20);
        put16(c,0x10,0);      // ldr r0,result
        put16(c,0x12,0x8801); // ldrh r1,[r0]
        put16(c,0x14,0);      // ldr r2,beef
        put16(c,0x16,0x4291); // cmp
        put16(c,0x18,0xD102); // bne fail @20? target 0x20 is thunk; use fail 0x1E
        put16(c,0x1A,0);      // ldr r1,cafe
        put16(c,0x1C,0x8001); // strh
        put16(c,0x1E,0xBD00); // pop pc
        put16(c,0x20,0x4718); // thunk bx r3
        put16(c,0x22,0x2100); // fail movs r1,0 (not reached with current bne; patch below)
        put16(c,0x24,0x8001); put16(c,0x26,0xBD00);
        put32(c,0x28,rom.saveBlock1Ptr); put32(c,0x2C,B_OFFSET); put32(c,0x30,rom.specialVarResult); put32(c,0x34,0xBEEF); put32(c,0x38,0xCAFE);
        patchLdr(c,0x02,0,0x28); patchLdr(c,0x06,1,0x2C); patchLdr(c,0x10,0,0x30); patchLdr(c,0x14,2,0x34); patchLdr(c,0x1A,1,0x38);
        // bne from 0x18 -> fail 0x22: (0x22-(0x18+4))/2=3
        put16(c,0x18,0xD103);
        return c;
    }
    static byte[] moduleB(RomProfile rom){
        byte[] c=new byte[16]; put16(c,0,0); put16(c,2,0); put16(c,4,0x8001); put16(c,6,0x4770); put32(c,8,rom.specialVarResult); put16(c,12,0xBEEF); put16(c,14,0);
        patchLdr(c,0,0,8); patchLdr(c,2,1,12); return c;
    }
    static byte[] manifest(boolean a,boolean b,boolean ready){byte[]m=new byte[32];put32(m,0,MAGIC);m[4]=(byte)VERSION;m[5]=(byte)(ready?1:0);m[6]=(byte)(a?1:0);m[7]=(byte)(b?1:0);put16(m,8,A_OFFSET);put16(m,10,B_OFFSET);put16(m,12,moduleA(RomProfile.FIRE_RED_EN_10).length);put16(m,14,moduleB(RomProfile.FIRE_RED_EN_10).length);return m;}
    private static void patchLdr(byte[]a,int at,int r,int target){int pc=(at+4)&~3,d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException();put16(a,at,0x4800|(r<<8)|(d/4));}
    private static void patchBl(byte[]a,int at,int target){int d=target-(at+4);put16(a,at,0xF000|((d>>12)&0x7FF));put16(a,at+2,0xF800|((d>>1)&0x7FF));}
    static void put16(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);} static void put32(byte[]a,int p,long v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);a[p+2]=(byte)(v>>>16);a[p+3]=(byte)(v>>>24);}
}
