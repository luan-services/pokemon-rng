/*
   Diagnostic/prototype preset for the Run Anywhere research.

   This intentionally does NOT claim persistent behavior across map loads. It
   proves the smallest safe primitive first: set gMapHeader.allowRunning for
   the currently loaded map without patching ROM or reserving resident IWRAM.
*/
final class RunAnywhereProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;

    private RunAnywhereProbePreset() {}

    static RamScript build(RomProfile rom) {
        return RamScript.createWonderCard(buildScript(rom));
    }

    static byte[] buildScript(RomProfile rom) {
        NativeHelper helper = RunAnywhereNativeHelper.build(rom);
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress().lockAll();
        helper.installAndCall(builder);
        return builder
                .vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("message", "Running enabled for this map.")
                .buildScript();
    }

    static int payloadSize(RomProfile rom) {
        return buildScript(rom).length;
    }
}
