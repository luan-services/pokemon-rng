final class ShowSecretIdPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_RESULT = 0x800D;
    private static final int STRING_VAR_1 = 0;

    private ShowSecretIdPreset() {}

    static RamScript build(RomProfile rom) {
        return buildDeliveryman(rom).ramScript();
    }

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildScript(rom));
    }

    static TriggerBuildResult buildHotkey(RomProfile rom) {
        return buildHotkey(rom, new Hotkey(HotkeyButton.R, HotkeyButton.START));
    }

    static TriggerBuildResult buildHotkey(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(EventTrigger.HOTKEY_RUNTIME, rom, buildScript(rom), hotkey);
    }

    static byte[] buildScript(RomProfile rom) {
        NativeHelper helper = SecretIdNativeHelper.build(rom);

        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder
                .setVAddress()
                .lockAll();

        helper.installAndCall(builder);

        return builder
                .bufferNumberString(STRING_VAR_1, VAR_RESULT)
                .vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("message", "Your Secret ID is {STR_VAR_1}.")
                .buildScript();
    }

    static int payloadSize(RomProfile rom) {
        return buildScript(rom).length;
    }
}
