/* Small transport helper for Mew Core V1.
   IMPORTANT: the 504-byte core is NOT staged in gStringVar4. It remains raw
   inside the Wonder Card RamScript and this helper CpuSets it directly from
   the relocated RamScript source into the live SaveBlock2 storage area. */
final class MewCoreInstallerNative {
    private static final long S_ADDRESS_OFFSET = 0x020370A8L;
    private MewCoreInstallerNative() {}

    static NativeHelper buildAt(RomProfile rom, long address, long virtualSource, int byteLength) {
        if (byteLength <= 0 || (byteLength & 3) != 0) throw new IllegalArgumentException("Mew Core copy must be word aligned");
        int words = byteLength / 4;
        if (words > 0xFF) throw new IllegalArgumentException("Mew Core transport currently supports <= 0xFF words");

        // Thumb-1:
        //   source = virtualSource - sAddressOffset
        //   dest   = *gSaveBlock2Ptr + 0xB20
        //   CpuSet(source,dest,32-bit|words)
        //   verify destination magic "MEW1"
        //   VAR_RESULT = 1 on success, 0 otherwise
        byte[] c = new byte[] {
            0x0D,0x4B,             // 00 ldr r3, =sAddressOffset
            0x1B,0x68,             // 02 ldr r3,[r3]
            0x0D,0x48,             // 04 ldr r0, =virtualSource
            (byte)0xC0,0x1A,       // 06 subs r0,r0,r3
            0x0C,0x4B,             // 08 ldr r3, =gSaveBlock2Ptr
            0x1B,0x68,             // 0A ldr r3,[r3]
            0x0C,0x49,             // 0C ldr r1, =0xB20
            0x59,0x18,             // 0E adds r1,r3,r1
            0x40,0x22,             // 10 movs r2,#0x40
            0x12,0x05,             // 12 lsls r2,r2,#20 => 0x04000000
            (byte)words,0x32,      // 14 adds r2,#words
            0x0B,(byte)0xDF,       // 16 svc #0x0B
            0x08,0x68,             // 18 ldr r0,[r1] (destination magic)
            0x09,0x4A,             // 1A ldr r2, =MEW1
            (byte)0x90,0x42,       // 1C cmp r0,r2
            0x01,(byte)0xD1,       // 1E bne fail
            0x01,0x20,             // 20 movs r0,#1
            0x00,(byte)0xE0,       // 22 b store
            0x00,0x20,             // 24 fail: movs r0,#0
            0x07,0x49,             // 26 store: ldr r1, =gSpecialVar_Result
            0x08,(byte)0x80,       // 28 strh r0,[r1]
            0x70,0x47,             // 2A bx lr
            0x00,0x00,             // 2C align
            0,0,0,0,               // 2E literals (patched below; see offsets)
            0,0,0,0,
            0,0,0,0,
            0,0,0,0,
            0,0,0,0,
            0,0,0,0
        };
        // Rebuild with a conventional aligned literal pool at 0x2C.
        c = java.util.Arrays.copyOf(c, 0x44);
        put32(c,0x2C,S_ADDRESS_OFFSET);
        put32(c,0x30,virtualSource);
        put32(c,0x34,rom.saveBlock2Ptr);
        put32(c,0x38,MewCoreV1.STORAGE_OFFSET);
        put32(c,0x3C,MewCoreV1.MAGIC);
        put32(c,0x40,rom.specialVarResult);
        patchLdr(c,0x00,3,0x2C);
        patchLdr(c,0x04,0,0x30);
        patchLdr(c,0x08,3,0x34);
        patchLdr(c,0x0C,1,0x38);
        patchLdr(c,0x1A,2,0x3C);
        patchLdr(c,0x26,1,0x40);
        return new NativeHelper(address,c);
    }

    private static void patchLdr(byte[] a,int at,int reg,int target){int pc=(at+4)&~3;int d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("ldr range");put16(a,at,0x4800|(reg<<8)|(d/4));}
    private static void put16(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);}
    private static void put32(byte[]a,int p,long v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);a[p+2]=(byte)(v>>>16);a[p+3]=(byte)(v>>>24);}
}
