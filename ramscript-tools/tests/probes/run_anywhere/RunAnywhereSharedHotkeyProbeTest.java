import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public final class RunAnywhereSharedHotkeyProbeTest {
    private static final String STOCK_LG10_TWO_BINDING_SHA256 =
            "cffb2cdae7d88d8485aaf2243da60d9728bd3c8ac578d118daa4879483d29ee1";

    public static void main(String[] args) throws Exception {
        RomProfile rom = RomProfile.LEAF_GREEN_EN_10;
        PresetCompositionPlan base = PresetCompositionPlanner.planHotkeys(
                rom, List.of("seed-modifier", "mute-music"));

        // Guardrail: adding the extension API must not mutate the frozen/default
        // SharedHotkeyRuntime output used by production compositions.
        TriggerBuildResult stock = CompositionArtifactBuilder.build(base, 0xB5B1E7AD).runtime();
        check(stock.totalScriptBytes() == 411, "stock Seed+Mute shared runtime size changed");
        check(sha256(stock.ramScript().scriptCopy()).equals(STOCK_LG10_TWO_BINDING_SHA256),
                "stock SharedHotkeyRuntime bytes changed");

        TriggerBuildResult extended = RunAnywhereSharedHotkeyProbe.buildRuntime(base);
        check(extended.totalScriptBytes() == 847, "experimental shared runtime size changed");
        check(extended.totalScriptBytes() <= RamScript.SCRIPT_SIZE, "experimental runtime does not fit");
        check(RunAnywhereSharedHotkeyProbe.localPayloadOffset(base) == 52,
                "Run Anywhere local payload offset changed unexpectedly");

        byte[] local = RunAnywhereSharedHotkeyProbe.buildLocalTogglePayload(
                rom, RunAnywhereSharedHotkeyProbe.localPayloadOffset(base));
        check(local.length == 103, "shared local toggle payload size changed");
        check(contains(extended.ramScript().scriptCopy(), local),
                "shared runtime does not contain local Run Anywhere payload");
        byte[] encodedSidecarInstall = new FieldScriptWriter()
                .writeBytes(RunAnywhereHotkeyRuntimeV1.SIDECAR_ADDRESS, RunAnywhereHotkeyRuntimeV1.sidecar(rom))
                .build();
        check(contains(extended.ramScript().scriptCopy(), encodedSidecarInstall),
                "shared runtime installer does not contain encoded EWRAM sidecar write");

        // The production runtime must remain stable after building the extension.
        TriggerBuildResult stockAgain = CompositionArtifactBuilder.build(base, 0xB5B1E7AD).runtime();
        check(Arrays.equals(stock.ramScript().scriptCopy(), stockAgain.ramScript().scriptCopy()),
                "experimental build mutated stock shared runtime state");

        // Keep the first hardware probe deliberately isolated from the shared
        // native service until this Seed+Mute+RunAnywhere composition is cart-validated.
        PresetCompositionPlan nativePlan = PresetCompositionPlanner.planHotkeys(
                rom, List.of("show-secret-id", "party-iv-viewer"));
        expectFailure(() -> RunAnywhereSharedHotkeyProbe.buildRuntime(nativePlan),
                "first shared probe should reject shared native staging plans");

        System.out.println("RunAnywhereSharedHotkeyProbeTest passed");
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

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
