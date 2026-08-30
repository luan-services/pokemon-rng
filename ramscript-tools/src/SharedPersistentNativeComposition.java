import java.util.ArrayList;
import java.util.List;

/* Validated shared persistent-native composition baseline (Build 28).
   R+SELECT -> Seed Modifier (pure Field Script)
   R+B      -> Repel (pure Field Script)
   R+A      -> Party IV Viewer (persistent native module)
   R+START  -> Show Secret ID (persistent native module)

   Party IV and SID share one staging service in the RamScript package and one
   EWRAM staging destination. Only one module is staged/executed at a time.
*/
final class SharedPersistentNativeComposition {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int CATALOG_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset();
    private static final int VAR_RESULT = 0x800D;
    private static final int BINDING_COUNT = 4;
    private static final long STATIC_SB1 = 0x0202552CL;
    private static final long STATIC_SB2 = 0x02024588L;
    private static final int RAMSCRIPT_OFFSET_IN_SB1 = 0x3624;

    private SharedPersistentNativeComposition() {}

    static Layout layout(RomProfile rom, int seed) {
        byte[] catalog = buildNativeCatalog(rom);
        byte[] seedBody = SeedModifierPreset.buildPayload(rom, seed);
        byte[] repelBody = RepelHotkeyPreset.buildPayload();

        int cursor = CATALOG_OFFSET + catalog.length;
        int seedOffset = cursor;
        cursor += seedBody.length;
        int repelOffset = cursor;
        cursor += repelBody.length;

        int serviceOffset = sharedNativeServiceOffset();

        int partyBridgeOffset = cursor;
        long partyServiceTarget = virtualTargetFromSb2ToRamScript(partyBridgeOffset, serviceOffset);
        byte[] partyBridge = buildPartyBridge(rom, partyServiceTarget).fieldScript();
        cursor += partyBridge.length;

        int sidBridgeOffset = cursor;
        long sidServiceTarget = virtualTargetFromSb2ToRamScript(sidBridgeOffset, serviceOffset);
        byte[] sidBridge = buildSidBridge(rom, sidServiceTarget).fieldScript();
        cursor += sidBridge.length;

        int end = CATALOG_OFFSET + PayloadStorageArea.SAVE_BLOCK2.capacity();
        if (cursor > end) {
            throw new IllegalArgumentException("shared persistent-native composition does not fit SB2: needs " +
                    (cursor - CATALOG_OFFSET) + "/" + PayloadStorageArea.SAVE_BLOCK2.capacity() + " bytes");
        }

        int gwSeed = 0x3612;
        int gwRepel = 0x3608;
        int gwParty = 0x35FE;
        int gwSid = 0x35F4;
        return new Layout(catalog, seedBody, repelBody, partyBridge, sidBridge,
                seedOffset, repelOffset, partyBridgeOffset, sidBridgeOffset,
                gwSeed, gwRepel, gwParty, gwSid, cursor - CATALOG_OFFSET);
    }

    static RamScript buildInstallerA(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<CopySpec> copies = new ArrayList<>();
        addChunked(copies, false, CATALOG_OFFSET, l.catalog());
        return buildInstallerForCopies(rom, copies, "dual_native_shared_install_a",
                "Party IV + SID natives installed.\\nSave, then install part B.");
    }

    static RamScript buildInstallerB(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<CopySpec> copies = new ArrayList<>();
        copies.add(new CopySpec(true, l.gatewaySeed(), gatewayFor(l.seedOffset(), l.gatewaySeed())));
        copies.add(new CopySpec(true, l.gatewayRepel(), gatewayFor(l.repelOffset(), l.gatewayRepel())));
        copies.add(new CopySpec(true, l.gatewayParty(), gatewayFor(l.partyBridgeOffset(), l.gatewayParty())));
        copies.add(new CopySpec(true, l.gatewaySid(), gatewayFor(l.sidBridgeOffset(), l.gatewaySid())));
        addChunked(copies, false, l.seedOffset(), l.seedBody());
        addChunked(copies, false, l.repelOffset(), l.repelBody());
        return buildInstallerForCopies(rom, copies, "dual_native_shared_install_b",
                "Field modules installed.\\nSave, then install part C.");
    }

    static RamScript buildInstallerC(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<CopySpec> copies = new ArrayList<>();
        addChunked(copies, false, l.partyBridgeOffset(), l.partyBridge());
        addChunked(copies, false, l.sidBridgeOffset(), l.sidBridge());
        return buildInstallerForCopies(rom, copies, "dual_native_shared_install_c",
                "Native bridges installed.\\nSave, then install runtime.");
    }

    static TriggerBuildResult buildRuntime(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        List<SharedHotkeyDispatcher.Entry> entries = List.of(
                new SharedHotkeyDispatcher.Entry(HotkeyButton.SELECT, gatewayDelta(l.gatewaySeed())),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.B, gatewayDelta(l.gatewayRepel())),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.A, gatewayDelta(l.gatewayParty())),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.START, gatewayDelta(l.gatewaySid()))
        );
        int serviceOffset = sharedNativeServiceOffset();
        SharedPersistentNativeStagingService.Build service = SharedPersistentNativeStagingService.build(
                rom, serviceOffset, rom.stringVar4 + 0x140L, 0x140);
        return SharedHotkeyRuntime.compose(
                rom, HotkeyButton.R, entries, service.fieldScript(), service.requiredBaseAlignment());
    }

    static String report(RomProfile rom, int seed) {
        Layout l = layout(rom, seed);
        return String.format(
                "  native catalog Party IV+SID SB2+0x%04X..0x%04X (%d B; modules %d + %d B)%n" +
                "  seed                       SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  repel                      SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  Party IV bridge            SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  SID bridge                 SB2+0x%04X, %d B -> gateway SB1+0x%04X%n" +
                "  SB2 used/free:             %d / %d B%n",
                CATALOG_OFFSET, CATALOG_OFFSET + l.catalog().length - 1, l.catalog().length,
                PersistentPartyIvViewerModule.payload(rom).length, PersistentSecretIdModule.payload(rom).length,
                l.seedOffset(), l.seedBody().length, l.gatewaySeed(),
                l.repelOffset(), l.repelBody().length, l.gatewayRepel(),
                l.partyBridgeOffset(), l.partyBridge().length, l.gatewayParty(),
                l.sidBridgeOffset(), l.sidBridge().length, l.gatewaySid(),
                l.sb2Used(), PayloadStorageArea.SAVE_BLOCK2.capacity() - l.sb2Used());
    }

    private static PersistentNativeCallBridge.Build buildPartyBridge(RomProfile rom, long sharedServiceVirtualTarget) {
        return PersistentNativeCallBridge.buildViaSharedStagingService(
                rom, PersistentPartyIvViewerModule.MODULE_ID, sharedServiceVirtualTarget,
                rom.stringVar4 + 0x140L, b -> {},
                b -> b.message(PartyMonDataNativeHelper.dynamicMessageAddress(rom))
                        .waitMessage().waitButtonPressStrict().releaseAll().end(),
                b -> b.vMessage("party_bad").waitMessage().waitButtonPress().releaseAll().end()
                        .text("party_bad", "Persistent Party IV module invalid.")
        );
    }

    private static PersistentNativeCallBridge.Build buildSidBridge(RomProfile rom, long sharedServiceVirtualTarget) {
        return PersistentNativeCallBridge.buildViaSharedStagingService(
                rom, PersistentSecretIdModule.MODULE_ID, sharedServiceVirtualTarget,
                rom.stringVar4 + 0x140L, b -> {},
                b -> b.bufferNumberString(0, VAR_RESULT)
                        .vMessage("sid_msg").waitMessage().waitButtonPressStrict().releaseAll().end()
                        .text("sid_msg", "Your Secret ID is {STR_VAR_1}."),
                b -> b.vMessage("sid_bad").waitMessage().waitButtonPressStrict().releaseAll().end()
                        .text("sid_bad", "Persistent SID module invalid.")
        );
    }

    private static byte[] buildNativeCatalog(RomProfile rom) {
        return PersistentNativeModuleCatalog.build(
                CATALOG_OFFSET,
                List.of(
                        new PersistentNativeModuleSpec(PersistentPartyIvViewerModule.MODULE_ID,
                                PersistentPartyIvViewerModule.payload(rom)),
                        new PersistentNativeModuleSpec(PersistentSecretIdModule.MODULE_ID,
                                PersistentSecretIdModule.payload(rom))
                )
        ).bytes();
    }

    private static int sharedNativeServiceOffset() {
        return SharedPersistentNativeStagingService.offsetForBindings(BINDING_COUNT, 4);
    }

    private static long virtualTargetFromSb2ToRamScript(int sb2CallerOffset, int ramScriptTargetOffset) {
        long callerStatic = STATIC_SB2 + sb2CallerOffset;
        long targetStatic = STATIC_SB1 + RAMSCRIPT_OFFSET_IN_SB1 + ramScriptTargetOffset;
        long delta = targetStatic - callerStatic;
        return (VIRTUAL_BASE + delta) & 0xFFFF_FFFFL;
    }

    private static byte[] gatewayFor(int sb2TargetOffset, int gatewayOffset) {
        long entryStatic = STATIC_SB1 + gatewayOffset;
        long targetStatic = STATIC_SB2 + sb2TargetOffset;
        long delta = targetStatic - entryStatic;
        long virtualTarget = (VIRTUAL_BASE + delta) & 0xFFFFFFFFL;
        byte[] out = new byte[10];
        out[0] = (byte)0xB8;
        putU32(out, 1, VIRTUAL_BASE);
        out[5] = (byte)0xB9;
        putU32(out, 6, virtualTarget);
        return out;
    }

    private static int gatewayDelta(int gatewayOffset) { return gatewayOffset - 0x3624; }


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

    record Layout(byte[] catalog, byte[] seedBody, byte[] repelBody, byte[] partyBridge, byte[] sidBridge,
                  int seedOffset, int repelOffset, int partyBridgeOffset, int sidBridgeOffset,
                  int gatewaySeed, int gatewayRepel, int gatewayParty, int gatewaySid, int sb2Used) {
        Layout {
            catalog=catalog.clone(); seedBody=seedBody.clone(); repelBody=repelBody.clone();
            partyBridge=partyBridge.clone(); sidBridge=sidBridge.clone();
        }
        @Override public byte[] catalog(){return catalog.clone();}
        @Override public byte[] seedBody(){return seedBody.clone();}
        @Override public byte[] repelBody(){return repelBody.clone();}
        @Override public byte[] partyBridge(){return partyBridge.clone();}
        @Override public byte[] sidBridge(){return sidBridge.clone();}
    }

    private record CopySpec(boolean saveBlock1, int offset, byte[] data) {
        CopySpec { if (data == null || data.length == 0 || data.length > 0xFF) throw new IllegalArgumentException("copy size"); data=data.clone(); }
        @Override public byte[] data(){return data.clone();}
    }

    private static NativeHelper buildBatchCopyHelper(RomProfile rom, long address, List<CopySpec> copies) {
        final int loopSize=24, codeEnd=copies.size()*loopSize, bxOffset=codeEnd, literalOffset=align4(bxOffset+2), literalsSize=copies.size()*12;
        int dataOffset=align4(literalOffset+literalsSize); int[] srcOffsets=new int[copies.size()];
        for(int i=0;i<copies.size();i++){srcOffsets[i]=dataOffset;dataOffset=align4(dataOffset+copies.get(i).data().length);} byte[] code=new byte[dataOffset];
        for(int i=0;i<copies.size();i++){int base=i*loopSize,lit=literalOffset+i*12,loop=base+0x0C;emitCopy(code,base,lit,lit+4,lit+8,copies.get(i).data().length,loop);putU32(code,lit,copies.get(i).saveBlock1()?rom.saveBlock1Ptr:rom.saveBlock2Ptr);putU32(code,lit+4,copies.get(i).offset());putU32(code,lit+8,address+srcOffsets[i]);System.arraycopy(copies.get(i).data(),0,code,srcOffsets[i],copies.get(i).data().length);} putU16(code,bxOffset,0x4770);for(int p=bxOffset+2;p<literalOffset;p+=2)putU16(code,p,0x46C0);return new NativeHelper(address,code);
    }
    private static void emitCopy(byte[]c,int base,int ptrLit,int offLit,int srcLit,int len,int loop){putU16(c,base,ldrLiteral(0,base,ptrLit));putU16(c,base+2,0x6800);putU16(c,base+4,ldrLiteral(1,base+4,offLit));putU16(c,base+6,0x1840);putU16(c,base+8,ldrLiteral(1,base+8,srcLit));putU16(c,base+10,0x2200|len);putU16(c,base+12,0x780B);putU16(c,base+14,0x7003);putU16(c,base+16,0x3101);putU16(c,base+18,0x3001);putU16(c,base+20,0x3A01);putU16(c,base+22,branchCond(1,base+22,loop));}
    private static int align4(int n){return(n+3)&~3;} private static int align(int n,int a){if(a<=1)return n;return(n+(a-1))&~(a-1);} private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3,d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);} private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);} private static void putU16(byte[]b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);} private static void putU32(byte[]b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}
}
