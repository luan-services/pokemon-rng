import java.util.Arrays;

final class EarlyObjectBoundHotkeyInstallerTest {
    private EarlyObjectBoundHotkeyInstallerTest() {}

    static void run() {
        for (RomProfile rom : RomProfile.values()) {
            testSeedInstallerShape(rom);
            testResidentBytesFrozen(rom);
            testChecksumSymbol(rom);
        }
    }

    private static void testSeedInstallerShape(RomProfile rom) {
        ObjectEventTarget target = ObjectEventCatalog.OAKS_LAB_AIDE1_EARLY_INSTALLER;
        byte[] payload = SeedModifierPreset.buildPayload(rom, 0x1234);
        TriggerBuildResult result = EarlyObjectBoundHotkeyInstaller.compose(rom, payload, Hotkey.DEFAULT, target);
        RamScript script = result.ramScript();

        require(script.isChecksumValid(), rom + ": early installer CRC invalid");
        require(script.magic() == RamScript.EXPECTED_MAGIC, rom + ": bad magic");
        require(script.mapGroup() == 4, rom + ": wrong Oak Lab map group");
        require(script.mapNum() == 3, rom + ": wrong Oak Lab map number");
        require(script.objectId() == 1, rom + ": wrong Oak aide local id");
        require(result.totalScriptBytes() <= RamScript.SCRIPT_SIZE, rom + ": early installer overflow");
        require(result.totalScriptBytes() > HotkeyRuntimeV1.scriptSize(rom, payload),
                rom + ": early installer should include transient self-detach overhead");
    }

    private static void testResidentBytesFrozen(RomProfile rom) {
        require(EarlyObjectBoundHotkeyInstaller.residentTableAndDataMatchValidated(rom, Hotkey.DEFAULT),
                rom + ": early installer changed validated resident table/runtime bytes");
    }

    private static void testChecksumSymbol(RomProfile rom) {
        long expected = switch (rom) {
            case FIRE_RED_EN_10, LEAF_GREEN_EN_10 -> 0x08069CB1L;
            case FIRE_RED_EN_11, LEAF_GREEN_EN_11 -> 0x08069CC5L;
        };
        require(EarlyObjectBoundHotkeyInstaller.calculateRamScriptChecksumThumb(rom) == expected,
                rom + ": CalculateRamScriptChecksum profile mismatch");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
