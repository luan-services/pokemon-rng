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
        return buildScriptInternal(rom, 0, false);
    }

    /* SharedHotkeyRuntime may pack this Field Script at a non-zero RamScript
       offset. setvaddress must then use the virtual address of the actual
       opcode location, otherwise every v* pointer would be shifted by the
       packing offset. */
    static byte[] buildScriptAtOffset(RomProfile rom, int ramScriptOffset) {
        return buildScriptInternal(rom, ramScriptOffset, true);
    }

    private static byte[] buildScriptInternal(RomProfile rom, int ramScriptOffset, boolean strictButtonWait) {
        if (ramScriptOffset < 0) throw new IllegalArgumentException("ramScriptOffset must be >= 0");
        NativeHelper helper = strictButtonWait
                ? SecretIdNativeHelper.buildAt(rom, rom.stringVar4 + 0x100L)
                : SecretIdNativeHelper.build(rom);

        RamScriptBuilder builder = new RamScriptBuilder((VIRTUAL_BASE + Integer.toUnsignedLong(ramScriptOffset)) & 0xFFFF_FFFFL);
        builder
                .setVAddress()
                .lockAll();

        helper.installAndCall(builder);

        builder
                .bufferNumberString(STRING_VAR_1, VAR_RESULT)
                .vMessage("message")
                .waitMessage();
        if (strictButtonWait) builder.waitButtonPressStrict();
        else builder.waitButtonPress();
        return builder
                .releaseAll()
                .end()
                .text("message", "Your Secret ID is {STR_VAR_1}.")
                .buildScript();
    }

    static int payloadSize(RomProfile rom) {
        return buildScript(rom).length;
    }

    static int sharedLocalPayloadSize(RomProfile rom) {
        return buildScriptAtOffset(rom, 0).length;
    }
}
