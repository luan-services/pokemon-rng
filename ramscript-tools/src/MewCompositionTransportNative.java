final class MewCompositionTransportNative {
    private static final long S_ADDRESS_OFFSET=0x020370A8L;
    private MewCompositionTransportNative(){}
    static NativeHelper copyAt(RomProfile rom,long address,long virtualSource,int length,boolean sb1,int destOffset,long verifyMagic,int verifyOffset){
        if(length<=0||length>255)throw new IllegalArgumentException("copy length"); byte[]c=new byte[0x54];
        // source virtual -> physical
        ldr(c,0x00,3,0x3C); MewCompositionProbe.put16(c,0x02,0x681B); ldr(c,0x04,0,0x40); MewCompositionProbe.put16(c,0x06,0x1AC0);
        ldr(c,0x08,3,0x44); MewCompositionProbe.put16(c,0x0A,0x681B); ldr(c,0x0C,1,0x48); MewCompositionProbe.put16(c,0x0E,0x1859); MewCompositionProbe.put16(c,0x10,0x2200|length);
        MewCompositionProbe.put16(c,0x12,0x7803);MewCompositionProbe.put16(c,0x14,0x700B);MewCompositionProbe.put16(c,0x16,0x3001);MewCompositionProbe.put16(c,0x18,0x3101);MewCompositionProbe.put16(c,0x1A,0x3A01);MewCompositionProbe.put16(c,0x1C,0xD1F9);
        // verify target
        ldr(c,0x1E,3,0x44);MewCompositionProbe.put16(c,0x20,0x681B);ldr(c,0x22,1,0x48);MewCompositionProbe.put16(c,0x24,0x1859);MewCompositionProbe.put16(c,0x26,0x6808);ldr(c,0x28,2,0x4C);MewCompositionProbe.put16(c,0x2A,0x4290);MewCompositionProbe.put16(c,0x2C,0xD101);MewCompositionProbe.put16(c,0x2E,0x2001);MewCompositionProbe.put16(c,0x30,0xE000);MewCompositionProbe.put16(c,0x32,0x2000);ldr(c,0x34,1,0x50);MewCompositionProbe.put16(c,0x36,0x8008);MewCompositionProbe.put16(c,0x38,0x4770);MewCompositionProbe.put16(c,0x3A,0x46C0);
        MewCompositionProbe.put32(c,0x3C,S_ADDRESS_OFFSET);MewCompositionProbe.put32(c,0x40,virtualSource);MewCompositionProbe.put32(c,0x44,sb1?rom.saveBlock1Ptr:rom.saveBlock2Ptr);MewCompositionProbe.put32(c,0x48,destOffset+verifyOffset);MewCompositionProbe.put32(c,0x4C,verifyMagic);MewCompositionProbe.put32(c,0x50,rom.specialVarResult);
        // NOTE dest copy offset and verify offset are same in this probe (verifyOffset=0). Keep contract explicit.
        return new NativeHelper(address,c);
    }
    static NativeHelper launcherAt(RomProfile rom,long address){
        byte[]c=new byte[0x38];
        MewCompositionProbe.put16(c,0x00,0xB500); ldr(c,0x02,0,0x2C);MewCompositionProbe.put16(c,0x04,0x6800);ldr(c,0x06,1,0x30);MewCompositionProbe.put16(c,0x08,0x1840);MewCompositionProbe.put16(c,0x0A,0x7801);MewCompositionProbe.put16(c,0x0C,0x2901);MewCompositionProbe.put16(c,0x0E,0xD105);
        ldr(c,0x10,0,0x2C);MewCompositionProbe.put16(c,0x12,0x6800);ldr(c,0x14,1,0x34);MewCompositionProbe.put16(c,0x16,0x1843);MewCompositionProbe.put16(c,0x18,0x3301);patchBl(c,0x1A,0x28);MewCompositionProbe.put16(c,0x1E,0xBD00);
        MewCompositionProbe.put16(c,0x20,0x2000);ldr(c,0x22,1,0x34); // harmless failure return; caller pre-clears result
        MewCompositionProbe.put16(c,0x24,0xBD00);MewCompositionProbe.put16(c,0x26,0x46C0);MewCompositionProbe.put16(c,0x28,0x4718);MewCompositionProbe.put16(c,0x2A,0x46C0);
        MewCompositionProbe.put32(c,0x2C,rom.saveBlock2Ptr);MewCompositionProbe.put32(c,0x30,MewCompositionProbe.READY_OFFSET);MewCompositionProbe.put32(c,0x34,MewCompositionProbe.A_OFFSET);
        return new NativeHelper(address,c);
    }

    // Diagnostic launcher for the already game-installed Composition A layout.
    // No manifest/ready flag: resolve live SB2 and call A directly at SB2+0xB40.
    static NativeHelper directExistingAAt(RomProfile rom,long address){
        byte[] c=new byte[0x20];
        MewCompositionProbe.put16(c,0x00,0xB500); // push lr
        ldr(c,0x02,0,0x14); MewCompositionProbe.put16(c,0x04,0x6800); // *SB2
        ldr(c,0x06,1,0x18); MewCompositionProbe.put16(c,0x08,0x1843); // + A offset
        MewCompositionProbe.put16(c,0x0A,0x3301); // Thumb
        patchBl(c,0x0C,0x12);
        MewCompositionProbe.put16(c,0x10,0xBD00); // pop pc
        MewCompositionProbe.put16(c,0x12,0x4718); // bx r3
        MewCompositionProbe.put32(c,0x14,rom.saveBlock2Ptr);
        MewCompositionProbe.put32(c,0x18,MewCompositionProbe.A_OFFSET);
        return new NativeHelper(address,c);
    }
    private static void ldr(byte[]a,int at,int r,int target){int pc=(at+4)&~3,d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("ldr");MewCompositionProbe.put16(a,at,0x4800|(r<<8)|(d/4));}
    private static void patchBl(byte[]a,int at,int target){int d=target-(at+4);MewCompositionProbe.put16(a,at,0xF000|((d>>12)&0x7FF));MewCompositionProbe.put16(a,at+2,0xF800|((d>>1)&0x7FF));}
}
