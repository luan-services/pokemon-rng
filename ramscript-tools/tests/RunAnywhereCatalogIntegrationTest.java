import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public final class RunAnywhereCatalogIntegrationTest {
    private static final String BASELINE_FULL_SHARED_SHA256 =
            "2641f3b3882c3fa9faf6ae583b39a9b53f826ac671a10fe943f9cc5a8db57f8a";

    private RunAnywhereCatalogIntegrationTest() {}

    static void run() {
        try {
            RomProfile rom = RomProfile.LEAF_GREEN_EN_10;
            PresetDefinition preset = PresetCatalog.byId("run-anywhere");
            check(preset.defaultHotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT)),
                    "Run Anywhere default hotkey must be R+RIGHT");
            check(preset.supportsDeployment(PresetDeploymentKind.HOTKEY_LOCAL),
                    "Run Anywhere must expose standalone V1 deployment");
            check(preset.supportsDeployment(PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT),
                    "Run Anywhere must expose shared-local deployment");
            check(preset.isValidated(PresetUsageMode.SINGLE_HOTKEY, rom),
                    "standalone Run Anywhere LG1.0 validation missing");
            check(preset.isValidated(PresetUsageMode.SHARED_N_HOTKEY, rom),
                    "shared Run Anywhere LG1.0 validation missing");

            List<String> baseIds = List.of("seed-modifier", "show-secret-id", "party-iv-viewer", "mute-music");
            PresetCompositionPlan base = PresetCompositionPlanner.planHotkeys(rom, baseIds);
            TriggerBuildResult baseRuntime = CompositionArtifactBuilder.build(base, 0xB5B1E7AD).runtime();
            check(baseRuntime.totalScriptBytes() == 795, "baseline full Shared runtime size changed");
            check(sha256(baseRuntime.ramScript().scriptCopy()).equals(BASELINE_FULL_SHARED_SHA256),
                    "baseline full Shared runtime bytes changed");

            PresetCompositionPlan full = PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier", "show-secret-id", "party-iv-viewer", "mute-music", "run-anywhere"));
            check(full.hotkeyBindings() == 5, "full pack must have five hotkeys");
            check(full.ramScriptBytes() == 985, "full Run Anywhere Shared runtime size changed");
            check(full.ramScriptFree() == 10, "full Run Anywhere Shared runtime free space changed");
            check(full.sb1Bytes() == base.sb1Bytes(), "Run Anywhere must not consume SB1");
            check(full.sb2Bytes() == base.sb2Bytes(), "Run Anywhere must not consume SB2");
            check(full.infrastructure().contains(PresetInfrastructure.RUN_ANYWHERE_EWRAM_SIDECAR),
                    "Run Anywhere EWRAM sidecar infrastructure missing");

            ConcretePresetAllocation ra = full.concreteLayout().allocations().stream()
                    .filter(a -> a.presetId().equals("run-anywhere"))
                    .findFirst().orElseThrow();
            check(!ra.hasGateway() && !ra.hasSb2FieldScript() && !ra.hasNativeModule(),
                    "Run Anywhere shared deployment must stay local to Runtime RamScript");

            SharedRuntimeSupportLayout support = SharedRuntimeSupportLayout.build(rom, 5, true, true, 0x140);
            check(support.runAnywhereOffset() == 72, "Run Anywhere local payload offset changed");
            check(support.serviceOffset() == 176, "native staging service offset must follow Run Anywhere payload");

            InstallationPlan install = CompositionInstallationPlanner.plan(full);
            InstallationEmitter.EmittedInstallation emitted = InstallationEmitter.emit(install, 0xB5B1E7AD);
            check(emitted.persistentStages().size() == 3, "full pack installer stage count changed");
            check(emitted.runtime() != null, "full pack runtime missing");

            byte[] runtime = emitted.runtime().scriptCopy();
            byte[] sidecar = RunAnywhereHotkeyRuntimeV1.sidecar(rom);
            check(contains(runtime, sidecar), "compact native installer must carry the EWRAM sidecar bytes");
            byte[] oldFieldWrite = new FieldScriptWriter()
                    .writeBytes(RunAnywhereHotkeyRuntimeV1.SIDECAR_ADDRESS, sidecar).build();
            check(!contains(runtime, oldFieldWrite),
                    "catalog runtime must use compact native sidecar copy, not the oversized Field Script write");

            // Building the new composition must not mutate the frozen baseline path.
            TriggerBuildResult baseAgain = CompositionArtifactBuilder.build(base, 0xB5B1E7AD).runtime();
            check(Arrays.equals(baseRuntime.ramScript().scriptCopy(), baseAgain.ramScript().scriptCopy()),
                    "Run Anywhere catalog build mutated baseline Shared runtime state");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        run();
        System.out.println("RunAnywhereCatalogIntegrationTest passed");
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (haystack[i + j] != needle[j]) continue outer;
            return true;
        }
        return false;
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format("%02x", b));
        return out.toString();
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
