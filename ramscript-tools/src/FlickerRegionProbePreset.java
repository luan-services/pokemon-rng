final class FlickerRegionProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;

    private FlickerRegionProbePreset() {}

    static RamScript build(RomProfile rom) {
        long copierAddress = FlickerRegionProbeNativeHelper.copierStagingAddress(rom);
        NativeHelper helper = FlickerRegionProbeNativeHelper.build(rom);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().lockAll();

        // Follow the same production staging path used by Mute Music: raw helper
        // embedded in the relocatable Field Script, tiny CpuSet copier staged in
        // gStringVar4, then callnative into the copied helper.
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b,
                VIRTUAL_BASE,
                helper,
                copierAddress,
                "flicker_ewram_probe",
                NativeHelperInstaller.Mode.AUTO
        );
        install.installAndCall(b);

        return RamScript.createWonderCard(b
                .compareVarToValue(VAR_RESULT, FlickerRegionProbeNativeHelper.RESULT_OK)
                .vGotoIfEqual("ok")
                .compareVarToValue(VAR_RESULT, FlickerRegionProbeNativeHelper.RESULT_FAILED)
                .vGotoIfEqual("failed")
                .vMessage("installed").waitMessage().waitButtonPress().releaseAll().end()
                .label("ok")
                .vMessage("okMsg").waitMessage().waitButtonPress().releaseAll().end()
                .label("failed")
                .vMessage("failedMsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("installed", "EWRAM probe installed.\\nKeep this game session running.")
                .text("okMsg", "EWRAM probe OK!\\nAll 68 bytes survived.")
                .text("failedMsg", "EWRAM probe FAILED.\\nPattern changed; not repaired.")
                .buildScript());
    }
}
