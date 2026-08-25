/*
   Build 48: first user-facing Deliveryman trade-evolution NPC.

   This is the stabilized Build 47c mechanism with technical markers removed
   from the normal path and short field-script dialogue added around it.

   The preset remains dedicated/exclusive: it reuses the validated 32-byte
   IWRAM continuation slot and does not coexist with the shared hotkey runtime.
*/
final class TradeEvolutionNpcPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_TARGET = 0x8007;

    private TradeEvolutionNpcPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 48 trade-evolution NPC currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        long evoHelperAddress = rom.stringVar4 + 0x180L;
        long evoCopierAddress = evoHelperAddress - CpuSetNativeHelperInstaller.HELPER_DESTINATION_DELTA;
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("installer").raw(new byte[] {0x48, 0x00});

        // EvolutionScene -> field continuation. SaveBlock1 has moved again,
        // therefore virtual-address state must be refreshed before vmessage.
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

        // Party -> field continuation.
        int preEvolutionOffset = b.position();
        b.label("pre_evolution")
         .setVAddressHere()
         .compareVarToValue(VAR_SELECTED, 6)
         .vGotoIf(RamScriptBuilder.COND_GE, "cancelled");

        NativeHelper evoHelper = TradeEvolutionSceneProbe47bHelper.buildAt(
                rom, evoHelperAddress, postEvolutionOffset);
        NativeHelperInstaller.Plan evoInstall = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, evoHelper, evoCopierAddress, "trade_evo_48",
                NativeHelperInstaller.Mode.AUTO);
        evoInstall.installAndCall(b);

        b.compareVarToValue(VAR_TARGET, 0)
         .vGotoIfEqual("no_evolution")
         .waitState()
         .end();

        byte[] firstCallback = PartyContinuationRuntime.callback(rom, preEvolutionOffset);
        b.label("installer")
         .lockAll()
         .facePlayer()
         .vMessage("ask").callStd(5)
         .compareVarToValue(VAR_RESULT, 0)
         .vGotoIfEqual("declined")
         .vMessage("choose").waitMessage().waitButtonPressStrict().closeMessage()
         .writeBytes(PartyContinuationRuntime.CALLBACK, firstCallback)
         .writeBytes(PartyContinuationRuntime.LITERAL_GET_RAM_SCRIPT, callbackLiterals)
         .writeBytes(launcherAddress, launcher)
         .fadeScreen(1)
         .callNative(launcherAddress | 1L)
         .waitState()
         .end();

        b.text("ask", "Want me to trade a POKeMON?")
         .text("choose", "Choose a POKeMON.")
         .text("no_evo", "It won't evolve by trade.")
         .text("done", "Your POKeMON evolved!")
         .text("cancel_msg", "Maybe another time.");

        byte[] result = b.buildScript();
        if (postEvolutionOffset > 0xFF || preEvolutionOffset > 0xFF)
            throw new IllegalStateException("Build 48 continuation offsets exceed Thumb immediate range");
        return result;
    }
}
