/* Build 22E diagnostic: validate SB2 -> gStringVar4+0x140 staging without
   executing the Party IV module. Requires only installer A to have been saved. */
final class PartyIvStagingDiagnostic {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_MODULE_ID = 0x8005;

    private PartyIvStagingDiagnostic() {}

    static RamScript build(RomProfile rom) {
        long copier = rom.stringVar4;
        long dispatcherAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper checker = PersistentToolkitStorageV6NativeHelper.buildStagingCopyCheckAt(
                rom, dispatcherAddress, rom.stringVar4 + 0x140L, 0x140);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, checker, copier, "party_iv_stage_check", NativeHelperInstaller.Mode.AUTO);
        b.lockAll()
                .setVar(VAR_RESULT, 0)
                .setVar(VAR_MODULE_ID, PersistentPartyIvViewerModule.MODULE_ID);
        install.installAndCall(b);
        b.compareVarToValue(VAR_RESULT, 1)
                .vGotoIfNotEqual("bad")
                .vMessage("ok")
                .waitMessage().waitButtonPress().releaseAll().end()
                .label("bad")
                .vMessage("badmsg")
                .waitMessage().waitButtonPress().releaseAll().end()
                .text("ok", "Party IV staging copy: OK.")
                .text("badmsg", "Party IV staging copy: BAD.");
        return RamScript.createWonderCard(b.buildScript());
    }
}
