final class MewEventDataCoreInstallerNative {
    private static final long S_ADDRESS_OFFSET=0x020370A8L;
    private MewEventDataCoreInstallerNative(){}
    static NativeHelper buildAt(RomProfile rom,long address,long virtualSource,int byteLength){
        if(byteLength<=0||(byteLength&3)!=0) throw new IllegalArgumentException("Module B copy must be word aligned");
        int words=byteLength/4; if(words>0xFF) throw new IllegalArgumentException("Module B too large");
        byte[] c=new byte[0x44];
        put16(c,0x00,0); put16(c,0x02,0x681B); // ldr r3,lit; ldr r3,[r3]
        put16(c,0x04,0); put16(c,0x06,0x1AC0); // source -= sAddressOffset
        put16(c,0x08,0); put16(c,0x0A,0x681B); // live SB1
        put16(c,0x0C,0); put16(c,0x0E,0x1859); // r1=r3+offset
        put16(c,0x10,0x2240); put16(c,0x12,0x0512); put16(c,0x14,0x3200|words); put16(c,0x16,0xDF0B);
        put16(c,0x18,0x6808); put16(c,0x1A,0); put16(c,0x1C,0x4290); put16(c,0x1E,0xD101);
        put16(c,0x20,0x2001); put16(c,0x22,0xE000); put16(c,0x24,0x2000); put16(c,0x26,0); put16(c,0x28,0x8008); put16(c,0x2A,0x4770);
        put32(c,0x2C,S_ADDRESS_OFFSET); put32(c,0x30,virtualSource); put32(c,0x34,rom.saveBlock1Ptr); put32(c,0x38,MewEventDataCore.STORAGE_OFFSET); put32(c,0x3C,MewEventDataCore.MAGIC); put32(c,0x40,rom.specialVarResult);
        patchLdr(c,0x00,3,0x2C);patchLdr(c,0x04,0,0x30);patchLdr(c,0x08,3,0x34);patchLdr(c,0x0C,1,0x38);patchLdr(c,0x1A,2,0x3C);patchLdr(c,0x26,1,0x40);
        return new NativeHelper(address,c);
    }
    private static void patchLdr(byte[]a,int at,int reg,int target){int pc=(at+4)&~3,d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("ldr range");put16(a,at,0x4800|(reg<<8)|(d/4));}
    private static void put16(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);} private static void put32(byte[]a,int p,long v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);a[p+2]=(byte)(v>>>16);a[p+3]=(byte)(v>>>24);}
}
