/* Production-facing Run Anywhere shared-preset payload.

   The hotkey Field Script lives in the final Shared Runtime RamScript while
   the 54-byte post-map callback is copied to the game-validated fixed EWRAM
   tail at 02022B08. No SB1/SB2 storage is consumed by this preset itself.
*/
final class RunAnywhereSharedPreset {
    static final Hotkey HOTKEY = new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT);
    static final int SE_TOGGLE_CLICK = 0x0066;

    private RunAnywhereSharedPreset() {}

    static byte[] buildLocalTogglePayload(RomProfile rom, int ramScriptOffset) {
        long scriptVirtualBase = HotkeyRuntimeV1.VIRTUAL_BASE + Integer.toUnsignedLong(ramScriptOffset);
        long flagsAddress = rom.mapHeader + RunAnywhereNativeHelper.ALLOW_RUNNING_OFFSET;
        RamScriptBuilder b = new RamScriptBuilder(scriptVirtualBase);
        b.setVAddress();

        b.comparePtrToValue(RunAnywhereHotkeyRuntimeV1.STATE_ADDRESS, 0)
                .vGotoIfEqual("turn_on");

        b.writeBytes(RunAnywhereHotkeyRuntimeV1.STATE_ADDRESS, new byte[] {0})
                .comparePtrToValue(RunAnywhereHotkeyRuntimeV1.ORIGINAL_ADDRESS, 0)
                .vGotoIfEqual("restore_zero")
                .writeBytes(flagsAddress, new byte[] {2})
                .vGoto("click");

        b.label("restore_zero")
                .writeBytes(flagsAddress, new byte[] {0})
                .vGoto("click");

        b.label("turn_on")
                .comparePtrToValue(flagsAddress, 0)
                .vGotoIfEqual("capture_zero")
                .writeBytes(RunAnywhereHotkeyRuntimeV1.ORIGINAL_ADDRESS, new byte[] {2})
                .vGoto("arm");

        b.label("capture_zero")
                .writeBytes(RunAnywhereHotkeyRuntimeV1.ORIGINAL_ADDRESS, new byte[] {0});

        b.label("arm")
                .writeBytes(RunAnywhereHotkeyRuntimeV1.STATE_ADDRESS, new byte[] {1})
                .writeBytes(flagsAddress, new byte[] {2});

        b.label("click")
                .playSe(SE_TOGGLE_CLICK)
                .waitSe()
                .end();
        return b.buildScript();
    }

    static int localPayloadSize(RomProfile rom) {
        return buildLocalTogglePayload(rom, 0x40).length;
    }

    static SharedHotkeyRuntime.ResidentSidecar residentSidecar(RomProfile rom) {
        return new SharedHotkeyRuntime.ResidentSidecar(
                RunAnywhereHotkeyRuntimeV1.SIDECAR_ADDRESS,
                RunAnywhereHotkeyRuntimeV1.CALLBACK_THUMB,
                RunAnywhereHotkeyRuntimeV1.sidecar(rom)
        );
    }
}
