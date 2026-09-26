final class MewCoreLauncherNative {
    private MewCoreLauncherNative() {}

    static NativeHelper buildDispatcherAt(RomProfile rom, long address) {
        // VAR_8005 selector: 0 = prepare, 1 = launch.
        // Resolves live SB2 every call, validates MEW1/version/installed, then
        // tail-jumps into the selected persistent Thumb entry with LR intact.
        byte[] c = new byte[] {
            0x0D,0x4B,             // ldr r3, =gSaveBlock2Ptr
            0x1B,0x68,             // ldr r3,[r3]
            0x0D,0x48,             // ldr r0, =0xB20
            0x1B,0x18,             // adds r3,r3,r0
            0x18,0x68,             // ldr r0,[r3]
            0x0C,0x49,             // ldr r1, =MEW1
            (byte)0x88,0x42,       // cmp r0,r1
            0x10,(byte)0xD1,       // bne fail
            0x18,0x79,             // ldrb r0,[r3,#4] version
            0x01,0x28,             // cmp r0,#1
            0x0D,(byte)0xD1,       // bne fail
            0x58,0x79,             // ldrb r0,[r3,#5] installed
            0x01,0x28,             // cmp r0,#1
            0x0A,(byte)0xD1,       // bne fail
            0x08,0x48,             // ldr r0, =VAR_8005
            0x00,(byte)0x88,       // ldrh r0,[r0]
            0x00,0x28,             // cmp r0,#0
            0x02,(byte)0xD0,       // beq prepare
            0x01,0x28,             // cmp r0,#1
            0x02,(byte)0xD0,       // beq launch
            0x05,(byte)0xE0,       // b fail
            0x58,(byte)0x89,       // prepare: ldrh r0,[r3,#0x0A]
            0x01,(byte)0xE0,       // b dispatch
            (byte)0xD8,(byte)0x89, // launch: ldrh r0,[r3,#0x0E]
            0x1B,0x18,             // dispatch: adds r3,r3,r0
            0x01,0x33,             // adds r3,#1
            0x18,0x47,             // bx r3
            0x00,0x20,             // fail: movs r0,#0
            0x03,0x49,             // ldr r1, =VAR_RESULT
            0x08,(byte)0x80,       // strh r0,[r1]
            0x70,0x47,             // bx lr
            (byte)0xC0,0x46,       // align
            0,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0
        };
        put32(c,0x38,rom.saveBlock2Ptr);
        put32(c,0x3C,MewCoreV1.STORAGE_OFFSET);
        put32(c,0x40,MewCoreV1.MAGIC);
        put32(c,0x44,rom.specialVar8005);
        put32(c,0x48,rom.specialVarResult);
        patchLdr(c,0x00,3,0x38); patchLdr(c,0x04,0,0x3C); patchLdr(c,0x0A,1,0x40);
        patchLdr(c,0x1C,0,0x44); patchLdr(c,0x32,1,0x48);
        return new NativeHelper(address,c);
    }
    private static void patchLdr(byte[]a,int at,int reg,int target){int pc=(at+4)&~3;int d=target-pc;if(d<0||(d&3)!=0||d/4>255)throw new IllegalStateException("ldr range");put16(a,at,0x4800|(reg<<8)|(d/4));}
    private static void put16(byte[]a,int p,int v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);}
    private static void put32(byte[]a,int p,long v){a[p]=(byte)v;a[p+1]=(byte)(v>>>8);a[p+2]=(byte)(v>>>16);a[p+3]=(byte)(v>>>24);}
}
