/*
   Build-43 trade-evolution event prototype.

   Uses the stock party picker, the stock evolution table, and the stock
   evolution scene. No persistent IWRAM/runtime is installed.
*/
final class TradeEvolutionPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int SPECIAL_CHOOSE_PARTY_MON = 0x009F;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_8004 = 0x8004;
    private static final int PARTY_SIZE = 6;

    private TradeEvolutionPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 43g trade evolution prototype currently supports fr10 only");

        long copierAddress = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = TradeEvolutionEventNativeHelper.buildAt(rom, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();

        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress, "trade_evo_event",
                NativeHelperInstaller.Mode.AUTO
        );

        b.lockAll()
         .vMessage("ask").callStd(5)
         .compareVarToValue(VAR_RESULT, 0)
         .vGotoIfEqual("decline")
         .vMessage("choose").waitMessage().waitButtonPressStrict().closeMessage();

        // Install only the temporary launcher/evolution helper.
        install.install(b);

        // This launcher uses stock InitPartyMenu indirectly and supplies the
        // stock ContinueScript callback. No manual fade or stock ChoosePartyMon.
        b.callNative(TradeEvolutionEventNativeHelper.menuEntry(helperAddress))
         .waitState()

         // The callback above resumes this exact script.
         .setVAddressHere()
         .compareVarToValue(VAR_8004, 0x00FF)
         .vGotoIfEqual("decline")
         .compareVarToValue(VAR_8004, PARTY_SIZE)
         .vGotoIf(RamScriptBuilder.COND_GE, "decline");

        // Party menu may reuse scratch EWRAM, so restore the helper before evo.
        b.setVAddressHere();
        install.install(b);

        b.callNative(TradeEvolutionEventNativeHelper.evolutionEntry(helperAddress))
         .compareVarToValue(VAR_RESULT, 0)
         .vGotoIfEqual("no_evolution")

         // BeginEvolutionScene switches callback2. Resume through stock callback.
         .waitState()
         .setVAddressHere()
         .vMessage("done").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.label("no_evolution")
         .vMessage("no_evo").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.label("decline")
         .vMessage("decline_msg").waitMessage().waitButtonPressStrict()
         .releaseAll().end();

        b.text("ask", "Lend me a POKéMON for a while?")
         .text("choose", "Which POKéMON will you lend me?")
         .text("no_evo", "Hmm... This POKéMON doesn't seem ready to change.")
         .text("done", "There you go! Take good care of your POKéMON.")
         .text("decline_msg", "All right. Maybe another time.");

        return b.buildScript();
    }

    static int payloadSize(RomProfile rom) {
        return buildPayload(rom).length;
    }
}
