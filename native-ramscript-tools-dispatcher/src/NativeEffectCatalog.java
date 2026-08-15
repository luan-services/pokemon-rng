import java.util.List;

/* User-facing catalog of implemented native effects.

   Only effects that have an actual builder belong here. Research ideas are
   documented separately and are not exposed as if they already work. */
final class NativeEffectCatalog {
    record Entry(
            String id,
            String name,
            String status,
            String romSupport,
            String trigger,
            String description
    ) {}

    private NativeEffectCatalog() {}

    static List<Entry> entries() {
        return List.of(
                new Entry(
                        "seed-hotkey",
                        "RNG initial-seed hotkey",
                        "validated in emulator/gameplay",
                        "FireRed English 1.0 (fr10)",
                        Hotkey.seedModifierDefault().toString(),
                        "Restarts the RNG sequence at a chosen 16-bit initial seed"
                )
        );
    }
}
