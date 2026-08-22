/* Opt-in persistent deployment of Show Secret ID.

   ShowSecretIdPreset remains the normal/simple implementation. These builders
   exist only for users who explicitly choose the persistent module workflow.
*/
final class PersistentShowSecretIdPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_8004 = 0x8004;
    private static final int STRING_VAR_1 = 0;

    private PersistentShowSecretIdPreset() {}

    private static long copier(RomProfile rom) { return rom.stringVar4 + 0x100L; }
    private static long helper(RomProfile rom) { return CpuSetNativeHelperInstaller.helperDestination(copier(rom)); }

    static RamScript buildInstaller(RomProfile rom) {
        NativeHelper nativeHelper = PersistentToolkitStorageV4NativeHelper.buildInstallerAt(rom, helper(rom));
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                builder, VIRTUAL_BASE, nativeHelper, copier(rom), "show_sid_persist_install", NativeHelperInstaller.Mode.AUTO);
        builder.lockAll();
        plan.installAndCall(builder);
        return RamScript.createWonderCard(builder
                .vMessage("message").waitMessage().waitButtonPress().releaseAll().end()
                .text("message", "Persistent Secret ID module installed.\\nSave before using its launcher.")
                .buildScript());
    }

    static RamScript buildLauncher(RomProfile rom) {
        NativeHelper nativeHelper = PersistentToolkitStorageV4NativeHelper.buildLauncherAt(rom, helper(rom));
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(
                builder, VIRTUAL_BASE, nativeHelper, copier(rom), "show_sid_persist_launch", NativeHelperInstaller.Mode.AUTO);
        builder.lockAll().setVar(VAR_RESULT, 0).setVar(VAR_8004, 0);
        plan.installAndCall(builder);
        return RamScript.createWonderCard(builder
                .compareVarToValue(VAR_RESULT, PersistentShowSecretIdModule.SUCCESS_VALUE)
                .vGotoIfNotEqual("bad")
                .bufferNumberString(STRING_VAR_1, VAR_8004)
                .vMessage("good").waitMessage().waitButtonPress().releaseAll().end()
                .label("bad")
                .vMessage("bad_message").waitMessage().waitButtonPress().releaseAll().end()
                .text("good", "Persistent Secret ID: {STR_VAR_1}.")
                .text("bad_message", "Persistent Secret ID module missing or invalid.")
                .buildScript());
    }
}
