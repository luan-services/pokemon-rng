import java.util.List;

/* Runtime storage/hook layout for the known-good FR10 seed modifier.

   These addresses are intentionally centralized as documentation/inspection
   metadata. The frozen FixedRamSeedModifier remains the source of the actual
   bytes until another implementation is independently tested in-game. */
final class NativeRuntimeLayout {
    private NativeRuntimeLayout() {}

    static List<RuntimeRegion> regions(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10) {
            throw new IllegalArgumentException(
                    "Runtime region catalog is currently validated only for fr10"
            );
        }

        return List.of(
                new RuntimeRegion(
                        "main-hook",
                        rom.mainHook,
                        32,
                        "VBlank hotkey detector; reads held/new keys and dispatches the RNG extension",
                        true
                ),
                new RuntimeRegion(
                        "rng-extension",
                        rom.rngExtension,
                        12,
                        "Writes the RNG predecessor then tail-chains to the original VBlank",
                        true
                ),
                new RuntimeRegion(
                        "tail-stub",
                        rom.tailStub,
                        4,
                        "Thumb tail jump to the original VBlank handler",
                        true
                ),
                new RuntimeRegion(
                        "original-vblank-literal",
                        rom.originalVBlankLiteral,
                        4,
                        "Stores the original VBlank Thumb function pointer",
                        true
                ),
                new RuntimeRegion(
                        "installer-staging",
                        rom.installerStaging,
                        16,
                        "Temporary installer executed once by the Wonder Card RamScript",
                        false
                )
        );
    }
}
