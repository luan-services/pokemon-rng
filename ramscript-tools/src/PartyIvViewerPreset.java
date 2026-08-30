final class PartyIvViewerPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;

    private PartyIvViewerPreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        return build(rom, Hotkey.DEFAULT);
    }

    static TriggerBuildResult build(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                buildPayload(rom),
                hotkey
        );
    }

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = PartyMonDataNativeHelper.buildAt(rom, helperAddress);

        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();

        NativeHelperInstaller.Plan installPlan = NativeHelperInstaller.prepare(
                builder,
                VIRTUAL_BASE,
                helper,
                copierAddress,
                "party_iv",
                NativeHelperInstaller.Mode.AUTO
        );

        builder.lockAll();
        installPlan.installAndCall(builder);

        // The helper has already built one continuous Gen III text stream for
        // the whole party. CHAR_PROMPT_CLEAR (0xFB / "\\p") keeps the stock
        // field message box open, displays the normal down-arrow, clears the
        // contents after A/B, and continues with the next page. After the final
        // EOS, waitButtonPressStrict keeps the final normal textbox visible until
        // A/B instead of closing immediately after the last character renders.
        builder
                .message(PartyMonDataNativeHelper.dynamicMessageAddress(rom))
                .waitMessage()
                .waitButtonPressStrict()
                .releaseAll()
                .end();

        return builder.buildScript();
    }

    static NativeHelperInstaller.Mode selectedInstallerMode(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100L;
        NativeHelper helper = PartyMonDataNativeHelper.buildAt(
                rom,
                CpuSetNativeHelperInstaller.helperDestination(copierAddress)
        );
        // The real preset calls setvaddress first, so AUTO evaluates from position 5.
        return NativeHelperInstaller.chooseMode(5, helper, copierAddress);
    }

    static int payloadSize(RomProfile rom) {
        return buildPayload(rom).length;
    }
}
