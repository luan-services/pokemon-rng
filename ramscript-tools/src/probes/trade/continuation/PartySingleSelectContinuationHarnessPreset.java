/*
   Build 45 diagnostic harness.

   Proves the boundary that Build 44a could not:
     saved RamScript -> stock single-mon Party menu -> SaveBlock relocation
     -> stable IWRAM continuation -> newly resolved RamScript.

   Deliberately contains no evolution logic.
*/
final class PartySingleSelectContinuationHarnessPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_SELECTED = 0x8004;
    private static final int VAR_PROBE = 0x8005;
    private static final int VAR_COPY = 0x8006;
    private static final int PROBE_BEFORE = 0x4501;
    private static final int PROBE_AFTER = 0x4502;

    private PartySingleSelectContinuationHarnessPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45 single-select continuation harness currently supports fr10 only");

        long launcherAddress = rom.stringVar4 + 0x100L;
        byte[] callback = PartyContinuationRuntime.callback(rom);
        byte[] callbackLiterals = PartyContinuationRuntime.callbackLiterals(rom);
        byte[] launcher = PartyContinuationRuntime.launcher(rom, launcherAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);

        // Keep +0x0C as a fixed continuation entry, matching the relocation-safe
        // convention already used by the validated runtime family.
        b.setVAddress()
         .vGoto("installer")
         .raw(new byte[] { 0x45, 0x01 });

        if (b.position() != PartyContinuationRuntime.CONTINUATION_OFFSET)
            throw new IllegalStateException("Build 45 continuation must begin at RamScript+0x0C");

        b.label("continuation")
         .fadeScreen(0) // FADE_FROM_BLACK
         .setVar(VAR_PROBE, PROBE_AFTER)
         .copyVar(VAR_COPY, VAR_SELECTED)
         .releaseAll()
         .end();

        b.label("installer")
         .setVar(VAR_PROBE, PROBE_BEFORE)

         // Dedicated build: reuse the known 32-byte wrapper slot and the exact
         // literal gap already validated by the shared hotkey runtime. No
         // callback1/VBlank/supervisor is installed.
         .writeBytes(PartyContinuationRuntime.CALLBACK, callback)
         .writeBytes(PartyContinuationRuntime.LITERAL_GET_RAM_SCRIPT, callbackLiterals)

         // Temporary launcher is consumed before the Party menu can reuse its
         // EWRAM scratch area, so it does not need to survive the transition.
         .writeBytes(launcherAddress, launcher)

         // fadescreen itself waits for the palette operation to finish.
         .fadeScreen(1) // FADE_TO_BLACK
         .callNative(launcherAddress | 1L)

         // This old physical ScriptContext pointer is expected to become stale.
         // The IWRAM gFieldCallback2 replaces the context after relocation.
         .waitState()
         .end();

        return b.buildScript();
    }
}
