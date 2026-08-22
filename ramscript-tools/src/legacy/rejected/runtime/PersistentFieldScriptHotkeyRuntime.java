import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/*
   Build 14a: persistent FIELD-SCRIPT hotkeys.

   Key observation: FireRed/LeafGreen SetSaveBlocksPointers() adds the same
   random ASLR offset to gSaveBlock1 and gSaveBlock2. Therefore the distance
   from the current RamScript script pointer (SaveBlock1 + 0x3624) to the
   validated SaveBlock2 filler_B20 storage is constant even when the blocks
   move in EWRAM.

   This keeps the validated deferred HotkeyRuntime model:
     callback -> GetSavedRamScriptIfValid -> ScriptContext_SetupScript -> return

   The only stage2 change is that the returned current RamScript pointer is
   translated by a fixed delta into SaveBlock2 persistent storage before the
   selected module offset is added. No native dispatcher, no temporary code,
   no gStringVar4 resolver, and no live IWRAM rewriting are used at runtime.

   Prototype scope deliberately uses two already-validated pure Field Script
   presets:
     R+B      -> Repel
     R+SELECT -> Seed Modifier

   Native/hybrid persistent modules remain a separate later problem.
*/
final class PersistentFieldScriptHotkeyRuntime {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    // Static EWRAM bases are identical in the four supplied English FR/LG .sym files.
    private static final long STATIC_SAVE_BLOCK2 = 0x02024588L;
    private static final long STATIC_SAVE_BLOCK1 = 0x0202552CL;
    private static final int RAMSCRIPT_SCRIPT_IN_SB1 = 0x3624;

    // Build 14a uses the already validated 400-byte SaveBlock1 filler immediately
    // before the RamScript structure.  This is important: its proximity lets the
    // validated multi-hotkey stage carry a one-byte BACKWARD distance instead of
    // needing a new SaveBlock resolver or a callee-saved register.
    private static final int STORAGE_IN_SB1 = 0x348C;
    private static final int STORAGE_SIZE = 400;

    // Repel (179 B) + Seed (68 B) = 247 B.  Pack them into the final 255 bytes
    // before ramScript.data.script.  The end of Seed lands exactly at +0x361C,
    // the start of struct RamScript, so neither payload overlaps live RamScript.
    static final int REPEL_SB1_OFFSET = RAMSCRIPT_SCRIPT_IN_SB1 - 0xFF; // +0x3525
    static final int REPEL_DISTANCE = 0xFF;
    static final int SEED_SB1_OFFSET = REPEL_SB1_OFFSET + 179;          // +0x35D8
    static final int SEED_DISTANCE = RAMSCRIPT_SCRIPT_IN_SB1 - SEED_SB1_OFFSET; // 0x4C

    private static final long SETUP_SCRIPT_LITERAL = 0x0300504CL;
    private static final long MULTI_LITERAL_POOL = 0x03005356L;
    private static final long OFFSET_TABLE = 0x0300539CL;

    private static final int NATIVE_CODE_AND_LITERALS_SIZE = 56;
    private static final int BLOCK_COUNT = 15;
    private static final int TABLE_SIZE = BLOCK_COUNT * 4;

    private PersistentFieldScriptHotkeyRuntime() {}

    static RamScript buildInstaller(RomProfile rom, int desiredSeed) {
        byte[] repel = RepelHotkeyPreset.buildPayload();
        byte[] seed = SeedModifierPreset.buildPayload(rom, desiredSeed);
        requireSb1Layout(repel, seed);

        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = installerHelper(rom, helperAddress, repel, seed);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, "persistent_field_hotkey_install",
                NativeHelperInstaller.Mode.AUTO
        );
        b.lockAll();
        p.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end()
                .text("ok", "Persistent Field Script modules installed.\\nSave, then install runtime.");
        return RamScript.createWonderCard(b.buildScript());
    }

    static TriggerBuildResult buildRuntime(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(rom);
        int residentBytes = totalResidentBytes(blocks);
        byte[] nativeBlob = nativeInstallerBlob(blocks, residentBytes);

        int nativeBlobOffset = 0x0C; // header/signature occupies exactly 12 bytes
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("Build 14a runtime requires " + total + " bytes");
        }

        byte[] script = new byte[total];
        int p = 0;
        script[p++] = (byte)0xB8; // setvaddress
        putU32(script, p, VIRTUAL_BASE); p += 4;
        script[p++] = (byte)0xB9; // vgoto installer
        putU32(script, p, VIRTUAL_BASE + fieldInstallerOffset); p += 4;
        if (p != HotkeyRuntimeV1.SIGNATURE_OFFSET) throw new IllegalStateException("signature offset mismatch");
        script[p++] = (byte)(HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);
        if (p != nativeBlobOffset) throw new IllegalStateException("native blob offset mismatch");
        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length); p += nativeBlob.length;
        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        RamScript rs = RamScript.createWonderCard(script);
        return new TriggerBuildResult(
                rs, EventTrigger.HOTKEY_RUNTIME, rom,
                0, total, total, RamScript.SCRIPT_SIZE - total
        );
    }

    static byte[] stage2BytesForTest() { return sb1Stage2(); }
    static byte[] wrapperBytesForTest(RomProfile rom) { return wrapper(rom); }

    private static List<RuntimeV1ResidentBlocks.Block> residentBlocks(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = new ArrayList<>();
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_TAIL,
                new byte[]{0x02,0x4B,0x18,0x47}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_LITERAL,
                le32(rom.originalVBlankThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SUPERVISOR, supervisor()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS,
                supervisorLiterals(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.STAGE2, sb1Stage2()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(SETUP_SCRIPT_LITERAL, le32(rom.scriptContextSetupThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.STAGE1, multiStage1()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.PRIMARY_THUNK,
                new byte[]{0x00,0x4C,0x20,0x47}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.FUNCTION_LITERAL,
                le32(rom.getSavedRamScriptThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.MARKER, new byte[]{0x00}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(MULTI_LITERAL_POOL, literalPool(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(OFFSET_TABLE, distanceTable()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SAFETY_GATE, multiSafetyGate()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.FORMAT_VALIDATOR, validator()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.WRAPPER, wrapper(rom)));
        if (blocks.size() != BLOCK_COUNT) throw new IllegalStateException("Build 14a block count mismatch");
        return List.copyOf(blocks);
    }

    private static byte[] wrapper(RomProfile rom) {
        // Byte-for-byte dispatch shape from validated MultiHotkeyRuntimeV1.
        // Crucially, it leaves r4 untouched. Build 14 incorrectly copied the
        // selected offset into r4 before stage1 saved it, violating the callback's
        // callee-saved-register contract and causing the observed freezes.
        byte[] out = new byte[]{
                0x06,0x48,
                0x00,0x68,
                0x00,0x00,
                0x06,(byte)0xD3,
                0x00,0x00,
                (byte)0x89,0x0F,
                0x03,(byte)0xD0,
                0x1F,(byte)0xA2,             // adr r2,0300539C distance table
                0x51,0x5C,                   // ldrb r1,[r2,r1]
                0x0D,0x4A,
                (byte)0x86,(byte)0xE0,
                0x0D,0x4B,
                0x18,0x47,
                (byte)0xC0,0x46,
                0,0,0,0
        };
        putU16(out,0x04,thumbLsrsImm(2,0,9));   // R held
        putU16(out,0x08,thumbLslsImm(1,0,13));  // B/SELECT -> index 1/2/3
        putU32(out,0x1C,rom.heldKeysRaw);
        if (out.length != 32) throw new IllegalStateException("Build 14a wrapper must be 32 bytes");
        return out;
    }

    private static byte[] multiStage1() {
        // Preserve selected one-byte distance in the stack exactly like the
        // validated MultiHotkeyRuntimeV1, while also preserving caller r4.
        return new byte[]{
                0x12,(byte)0xB5,             // push {r1,r4,lr}
                (byte)0xFE,(byte)0xF7,(byte)0x88,(byte)0xFF,
                0x00,0x28,
                0x00,(byte)0xD0,
                (byte)0x8C,(byte)0xE1,
                0x12,(byte)0xBD              // pop {r1,r4,pc}
        };
    }

    private static byte[] sb1Stage2() {
        // r0 = current RamScript script pointer. The selected module lies no more
        // than 255 bytes BEFORE it in the validated SB1 filler, so stage2 only
        // changes the validated multi-hotkey ADD into SUB. Everything else,
        // including SetupScript scheduling and r4 preservation, stays unchanged.
        return new byte[]{
                0x00,(byte)0x99,             // ldr  r1,[sp] distance
                0x40,0x1A,                   // subs r0,r0,r1
                0x05,0x4C,                   // ldr  r4,0300504C SetupScript
                (byte)0xFE,(byte)0xF7,(byte)0xAF,(byte)0xFF,
                0x27,(byte)0xE0,
                (byte)0xC0,0x46
        };
    }

    private static byte[] distanceTable() {
        // B is index 1, SELECT index 2, simultaneous is deterministic B priority.
        return new byte[]{0, (byte)REPEL_DISTANCE, (byte)SEED_DISTANCE, (byte)REPEL_DISTANCE};
    }

    private static byte[] multiSafetyGate() {
        return new byte[]{
                0x10,0x78,0x00,0x28,0x00,(byte)0xD0,0x74,(byte)0xE7,
                0x21,(byte)0xE6,0x12,(byte)0xBD // pop {r1,r4,pc}
        };
    }

    private static byte[] validator() {
        return new byte[]{0x41,(byte)0x89,(byte)0xA7,0x29,0x47,(byte)0xD1,0x40,(byte)0xE6};
    }

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
                (byte)0xF0,(byte)0xB4,
                0x0D,(byte)0xA4,
                0x1B,(byte)0xA6,
                0x03,0x27,
                0x3F,0x06,
                0x0F,0x25,
                0x21,(byte)0x88,
                0x62,(byte)0x88,
                0x04,0x34,
                (byte)0xC9,0x19,
                0x33,0x78,
                0x0B,0x70,
                0x01,0x36,
                0x01,0x31,
                0x01,0x3A,
                (byte)0xF9,(byte)0xD1,
                0x01,0x3D,
                (byte)0xF3,(byte)0xD1,
                0x02,0x48,
                0x03,0x49,
                0x01,0x60,
                (byte)0xF0,(byte)0xBC,
                0x70,0x47,
                (byte)0xC0,0x46,
                0x50,0x35,0x00,0x03,
                0x43,0x3F,0x00,0x03
        };
        if (codeAndLiterals.length != NATIVE_CODE_AND_LITERALS_SIZE) throw new IllegalStateException("native size");
        out.writeBytes(codeAndLiterals);
        for (RuntimeV1ResidentBlocks.Block block: blocks) {
            u16(out,(int)(block.address() & 0xFFFF));
            u16(out,block.data().length);
        }
        for (RuntimeV1ResidentBlocks.Block block: blocks) out.writeBytes(block.data());
        byte[] blob=out.toByteArray();
        int expected=NATIVE_CODE_AND_LITERALS_SIZE+TABLE_SIZE+residentBytes;
        if(blob.length!=expected) throw new IllegalStateException("native blob expected "+expected+", got "+blob.length);
        return blob;
    }

    private static NativeHelper installerHelper(RomProfile rom,long address,byte[] repel,byte[] seed) {
        if(repel.length>0xFF||seed.length>0xFF) throw new IllegalStateException("Build 14a installer copy length overflow");
        // Two copy loops, then bx lr. Literals and payload data follow.
        int lit=0x38;
        int repelSrc=0x50;
        int seedSrc=align4(repelSrc+repel.length);
        byte[] code=new byte[seedSrc+seed.length];

        emitCopy(code,0x00,lit+0x00,lit+0x04,lit+0x08,repel.length,0x0C);
        emitCopy(code,0x18,lit+0x0C,lit+0x10,lit+0x14,seed.length,0x24);
        putU16(code,0x30,0x4770);
        for(int o=0x32;o<lit;o+=2) putU16(code,o,0x46C0);

        putU32(code,lit+0x00,rom.saveBlock1Ptr);
        putU32(code,lit+0x04,REPEL_SB1_OFFSET);
        putU32(code,lit+0x08,address+repelSrc);
        putU32(code,lit+0x0C,rom.saveBlock1Ptr);
        putU32(code,lit+0x10,SEED_SB1_OFFSET);
        putU32(code,lit+0x14,address+seedSrc);

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

    private static void requireSb1Layout(byte[] repel, byte[] seed) {
        int storageEnd = STORAGE_IN_SB1 + STORAGE_SIZE;
        if (REPEL_SB1_OFFSET < STORAGE_IN_SB1) throw new IllegalStateException("repel starts before validated SB1 storage");
        if (REPEL_SB1_OFFSET + repel.length != SEED_SB1_OFFSET) throw new IllegalStateException("unexpected repel size/layout");
        if (SEED_SB1_OFFSET + seed.length > storageEnd) throw new IllegalArgumentException("Seed exceeds validated SB1 storage");
        if (SEED_SB1_OFFSET + seed.length > RAMSCRIPT_SCRIPT_IN_SB1 - 8) throw new IllegalArgumentException("payloads overlap RamScript structure");
        if (REPEL_DISTANCE > 0xFF || SEED_DISTANCE > 0xFF) throw new IllegalStateException("SB1 distances must fit u8");
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
