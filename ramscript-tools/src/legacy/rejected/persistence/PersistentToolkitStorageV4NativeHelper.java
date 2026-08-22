final class PersistentToolkitStorageV4NativeHelper {
    private PersistentToolkitStorageV4NativeHelper() {}

    static NativeHelper buildInstallerAt(RomProfile rom, long address) {
        byte[] image = PersistentToolkitStorageV4.buildImage(rom);
        int dataOffset = 0x1C;
        int litPtr = align4(dataOffset + image.length);
        byte[] code = new byte[litPtr + 8];
        put16(code, 0x00, ldrLiteral(0, 0x00, litPtr));
        put16(code, 0x02, 0x6800); // ldr r0,[r0]
        put16(code, 0x04, ldrLiteral(1, 0x04, litPtr + 4));
        put16(code, 0x06, 0x1840); // adds r0,r0,r1
        put16(code, 0x08, adr(1, 0x08, dataOffset));
        put16(code, 0x0A, 0x2200 | image.length); // movs r2,#len
        put16(code, 0x0C, 0x780B); // ldrb r3,[r1]
        put16(code, 0x0E, 0x7003); // strb r3,[r0]
        put16(code, 0x10, 0x3101);
        put16(code, 0x12, 0x3001);
        put16(code, 0x14, 0x3A01);
        put16(code, 0x16, 0xD1F9); // bne 0x0C
        put16(code, 0x18, 0x4770);
        put16(code, 0x1A, 0x46C0);
        System.arraycopy(image, 0, code, dataOffset, image.length);
        PersistentToolkitStorageV2.putU32(code, litPtr, rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code, litPtr + 4, PayloadStorageArea.SAVE_BLOCK2.offset());
        return new NativeHelper(address, code);
    }

    static NativeHelper buildLauncherAt(RomProfile rom, long address) {
        NativeHelper old = PersistentToolkitStorageV2NativeHelper.buildLauncherAt(
                rom, address, 1);
        byte[] code = old.codeCopy();
        code[0x1E] = (byte)PersistentShowSecretIdModule.MODULE_ID;
        code[0x14] = (byte)PersistentToolkitStorageV4.VERSION;
        PersistentToolkitStorageV2.putU32(code, 0x7C, PersistentToolkitStorageV4.ENTRY_OFFSET);
        return new NativeHelper(address, code);
    }

    private static int align4(int n) { return (n + 3) & ~3; }
    private static void put16(byte[] b,int o,int v){ b[o]=(byte)v; b[o+1]=(byte)(v>>>8); }
    private static int ldrLiteral(int rt,int insn,int literal){ int base=(insn+4)&~3; return 0x4800|(rt<<8)|((literal-base)/4); }
    private static int adr(int rd,int insn,int target){ int base=(insn+4)&~3; return 0xA000|(rd<<8)|((target-base)/4); }
}
