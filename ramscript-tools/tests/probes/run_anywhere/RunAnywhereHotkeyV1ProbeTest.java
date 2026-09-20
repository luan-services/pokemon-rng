import java.util.Arrays;

public final class RunAnywhereHotkeyV1ProbeTest {
    public static void main(String[] args) {
        for (RomProfile rom : RomProfile.values()) {
            byte[] sidecar = RunAnywhereHotkeyRuntimeV1.sidecar(rom);
            check(sidecar.length == RunAnywhereHotkeyRuntimeV1.SIDECAR_CODE_SIZE, "sidecar code size changed");
            check(sidecar.length <= RunAnywhereHotkeyRuntimeV1.SIDECAR_RESERVED_SIZE, "sidecar exceeds validated 68-byte region");
            check(sidecar[RunAnywhereHotkeyRuntimeV1.STATE_OFFSET] == 0, "state must start disabled");
            check(sidecar[RunAnywhereHotkeyRuntimeV1.ORIGINAL_OFFSET] == 0, "captured stock bit must start zero");

            byte[] toggle = RunAnywhereHotkeyRuntimeV1.toggleHelper(rom);
            check(toggle.length == 64, "toggle helper must stay 64 bytes");
            check(RunAnywhereHotkeyRuntimeV1.selectedToggleInstallerMode(rom) == NativeHelperInstaller.Mode.CPU_SET_BLOCK,
                    "toggle helper should use compact CpuSet staging");
            check(RunAnywhereHotkeyRuntimeV1.scriptSize(rom) <= RamScript.SCRIPT_SIZE, "runtime must fit RamScript");

            byte[] stock = HotkeyRuntimeV1.nativeInstallerBlob(rom, Hotkey.DEFAULT);
            byte[] patched = RunAnywhereHotkeyRuntimeV1.patchedNativeInstallerBlob(rom);
            check(stock.length == patched.length, "specialized installer size changed");
            int differences = 0;
            for (int i = 0; i < stock.length; i++) if (stock[i] != patched[i]) differences++;
            check(differences > 0 && differences <= 4, "only one supervisor pointer literal may change");

            byte[] stockAgain = HotkeyRuntimeV1.nativeInstallerBlob(rom, Hotkey.DEFAULT);
            check(Arrays.equals(stock, stockAgain), "frozen HotkeyRuntimeV1 blob was mutated");
            RunAnywhereHotkeyRuntimeV1.build(rom);
        }
        System.out.println("RunAnywhereHotkeyV1ProbeTest passed");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
