/*
   Build 47c: complete two-boundary evolution-scene probe.

   Boundary 1 (Party -> field) uses the validated IWRAM resolver callback.
   Boundary 2 (EvolutionScene -> field) reuses that same 32-byte callback,
   patched only after its first invocation has finished.

   SPECIES_NONE is now handled before BeginEvolutionScene, so selecting a
   non-trade-evolving Pokemon can no longer corrupt the party.
*/
final class TradeEvolutionSceneProbe47cPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_PROBE = 0x8005;
    private static final int VAR_SLOT_COPY = 0x8006;
    private static final int VAR_TARGET = 0x8007;

    private static final int PROBE_BEFORE_PARTY = 0x47C1;
    private static final int PROBE_BEFORE_EVO = 0x47C2;
    private static final int PROBE_AFTER_EVO = 0x47C3;
    private static final int PROBE_NO_EVOLUTION = 0x47C0;
    private static final int PROBE_CANCELLED = 0x47CC;

    private TradeEvolutionSceneProbe47cPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 47c evolution-scene probe currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        long evoHelperAddress = rom.stringVar4 + 0x180L;
        long evoCopierAddress = evoHelperAddress - CpuSetNativeHelperInstaller.HELPER_DESTINATION_DELTA;
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("installer").raw(new byte[] {0x47, (byte)0xB0});

        // The second IWRAM continuation enters here after EvolutionScene has
        // returned through CB2_ReturnToField and SaveBlock1 has moved again.
        int postEvolutionOffset = b.position();
        b.label("post_evolution")
         .setVAddressHere()
         .setVar(VAR_PROBE, PROBE_AFTER_EVO)
         .releaseAll()
         .end();

        b.label("no_evolution")
         .setVar(VAR_PROBE, PROBE_NO_EVOLUTION)
         .releaseAll()
         .end();

        b.label("cancelled")
         .setVar(VAR_PROBE, PROBE_CANCELLED)
         .releaseAll()
         .end();

        // The first IWRAM continuation (Party -> field) enters here.
        int preEvolutionOffset = b.position();
        b.label("pre_evolution")
         .setVAddressHere()
         .copyVar(VAR_SLOT_COPY, VAR_SELECTED)
         // SLOT_CANCEL is 0xFF. Guard every value >= PARTY_SIZE before native
         // code computes &gPlayerParty[slot].
         .compareVarToValue(VAR_SELECTED, 6)
         .vGotoIf(RamScriptBuilder.COND_GE, "cancelled")
         .setVar(VAR_PROBE, PROBE_BEFORE_EVO);

        NativeHelper evoHelper = TradeEvolutionSceneProbe47bHelper.buildAt(
                rom, evoHelperAddress, postEvolutionOffset);
        NativeHelperInstaller.Plan evoInstall = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, evoHelper, evoCopierAddress, "trade_evo_47c",
                NativeHelperInstaller.Mode.AUTO);
        evoInstall.installAndCall(b);

        b.compareVarToValue(VAR_TARGET, 0)
         .vGotoIfEqual("no_evolution")
         // A real scene has switched callback2. The stale RamScript context is
         // intentionally stopped; the second IWRAM callback will replace it.
         .waitState()
         .end();

        byte[] firstCallback = PartyContinuationRuntime.callback(rom, preEvolutionOffset);
        b.label("installer")
         .setVar(VAR_PROBE, PROBE_BEFORE_PARTY)
         .writeBytes(PartyContinuationRuntime.CALLBACK, firstCallback)
         .writeBytes(PartyContinuationRuntime.LITERAL_GET_RAM_SCRIPT, callbackLiterals)
         .writeBytes(launcherAddress, launcher)
         .fadeScreen(1)
         .callNative(launcherAddress | 1L)
         .waitState()
         .end();

        byte[] result = b.buildScript();
        if (postEvolutionOffset > 0xFF || preEvolutionOffset > 0xFF)
            throw new IllegalStateException("Build 47b continuation offsets exceed Thumb immediate range");
        return result;
    }
}
