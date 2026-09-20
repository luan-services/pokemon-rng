import java.util.ArrayList;
import java.util.List;

/* Exhaustive LG1.0 catalog/planner/materializer parity check for every hotkey
   subset up to the SharedHotkeyRuntime binding limit. A planner rejection is
   allowed; any plan that succeeds must also materialize and emit its installer. */
public final class PresetCombinationAuditTest {
    private static final List<String> HOTKEY_PRESETS = List.of(
            "seed-modifier",
            "seed-modifier-box14",
            "repel",
            "party-iv-viewer",
            "lead-iv-viewer",
            "party-ev-viewer",
            "lead-ev-viewer",
            "show-secret-id",
            "mute-music",
            "run-anywhere",
            "run-bike-anywhere"
    );

    private PresetCombinationAuditTest() {}

    static void run() {
        for (RomProfile rom : RomProfile.values()) {
            auditRom(rom);
        }
    }

    private static void auditRom(RomProfile rom) {
        int accepted = 0;
        int rejected = 0;

        for (int mask = 1; mask < (1 << HOTKEY_PRESETS.size()); mask++) {
            if (Integer.bitCount(mask) > 8) continue;
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < HOTKEY_PRESETS.size(); i++) {
                if ((mask & (1 << i)) != 0) ids.add(HOTKEY_PRESETS.get(i));
            }

            PresetCompositionPlan plan;
            try {
                plan = PresetCompositionPlanner.planHotkeys(rom, ids);
            } catch (IllegalArgumentException expectedPlannerOrCapacityRejection) {
                rejected++;
                continue;
            }

            try {
                if (ids.size() == 1) {
                    CompositionArtifactBuilder.buildLocal(plan, 0x12345678);
                } else {
                    CompositionArtifactBuilder.build(plan, 0x12345678);
                }
                InstallationPlan installation = CompositionInstallationPlanner.plan(plan);
                InstallationEmitter.emit(installation, 0x12345678);
                accepted++;
            } catch (Exception ex) {
                throw new RuntimeException(
                        "planner accepted composition but materialization failed on " + rom.id() + ": " + ids, ex);
            }
        }

        check(accepted > 0, "combination audit accepted no compositions on " + rom.id());
        check(rejected > 0, "combination audit exercised no rejection paths on " + rom.id());
    }

    public static void main(String[] args) {
        run();
        System.out.println("PresetCombinationAuditTest passed");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
