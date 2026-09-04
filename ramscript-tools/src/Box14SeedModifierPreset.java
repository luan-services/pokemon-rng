/*
   Production BOX 14 convenience variant of Seed Modifier.

   The normal seed-modifier remains unchanged. This variant reads the desired
   seed from the eight-character uppercase hexadecimal name of BOX 14.

   Conservative architecture:
     - same Field Script / hotkey model as the existing preset
     - same RFU normalization sequence
     - one native helper, staged before the prompt
     - no persistent scratch state and no second helper
     - after A, callnative parses BOX 14 and writes the predecessor
*/
final class Box14SeedModifierPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int SPECIAL_CLOSE_LINK = 0x001F;
    private static final long WIRELESS_COMM_TYPE = 0x03003F3CL;

    private Box14SeedModifierPreset() {}

    static TriggerBuildResult build(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(
                EventTrigger.HOTKEY_RUNTIME, rom, buildPayload(rom), hotkey);
    }

    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = Box14SeedNativeHelper.buildAt(rom, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress, "box14_seed", NativeHelperInstaller.Mode.AUTO);

        b.lockAll()
                .special(SPECIAL_CLOSE_LINK)
                .writeBytes(WIRELESS_COMM_TYPE, new byte[] { 0 });

        // Stage the helper before showing the prompt. The only work after the
        // confirmation is the single native call itself.
        install.install(b);

        return b.vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .callNative(helper.thumbEntryAddress())
                .releaseAll()
                .end()
                .text("message", "Press A to set BOX 14 seed.")
                .buildScript();
    }

    static int payloadSize(RomProfile rom) {
        return buildPayload(rom).length;
    }
}
