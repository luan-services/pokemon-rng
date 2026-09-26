/* Small field-only loader. It executes at the copier slot (stringVar4+0x100),
   rereads the live SaveBlock pointer, copies one persistent native helper to
   the Probe-24 staging address (copier+0x40), then returns. The staged helper
   is called by the RamScript only after this loader has returned. */
final class MewPersistentStageLoaderNative {
    private MewPersistentStageLoaderNative() {}
    static NativeHelper buildAt(long address,long saveBlockPtr,int sourceOffset,long destination,int byteLength){
        if((address&3)!=0||(destination&3)!=0||(byteLength&3)!=0||byteLength<=0)throw new IllegalArgumentException("stage loader alignment/size");
        int words=byteLength/4;if(words>0xFF)throw new IllegalArgumentException("stage helper too large");
        byte[] c=new byte[0x28];
        // push {lr}; r0=*saveBlockPtr + sourceOffset; r1=destination;
        // r2=CpuSet 32-bit | wordCount; svc 0x0B; pop {pc}
        u16(c,0x00,0xB500);
        ldr(c,0x02,0,0x18); u16(c,0x04,0x6800);
        ldr(c,0x06,1,0x1C); u16(c,0x08,0x1840);
        ldr(c,0x0A,1,0x20);
        u16(c,0x0C,0x2240);u16(c,0x0E,0x0512);u16(c,0x10,0x3200|words);u16(c,0x12,0xDF0B);
        u16(c,0x14,0xBD00);u16(c,0x16,0x46C0);
        u32(c,0x18,saveBlockPtr);u32(c,0x1C,sourceOffset);u32(c,0x20,destination);u32(c,0x24,byteLength);
        return new NativeHelper(address,c);
    }
    private static void ldr(byte[]a,int at,int r,int target){int pc=(at+4)&~3,d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException();u16(a,at,0x4800|(r<<8)|(d/4));}
    private static void u16(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);} private static void u32(byte[]a,int p,long v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);a[p+2]=(byte)(v>>>16);a[p+3]=(byte)(v>>>24);}
}
