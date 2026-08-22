final class PersistentToolkitStorageV3Preset {
    private static final long VIRTUAL_BASE=0x08010000L;
    private static final int VAR_RESULT=0x800D;
    private PersistentToolkitStorageV3Preset(){}
    private static long copier(RomProfile r){return r.stringVar4+0x100L;}
    private static long helper(RomProfile r){return CpuSetNativeHelperInstaller.helperDestination(copier(r));}

    static RamScript buildInstallerA(RomProfile rom){ return installer(rom, PersistentToolkitStorageV3NativeHelper.buildInstallerAAt(rom,helper(rom)), "Module 1 installed.\\nSave before installing module 2.", "pts_v3_a"); }
    static RamScript buildInstallerB(RomProfile rom){ return installer(rom, PersistentToolkitStorageV3NativeHelper.buildInstallerBAt(rom,helper(rom)), "Module 2 added.\\nModule 1 was preserved.", "pts_v3_b"); }
    private static RamScript installer(RomProfile rom,NativeHelper h,String msg,String id){
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE); b.setVAddress();
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier(rom),id,NativeHelperInstaller.Mode.AUTO);
        b.lockAll(); p.installAndCall(b);
        return RamScript.createWonderCard(b.vMessage("m").waitMessage().waitButtonPress().releaseAll().end().text("m",msg).buildScript());
    }
    static RamScript buildLauncher(RomProfile rom,int id){
        if(id!=1&&id!=2) throw new IllegalArgumentException("module id must be 1 or 2");
        NativeHelper h=PersistentToolkitStorageV3NativeHelper.buildLauncherAt(rom,helper(rom),id);
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE); b.setVAddress();
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier(rom),"pts_v3_launch_"+id,NativeHelperInstaller.Mode.AUTO);
        b.lockAll(); b.setVar(VAR_RESULT,0); p.installAndCall(b);
        return RamScript.createWonderCard(b.compareVarToValue(VAR_RESULT,id).vGotoIfEqual("ok").vMessage("bad").waitMessage().waitButtonPress().releaseAll().end().label("ok").vMessage("good").waitMessage().waitButtonPress().releaseAll().end().text("bad","Persistent module missing or invalid.").text("good","Persistent module "+id+" executed!\\nIncremental install succeeded.").buildScript());
    }
}
