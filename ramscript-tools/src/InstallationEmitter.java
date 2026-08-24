import java.util.ArrayList;
import java.util.List;

/* Converts an InstallationPlan into real RamScripts. This is the first layer
   that consumes the automatic planner and emits executable installation WCs. */
final class InstallationEmitter {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    private InstallationEmitter() {}

    static EmittedInstallation emit(InstallationPlan plan, int seed) {
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        if (plan.localOnly()) {
            RamScript local = CompositionArtifactBuilder.buildLocal(plan.composition(), seed);
            return new EmittedInstallation(List.of(new EmittedStage("local", local)), null);
        }

        CompositionArtifactBuilder.Build artifacts = CompositionArtifactBuilder.build(plan.composition(), seed);
        List<EmittedStage> persistent = new ArrayList<>();
        for (InstallationStage stage : plan.persistentStages()) {
            List<CopySpec> copies = new ArrayList<>();
            for (InstallationChunk chunk : stage.chunks()) {
                byte[] whole = artifacts.component(chunk.componentId());
                InstallationWrite write = findWrite(plan, chunk.componentId());
                int sourceOffset = chunk.offset() - write.offset();
                if (sourceOffset < 0 || sourceOffset + chunk.size() > whole.length) {
                    throw new IllegalStateException("chunk outside component: " + chunk.componentId());
                }
                byte[] data = new byte[chunk.size()];
                System.arraycopy(whole, sourceOffset, data, 0, chunk.size());
                copies.add(new CopySpec(chunk.target(), chunk.offset(), data));
            }
            BuiltInstaller built = buildInstaller(plan.composition().rom(), copies,
                    "planned_install_" + stage.index(),
                    "Installation stage complete.\\nSave, then continue.");
            if (built.scriptBytes() != stage.encodedInstallerBytes()) {
                throw new IllegalStateException("planner/emitter size mismatch for stage " + stage.index() +
                        ": planned " + stage.encodedInstallerBytes() + ", emitted " + built.scriptBytes());
            }
            persistent.add(new EmittedStage("install-" + stage.index(), built.ramScript()));
        }

        RamScript runtime = null;
        if (plan.runtimeStageRequired()) {
            if (artifacts.runtime() == null) throw new IllegalStateException("runtime stage planned but no runtime materialized");
            runtime = artifacts.runtime().ramScript();
            if (artifacts.runtime().totalScriptBytes() != plan.runtimeStageBytes()) {
                throw new IllegalStateException("planned/emitted runtime size mismatch");
            }
        }
        return new EmittedInstallation(List.copyOf(persistent), runtime);
    }

    private static InstallationWrite findWrite(InstallationPlan plan, String componentId) {
        for (InstallationWrite write : plan.writes()) if (write.componentId().equals(componentId)) return write;
        throw new IllegalArgumentException("missing logical write for " + componentId);
    }

    private static BuiltInstaller buildInstaller(RomProfile rom, List<CopySpec> copies, String id, String message) {
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = buildBatchCopyHelper(rom, helperAddress, copies);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, id, NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        install.installAndCall(b);
        b.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end().text("ok", message);
        byte[] script = b.buildScript();
        return new BuiltInstaller(RamScript.createWonderCard(script), script.length);
    }

    private record CopySpec(InstallationTarget target, int offset, byte[] data) {
        CopySpec {
            if (target == null || offset < 0 || data == null || data.length == 0 || data.length > 0xFF) throw new IllegalArgumentException("invalid copy spec");
            data = data.clone();
        }
        @Override public byte[] data() { return data.clone(); }
    }

    private static NativeHelper buildBatchCopyHelper(RomProfile rom, long address, List<CopySpec> copies) {
        final int loopSize=24, codeEnd=copies.size()*loopSize, bxOffset=codeEnd, literalOffset=align4(bxOffset+2), literalsSize=copies.size()*12;
        int dataOffset=align4(literalOffset+literalsSize); int[] srcOffsets=new int[copies.size()];
        for(int i=0;i<copies.size();i++){srcOffsets[i]=dataOffset;dataOffset=align4(dataOffset+copies.get(i).data().length);} byte[] code=new byte[dataOffset];
        for(int i=0;i<copies.size();i++){int base=i*loopSize,lit=literalOffset+i*12,loop=base+0x0C;emitCopy(code,base,lit,lit+4,lit+8,copies.get(i).data().length,loop);putU32(code,lit,copies.get(i).target()==InstallationTarget.SAVE_BLOCK1?rom.saveBlock1Ptr:rom.saveBlock2Ptr);putU32(code,lit+4,copies.get(i).offset());putU32(code,lit+8,address+srcOffsets[i]);System.arraycopy(copies.get(i).data(),0,code,srcOffsets[i],copies.get(i).data().length);} putU16(code,bxOffset,0x4770);for(int p=bxOffset+2;p<literalOffset;p+=2)putU16(code,p,0x46C0);return new NativeHelper(address,code);
    }
    private static void emitCopy(byte[]c,int base,int ptrLit,int offLit,int srcLit,int len,int loop){putU16(c,base,ldrLiteral(0,base,ptrLit));putU16(c,base+2,0x6800);putU16(c,base+4,ldrLiteral(1,base+4,offLit));putU16(c,base+6,0x1840);putU16(c,base+8,ldrLiteral(1,base+8,srcLit));putU16(c,base+10,0x2200|len);putU16(c,base+12,0x780B);putU16(c,base+14,0x7003);putU16(c,base+16,0x3101);putU16(c,base+18,0x3001);putU16(c,base+20,0x3A01);putU16(c,base+22,branchCond(1,base+22,loop));}
    private static int align4(int n){return(n+3)&~3;} private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3,d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);} private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);} private static void putU16(byte[]b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);} private static void putU32(byte[]b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}

    private record BuiltInstaller(RamScript ramScript, int scriptBytes) {}
    record EmittedStage(String name, RamScript ramScript) {}
    record EmittedInstallation(List<EmittedStage> persistentStages, RamScript runtime) {
        EmittedInstallation { persistentStages = List.copyOf(persistentStages); }
    }
}
