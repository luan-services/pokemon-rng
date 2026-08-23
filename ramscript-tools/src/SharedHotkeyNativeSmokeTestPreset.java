import java.util.ArrayList;
import java.util.List;

/*
   Build 20 LAB: shared N-hotkey runtime + persistent native SID module.

   IWRAM remains shared hotkey infrastructure only.
   SB1 remains gateway-only.
   SB2 contains:
     - a tiny V6-compatible catalog + persistent SID THUMB module;
     - Seed Modifier Field Script;
     - Repel Field Script;
     - SID bridge Field Script.

   The SID bridge runs deferred as normal Field Script. Only then it stages the
   already Build-10-validated full native dispatcher in gStringVar4 scratch and
   calls it. No live IWRAM code is rewritten.
*/
final class SharedHotkeyNativeSmokeTestPreset {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_8004 = 0x8004;
    private static final int VAR_8005 = 0x8005;
    private static final int STRING_VAR_1 = 0;

    private static final int CATALOG_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset();
    private static final int CATALOG_SIZE = 0x40;
    private static final int SID_NATIVE_REL = 0x20;

    private static final int SEED_OFFSET = CATALOG_OFFSET + CATALOG_SIZE;

    private SharedHotkeyNativeSmokeTestPreset() {}

    static Layout layout(RomProfile rom, int seed) {
        byte[] seedBody = SeedModifierPreset.buildPayload(rom, seed);
        byte[] repelBody = RepelHotkeyPreset.buildPayload();
        byte[] sidBridge = buildSidBridge(rom);

        int repelOffset = align4(SEED_OFFSET + seedBody.length);
        int sidBridgeOffset = align4(repelOffset + repelBody.length);
        int end = sidBridgeOffset + sidBridge.length;
        int sb2End = PayloadStorageArea.SAVE_BLOCK2.offset() + PayloadStorageArea.SAVE_BLOCK2.capacity();
        if (end > sb2End) {
            throw new IllegalStateException(String.format(
                    "Build 20 SB2 overflow: end 0x%04X > 0x%04X", end, sb2End));
        }

        int gwSeed = 0x3612;
        int gwRepel = 0x3608;
        int gwSid = 0x35FE;

        return new Layout(seedBody, repelBody, sidBridge, SEED_OFFSET, repelOffset, sidBridgeOffset,
                gwSeed, gwRepel, gwSid, end - CATALOG_OFFSET);
    }

    static RamScript buildInstallerA(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<CopySpec> copies = new ArrayList<>();
        copies.add(new CopySpec(false, CATALOG_OFFSET, buildSidCatalog(rom)));
        copies.add(new CopySpec(true, l.gatewaySeed(), gatewayFor(l.seedOffset(), l.gatewaySeed())));
        copies.add(new CopySpec(true, l.gatewayRepel(), gatewayFor(l.repelOffset(), l.gatewayRepel())));
        copies.add(new CopySpec(true, l.gatewaySid(), gatewayFor(l.sidBridgeOffset(), l.gatewaySid())));
        addChunked(copies, false, l.seedOffset(), l.seedBody());
        addChunked(copies, false, l.repelOffset(), l.repelBody());
        return buildInstallerForCopies(rom, copies, "shared_native_smoke_install_a",
                "Native smoke A installed.\\nSave, then install part B.");
    }

    static RamScript buildInstallerB(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<CopySpec> copies = new ArrayList<>();
        addChunked(copies, false, l.sidBridgeOffset(), l.sidBridge());
        return buildInstallerForCopies(rom, copies, "shared_native_smoke_install_b",
                "Native smoke B installed.\\nSave, then install runtime.");
    }

    private static RamScript buildInstallerForCopies(RomProfile rom, List<CopySpec> copies, String id, String message) {
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = buildBatchCopyHelper(rom, helperAddress, copies);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, id, NativeHelperInstaller.Mode.AUTO
        );
        b.lockAll();
        install.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end().text("ok", message);
        return RamScript.createWonderCard(b.buildScript());
    }

    static TriggerBuildResult buildRuntime(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<SharedHotkeyDispatcher.Entry> entries = List.of(
                new SharedHotkeyDispatcher.Entry(HotkeyButton.SELECT, gatewayDelta(l.gatewaySeed())),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.B, gatewayDelta(l.gatewayRepel())),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.A, gatewayDelta(l.gatewaySid()))
        );
        return SharedHotkeyRuntimeCandidate.compose(rom, HotkeyButton.R, entries);
    }

    static String report(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        return String.format(
                "  catalog/native SID SB2+0x%04X..0x%04X (%d B)%n" +
                "  seed               SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  repel              SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  SID bridge         SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  SB2 used/free:     %d / %d B%n",
                CATALOG_OFFSET, CATALOG_OFFSET + CATALOG_SIZE - 1, CATALOG_SIZE,
                l.seedOffset(), l.seedBody().length, l.gatewaySeed(),
                l.repelOffset(), l.repelBody().length, l.gatewayRepel(),
                l.sidBridgeOffset(), l.sidBridge().length, l.gatewaySid(),
                l.sb2Used(), PayloadStorageArea.SAVE_BLOCK2.capacity() - l.sb2Used()
        );
    }

    private static byte[] buildSidBridge(RomProfile rom) {
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper dispatcher = PersistentToolkitStorageV6NativeHelper.buildDispatcherAt(rom, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, dispatcher, copier, "shared_native_sid_dispatch", NativeHelperInstaller.Mode.AUTO
        );

        b.lockAll()
                .setVar(VAR_RESULT, 0)
                .setVar(VAR_8004, 0)
                .setVar(VAR_8005, PersistentShowSecretIdModule.MODULE_ID);
        install.installAndCall(b);
        return b.compareVarToValue(VAR_RESULT, PersistentShowSecretIdModule.SUCCESS_VALUE)
                .vGotoIfNotEqual("bad_path")
                .bufferNumberString(STRING_VAR_1, VAR_8004)
                .vMessage("good")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .label("bad_path")
                .vMessage("badmsg")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("good", "SID persistent native: {STR_VAR_1}.")
                .text("badmsg", "Persistent SID module invalid.")
                .buildScript();
    }

    private static byte[] buildSidCatalog(RomProfile rom) {
        byte[] sid = PersistentShowSecretIdModule.payload(rom).bytes();
        if (sid.length != 0x20) throw new IllegalStateException("unexpected SID native size");
        byte[] image = new byte[CATALOG_SIZE];
        PersistentToolkitStorageV2.putU32(image, 0, PersistentToolkitStorageV6.MAGIC);
        image[4] = (byte) PersistentToolkitStorageV6.VERSION;
        image[5] = 1;
        PersistentToolkitStorageV2.putU16(image, 6, PersistentToolkitStorageV6.HEADER_SIZE);
        PersistentToolkitStorageV2.putU16(image, 8, image.length);
        int e = PersistentToolkitStorageV6.ENTRY_1;
        PersistentToolkitStorageV2.putU16(image, e, PersistentShowSecretIdModule.MODULE_ID);
        image[e + 2] = (byte) PersistentModule.KIND_THUMB;
        image[e + 3] = 2; // SaveBlock2
        PersistentToolkitStorageV2.putU16(image, e + 4, SID_NATIVE_REL);
        PersistentToolkitStorageV2.putU16(image, e + 6, sid.length);
        PersistentToolkitStorageV2.putU16(image, e + 8, PersistentToolkitStorageV2.checksum16(sid));
        PersistentToolkitStorageV2.putU16(image, e + 0x0A, 1);
        System.arraycopy(sid, 0, image, SID_NATIVE_REL, sid.length);
        return image;
    }

    private static byte[] gatewayFor(int sb2TargetOffset, int gatewayOffset) {
        // Same static-address delta used by PersistentFieldScriptGatewayRuntime.
        long staticSb1 = 0x0202552CL;
        long staticSb2 = 0x02024588L;
        long entryStatic = staticSb1 + gatewayOffset;
        long targetStatic = staticSb2 + sb2TargetOffset;
        long delta = targetStatic - entryStatic;
        long virtualTarget = (VIRTUAL_BASE + delta) & 0xFFFFFFFFL;
        byte[] out = new byte[10];
        out[0] = (byte)0xB8; // setvaddress
        putU32(out, 1, VIRTUAL_BASE);
        out[5] = (byte)0xB9; // vgoto
        putU32(out, 6, virtualTarget);
        return out;
    }

    private static int gatewayDelta(int gatewayOffset) {
        int ramScriptOffset = 0x3624;
        return gatewayOffset - ramScriptOffset;
    }

    private static void addChunked(List<CopySpec> out, boolean sb1, int offset, byte[] data) {
        int p = 0;
        while (p < data.length) {
            int n = Math.min(0xFF, data.length - p);
            byte[] chunk = new byte[n];
            System.arraycopy(data, p, chunk, 0, n);
            out.add(new CopySpec(sb1, offset + p, chunk));
            p += n;
        }
    }

    record Layout(byte[] seedBody, byte[] repelBody, byte[] sidBridge,
                  int seedOffset, int repelOffset, int sidBridgeOffset,
                  int gatewaySeed, int gatewayRepel, int gatewaySid, int sb2Used) {
        Layout {
            seedBody = seedBody.clone(); repelBody = repelBody.clone(); sidBridge = sidBridge.clone();
        }
        @Override public byte[] seedBody(){return seedBody.clone();}
        @Override public byte[] repelBody(){return repelBody.clone();}
        @Override public byte[] sidBridge(){return sidBridge.clone();}
    }

    private record CopySpec(boolean saveBlock1, int offset, byte[] data) {
        CopySpec {
            if (data == null || data.length == 0 || data.length > 0xFF) throw new IllegalArgumentException("copy size");
            data = data.clone();
        }
        @Override public byte[] data(){return data.clone();}
    }

    private static NativeHelper buildBatchCopyHelper(RomProfile rom, long address, List<CopySpec> copies) {
        final int loopSize = 24;
        final int codeEnd = copies.size() * loopSize;
        final int bxOffset = codeEnd;
        final int literalOffset = align4(bxOffset + 2);
        final int literalsSize = copies.size() * 12;
        int dataOffset = align4(literalOffset + literalsSize);
        int[] srcOffsets = new int[copies.size()];
        for (int i=0;i<copies.size();i++) { srcOffsets[i]=dataOffset; dataOffset=align4(dataOffset+copies.get(i).data().length); }
        byte[] code = new byte[dataOffset];
        for (int i=0;i<copies.size();i++) {
            int base=i*loopSize, lit=literalOffset+i*12, loop=base+0x0C;
            emitCopy(code,base,lit,lit+4,lit+8,copies.get(i).data().length,loop);
            putU32(code,lit,copies.get(i).saveBlock1()?rom.saveBlock1Ptr:rom.saveBlock2Ptr);
            putU32(code,lit+4,copies.get(i).offset());
            putU32(code,lit+8,address+srcOffsets[i]);
            System.arraycopy(copies.get(i).data(),0,code,srcOffsets[i],copies.get(i).data().length);
        }
        putU16(code,bxOffset,0x4770);
        for(int p=bxOffset+2;p<literalOffset;p+=2)putU16(code,p,0x46C0);
        return new NativeHelper(address,code);
    }

    private static void emitCopy(byte[] c,int base,int ptrLit,int offLit,int srcLit,int len,int loop){
        putU16(c,base+0x00,ldrLiteral(0,base+0x00,ptrLit)); putU16(c,base+0x02,0x6800);
        putU16(c,base+0x04,ldrLiteral(1,base+0x04,offLit)); putU16(c,base+0x06,0x1840);
        putU16(c,base+0x08,ldrLiteral(1,base+0x08,srcLit)); putU16(c,base+0x0A,0x2200|len);
        putU16(c,base+0x0C,0x780B); putU16(c,base+0x0E,0x7003); putU16(c,base+0x10,0x3101);
        putU16(c,base+0x12,0x3001); putU16(c,base+0x14,0x3A01); putU16(c,base+0x16,branchCond(1,base+0x16,loop));
    }
    private static int align4(int n){return(n+3)&~3;}
    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3,d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);}
    private static void putU16(byte[]b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[]b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}
}
