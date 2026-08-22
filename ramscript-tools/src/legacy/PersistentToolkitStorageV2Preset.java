final class PersistentToolkitStorageV2Preset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;

    private PersistentToolkitStorageV2Preset() {}
    private static long copierAddress(RomProfile rom) { return rom.stringVar4 + 0x100L; }
    private static long helperAddress(RomProfile rom) { return CpuSetNativeHelperInstaller.helperDestination(copierAddress(rom)); }

    static RamScript buildInstaller(RomProfile rom) {
        NativeHelper helper = PersistentToolkitStorageV2NativeHelper.buildInstallerAt(rom, helperAddress(rom));
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress(rom), "pts_v2_install", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        p.installAndCall(b);
        return RamScript.createWonderCard(
                b.vMessage("message").waitMessage().waitButtonPress().releaseAll().end()
                        .text("message", "Persistent modules installed.\\nSave normally, then test module 1 or 2.")
                        .buildScript());
    }

    static RamScript buildLauncher(RomProfile rom, int moduleId) {
        if (moduleId != 1 && moduleId != 2) throw new IllegalArgumentException("V2 proof module must be 1 or 2");
        NativeHelper helper = PersistentToolkitStorageV2NativeHelper.buildLauncherAt(rom, helperAddress(rom), moduleId);
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress(rom), "pts_v2_launch_" + moduleId, NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        b.setVar(VAR_RESULT, 0);
        p.installAndCall(b);
        String ok = moduleId == 1 ? "Persistent module 1 executed!" : "Persistent module 2 executed!";
        return RamScript.createWonderCard(
                b.compareVarToValue(VAR_RESULT, moduleId).vGotoIfEqual("ok")
                        .vMessage("bad").waitMessage().waitButtonPress().releaseAll().end()
                        .label("ok")
                        .vMessage("okMsg").waitMessage().waitButtonPress().releaseAll().end()
                        .text("bad", "Persistent module missing or invalid.")
                        .text("okMsg", ok + "\\nLookup by module ID succeeded.")
                        .buildScript());
    }
}
