final class PersistenceFullRegionProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;

    private PersistenceFullRegionProbePreset() {}

    private static long copierAddress(RomProfile rom) { return rom.stringVar4 + 0x100L; }
    private static long helperAddress(RomProfile rom) { return CpuSetNativeHelperInstaller.helperDestination(copierAddress(rom)); }

    static RamScript buildInstaller(RomProfile rom) {
        NativeHelper helper = PersistenceProbeNativeHelper.buildFullWriterAt(rom, helperAddress(rom));
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(builder, VIRTUAL_BASE, helper,
                copierAddress(rom), "persist400_write", NativeHelperInstaller.Mode.AUTO);
        builder.lockAll();
        plan.installAndCall(builder);
        return RamScript.createWonderCard(builder
                .vMessage("message").waitMessage().waitButtonPress().releaseAll().end()
                .text("message", "400-byte persistence pattern installed.\\nSave normally, then reset.")
                .buildScript());
    }

    static RamScript buildChecker(RomProfile rom) {
        NativeHelper helper = PersistenceProbeNativeHelper.buildFullCheckerAt(rom, helperAddress(rom));
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();
        NativeHelperInstaller.Plan plan = NativeHelperInstaller.prepare(builder, VIRTUAL_BASE, helper,
                copierAddress(rom), "persist400_check", NativeHelperInstaller.Mode.AUTO);
        builder.lockAll();
        plan.installAndCall(builder);
        return RamScript.createWonderCard(builder
                .compareVarToValue(VAR_RESULT, 1).vGotoIfEqual("found")
                .vMessage("missing").waitMessage().waitButtonPress().releaseAll().end()
                .label("found")
                .vMessage("foundMsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("missing", "400-byte persistence check FAILED.")
                .text("foundMsg", "Persistent storage OK!\\n400/400 bytes survived.")
                .buildScript());
    }
}
