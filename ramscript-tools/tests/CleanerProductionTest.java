import java.util.Map;
import java.util.Set;

public final class CleanerProductionTest {
    private CleanerProductionTest() {}

    public static void run() {
        ToolkitCleanerPlan infrastructureOnly = ToolkitCleanerPlan.resolve(ToolkitCleanerOptions.infrastructureOnly());
        check(infrastructureOnly.flagsToClear().isEmpty(), "infrastructure-only Cleaner must preserve authored progress");
        check(infrastructureOnly.flagsToPreserve().size() == 8, "infrastructure-only Cleaner must expose all current progress flags as preserved");
        check(infrastructureOnly.flagAndMasks().isEmpty(), "infrastructure-only Cleaner must not emit flag masks");

        ToolkitCleanerPlan wipeAll = ToolkitCleanerPlan.resolve(ToolkitCleanerOptions.wipeAllProgress());
        check(wipeAll.flagsToClear().size() == 8, "wipe Cleaner must clear all current toolkit-owned progress flags");
        check(wipeAll.flagsToPreserve().isEmpty(), "wipe-all Cleaner must preserve no current toolkit-owned progress flags");
        check(wipeAll.flagAndMasks().equals(Map.of(0x0F74, 0x7F, 0x0F75, 0x80)),
                "wipe-all Cleaner must clear exactly 0x4A7..0x4AE while preserving neighboring bits");

        ToolkitCleanerPlan keepGiovanni = ToolkitCleanerPlan.resolve(new ToolkitCleanerOptions(
                ToolkitCleanerMode.WIPE_PROGRESS, Set.of("GYM_GIOVANNI_COMPLETED")));
        check(keepGiovanni.flagsToClear().size() == 7, "Giovanni exclusion must leave seven flags selected for wipe");
        check(keepGiovanni.flagsToPreserve().stream().anyMatch(f -> f.id().equals("GYM_GIOVANNI_COMPLETED")),
                "Giovanni exclusion must be reflected in UI-facing preserved flags");
        check(keepGiovanni.flagAndMasks().equals(Map.of(0x0F74, 0x7F, 0x0F75, 0xC0)),
                "Giovanni exclusion must preserve bit 0x4AE and neighboring bits");

        for (RomProfile rom : new RomProfile[] {
                RomProfile.FIRE_RED_EN_10,
                RomProfile.FIRE_RED_EN_11,
                RomProfile.LEAF_GREEN_EN_10,
                RomProfile.LEAF_GREEN_EN_11
        }) {
            RamScript cleaner = ToolkitCleanerPreset.build(rom, infrastructureOnly);
            RamScript wipeCleaner = ToolkitCleanerPreset.build(rom, wipeAll);
            RamScript keepGiovanniCleaner = ToolkitCleanerPreset.build(rom, keepGiovanni);
            check(cleaner.isChecksumValid(), "Cleaner RamScript checksum failed for " + rom.toString());
            check(wipeCleaner.isChecksumValid(), "wipe Cleaner RamScript checksum failed for " + rom.toString());
            check(keepGiovanniCleaner.isChecksumValid(), "excluded-flag Cleaner RamScript checksum failed for " + rom.toString());
            check(ToolkitCleanerPreset.helperBytesForTest(rom, infrastructureOnly).length < 256,
                    "Cleaner native helper should remain compact for " + rom.toString());
            check(ToolkitCleanerPreset.helperBytesForTest(rom, wipeAll).length < 256,
                    "wipe Cleaner native helper should remain compact for " + rom.toString());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
