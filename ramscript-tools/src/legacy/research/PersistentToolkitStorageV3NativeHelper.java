final class PersistentToolkitStorageV3NativeHelper {
    private PersistentToolkitStorageV3NativeHelper() {}

    static NativeHelper buildInstallerAAt(RomProfile rom, long address) {
        byte[] image = PersistentToolkitStorageV3.buildInitialImage(rom);
        // same compact byte-copy pattern as earlier probes; image starts at 0x1C
        byte[] code = new byte[0x1C + image.length + 8];
        int litPtr = align4(0x1C + image.length);
        if (code.length < litPtr + 8) code = java.util.Arrays.copyOf(code, litPtr + 8);
        put16(code, 0x00, ldrLiteral(0, 0x00, litPtr));
        put16(code, 0x02, 0x6800); // ldr r0,[r0]
        put16(code, 0x04, ldrLiteral(1, 0x04, litPtr + 4));
        put16(code, 0x06, 0x1840); // adds r0,r0,r1
        put16(code, 0x08, adr(1, 0x08, 0x1C));
        put16(code, 0x0A, 0x2200 | image.length); // movs r2,#len
        put16(code, 0x0C, 0x780B); put16(code, 0x0E, 0x7003);
        put16(code, 0x10, 0x3101); put16(code, 0x12, 0x3001); put16(code, 0x14, 0x3A01);
        put16(code, 0x16, 0xD1F9); // bne 0x0C
        put16(code, 0x18, 0x4770); put16(code, 0x1A, 0x46C0);
        System.arraycopy(image, 0, code, 0x1C, image.length);
        PersistentToolkitStorageV2.putU32(code, litPtr, rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code, litPtr + 4, PayloadStorageArea.SAVE_BLOCK2.offset());
        return new NativeHelper(address, code);
    }

    static NativeHelper buildInstallerBAt(RomProfile rom, long address) {
        byte[] patch = PersistentToolkitStorageV3.buildModuleBPatch(rom);
        // sparse patch: write only [entry B + payload B], then set moduleCount=2.
        byte[] code = new byte[0x50];
        put16(code,0x00,0x4811); put16(code,0x02,0x6800);
        put16(code,0x04,0x4911); put16(code,0x06,0x1840);
        put16(code,0x08,0x3020); // destination = storage + entry B
        put16(code,0x0A,0xA108); // adr r1, patch at 0x2C (PC base 0x0C -> +0x20)
        put16(code,0x0C,0x221C); // 28 bytes
        put16(code,0x0E,0x780B); put16(code,0x10,0x7003);
        put16(code,0x12,0x3101); put16(code,0x14,0x3001); put16(code,0x16,0x3A01);
        put16(code,0x18,0xD1F9); // bne 0x0E
        put16(code,0x1A,0x480B); put16(code,0x1C,0x6800);
        put16(code,0x1E,0x490B); put16(code,0x20,0x1840);
        put16(code,0x22,0x2102); put16(code,0x24,0x7141); // strb r1,[r0,#5]
        put16(code,0x26,0x4770); put16(code,0x28,0x46C0); put16(code,0x2A,0x46C0);
        System.arraycopy(patch,0,code,0x2C,patch.length);
        PersistentToolkitStorageV2.putU32(code,0x48,rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code,0x4C,PayloadStorageArea.SAVE_BLOCK2.offset());
        return new NativeHelper(address, code);
    }

    static NativeHelper buildLauncherAt(RomProfile rom, long address, int moduleId) {
        NativeHelper old = PersistentToolkitStorageV2NativeHelper.buildLauncherAt(rom, address, moduleId);
        byte[] code = old.codeCopy();
        code[0x14] = 0x03; // cmp r0,#VERSION (immediate byte of `cmp r0,#2`)
        PersistentToolkitStorageV2.putU32(code,0x7C,PersistentToolkitStorageV3.entryOffsetForModule(moduleId));
        return new NativeHelper(address, code);
    }

    private static int align4(int n) { return (n + 3) & ~3; }
    private static void put16(byte[] b,int o,int v){ b[o]=(byte)v; b[o+1]=(byte)(v>>>8); }
    private static int ldrLiteral(int rt,int insn,int literal){ int base=(insn+4)&~3; return 0x4800|(rt<<8)|((literal-base)/4); }
    private static int adr(int rd,int insn,int target){ int base=(insn+4)&~3; return 0xA000|(rd<<8)|((target-base)/4); }
}
