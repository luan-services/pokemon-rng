/* Build 9 runtime proof: modules physically live in different SaveBlocks and
   one dispatcher/launcher WC resolves both from the same catalog.

   This is opt-in research for persistent deployment. The normal single-preset
   RamScript commands remain unchanged.
*/
final class PersistentCrossAreaDispatcherPreset {
    private static final long VIRTUAL_BASE=0x08010000L;
    private static final int VAR_RESULT=0x800D;
    private static final int VAR_8004=0x8004;
    private static final int VAR_8005=0x8005;
    private static final int STRING_VAR_1=0;

    private PersistentCrossAreaDispatcherPreset(){}
    private static long copier(RomProfile r){return r.stringVar4+0x100L;}
    private static long helper(RomProfile r){return CpuSetNativeHelperInstaller.helperDestination(copier(r));}

    static RamScript buildInstaller(RomProfile rom){
        NativeHelper h=PersistentToolkitStorageV5NativeHelper.buildInstallerAt(rom,helper(rom));
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);b.setVAddress();
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier(rom),"cross_area_install",NativeHelperInstaller.Mode.AUTO);
        b.lockAll();p.installAndCall(b);
        return RamScript.createWonderCard(b.vMessage("m").waitMessage().waitButtonPress().releaseAll().end()
                .text("m","Cross-area modules installed.\\nSave before launching them.").buildScript());
    }

    static RamScript buildLauncher(RomProfile rom){
        NativeHelper h=PersistentToolkitStorageV5NativeHelper.buildDispatcherAt(rom,helper(rom));
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);b.setVAddress();
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier(rom),"cross_area_dispatch",NativeHelperInstaller.Mode.AUTO);
        b.lockAll().setVar(VAR_RESULT,0).setVar(VAR_8004,0).setVar(VAR_8005,PersistentShowSecretIdModule.MODULE_ID);
        p.installAndCall(b);
        b.compareVarToValue(VAR_RESULT,PersistentShowSecretIdModule.SUCCESS_VALUE).vGotoIfNotEqual("bad");
        b.setVar(VAR_RESULT,0).setVar(VAR_8005,PersistentDispatcherProofModule.MODULE_ID)
                .callNative(h.thumbEntryAddress())
                .compareVarToValue(VAR_RESULT,PersistentDispatcherProofModule.SUCCESS_VALUE).vGotoIfNotEqual("bad")
                .bufferNumberString(STRING_VAR_1,VAR_8004)
                .vMessage("good").waitMessage().waitButtonPress().releaseAll().end()
                .label("bad").vMessage("badmsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("good","Cross-area dispatcher OK!\\nSID module: {STR_VAR_1}.")
                .text("badmsg","Cross-area module missing or invalid.");
        return RamScript.createWonderCard(b.buildScript());
    }
}
