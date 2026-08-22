import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/* Build 12: direct persistent hotkey runtime.

   Goal: connect the already-validated resident multi-hotkey listener directly
   to the already-validated persistent SaveBlock dispatcher without executing a
   temporary resolver from gStringVar4 and without a Field Script bridge.

   The resident layout does not claim any new IWRAM. It reuses exactly the same
   symbol-verified blocks as MultiHotkeyRuntimeV1. The old RamScript payload
   locator stages are repurposed:

     wrapper -> r1 = module id (0x10 / 0x11)
     stage1  -> dynamically read *gSaveBlock2Ptr into r4
     stage2  -> add toolkit dispatcher offset and BL through a tiny bx-r4 thunk
     persistent dispatcher -> catalog lookup -> SB1/SB2 module -> return

   gStringVar4 is not executable scratch in this design.
*/
final class PersistentDirectHotkeyRuntime {
    static final int DISPATCHER_SB2_OFFSET = 0x200;
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final long MULTI_LITERAL_POOL = 0x03005356L;
    private static final long DYNAMIC_PTR_LITERAL = 0x0300539CL;
    private static final long UNUSED_LITERAL_SLOT = 0x0300504CL;
    private static final int BLOCK_COUNT = 15;
    private static final int NATIVE_CODE_AND_LITERALS_SIZE = 56;
    private static final int TABLE_SIZE = BLOCK_COUNT * 4;

    private PersistentDirectHotkeyRuntime() {}

    static RamScript buildInstaller(RomProfile rom, int desiredSeed) {
        long copier = rom.stringVar4 + 0x100L; // installation scratch only; never executed by hotkeys
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper h = buildInstallerHelper(rom, desiredSeed, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, h, copier, "persistent_direct_hotkey_install", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        p.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end()
                .text("ok", "Direct hotkey modules installed.\\nSave, then install runtime.");
        return RamScript.createWonderCard(b.buildScript());
    }

    static TriggerBuildResult buildRuntime(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = residentBlocks(rom);
        int residentBytes = totalResidentBytes(blocks);
        byte[] nativeBlob = nativeInstallerBlob(blocks, residentBytes);

        // No hotkey Field Script payloads live in RamScript in Build 12.
        // Keep the historical signature at +0x0A for format continuity.
        int nativeBlobOffset = 0x0C;
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;
        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("Build 12 direct runtime requires " + total + " bytes");
        }

        byte[] script = new byte[total];
        int p = 0;
        script[p++] = (byte)0xB8;
        putU32(script,p,VIRTUAL_BASE); p += 4;
        script[p++] = (byte)0xB9;
        putU32(script,p,VIRTUAL_BASE + fieldInstallerOffset); p += 4;
        if (p != HotkeyRuntimeV1.SIGNATURE_OFFSET) throw new IllegalStateException("Build 12 signature offset");
        script[p++] = (byte)(HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);
        if (p != nativeBlobOffset) throw new IllegalStateException("Build 12 native offset");
        System.arraycopy(nativeBlob,0,script,p,nativeBlob.length); p += nativeBlob.length;
        if (p != fieldInstallerOffset) throw new IllegalStateException("Build 12 installer offset");
        System.arraycopy(fieldInstaller,0,script,p,fieldInstaller.length);

        RamScript rs = RamScript.createWonderCard(script);
        return new TriggerBuildResult(rs, EventTrigger.HOTKEY_RUNTIME, rom, 0, total, total, RamScript.SCRIPT_SIZE-total);
    }

    static byte[] stage1BytesForTest(RomProfile rom) { return stage1(rom); }
    static byte[] stage2BytesForTest() { return stage2(); }
    static byte[] wrapperBytesForTest(RomProfile rom) { return wrapper(rom); }
    static byte[] dispatcherBytesForTest(RomProfile rom) {
        return PersistentToolkitStorageV6NativeHelper.buildDispatcherFromR1At(rom,0x02000000L).codeCopy();
    }

    private static List<RuntimeV1ResidentBlocks.Block> residentBlocks(RomProfile rom) {
        List<RuntimeV1ResidentBlocks.Block> blocks = new ArrayList<>();
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_TAIL,new byte[]{0x02,0x4B,0x18,0x47}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.ORIGINAL_VBLANK_LITERAL,le32(rom.originalVBlankThumb)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SUPERVISOR,supervisor()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS,supervisorLiterals(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.STAGE2,stage2()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(UNUSED_LITERAL_SLOT,new byte[4]));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.STAGE1,stage1(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.PRIMARY_THUNK,new byte[]{0x20,0x47,(byte)0xC0,0x46})); // bx r4; nop
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.FUNCTION_LITERAL,new byte[4]));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.MARKER,new byte[]{0x00}));
        blocks.add(new RuntimeV1ResidentBlocks.Block(MULTI_LITERAL_POOL,literalPool(rom)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(DYNAMIC_PTR_LITERAL,le32(rom.saveBlock2Ptr)));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.SAFETY_GATE,multiSafetyGate()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.FORMAT_VALIDATOR,validator()));
        blocks.add(new RuntimeV1ResidentBlocks.Block(RuntimeV1ResidentBlocks.WRAPPER,wrapper(rom)));
        if (blocks.size()!=BLOCK_COUNT) throw new IllegalStateException("Build 12 block count");
        return List.copyOf(blocks);
    }

    private static byte[] wrapper(RomProfile rom) {
        // R+B (lower pressed bit) -> module 0x10; R+SELECT -> module 0x11.
        // If both new bits arrive simultaneously, SELECT/module 0x11 wins.
        byte[] out = new byte[]{
                0x06,0x48, 0x00,0x68,
                0,0, 0x06,(byte)0xD3,
                0,0, (byte)0x89,0x0F,
                0x03,(byte)0xD0,
                0x49,0x08,             // lsrs r1,r1,#1: 1->0, 2/3->1
                0x10,0x31,             // adds r1,#0x10
                0x0D,0x4A,
                (byte)0x86,(byte)0xE0,
                0x0D,0x4B, 0x18,0x47,
                (byte)0xC0,0x46,
                0,0,0,0
        };
        // held R is bit 8; extract adjacent B(bit1)/SELECT(bit2) from high half.
        putU16(out,0x04,thumbLsrsImm(2,0,HotkeyButton.R.bit()+1));
        putU16(out,0x08,thumbLslsImm(1,0,15-HotkeyButton.SELECT.bit()));
        putU32(out,0x1C,rom.heldKeysRaw);
        if(out.length!=32) throw new IllegalStateException("Build 12 wrapper size");
        return out;
    }

    private static byte[] stage1(RomProfile rom) {
        // Existing 14-byte allocation at 03005082. The saveBlock2Ptr literal is
        // stored in the already-validated 4-byte slot at 0300539C.
        return new byte[]{
                0x12,(byte)0xB5,             // push {r1,r4,lr}
                (byte)0xC5,0x48,             // ldr r0, [pc,#0x314] -> 0300539C
                0x04,0x68,                   // ldr r4,[r0] current SaveBlock2
                (byte)0xD3,(byte)0xE7,       // b 03005032 stage2
                (byte)0xC0,0x46,
                (byte)0xC0,0x46,
                0x12,(byte)0xBD              // cleanup: pop {r1,r4,pc}
        };
    }

    private static byte[] stage2() {
        // Existing 14-byte allocation at 03005032. Compute 0xD20 without a
        // literal: 0xD2 << 4 = SB2 toolkit 0xB20 + dispatcher 0x200.
        return new byte[]{
                (byte)0xD2,0x20,             // movs r0,#0xD2
                0x00,0x01,                   // lsls r0,r0,#4
                0x24,0x18,                   // adds r4,r4,r0
                0x01,0x34,                   // adds r4,#1 Thumb bit
                (byte)0xFE,(byte)0xF7,(byte)0xAD,(byte)0xFF, // bl 03003F98 (bx r4)
                0x26,(byte)0xE0              // b 0300508E cleanup
        };
    }

    private static byte[] literalPool(RomProfile rom) {
        byte[] out=new byte[10];
        putU32(out,2,rom.lockFieldControls);
        putU32(out,6,rom.cb1OverworldThumb);
        return out;
    }

    private static byte[] multiSafetyGate(){return new byte[]{0x10,0x78,0x00,0x28,0x00,(byte)0xD0,0x74,(byte)0xE7,0x21,(byte)0xE6,0x12,(byte)0xBD};}
    private static byte[] validator(){return new byte[]{0x41,(byte)0x89,(byte)0xA7,0x29,0x47,(byte)0xD1,0x40,(byte)0xE6};}
    private static byte[] supervisor(){return new byte[]{0x18,(byte)0xA3,0x07,(byte)0xCB,0x03,0x68,(byte)0x8B,0x42,(byte)0xB3,(byte)0xD1,0x02,0x60,(byte)0xB1,(byte)0xE7};}
    private static byte[] supervisorLiterals(RomProfile rom){byte[]o=new byte[12];putU32(o,0,0x030030F0L);putU32(o,4,rom.cb1OverworldThumb);putU32(o,8,RuntimeV1ResidentBlocks.WRAPPER|1L);return o;}

    private static byte[] nativeInstallerBlob(List<RuntimeV1ResidentBlocks.Block> blocks,int residentBytes){
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] code=new byte[]{
                (byte)0xF0,(byte)0xB4,0x0D,(byte)0xA4,0x1B,(byte)0xA6,0x03,0x27,0x3F,0x06,0x0F,0x25,
                0x21,(byte)0x88,0x62,(byte)0x88,0x04,0x34,(byte)0xC9,0x19,0x33,0x78,0x0B,0x70,0x01,0x36,
                0x01,0x31,0x01,0x3A,(byte)0xF9,(byte)0xD1,0x01,0x3D,(byte)0xF3,(byte)0xD1,0x02,0x48,0x03,0x49,
                0x01,0x60,(byte)0xF0,(byte)0xBC,0x70,0x47,(byte)0xC0,0x46,0x50,0x35,0x00,0x03,0x43,0x3F,0x00,0x03
        };
        if(code.length!=NATIVE_CODE_AND_LITERALS_SIZE) throw new IllegalStateException("Build 12 installer code size");
        out.writeBytes(code);
        for(RuntimeV1ResidentBlocks.Block b:blocks){u16(out,(int)(b.address()&0xFFFF));u16(out,b.data().length);}
        for(RuntimeV1ResidentBlocks.Block b:blocks)out.writeBytes(b.data());
        byte[] blob=out.toByteArray();
        if(blob.length!=NATIVE_CODE_AND_LITERALS_SIZE+TABLE_SIZE+residentBytes)throw new IllegalStateException("Build 12 blob size");
        return blob;
    }

    private static NativeHelper buildInstallerHelper(RomProfile rom,int desiredSeed,long address){
        byte[] catalog=PersistentToolkitStorageV6.buildCatalogImage(rom,desiredSeed);
        byte[] sid=PersistentToolkitStorageV6.buildSaveBlock1Payload(rom);
        byte[] dispatcher=PersistentToolkitStorageV6NativeHelper.buildDispatcherFromR1At(rom,0x02000000L).codeCopy();
        if(catalog.length>0xFF||sid.length>0xFF||dispatcher.length>0xFF)throw new IllegalStateException("Build 12 installer length overflow");
        int lit=0x54,catSrc=0x70,sidSrc=align4(catSrc+catalog.length),dispSrc=align4(sidSrc+sid.length);
        byte[] code=new byte[dispSrc+dispatcher.length];
        emitCopy(code,0x00,lit+0x00,lit+0x04,lit+0x08,catalog.length,0x0C);
        emitCopy(code,0x18,lit+0x0C,lit+0x10,lit+0x14,sid.length,0x24);
        emitCopy(code,0x30,lit+0x18,lit+0x1C,lit+0x20,dispatcher.length,0x3C);
        putU16(code,0x48,0x4770); for(int o=0x4A;o<lit;o+=2)putU16(code,o,0x46C0);
        putU32(code,lit+0x00,rom.saveBlock2Ptr);putU32(code,lit+0x04,PayloadStorageArea.SAVE_BLOCK2.offset());putU32(code,lit+0x08,address+catSrc);
        putU32(code,lit+0x0C,rom.saveBlock1Ptr);putU32(code,lit+0x10,PayloadStorageArea.SAVE_BLOCK1.offset()+PersistentToolkitStorageV6.SID_SB1_OFFSET);putU32(code,lit+0x14,address+sidSrc);
        putU32(code,lit+0x18,rom.saveBlock2Ptr);putU32(code,lit+0x1C,PayloadStorageArea.SAVE_BLOCK2.offset()+DISPATCHER_SB2_OFFSET);putU32(code,lit+0x20,address+dispSrc);
        System.arraycopy(catalog,0,code,catSrc,catalog.length);System.arraycopy(sid,0,code,sidSrc,sid.length);System.arraycopy(dispatcher,0,code,dispSrc,dispatcher.length);
        return new NativeHelper(address,code);
    }

    private static void emitCopy(byte[]c,int base,int ptrLit,int offLit,int srcLit,int len,int loop){
        putU16(c,base,ldrLiteral(0,base,ptrLit));putU16(c,base+2,0x6800);putU16(c,base+4,ldrLiteral(1,base+4,offLit));putU16(c,base+6,0x1840);
        putU16(c,base+8,ldrLiteral(1,base+8,srcLit));putU16(c,base+10,0x2200|len);putU16(c,base+12,0x780B);putU16(c,base+14,0x7003);
        putU16(c,base+16,0x3101);putU16(c,base+18,0x3001);putU16(c,base+20,0x3A01);putU16(c,base+22,branchCond(1,base+22,loop));
    }

    private static int totalResidentBytes(List<RuntimeV1ResidentBlocks.Block>b){int n=0;for(var x:b)n+=x.data().length;return n;}
    private static int align4(int n){return(n+3)&~3;}
    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3,d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);}
    private static int thumbLsrsImm(int rd,int rm,int sh){return 0x0800|(sh<<6)|(rm<<3)|rd;}
    private static int thumbLslsImm(int rd,int rm,int sh){return(sh<<6)|(rm<<3)|rd;}
    private static byte[] le32(long v){byte[]o=new byte[4];putU32(o,0,v);return o;}
    private static void u16(ByteArrayOutputStream o,int v){o.write(v&0xFF);o.write((v>>>8)&0xFF);}
    private static void putU16(byte[]b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[]b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}
}
