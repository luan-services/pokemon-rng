final class PersistenceProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;

    private PersistenceProbePreset() {}

    static RamScript buildInstaller(RomProfile rom) {
        NativeHelper helper = PersistenceProbeNativeHelper.buildWriter(rom);
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress().lockAll();
        helper.installAndCall(builder);
        return RamScript.createWonderCard(builder
                .vMessage("message")
                .waitMessage().waitButtonPress().releaseAll().end()
                .text("message", "Persistence marker installed.\\nSave the game normally, then reset.")
                .buildScript());
    }

    static RamScript buildChecker(RomProfile rom) {
        NativeHelper helper = PersistenceProbeNativeHelper.buildChecker(rom);
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress().lockAll();
        helper.installAndCall(builder);
        return RamScript.createWonderCard(builder
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfEqual("found")
                .vMessage("missing").waitMessage().waitButtonPress().releaseAll().end()
                .label("found")
                .vMessage("foundMsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("missing", "Persistence marker NOT found.")
                .text("foundMsg", "Persistence marker found!\\nSaveBlock1 storage survived.")
                .buildScript());
    }
}
