/* Build 11: persistent-module hotkey prototype.

   Safety decision:
   - DO NOT move key decoding/native dispatch into RamScript callback flow.
   - reuse the FR1.0-validated MultiHotkeyRuntimeV1 resident listener unchanged;
   - its two RamScript payloads are small Field Script bridges only;
   - each bridge re-installs a 20-byte resolver in gStringVar4 scratch on demand;
   - resolver dynamically follows gSaveBlock2Ptr and tail-jumps to the persistent
     dispatcher stored at a fixed offset inside the validated SaveBlock2 area.

   This deliberately avoids the two rejected generic-dispatch experiments that
   swallowed normal input / froze on first button press.
*/
final class PersistentHotkeyRuntimePrototype {
    static final int DISPATCHER_SB2_OFFSET = 0x200;
    static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_8004 = 0x8004;
    private static final int VAR_8005 = 0x8005;
    private static final int STRING_VAR_1 = 0;

    private PersistentHotkeyRuntimePrototype() {}

    static RamScript buildInstaller(RomProfile rom, int desiredSeed) {
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper h = buildInstallerHelper(rom, desiredSeed, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, h, copier, "persistent_hotkey_install", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        p.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end()
                .text("ok", "Persistent hotkey modules installed.\\nSave, then install runtime.");
        return RamScript.createWonderCard(b.buildScript());
    }

    static TriggerBuildResult buildRuntime(RomProfile rom, int desiredSeed) {
        byte[] sid = sidBridge(rom);
        byte[] seed = seedBridge(rom, desiredSeed);
        return MultiHotkeyRuntimeV1.compose(
                rom,
                new HotkeyPayload(new Hotkey(HotkeyButton.R, HotkeyButton.B), sid),
                new HotkeyPayload(new Hotkey(HotkeyButton.R, HotkeyButton.SELECT), seed)
        );
    }

    static byte[] installerHelperBytesForTest(RomProfile rom, int desiredSeed) {
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        return buildInstallerHelper(rom, desiredSeed, helperAddress).codeCopy();
    }

    static byte[] resolverBytes(RomProfile rom) {
        // Thumb-1: resolve current SaveBlock2 pointer every invocation, then
        // tail-jump to SB2 toolkit base + DISPATCHER_SB2_OFFSET. BX preserves LR.
        byte[] out = new byte[20];
        put16(out,0x00,0x4802); // ldr r0, =gSaveBlock2Ptr @ +0x0C
        put16(out,0x02,0x6800); // ldr r0,[r0]
        put16(out,0x04,0x4902); // ldr r1, =storage+dispatcher @ +0x10
        put16(out,0x06,0x1840); // adds r0,r0,r1
        put16(out,0x08,0x3001); // adds r0,#1 Thumb bit
        put16(out,0x0A,0x4700); // bx r0
        PersistentToolkitStorageV2.putU32(out,0x0C,rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(out,0x10,PayloadStorageArea.SAVE_BLOCK2.offset()+DISPATCHER_SB2_OFFSET);
        return out;
    }

    private static byte[] sidBridge(RomProfile rom) {
        long resolver = rom.stringVar4 + 0x140L;
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        return b.setVAddress()
                .writeBytes(resolver, resolverBytes(rom))
                .lockAll()
                .setVar(VAR_RESULT,0).setVar(VAR_8004,0).setVar(VAR_8005,PersistentShowSecretIdModule.MODULE_ID)
                .callNative(resolver | 1L)
                .compareVarToValue(VAR_RESULT,PersistentShowSecretIdModule.SUCCESS_VALUE).vGotoIfNotEqual("bad")
                .bufferNumberString(STRING_VAR_1,VAR_8004)
                .vMessage("good").waitMessage().waitButtonPress().releaseAll().end()
                .label("bad").vMessage("badmsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("good","SID: {STR_VAR_1}.")
                .text("badmsg","Persistent SID module invalid.")
                .buildScript();
    }

    private static byte[] seedBridge(RomProfile rom, int desiredSeed) {
        long resolver = rom.stringVar4 + 0x140L;
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        return b.setVAddress()
                .lockAll()
                .vMessage("prompt").waitMessage().waitButtonPress()
                // Install the resolver only after the prompt has finished. gStringVar4
                // is stock text scratch, so message expansion is allowed to reuse it.
                .writeBytes(resolver, resolverBytes(rom))
                .setVar(VAR_RESULT,0).setVar(VAR_8005,PersistentSeedModifierModule.MODULE_ID)
                .callNative(resolver | 1L)
                .compareVarToValue(VAR_RESULT,PersistentSeedModifierModule.SUCCESS_VALUE).vGotoIfNotEqual("bad")
                .releaseAll().end()
                .label("bad").vMessage("badmsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("prompt",SeedModifierPreset.message(desiredSeed))
                .text("badmsg","Persistent Seed module invalid.")
                .buildScript();
    }

    private static NativeHelper buildInstallerHelper(RomProfile rom, int desiredSeed, long address) {
        byte[] catalog = PersistentToolkitStorageV6.buildCatalogImage(rom, desiredSeed);
        byte[] sid = PersistentToolkitStorageV6.buildSaveBlock1Payload(rom);
        byte[] dispatcher = PersistentToolkitStorageV6NativeHelper.buildDispatcherAt(rom, 0x02000000L).codeCopy();
        // Dispatcher is position-independent except literals, so staging address is irrelevant.
        if (catalog.length > 0xFF || sid.length > 0xFF || dispatcher.length > 0xFF)
            throw new IllegalStateException("Build 11 compact installer length overflow");

        int lit = 0x54;
        int catSrc = 0x70;
        int sidSrc = align4(catSrc + catalog.length);
        int dispSrc = align4(sidSrc + sid.length);
        byte[] code = new byte[dispSrc + dispatcher.length];

        // copy catalog -> SB2 + 0xB20
        emitCopy(code,0x00,lit+0x00,lit+0x04,lit+0x08,catalog.length,0x0C);
        // copy SID -> SB1 + 0x348C + 0x20
        emitCopy(code,0x18,lit+0x0C,lit+0x10,lit+0x14,sid.length,0x24);
        // copy persistent dispatcher -> SB2 + 0xB20 + 0x200
        emitCopy(code,0x30,lit+0x18,lit+0x1C,lit+0x20,dispatcher.length,0x3C);
        put16(code,0x48,0x4770); // bx lr
        for(int o=0x4A;o<lit;o+=2) put16(code,o,0x46C0);

        PersistentToolkitStorageV2.putU32(code,lit+0x00,rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code,lit+0x04,PayloadStorageArea.SAVE_BLOCK2.offset());
        PersistentToolkitStorageV2.putU32(code,lit+0x08,address+catSrc);
        PersistentToolkitStorageV2.putU32(code,lit+0x0C,rom.saveBlock1Ptr);
        PersistentToolkitStorageV2.putU32(code,lit+0x10,PayloadStorageArea.SAVE_BLOCK1.offset()+PersistentToolkitStorageV6.SID_SB1_OFFSET);
        PersistentToolkitStorageV2.putU32(code,lit+0x14,address+sidSrc);
        PersistentToolkitStorageV2.putU32(code,lit+0x18,rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code,lit+0x1C,PayloadStorageArea.SAVE_BLOCK2.offset()+DISPATCHER_SB2_OFFSET);
        PersistentToolkitStorageV2.putU32(code,lit+0x20,address+dispSrc);
        System.arraycopy(catalog,0,code,catSrc,catalog.length);
        System.arraycopy(sid,0,code,sidSrc,sid.length);
        System.arraycopy(dispatcher,0,code,dispSrc,dispatcher.length);
        return new NativeHelper(address,code);
    }

    private static void emitCopy(byte[] c,int base,int ptrLit,int offLit,int srcLit,int len,int loop) {
        put16(c,base+0x00,ldrLiteral(0,base+0x00,ptrLit));
        put16(c,base+0x02,0x6800);
        put16(c,base+0x04,ldrLiteral(1,base+0x04,offLit));
        put16(c,base+0x06,0x1840);
        put16(c,base+0x08,ldrLiteral(1,base+0x08,srcLit));
        put16(c,base+0x0A,0x2200 | len);
        put16(c,base+0x0C,0x780B); put16(c,base+0x0E,0x7003);
        put16(c,base+0x10,0x3101); put16(c,base+0x12,0x3001); put16(c,base+0x14,0x3A01);
        put16(c,base+0x16,branchCond(1,base+0x16,loop));
    }
    private static int align4(int n){return(n+3)&~3;}
    private static void put16(byte[]b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3;int d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);}
}
