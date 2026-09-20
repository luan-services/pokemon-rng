/* Focused runner for the production features that should stay green even when
   historical planner/research assertions are intentionally left unresolved. */
public final class ProductionTestRunner {
    private ProductionTestRunner() {}

    public static void main(String[] args) throws Exception {
        ObjectEventBindingTest.run();
        EarlyObjectBoundHotkeyInstallerTest.run();
        EarlyObjectBoundSharedHotkeyInstallerTest.run();
        TradeEvolutionPresetProductionTest.run();
        CustomTrainerSharedRuntimeTest.run();
        CustomTrainerGymLeaderPresetsTest.run();
        CleanerProductionTest.run();
        RunAnywhereCatalogIntegrationTest.run();
        RunBikeAnywhereCatalogIntegrationTest.run();
        CrossVersionMobilityAndBox14PortTest.run();
        PresetCombinationAuditTest.run();
        ToolkitIntegrationApiContractTest.run();
        ResourceAwareSharedPackingTest.runAll();
        System.out.println("Production tests passed: object bindings, early single/shared hotkey installers, Trade Evolution, Custom Trainer V2, Gym Leader presets, Cleaner, Run Anywhere catalog integration, Run + Bike Anywhere catalog integration, cross-version BOX14/mobility ports, exhaustive cross-ROM preset-combination planner/materializer audit, JSON desktop integration contract");
    }
}
