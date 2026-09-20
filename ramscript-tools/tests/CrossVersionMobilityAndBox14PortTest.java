import java.util.List;

/* Source/build validation for the ports added after the LG1.0 baselines.
   This test deliberately does not claim in-game validation. It proves that
   every English FR/LG 1.0/1.1 profile exposes, plans and materializes the
   three formerly-LG1.0-only hotkey presets through the production pipeline. */
public final class CrossVersionMobilityAndBox14PortTest {
    private CrossVersionMobilityAndBox14PortTest() {}

    static void run() {
        PresetDefinition box14 = PresetCatalog.byId("seed-modifier-box14");
        PresetDefinition run = PresetCatalog.byId("run-anywhere");
        PresetDefinition runBike = PresetCatalog.byId("run-bike-anywhere");

        for (RomProfile rom : RomProfile.values()) {
            check(rom.wirelessCommType == 0x03003F3CL,
                    rom.id() + " gWirelessCommType profile changed");
            check(box14.supports(rom), "BOX14 missing support for " + rom.id());
            check(run.supports(rom), "Run Anywhere missing support for " + rom.id());
            check(runBike.supports(rom), "Run+Bike missing support for " + rom.id());

            if (rom != RomProfile.LEAF_GREEN_EN_10) {
                check(box14.validationStatus(PresetUsageMode.SINGLE_HOTKEY, rom)
                                == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                        "BOX14 port must remain unvalidated on " + rom.id());
                check(run.validationStatus(PresetUsageMode.SINGLE_HOTKEY, rom)
                                == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                        "Run Anywhere port must remain unvalidated on " + rom.id());
                check(runBike.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, rom)
                                == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                        "Run+Bike port must remain unvalidated on " + rom.id());
            }

            // Standalone paths.
            materialize(PresetCompositionPlanner.planHotkeys(rom, List.of("seed-modifier-box14")));
            materialize(PresetCompositionPlanner.planHotkeys(rom, List.of("run-anywhere")));
            materialize(PresetCompositionPlanner.planHotkeys(rom, List.of("run-bike-anywhere")));

            // Shared paths exercise the SB1/SB2 and resident-sidecar variants.
            materialize(PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier-box14", "party-iv-viewer")));
            materialize(PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier", "run-anywhere")));
            materialize(PresetCompositionPlanner.planHotkeys(
                    rom, List.of("seed-modifier", "run-bike-anywhere")));

            // Both mobility presets must continue to claim the same exclusive
            // EWRAM owner on every port.
            expectFailure(() -> PresetCompositionPlanner.planHotkeys(
                    rom, List.of("run-anywhere", "run-bike-anywhere")),
                    "mobility resource conflict missing on " + rom.id());
        }
    }

    private static void materialize(PresetCompositionPlan plan) {
        InstallationPlan install = CompositionInstallationPlanner.plan(plan);
        InstallationEmitter.EmittedInstallation emitted = InstallationEmitter.emit(install, 0x12345678);
        check(!emitted.persistentStages().isEmpty(), "materialization emitted no stage for " + plan.rom().id());
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (CompositionPlanningException expected) {
            check("RESOURCE_CONFLICT".equals(expected.code()),
                    "expected RESOURCE_CONFLICT, got " + expected.code());
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
