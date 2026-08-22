import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/*
   Build 15: SB1 gateway -> SB2 persistent Field Script payloads.

   The validated deferred MultiHotkeyRuntimeV1 only needs to reach tiny,
   fixed-distance Field Script entries in the validated 400-byte SB1 filler.
   Each 10-byte entry does:

       setvaddress 0x08010000
       vgoto <virtual target representing the fixed cross-SaveBlock delta>

   Because FR/LG applies the same ASLR offset to SaveBlock1 and SaveBlock2,
   the physical delta from an SB1 gateway to an SB2 payload is invariant.
   ScrCmd_vgoto resolves target - sAddressOffset, so a gateway can jump across
   SaveBlocks without native code, an IWRAM resolver, runtime self-modification,
   or patching the RamScript.

   Once control arrives at the SB2 payload, that payload's own setvaddress
   resets sAddressOffset for its internal vgoto/vmessage references.
*/
final class PersistentFieldScriptGatewayRuntime {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    private static final long STATIC_SAVE_BLOCK2 = 0x02024588L;
    private static final long STATIC_SAVE_BLOCK1 = 0x0202552CL;
    private static final int RAMSCRIPT_SCRIPT_IN_SB1 = 0x3624;

    private static final int SB1_STORAGE = 0x348C;
    private static final int SB1_STORAGE_SIZE = 400;
    private static final int SB2_STORAGE = 0x0B20;
    private static final int SB2_STORAGE_SIZE = 1024;

    // Two 10-byte gateways occupy the final 20 bytes before struct RamScript.
    static final int REPEL_ENTRY_SB1_OFFSET = RAMSCRIPT_SCRIPT_IN_SB1 - 0x1C; // 0x3608
    static final int SEED_ENTRY_SB1_OFFSET  = RAMSCRIPT_SCRIPT_IN_SB1 - 0x12; // 0x3612
    static final int REPEL_ENTRY_DISTANCE = 0x1C;
    static final int SEED_ENTRY_DISTANCE  = 0x12;

    // Real payloads live in the large validated SB2 area.
    static final int REPEL_SB2_OFFSET = SB2_STORAGE;          // 0x0B20
    static final int SEED_SB2_OFFSET  = SB2_STORAGE + 0x100;  // 0x0C20

    private static final long SETUP_SCRIPT_LITERAL = 0x0300504CL;
    private static final long MULTI_LITERAL_POOL = 0x03005356L;
    private static final long DISTANCE_TABLE = 0x0300539CL;

    private static final int NATIVE_CODE_AND_LITERALS_SIZE = 56;
    private static final int BLOCK_COUNT = 15;
    private static final int TABLE_SIZE = BLOCK_COUNT * 4;

    private PersistentFieldScriptGatewayRuntime() {}

    static RamScript buildInstaller(RomProfile rom, int desiredSeed) {
        byte[] repel = RepelHotkeyPreset.buildPayload();
        byte[] seed = SeedModifierPreset.buildPayload(rom, desiredSeed);
        byte[] repelEntry = gateway(REPEL_ENTRY_SB1_OFFSET, REPEL_SB2_OFFSET);
        byte[] seedEntry = gateway(SEED_ENTRY_SB1_OFFSET, SEED_SB2_OFFSET);
        requireLayout(repel, seed, repelEntry, seedEntry);

        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = installerHelper(rom, helperAddress, repelEntry, seedEntry, repel, seed);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, "persistent_field_gateway_install",
                NativeHelperInstaller.Mode.AUTO
        );
        b.lockAll();
        p.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end()
                .text("ok", "Persistent gateways + SB2 modules installed.\\nSave, then install runtime.");
        return RamScript.createWonderCard(b.buildScript());
    }

    static TriggerBuildResult buildRuntime(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(rom);
        int residentBytes = totalResidentBytes(blocks);
        byte[] nativeBlob = nativeInstallerBlob(blocks, residentBytes);

        int nativeBlobOffset = 0x0C;
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) throw new IllegalArgumentException("Build 15 runtime requires " + total + " bytes");

        byte[] script = new byte[total];
        int p = 0;
        script[p++] = (byte)0xB8;
        putU32(script, p, VIRTUAL_BASE); p += 4;
        script[p++] = (byte)0xB9;
        putU32(script, p, VIRTUAL_BASE + fieldInstallerOffset); p += 4;
        if (p != HotkeyRuntimeV1.SIGNATURE_OFFSET) throw new IllegalStateException("signature offset mismatch");
        script[p++] = (byte)(HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);
        if (p != nativeBlobOffset) throw new IllegalStateException("native blob offset mismatch");
        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length); p += nativeBlob.length;
        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        RamScript rs = RamScript.createWonderCard(script);
        return new TriggerBuildResult(rs, EventTrigger.HOTKEY_RUNTIME, rom, 0, total, total, RamScript.SCRIPT_SIZE - total);
    }

    static byte[] gatewayForTest(int entryOffset, int targetOffset) { return gateway(entryOffset, targetOffset); }
    static byte[] stage2BytesForTest() { return sb1Stage2(); }
    static byte[] wrapperBytesForTest(RomProfile rom) { return wrapper(rom); }
    static byte[] safetyGateBytesForTest() { return multiSafetyGate(); }

    private static byte[] gateway(int entrySb1Offset, int targetSb2Offset) {
        long entryStatic = STATIC_SAVE_BLOCK1 + entrySb1Offset;
        long targetStatic = STATIC_SAVE_BLOCK2 + targetSb2Offset;
        long delta = targetStatic - entryStatic;
        long virtualTarget = (VIRTUAL_BASE + delta) & 0xFFFFFFFFL;
        byte[] out = new byte[10];
        out[0] = (byte)0xB8; // setvaddress
        putU32(out, 1, VIRTUAL_BASE);
        out[5] = (byte)0xB9; // vgoto
        putU32(out, 6, virtualTarget);
        return out;
    }

    private static List<RuntimeV1ResidentBlocks.Block> residentBlocks(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = new ArrayList<>();
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_TAIL,
                new byte[]{0x02,0x4B,0x18,0x47}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_LITERAL,
                le32(rom.originalVBlankThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SUPERVISOR, supervisor()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS, supervisorLiterals(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.STAGE2, sb1Stage2()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(SETUP_SCRIPT_LITERAL, le32(rom.scriptContextSetupThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.STAGE1, multiStage1()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.PRIMARY_THUNK, new byte[]{0x00,0x4C,0x20,0x47}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.FUNCTION_LITERAL, le32(rom.getSavedRamScriptThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.MARKER, new byte[]{0x00}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(MULTI_LITERAL_POOL, literalPool(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(DISTANCE_TABLE, distanceTable()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SAFETY_GATE, multiSafetyGate()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.FORMAT_VALIDATOR, validator()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.WRAPPER, wrapper(rom)));
        if (blocks.size() != BLOCK_COUNT) throw new IllegalStateException("Build 15 block count mismatch");
        return List.copyOf(blocks);
    }

    private static byte[] wrapper(RomProfile rom) {
        byte[] out = new byte[]{
                0x06,0x48, 0x00,0x68, 0x00,0x00, 0x06,(byte)0xD3,
                0x00,0x00, (byte)0x89,0x0F, 0x03,(byte)0xD0,
                0x1F,(byte)0xA2, 0x51,0x5C, 0x0D,0x4A, (byte)0x86,(byte)0xE0,
                0x0D,0x4B, 0x18,0x47, (byte)0xC0,0x46, 0,0,0,0
        };
        putU16(out,0x04,thumbLsrsImm(2,0,9));
        putU16(out,0x08,thumbLslsImm(1,0,13));
        putU32(out,0x1C,rom.heldKeysRaw);
        if (out.length != 32) throw new IllegalStateException("Build 15 wrapper must be 32 bytes");
        return out;
    }

    private static byte[] multiStage1() {
        return new byte[]{
                0x12,(byte)0xB5,
                (byte)0xFE,(byte)0xF7,(byte)0x88,(byte)0xFF,
                0x00,0x28, 0x00,(byte)0xD0, (byte)0x8C,(byte)0xE1,
                0x12,(byte)0xBD
        };
    }

    private static byte[] sb1Stage2() {
        return new byte[]{
                0x00,(byte)0x99,
                0x40,0x1A,
                0x05,0x4C,
                (byte)0xFE,(byte)0xF7,(byte)0xAF,(byte)0xFF,
                0x27,(byte)0xE0,
                (byte)0xC0,0x46
        };
    }

    private static byte[] distanceTable() {
        return new byte[]{0, (byte)REPEL_ENTRY_DISTANCE, (byte)SEED_ENTRY_DISTANCE, (byte)REPEL_ENTRY_DISTANCE};
    }

    private static byte[] multiSafetyGate() {
        return new byte[]{
                0x10,0x78,0x00,0x28,0x00,(byte)0xD0,0x74,(byte)0xE7,
                0x21,(byte)0xE6,0x12,(byte)0xBD
        };
    }

    private static byte[] validator() { return new byte[]{0x41,(byte)0x89,(byte)0xA7,0x29,0x47,(byte)0xD1,0x40,(byte)0xE6}; }

    private static byte[] literalPool(RomProfile rom) {
        byte[] out = new byte[10];
        putU32(out,2,rom.lockFieldControls);
        putU32(out,6,rom.cb1OverworldThumb);
        return out;
    }

    private static byte[] supervisor() {
        return new byte[]{
                0x18,(byte)0xA3,0x07,(byte)0xCB,0x03,0x68,(byte)0x8B,0x42,
                (byte)0xB3,(byte)0xD1,0x02,0x60,(byte)0xB1,(byte)0xE7
        };
    }

    private static byte[] supervisorLiterals(RomProfile rom) {
        byte[] out = new byte[12];
        putU32(out,0,0x030030F0L);
        putU32(out,4,rom.cb1OverworldThumb);
        putU32(out,8,RuntimeV1ResidentBlocks.WRAPPER | 1L);
        return out;
    }

    private static byte[] nativeInstallerBlob(List<RuntimeV1ResidentBlocks.Block> blocks, int residentBytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] codeAndLiterals = new byte[]{
                (byte)0xF0,(byte)0xB4, 0x0D,(byte)0xA4, 0x1B,(byte)0xA6, 0x03,0x27,
                0x3F,0x06, 0x0F,0x25, 0x21,(byte)0x88, 0x62,(byte)0x88, 0x04,0x34,
                (byte)0xC9,0x19, 0x33,0x78, 0x0B,0x70, 0x01,0x36, 0x01,0x31,
                0x01,0x3A, (byte)0xF9,(byte)0xD1, 0x01,0x3D, (byte)0xF3,(byte)0xD1,
                0x02,0x48, 0x03,0x49, 0x01,0x60, (byte)0xF0,(byte)0xBC, 0x70,0x47,
                (byte)0xC0,0x46, 0x50,0x35,0x00,0x03, 0x43,0x3F,0x00,0x03
        };
        if (codeAndLiterals.length != NATIVE_CODE_AND_LITERALS_SIZE) throw new IllegalStateException("native size");
        out.writeBytes(codeAndLiterals);
        for (RuntimeV1ResidentBlocks.Block block: blocks) { u16(out,(int)(block.address() & 0xFFFF)); u16(out,block.data().length); }
        for (RuntimeV1ResidentBlocks.Block block: blocks) out.writeBytes(block.data());
        byte[] blob=out.toByteArray();
        int expected=NATIVE_CODE_AND_LITERALS_SIZE+TABLE_SIZE+residentBytes;
        if(blob.length!=expected) throw new IllegalStateException("native blob expected "+expected+", got "+blob.length);
        return blob;
    }

    private static NativeHelper installerHelper(RomProfile rom,long address,byte[] repelEntry,byte[] seedEntry,byte[] repel,byte[] seed) {
        if (repel.length>0xFF || seed.length>0xFF) throw new IllegalStateException("Build 15 installer copy length overflow");
        // 4 copy loops (24 bytes each) + bx lr, then 12 literals per copy.
        int lit = 0x68;
        int repelEntrySrc = 0x98;
        int seedEntrySrc = align4(repelEntrySrc + repelEntry.length);
        int repelSrc = align4(seedEntrySrc + seedEntry.length);
        int seedSrc = align4(repelSrc + repel.length);
        byte[] code = new byte[seedSrc + seed.length];

        emitCopy(code,0x00,lit+0x00,lit+0x04,lit+0x08,repelEntry.length,0x0C);
        emitCopy(code,0x18,lit+0x0C,lit+0x10,lit+0x14,seedEntry.length,0x24);
        emitCopy(code,0x30,lit+0x18,lit+0x1C,lit+0x20,repel.length,0x3C);
        emitCopy(code,0x48,lit+0x24,lit+0x28,lit+0x2C,seed.length,0x54);
        putU16(code,0x60,0x4770);
        for(int o=0x62;o<lit;o+=2) putU16(code,o,0x46C0);

        putU32(code,lit+0x00,rom.saveBlock1Ptr); putU32(code,lit+0x04,REPEL_ENTRY_SB1_OFFSET); putU32(code,lit+0x08,address+repelEntrySrc);
        putU32(code,lit+0x0C,rom.saveBlock1Ptr); putU32(code,lit+0x10,SEED_ENTRY_SB1_OFFSET);  putU32(code,lit+0x14,address+seedEntrySrc);
        putU32(code,lit+0x18,rom.saveBlock2Ptr); putU32(code,lit+0x1C,REPEL_SB2_OFFSET);      putU32(code,lit+0x20,address+repelSrc);
        putU32(code,lit+0x24,rom.saveBlock2Ptr); putU32(code,lit+0x28,SEED_SB2_OFFSET);       putU32(code,lit+0x2C,address+seedSrc);

        System.arraycopy(repelEntry,0,code,repelEntrySrc,repelEntry.length);
        System.arraycopy(seedEntry,0,code,seedEntrySrc,seedEntry.length);
        System.arraycopy(repel,0,code,repelSrc,repel.length);
        System.arraycopy(seed,0,code,seedSrc,seed.length);
        return new NativeHelper(address,code);
    }

    private static void emitCopy(byte[] c,int base,int ptrLit,int offLit,int srcLit,int len,int loop) {
        putU16(c,base+0x00,ldrLiteral(0,base+0x00,ptrLit));
        putU16(c,base+0x02,0x6800);
        putU16(c,base+0x04,ldrLiteral(1,base+0x04,offLit));
        putU16(c,base+0x06,0x1840);
        putU16(c,base+0x08,ldrLiteral(1,base+0x08,srcLit));
        putU16(c,base+0x0A,0x2200|len);
        putU16(c,base+0x0C,0x780B); putU16(c,base+0x0E,0x7003);
        putU16(c,base+0x10,0x3101); putU16(c,base+0x12,0x3001); putU16(c,base+0x14,0x3A01);
        putU16(c,base+0x16,branchCond(1,base+0x16,loop));
    }

    private static void requireLayout(byte[] repel, byte[] seed, byte[] repelEntry, byte[] seedEntry) {
        int sb1End = SB1_STORAGE + SB1_STORAGE_SIZE;
        if (repelEntry.length != 10 || seedEntry.length != 10) throw new IllegalStateException("Build 15 gateways must be 10 bytes");
        if (REPEL_ENTRY_SB1_OFFSET < SB1_STORAGE || SEED_ENTRY_SB1_OFFSET + seedEntry.length > sb1End) throw new IllegalStateException("gateway outside validated SB1 storage");
        if (SEED_ENTRY_SB1_OFFSET + seedEntry.length != RAMSCRIPT_SCRIPT_IN_SB1 - 8) throw new IllegalStateException("gateways must end at struct RamScript boundary");
        if (REPEL_SB2_OFFSET < SB2_STORAGE || REPEL_SB2_OFFSET + repel.length > SB2_STORAGE + SB2_STORAGE_SIZE) throw new IllegalStateException("Repel outside SB2 storage");
        if (SEED_SB2_OFFSET < SB2_STORAGE || SEED_SB2_OFFSET + seed.length > SB2_STORAGE + SB2_STORAGE_SIZE) throw new IllegalStateException("Seed outside SB2 storage");
        if (REPEL_ENTRY_DISTANCE > 0xFF || SEED_ENTRY_DISTANCE > 0xFF) throw new IllegalStateException("gateway distances must fit u8");
    }

    private static int totalResidentBytes(List<RuntimeV1ResidentBlocks.Block>b){int n=0;for(var x:b)n+=x.data().length;return n;}
    private static int align4(int n){return(n+3)&~3;}
    private static int thumbLsrsImm(int rd,int rm,int shift){return 0x0800|(shift<<6)|(rm<<3)|rd;}
    private static int thumbLslsImm(int rd,int rm,int shift){return (shift<<6)|(rm<<3)|rd;}
    private static byte[] le32(long v){byte[]b=new byte[4];putU32(b,0,v);return b;}
    private static void u16(ByteArrayOutputStream o,int v){o.write(v&0xFF);o.write((v>>>8)&0xFF);}
    private static void putU16(byte[]b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[]b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}
    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3;int d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);}
}
