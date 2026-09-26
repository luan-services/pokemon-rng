final class MewCompositionProbePreset {
    private static final long VIRTUAL_BASE=0x08010000L; private static final int VAR_RESULT=0x800D;
    private MewCompositionProbePreset(){}
    static RamScript installerA(RomProfile rom){
        byte[] man=MewCompositionProbe.manifest(true,false,false), a=MewCompositionProbe.moduleA(rom); byte[] image=new byte[0x20+a.length];System.arraycopy(man,0,image,0,man.length);System.arraycopy(a,0,image,0x20,a.length);
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);b.setVAddress().vGoto("afterraw");int pad=(4-(b.position()&3))&3;b.padding(pad);int raw=b.position();b.raw(image).label("afterraw");
        long copier=rom.stringVar4+0x100L,ha=CpuSetNativeHelperInstaller.helperDestination(copier);NativeHelper h=MewCompositionTransportNative.copyAt(rom,ha,VIRTUAL_BASE+raw,image.length,false,MewCompositionProbe.SB2_BASE,MewCompositionProbe.MAGIC,0);NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier,"mew_comp_a",NativeHelperInstaller.Mode.AUTO);
        b.lockAll().setVar(VAR_RESULT,0);p.installAndCall(b);b.compareVarToValue(VAR_RESULT,1).vGotoIfEqual("ok").vMessage("bad").waitMessage().waitButtonPress().releaseAll().end().label("ok").vMessage("good").waitMessage().waitButtonPress().releaseAll().end().text("bad","Mew composition A failed.").text("good","Mew module A installed.\\nSave, then install module B.");return RamScript.createWonderCard(b.buildScript());
    }
    static RamScript installerB(RomProfile rom){
        // Recovery probe for the A image already installed by the previous WC.
        // Intentionally does not use InstallationManifest/ready state.
        // 1) copy B to live SB1+0x348C; 2) call A directly from live SB2+0xB40;
        // 3) A resolves live SB1 and calls B; success is 0xCAFE.
        byte[] data=MewCompositionProbe.moduleB(rom); long first=u32(data,0);
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("afterraw"); int pad=(4-(b.position()&3))&3; b.padding(pad);
        int raw=b.position(); b.raw(data).label("afterraw");
        long copier=rom.stringVar4+0x100L, ha=CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper copy=MewCompositionTransportNative.copyAt(rom,ha,VIRTUAL_BASE+raw,data.length,true,MewCompositionProbe.B_OFFSET,first,0);
        NativeHelperInstaller.Plan cp=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,copy,copier,"mew_comp_bcopy2",NativeHelperInstaller.Mode.AUTO);
        NativeHelper call=MewCompositionTransportNative.directExistingAAt(rom,ha);
        NativeHelperInstaller.Plan ap=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,call,copier,"mew_comp_calla2",NativeHelperInstaller.Mode.AUTO);
        b.lockAll().setVar(VAR_RESULT,0); cp.installAndCall(b);
        b.compareVarToValue(VAR_RESULT,1).vGotoIfEqual("calla").vGoto("copybad");
        b.label("calla").setVar(VAR_RESULT,0); ap.installAndCall(b);
        b.compareVarToValue(VAR_RESULT,0xCAFE).vGotoIfEqual("ok").vGoto("callbad");
        b.label("copybad").vMessage("copybadm").waitMessage().waitButtonPress().releaseAll().end();
        b.label("callbad").vMessage("callbadm").waitMessage().waitButtonPress().releaseAll().end();
        b.label("ok").vMessage("good").waitMessage().waitButtonPress().releaseAll().end();
        b.text("copybadm","Module B copy failed.")
         .text("callbadm","B copied, but A to B call failed.")
         .text("good","A and B called each other successfully!");
        return RamScript.createWonderCard(b.buildScript());
    }
    static RamScript launcher(RomProfile rom){RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);b.setVAddress();long copier=rom.stringVar4+0x100L,ha=CpuSetNativeHelperInstaller.helperDestination(copier);NativeHelper h=MewCompositionTransportNative.launcherAt(rom,ha);NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier,"mew_comp_launch",NativeHelperInstaller.Mode.AUTO);b.lockAll().setVar(VAR_RESULT,0);p.installAndCall(b);return RamScript.createWonderCard(b.compareVarToValue(VAR_RESULT,0xCAFE).vGotoIfEqual("ok").vMessage("bad").waitMessage().waitButtonPress().releaseAll().end().label("ok").vMessage("good").waitMessage().waitButtonPress().releaseAll().end().text("bad","Composition call failed.").text("good","SB2 A called SB1 B and returned!").buildScript());}
    private static NativeHelper readyHelper(RomProfile rom,long addr){byte[]c=new byte[0x30];ldr(c,0,0,0x20);MewCompositionProbe.put16(c,2,0x6800);ldr(c,4,1,0x24);MewCompositionProbe.put16(c,6,0x1840);MewCompositionProbe.put16(c,8,0x6801);ldr(c,10,2,0x28);MewCompositionProbe.put16(c,12,0x4291);MewCompositionProbe.put16(c,14,0xD103);MewCompositionProbe.put16(c,16,0x2101);MewCompositionProbe.put16(c,18,0x7141);MewCompositionProbe.put16(c,20,0x2001);MewCompositionProbe.put16(c,22,0xE000);MewCompositionProbe.put16(c,24,0x2000);ldr(c,26,1,0x2C);MewCompositionProbe.put16(c,28,0x8008);MewCompositionProbe.put16(c,30,0x4770);MewCompositionProbe.put32(c,0x20,rom.saveBlock2Ptr);MewCompositionProbe.put32(c,0x24,MewCompositionProbe.MANIFEST);MewCompositionProbe.put32(c,0x28,MewCompositionProbe.MAGIC);MewCompositionProbe.put32(c,0x2C,rom.specialVarResult);return new NativeHelper(addr,c);}
    private static void ldr(byte[]a,int at,int r,int target){int pc=(at+4)&~3,d=target-pc;MewCompositionProbe.put16(a,at,0x4800|(r<<8)|(d/4));}private static long u32(byte[]a,int p){return Integer.toUnsignedLong((a[p]&255)|((a[p+1]&255)<<8)|((a[p+2]&255)<<16)|((a[p+3]&255)<<24));}
}
