import java.util.List;

final class EarlyObjectBoundSharedHotkeyInstallerTest {
    private EarlyObjectBoundSharedHotkeyInstallerTest() {}

    static void run() {
        for (RomProfile rom : RomProfile.values()) {
            testFrozenResidentBytes(rom);
            testThreeHotkeyOakRuntime(rom);
        }
    }

    private static void testFrozenResidentBytes(RomProfile rom) {
        require(EarlyObjectBoundSharedHotkeyInstaller.residentBytesMatchFrozenShared(rom, HotkeyButton.R),
                rom + ": early shared installer changed frozen R-modifier resident bytes");
        require(EarlyObjectBoundSharedHotkeyInstaller.residentBytesMatchFrozenShared(rom, HotkeyButton.L),
                rom + ": early shared installer changed frozen L-modifier resident bytes");
    }

    private static void testThreeHotkeyOakRuntime(RomProfile rom) {
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(
                rom, List.of("seed-modifier", "show-secret-id", "party-iv-viewer")
        );
        ObjectEventTarget target = ObjectEventCatalog.OAKS_LAB_AIDE1_EARLY_INSTALLER;
        TriggerBuildResult early = CompositionArtifactBuilder.buildObjectBoundSharedRuntime(composition, target);
        RamScript script = early.ramScript();

        require(script.isChecksumValid(), rom + ": early shared Oak runtime CRC invalid");
        require(script.magic() == RamScript.EXPECTED_MAGIC, rom + ": early shared Oak runtime magic invalid");
        require(script.mapGroup() == target.mapGroup(), rom + ": early shared Oak map group mismatch");
        require(script.mapNum() == target.mapNum(), rom + ": early shared Oak map number mismatch");
        require(script.objectId() == target.localId(), rom + ": early shared Oak object id mismatch");
        require(early.totalScriptBytes() <= RamScript.SCRIPT_SIZE, rom + ": early shared Oak runtime overflow");
        require(early.totalScriptBytes() == 823, rom + ": early shared three-hotkey runtime size drift");

        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        require(plan.runtimeStageBytes() == 787, rom + ": frozen normal shared runtime size drift");
        require(plan.runtimeStageRequired(), rom + ": three-hotkey composition must require runtime stage");
        require(composition.hotkeyBindings() == 3, rom + ": expected three hotkey bindings");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
