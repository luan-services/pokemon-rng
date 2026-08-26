/*
   Production Trade Evolution preset.

   Current UX trigger is the Mystery Gift Deliveryman. The mechanic itself is
   trigger-independent and is intentionally kept separate so a future release
   can bind it to a dedicated NPC/object script.

   Current deployment is exclusive/dedicated: it temporarily owns the same
   validated 32-byte IWRAM WRAPPER slot used by the hotkey research. It does
   not install the shared hotkey/VBlank runtime.
*/
final class TradeEvolutionPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_TARGET = 0x8007;

    private TradeEvolutionPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        long launcherAddress = rom.stringVar4 + 0x100L;
        long evoHelperAddress = rom.stringVar4 + 0x180L;
        long evoCopierAddress = evoHelperAddress - CpuSetNativeHelperInstaller.HELPER_DESTINATION_DELTA;
        byte[] callbackLiterals = TradeEvolutionContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = TradeEvolutionContinuationRuntime.launcher(rom);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("installer").raw(new byte[] {0x54, 0x45}); // "TE" marker only

        // EvolutionScene -> field continuation. SaveBlock1 relocates again.
        int postEvolutionOffset = b.position();
        b.label("post_evolution")
         .setVAddressHere()
         .vMessage("done").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.label("no_evolution")
         .vMessage("no_evo").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.label("cancelled")
         .vMessage("cancel_msg").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.label("declined")
         .vMessage("cancel_msg").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        // Party -> field continuation. Refresh virtual-address state before
        // any vgoto or raw-block installer because SaveBlock1 has moved.
        int preEvolutionOffset = b.position();
        b.label("pre_evolution")
         .setVAddressHere()
         .compareVarToValue(VAR_SELECTED, 6)
         .vGotoIf(RamScriptBuilder.COND_GE, "cancelled");

        NativeHelper evoHelper = TradeEvolutionRuntimeHelper.buildAt(rom, evoHelperAddress, postEvolutionOffset);
        NativeHelperInstaller.Plan evoInstall = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, evoHelper, evoCopierAddress, "trade_evolution",
                NativeHelperInstaller.Mode.AUTO);
        evoInstall.installAndCall(b);

        b.compareVarToValue(VAR_TARGET, 0)
         .vGotoIfEqual("no_evolution")
         .waitState()
         .end();

        byte[] firstCallback = TradeEvolutionContinuationRuntime.callback(rom, preEvolutionOffset);
        b.label("installer")
         .lockAll()
         .facePlayer()
         .vMessage("ask").callStd(5)
         .compareVarToValue(VAR_RESULT, 0)
         .vGotoIfEqual("declined")
         .closeMessage()
         .writeBytes(TradeEvolutionContinuationRuntime.CALLBACK, firstCallback)
         .writeBytes(TradeEvolutionContinuationRuntime.LITERAL_GET_RAM_SCRIPT, callbackLiterals)
         .writeBytes(launcherAddress, launcher)
         .fadeScreen(1)
         .callNative(launcherAddress | 1L)
         .waitState()
         .end();

        // Production v1 copy. The intermediate "Choose a POKéMON" prompt was
        // intentionally removed so the requested dialogue still fits in the
        // RamScript-local deployment; YES now opens the Party immediately.
        b.text("ask", "Would you lend me your POKéMON for a while?")
         .text("no_evo", "It doesn't seem special at all.")
         .text("done", "Woah! Looks like something happened.")
         .text("cancel_msg", "Okay. Maybe another time...");

        byte[] result = b.buildScript();
        if (postEvolutionOffset > 0xFF || preEvolutionOffset > 0xFF)
            throw new IllegalStateException("Trade Evolution continuation offset exceeds Thumb immediate range");
        return result;
    }

    static int payloadSize(RomProfile rom) {
        return buildPayload(rom).length;
    }
}
