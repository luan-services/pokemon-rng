import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ToolkitIntegrationApiContractTest {
    private ToolkitIntegrationApiContractTest() {}

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("ToolkitIntegrationApiContractTest PASS");
    }

    static void run() throws Exception {
        testVersion();
        testCatalogSeparatesTrade();
        testEmptySelection();
        testMissingSeed();
        testSeedMetadataAndGenericParameter();
        testInvalidSeedParameter();
        testBox14NeedsNoBuildParameter();
        testExclusiveEwramResourceConflict();
        testRedundantViewerConflicts();
        testHotkeyConflict();
        testValidSharedPlan();
        testRealBuildMatchesProductionShape();
    }

    private static void testVersion() {
        Result r = run("api", "version");
        require(r.code == 0 && r.json.contains("\"apiVersion\":1"), "api version");
    }

    private static void testCatalogSeparatesTrade() {
        Result all = run("api", "list-content", "--rom", "lg10", "--context", "all");
        require(all.code == 0, "catalog all");
        require(all.json.contains("\"id\":\"trade-evolution\""), "trade is discoverable");
        require(all.json.contains("\"kind\":\"DEDICATED_UTILITY\""), "trade is dedicated utility");
        Result hotkeys = run("api", "list-content", "--rom", "lg10", "--context", "hotkey-composition");
        require(!hotkeys.json.contains("\"id\":\"trade-evolution\""), "trade excluded from hotkey composition");
        require(hotkeys.json.contains("\"id\":\"party-iv-viewer\""), "hotkey preset included");
    }

    private static void testEmptySelection() {
        Result r = run("api", "plan-composition", "--rom", "lg10");
        require(r.code == 2 && r.json.contains("EMPTY_SELECTION"), "empty selection diagnostic");
    }

    private static void testMissingSeed() {
        Result r = run("api", "plan-composition", "--rom", "lg10", "--preset", "seed-modifier");
        require(r.code == 2 && r.json.contains("MISSING_PARAMETER"), "seed parameter diagnostic");
    }

    private static void testSeedMetadataAndGenericParameter() {
        Result catalog = run("api", "list-content", "--rom", "lg10", "--context", "hotkey-composition");
        require(catalog.code == 0, "seed metadata catalog");
        require(catalog.json.contains("\"id\":\"seed\"")
                && catalog.json.contains("\"type\":\"HEX_U32\"")
                && catalog.json.contains("\"example\":\"B5B1E7AD\""), "seed parameter metadata");

        Result plan = run("api", "plan-composition", "--rom", "lg10",
                "--preset", "seed-modifier", "--param", "seed=B5B1E7AD");
        require(plan.code == 0 && plan.json.contains("\"valid\":true"), "generic seed parameter accepted");

        Result legacy = run("api", "plan-composition", "--rom", "lg10",
                "--preset", "seed-modifier", "--seed", "B5B1E7AD");
        require(legacy.code == 0, "legacy --seed remains compatible");
    }

    private static void testInvalidSeedParameter() {
        Result r = run("api", "plan-composition", "--rom", "lg10",
                "--preset", "seed-modifier", "--param", "seed=NOTHEX");
        require(r.code == 2 && r.json.contains("INVALID_PARAMETER") && r.json.contains("\"detail\":\"seed\""),
                "invalid seed structured diagnostic");
    }

    private static void testBox14NeedsNoBuildParameter() {
        Result r = run("api", "plan-composition", "--rom", "lg10", "--preset", "seed-modifier-box14");
        require(r.code == 0 && r.json.contains("\"valid\":true"), "box14 seed needs no UI build parameter");
    }

    private static void testExclusiveEwramResourceConflict() {
        Result r = run("api", "plan-composition", "--rom", "lg10",
                "--preset", "run-anywhere", "--preset", "run-bike-anywhere");
        require(r.code == 2 && r.json.contains("RESOURCE_CONFLICT"), "mobility EWRAM conflict code");
        require(r.json.contains("mobility-ewram-sidecar")
                && r.json.contains("run-anywhere") && r.json.contains("run-bike-anywhere"), "resource conflict details");
    }


    private static void testRedundantViewerConflicts() {
        Result iv = run("api", "plan-composition", "--rom", "fr10",
                "--preset", "party-iv-viewer", "--preset", "lead-iv-viewer");
        require(iv.code == 2 && iv.json.contains("PRESET_CONFLICT")
                        && iv.json.contains("iv-viewer-scope"),
                "Party IV + Lead IV redundant viewer conflict");

        Result ev = run("api", "plan-composition", "--rom", "fr10",
                "--preset", "party-ev-viewer", "--preset", "lead-ev-viewer");
        require(ev.code == 2 && ev.json.contains("PRESET_CONFLICT")
                        && ev.json.contains("ev-viewer-scope"),
                "Party EV + Lead EV redundant viewer conflict");
    }

    private static void testHotkeyConflict() {
        Result r = run("api", "plan-composition", "--rom", "lg10",
                "--preset", "lead-ev-viewer", "--preset", "run-bike-anywhere");
        require(r.code == 2 && r.json.contains("HOTKEY_CONFLICT"), "duplicate hotkey structured diagnostic");
    }

    private static void testValidSharedPlan() {
        Result r = run("api", "plan-composition", "--rom", "lg10",
                "--preset", "seed-modifier-box14", "--preset", "repel",
                "--preset", "party-iv-viewer", "--preset", "run-bike-anywhere");
        require(r.code == 0 && r.json.contains("\"valid\":true"), "valid shared plan");
        require(r.json.contains("\"used\":878") && r.json.contains("\"used\":991"), "known capacity values");
        require(r.json.contains("SHARED_PERSISTENT_NATIVE"), "planner placement exposed");
        require(r.json.contains("\"resources\":[") && r.json.contains("mobility-ewram-sidecar")
                && r.json.contains("\"sharing\":\"EXCLUSIVE\""), "owned resource exposed");
    }

    private static void testRealBuildMatchesProductionShape() throws Exception {
        Path dir = Files.createTempDirectory("toolkit-api-test-");
        Path input = dir.resolve("base.wc3");
        Files.write(input, new byte[RamScript.WC3_FILE_SIZE]);
        Path prefix = dir.resolve("pack");
        Result r = run("api", "build-composition", "--rom", "lg10", "--input", input.toString(), "--output", prefix.toString(),
                "--preset", "seed-modifier-box14", "--preset", "repel",
                "--preset", "party-iv-viewer", "--preset", "run-bike-anywhere");
        require(r.code == 0, "build succeeds: " + r.json);
        require(Files.exists(Path.of(prefix + "-install-1.wc3")), "install 1 exists");
        require(Files.exists(Path.of(prefix + "-install-2.wc3")), "install 2 exists");
        require(Files.exists(Path.of(prefix + "-runtime.wc3")), "runtime exists");
        require(r.json.contains("\"role\":\"INSTALL_STAGE\"") && r.json.contains("\"role\":\"RUNTIME\""), "artifact roles");
        require(Files.size(input) == RamScript.WC3_FILE_SIZE, "base preserved");
    }

    private static Result run(String... args) {
        PrintStream old = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer));
            int code = ToolkitIntegrationApi.run(args);
            return new Result(code, buffer.toString());
        } finally { System.setOut(old); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record Result(int code, String json) {}
}
