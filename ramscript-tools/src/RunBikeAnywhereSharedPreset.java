/* Shared-local Run + Bike Anywhere payload.

   The hotkey Field Script stays in the final Runtime RamScript. It stages the
   temporary 76-byte toggle helper in gStringVar4, calls it, and plays click
   0x66. The post-map resident callback itself occupies 63/68 bytes of the same
   fixed EWRAM tail validated for Run Anywhere.
*/
final class RunBikeAnywhereSharedPreset {
    static final Hotkey HOTKEY = new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT);

    private RunBikeAnywhereSharedPreset() {}

    static byte[] buildLocalTogglePayload(RomProfile rom, int ramScriptOffset) {
        long scriptVirtualBase = HotkeyRuntimeV1.VIRTUAL_BASE + Integer.toUnsignedLong(ramScriptOffset);
        long copierAddress = rom.stringVar4 + 0x100;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = new NativeHelper(helperAddress, RunBikeAnywhereHotkeyRuntimeV1.toggleHelper(rom));

        RamScriptBuilder b = new RamScriptBuilder(scriptVirtualBase);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, scriptVirtualBase, helper, copierAddress, "run_bike_anywhere_shared_toggle",
                NativeHelperInstaller.Mode.AUTO);
        install.installAndCall(b);
        return b.playSe(RunBikeAnywhereHotkeyRuntimeV1.SE_TOGGLE_CLICK).waitSe().end().buildScript();
    }

    static int localPayloadSize(RomProfile rom) {
        return buildLocalTogglePayload(rom, 0x40).length;
    }

    static SharedHotkeyRuntime.ResidentSidecar residentSidecar(RomProfile rom) {
        return new SharedHotkeyRuntime.ResidentSidecar(
                RunBikeAnywhereHotkeyRuntimeV1.SIDECAR_ADDRESS,
                RunBikeAnywhereHotkeyRuntimeV1.CALLBACK_THUMB,
                RunBikeAnywhereHotkeyRuntimeV1.sidecar(rom));
    }
}
