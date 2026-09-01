/* Shared persistent Field Script for the BOX 14 Seed Modifier.

   This intentionally preserves the already GAME-VALIDATED standalone timing
   model: RFU is normalized first, the helper is staged before the prompt, and
   after A the only seed-setting work is one stock callnative into the staged
   72-byte helper. No parsed seed or other scratch state is kept across frames.
*/
final class PersistentBox14SeedBridge {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int SPECIAL_CLOSE_LINK = 0x001F;
    private static final long WIRELESS_COMM_TYPE_LG10 = 0x03003F3CL;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_MODULE_ID = 0x8005;

    private PersistentBox14SeedBridge() {}

    static byte[] build(RomProfile rom, long sharedServiceVirtualTarget) {
        if (rom != RomProfile.LEAF_GREEN_EN_10) {
            throw new IllegalArgumentException("BOX 14 Seed Modifier is currently profiled only for lg10");
        }
        long staging = rom.stringVar4 + 0x140L;

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress()
                .lockAll()
                .special(SPECIAL_CLOSE_LINK)
                .writeBytes(WIRELESS_COMM_TYPE_LG10, new byte[] { 0 })
                .setVar(VAR_RESULT, 0)
                .setVar(VAR_MODULE_ID, PersistentBox14SeedModule.MODULE_ID)
                .vCallAddress(sharedServiceVirtualTarget)
                // shared service changes relocation state; restore this SB2 script
                .setVAddressHere()
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfNotEqual("native_fail")
                .vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .callNative(staging | 1L)
                .releaseAll()
                .end()
                .label("native_fail")
                .vMessage("native_bad")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("message", "Press A to set BOX 14 seed.")
                .text("native_bad", "BOX 14 seed module invalid.");
        return b.buildScript();
    }

    static int fieldScriptSize(RomProfile rom) {
        return build(rom, VIRTUAL_BASE).length;
    }
}
