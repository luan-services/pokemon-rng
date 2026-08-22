final class PersistentToolkitStoragePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;

    private PersistentToolkitStoragePreset() {}

    private static long copierAddress(RomProfile rom) { return rom.stringVar4 + 0x100L; }
    private static long helperAddress(RomProfile rom) { return CpuSetNativeHelperInstaller.helperDestination(copierAddress(rom)); }

    static RamScript buildInstaller(RomProfile rom) {
        NativeHelper helper = PersistentToolkitStorageNativeHelper.buildInstallerAt(rom, helperAddress(rom));
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress(rom), "pts_v1_install", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        p.installAndCall(b);
        return RamScript.createWonderCard(
                b.vMessage("message").waitMessage().waitButtonPress().releaseAll().end()
                        .text("message", "Persistent payload installed.\\nSave normally, then use the launcher WC.")
                        .buildScript());
    }

    static RamScript buildLauncher(RomProfile rom) {
        NativeHelper helper = PersistentToolkitStorageNativeHelper.buildLauncherAt(rom, helperAddress(rom));
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan p = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress(rom), "pts_v1_launch", NativeHelperInstaller.Mode.AUTO);
        b.lockAll();
        b.setVar(VAR_RESULT, 0);
        p.installAndCall(b);
        return RamScript.createWonderCard(
                b.compareVarToValue(VAR_RESULT, 1).vGotoIfEqual("ok")
                        .vMessage("bad").waitMessage().waitButtonPress().releaseAll().end()
                        .label("ok")
                        .vMessage("okMsg").waitMessage().waitButtonPress().releaseAll().end()
                        .text("bad", "Persistent payload missing or invalid.")
                        .text("okMsg", "Persistent payload executed!\\nCode ran from SaveBlock2 storage.")
                        .buildScript());
    }
}
