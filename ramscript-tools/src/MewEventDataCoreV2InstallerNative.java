final class MewEventDataCoreV2InstallerNative{
 private static final long S=0x020370A8L;private MewEventDataCoreV2InstallerNative(){}
 static NativeHelper buildAt(RomProfile rom,long addr,long srcB,int lenB,long srcX,int lenX){if((lenB&3)!=0||(lenX&3)!=0)throw new IllegalArgumentException();int wb=lenB/4,wx=lenX/4;if(wb>255||wx>255)throw new IllegalArgumentException();byte[]c=new byte[0x54];
  // source B physical
  ldr(c,0,3,0x34);u16(c,2,0x681B);ldr(c,4,0,0x38);u16(c,6,0x1AC0);ldr(c,8,1,0x3C);u16(c,0xA,0x6809);ldr(c,0xC,2,0x40);u16(c,0xE,0x1889);u16(c,0x10,0x2340);u16(c,0x12,0x051B);u16(c,0x14,0x3300|wb);u16(c,0x16,0xDF0B);
  // source extension physical
  ldr(c,0x18,3,0x34);u16(c,0x1A,0x681B);ldr(c,0x1C,0,0x44);u16(c,0x1E,0x1AC0);ldr(c,0x20,1,0x48);u16(c,0x22,0x6809);ldr(c,0x24,2,0x4C);u16(c,0x26,0x1889);u16(c,0x28,0x2340);u16(c,0x2A,0x051B);u16(c,0x2C,0x3300|wx);u16(c,0x2E,0xDF0B);
  u16(c,0x30,0x4770);u16(c,0x32,0x46C0);u32(c,0x34,S);u32(c,0x38,srcB);u32(c,0x3C,rom.saveBlock1Ptr);u32(c,0x40,MewEventDataCore.STORAGE_OFFSET);u32(c,0x44,srcX);u32(c,0x48,rom.saveBlock2Ptr);u32(c,0x4C,MewBattleCore.STORAGE_OFFSET+MewBattleCoreExtension.START);u32(c,0x50,0);return new NativeHelper(addr,c);}
 static void ldr(byte[]a,int at,int r,int to){int d=to-((at+4)&~3);u16(a,at,0x4800|r<<8|d/4);}static void u16(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);}static void u32(byte[]a,int p,long v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);a[p+2]=(byte)(v>>>16);a[p+3]=(byte)(v>>>24);}
}
