/* Build 10 validation: two real preset modules, one per persistent area,
   launched through the same location-aware dispatcher.

   This remains opt-in. The original simple RamScript presets are untouched.
*/
final class PersistentRealPresetDispatcherPreset {
    private static final long VIRTUAL_BASE=0x08010000L;
    private static final int VAR_RESULT=0x800D;
    private static final int VAR_8004=0x8004;
    private static final int VAR_8005=0x8005;
    private static final int STRING_VAR_1=0;

    private PersistentRealPresetDispatcherPreset(){}
    private static long copier(RomProfile r){return r.stringVar4+0x100L;}
    private static long helper(RomProfile r){return CpuSetNativeHelperInstaller.helperDestination(copier(r));}

    static RamScript buildInstaller(RomProfile rom, int desiredSeed){
        NativeHelper h=PersistentToolkitStorageV6NativeHelper.buildInstallerAt(rom,desiredSeed,helper(rom));
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);b.setVAddress();
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier(rom),"real_modules_install",NativeHelperInstaller.Mode.AUTO);
        b.lockAll();p.installAndCall(b);
        return RamScript.createWonderCard(b.vMessage("m").waitMessage().waitButtonPress().releaseAll().end()
                .text("m","SID + Seed modules installed.\\nSave before launching them.").buildScript());
    }

    static RamScript buildLauncher(RomProfile rom, int desiredSeed){
        NativeHelper h=PersistentToolkitStorageV6NativeHelper.buildDispatcherAt(rom,helper(rom));
        RamScriptBuilder b=new RamScriptBuilder(VIRTUAL_BASE);b.setVAddress();
        NativeHelperInstaller.Plan p=NativeHelperInstaller.prepare(b,VIRTUAL_BASE,h,copier(rom),"real_modules_dispatch",NativeHelperInstaller.Mode.AUTO);

        // First real module: fetch SID from SaveBlock1 and freeze the formatted
        // string before the second module reuses special vars.
        b.lockAll().setVar(VAR_RESULT,0).setVar(VAR_8004,0).setVar(VAR_8005,PersistentShowSecretIdModule.MODULE_ID);
        p.installAndCall(b);
        b.compareVarToValue(VAR_RESULT,PersistentShowSecretIdModule.SUCCESS_VALUE).vGotoIfNotEqual("bad")
                .bufferNumberString(STRING_VAR_1,VAR_8004);

        // Second real module: write the configured RNG predecessor from
        // SaveBlock2. The next normal RNG advance produces desiredSeed.
        b.setVar(VAR_RESULT,0).setVar(VAR_8005,PersistentSeedModifierModule.MODULE_ID)
                .callNative(h.thumbEntryAddress())
                .compareVarToValue(VAR_RESULT,PersistentSeedModifierModule.SUCCESS_VALUE).vGotoIfNotEqual("bad")
                .vMessage("good").waitMessage().waitButtonPress().releaseAll().end()
                .label("bad").vMessage("badmsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("good",String.format("Real modules OK!\\nSID: {STR_VAR_1}. Seed: %04X.",desiredSeed))
                .text("badmsg","Persistent real module missing or invalid.");
        return RamScript.createWonderCard(b.buildScript());
    }
}
