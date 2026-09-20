import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public final class RunBikeAnywhereCatalogIntegrationTest {
    private static final String BASELINE_RUN_ANYWHERE_FULL_SHA256 =
            "2641f3b3882c3fa9faf6ae583b39a9b53f826ac671a10fe943f9cc5a8db57f8a";

    private RunBikeAnywhereCatalogIntegrationTest() {}

    static void run() {
        try {
            RomProfile rom = RomProfile.LEAF_GREEN_EN_10;
            PresetDefinition preset = PresetCatalog.byId("run-bike-anywhere");
            check(preset.defaultHotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT)),
                    "Run + Bike Anywhere default hotkey must be R+RIGHT");
            check(preset.supportsDeployment(PresetDeploymentKind.HOTKEY_LOCAL),
                    "Run + Bike Anywhere must expose standalone V1 deployment");
            check(preset.supportsDeployment(PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT),
                    "Run + Bike Anywhere must expose shared-local deployment");
            check(preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE),
                    "Run + Bike Anywhere must expose persistent-native deployment for service reuse");
            check(!preset.isValidated(PresetUsageMode.SINGLE_HOTKEY, rom),
                    "Run + Bike Anywhere standalone must remain conservative until its exact cart path is tested");
            check(preset.isValidated(PresetUsageMode.SHARED_N_HOTKEY, rom),
                    "Run + Bike Anywhere shared must remain GAME-VALIDATED after the LG1.0 cart test");

            byte[] sidecar = RunBikeAnywhereHotkeyRuntimeV1.sidecar(rom);
            check(sidecar.length == 63, "Run + Bike sidecar size changed");
            check(sidecar.length <= RunBikeAnywhereHotkeyRuntimeV1.SIDECAR_RESERVED_SIZE,
                    "Run + Bike sidecar exceeds validated 68-byte EWRAM region");
            check(sidecar[RunBikeAnywhereHotkeyRuntimeV1.STATE_OFFSET] == 0,
                    "Run + Bike state must start disabled");
            check(sidecar[RunBikeAnywhereHotkeyRuntimeV1.ORIGINAL_RUNNING_OFFSET] == 0,
                    "Run + Bike stock running state must start zero");
            check(sidecar[RunBikeAnywhereHotkeyRuntimeV1.ORIGINAL_BIKE_OFFSET] == 0,
                    "Run + Bike stock bike state must start zero");
            check(RunBikeAnywhereHotkeyRuntimeV1.toggleHelper(rom).length == 76,
                    "Run + Bike toggle helper size changed");

            PresetCompositionPlan standalone = PresetCompositionPlanner.planHotkeys(
                    rom, List.of("run-bike-anywhere"));
            check(standalone.ramScriptBytes() == 720, "standalone Run + Bike runtime size changed");
            check(standalone.ramScriptFree() == 275, "standalone Run + Bike free space changed");
            RamScript standaloneScript = CompositionArtifactBuilder.buildLocal(standalone, 0x12345678);
            check(contains(standaloneScript.scriptCopy(), sidecar),
                    "standalone compact installer must carry Run + Bike sidecar bytes");

            PresetCompositionPlan shared = PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier", "mute-music", "run-bike-anywhere"));
            check(shared.hotkeyBindings() == 3, "shared Run + Bike test pack must have three bindings");
            check(shared.ramScriptBytes() == 774, "shared Run + Bike runtime size changed");
            check(shared.ramScriptFree() == 221, "shared Run + Bike runtime free space changed");
            check(shared.infrastructure().contains(PresetInfrastructure.RUN_BIKE_ANYWHERE_EWRAM_SIDECAR),
                    "Run + Bike EWRAM infrastructure missing");
            TriggerBuildResult sharedRuntime = CompositionArtifactBuilder.build(shared, 0xB5B1E7AD).runtime();
            check(sharedRuntime.totalScriptBytes() == 774, "shared Run + Bike materialization size mismatch");
            check(contains(sharedRuntime.ramScript().scriptCopy(), sidecar),
                    "shared compact installer must carry Run + Bike sidecar bytes");

            // Optimized mixed-native composition: BOX14 + Party IV already require
            // the shared staging service, so Run + Bike must reuse it from SB2
            // rather than spending its 271-byte local payload in the Runtime WC.
            PresetCompositionPlan optimized = PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier-box14", "repel", "party-iv-viewer", "run-bike-anywhere"));
            check(optimized.ramScriptBytes() == 878, "optimized Run + Bike runtime size changed");
            check(optimized.ramScriptFree() == 117, "optimized Run + Bike runtime free space changed");
            check(optimized.sb2Bytes() == 991, "optimized Run + Bike SB2 size changed");
            check(optimized.hotkeyBindings() == 4, "optimized Run + Bike pack must have four bindings");
            var optimizedRunBike = optimized.selections().stream()
                    .filter(x -> x.preset().id().equals("run-bike-anywhere"))
                    .findFirst().orElseThrow();
            check(optimizedRunBike.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                    "Run + Bike must reuse existing shared native staging service in mixed-native packs");
            TriggerBuildResult optimizedRuntime = CompositionArtifactBuilder.build(optimized, 0).runtime();
            check(optimizedRuntime.totalScriptBytes() == 878, "optimized Run + Bike materialization size mismatch");
            check(contains(optimizedRuntime.ramScript().scriptCopy(), sidecar),
                    "optimized runtime installer must still carry the resident sidecar");

            expectFailure(() -> PresetCompositionPlanner.planHotkeys(
                            rom, List.of("run-anywhere", "run-bike-anywhere")),
                    "run-anywhere and run-bike-anywhere must be mutually exclusive");

            // Adding the new preset must not mutate the already validated Run Anywhere baseline.
            PresetCompositionPlan baseline = PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier", "show-secret-id", "party-iv-viewer", "mute-music"));
            TriggerBuildResult baselineRuntime = CompositionArtifactBuilder.build(baseline, 0xB5B1E7AD).runtime();
            check(baselineRuntime.totalScriptBytes() == 795, "validated Shared baseline size changed");
            check(sha256(baselineRuntime.ramScript().scriptCopy()).equals(BASELINE_RUN_ANYWHERE_FULL_SHA256),
                    "validated Shared baseline bytes changed");

            TriggerBuildResult baselineAgain = CompositionArtifactBuilder.build(baseline, 0xB5B1E7AD).runtime();
            check(Arrays.equals(baselineRuntime.ramScript().scriptCopy(), baselineAgain.ramScript().scriptCopy()),
                    "Run + Bike build mutated baseline Shared state");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        run();
        System.out.println("RunBikeAnywhereCatalogIntegrationTest passed");
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

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
