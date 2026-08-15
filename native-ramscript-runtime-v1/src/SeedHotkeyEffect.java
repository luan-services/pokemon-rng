/* Framework wrapper around the frozen, manually-validated seed modifier.

   IMPORTANT:
   The actual machine-code generation remains in FixedRamSeedModifier.
   This wrapper exists so native-ramscript-tools can grow into multiple effects
   without changing the bytes of the known-good seed payload. */
final class SeedHotkeyEffect implements NativeEffect {
    private final RomProfile rom;
    private final int seed;

    SeedHotkeyEffect(RomProfile rom, int seed) {
        if (seed < 0 || seed > 0xFFFF) {
            throw new IllegalArgumentException(
                    "Initial seed must fit in 16 bits (0000..FFFF)"
            );
        }

        this.rom = rom;
        this.seed = seed;
    }

    RomProfile rom() {
        return rom;
    }

    int seed() {
        return seed;
    }

    Hotkey hotkey() {
        return Hotkey.seedModifierDefault();
    }

    long predecessor() {
        return FixedRamSeedModifier.predecessorFor(seed);
    }

    @Override
    public String id() {
        return "seed-hotkey";
    }

    @Override
    public String displayName() {
        return "RNG initial-seed hotkey";
    }

    @Override
    public String description() {
        return "Restarts the FR/LG RNG sequence at a chosen 16-bit initial seed";
    }

    @Override
    public RamScript build() {
        return FixedRamSeedModifier.build(rom, seed);
    }
}
