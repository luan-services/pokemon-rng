/* Build 22F diagnostic.
   Requires installer A to have persisted the Party IV catalog/module.

   This deliberately removes the remaining execution-path difference from the
   validated standalone PartyIvViewerPreset:
     1. a temporary checker/loader resolves the persistent module and copies it
        byte-for-byte to gStringVar4+0x140, then RETURNS to Field Script;
     2. Field Script itself executes callnative(gStringVar4+0x141), exactly the
        entry style used by the standalone viewer;
     3. after return, the normal dynamic message is shown.

   The loader never tail-jumps into the Party IV helper. Therefore a successful
   viewer here proves that the helper is fine and that the tail-jump contract of
   the generic persistent dispatcher is the incompatible piece.
*/
final class PartyIvDirectCallDiagnostic {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_MODULE_ID = 0x8005;

    private PartyIvDirectCallDiagnostic() {}

    static RamScript build(RomProfile rom) {
        long loaderCopier = rom.stringVar4;
        long loaderAddress = CpuSetNativeHelperInstaller.helperDestination(loaderCopier); // +0x40
        long partyStage = rom.stringVar4 + 0x140L;

        NativeHelper loader = PersistentToolkitStorageV6NativeHelper.buildStagingCopyCheckAt(
                rom, loaderAddress, partyStage, PartyMonDataNativeHelper.CODE_SIZE);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, loader, loaderCopier,
                "party_iv_direct_call_loader", NativeHelperInstaller.Mode.AUTO);

        b.lockAll()
                .setVar(VAR_RESULT, 0)
                .setVar(VAR_MODULE_ID, PersistentPartyIvViewerModule.MODULE_ID);
        install.installAndCall(b);

        // Do not execute if source/staging validation failed.
        b.compareVarToValue(VAR_RESULT, 1)
                .vGotoIfNotEqual("bad");

        // Critical A/B test: this is the same direct callnative form used by
        // PartyIvViewerPreset after its helper has been staged at +0x140.
        b.callNative(partyStage | 1L)
                .message(PartyMonDataNativeHelper.dynamicMessageAddress(rom))
                .waitMessage()
                .releaseAll()
                .end()
                .label("bad")
                .vMessage("badmsg")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("badmsg", "Party IV staging validation failed.");

        return RamScript.createWonderCard(b.buildScript());
    }
}
