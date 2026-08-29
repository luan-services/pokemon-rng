import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Resolved Cleaner plan suitable for CLI today and a future UI tomorrow.
   It turns named progress-state selections into byte masks only at build time. */
record ToolkitCleanerPlan(
        ToolkitCleanerMode mode,
        List<ToolkitOwnedFlag> flagsToClear,
        List<ToolkitOwnedFlag> flagsToPreserve
) {
    private static final int FLAGS_SB1_OFFSET = 0x0EE0;

    ToolkitCleanerPlan {
        flagsToClear = List.copyOf(flagsToClear);
        flagsToPreserve = List.copyOf(flagsToPreserve);
    }

    static ToolkitCleanerPlan resolve(ToolkitCleanerOptions options) {
        List<ToolkitOwnedFlag> clear = new ArrayList<>();
        List<ToolkitOwnedFlag> preserve = new ArrayList<>();
        Set<String> excluded = options.excludedFlagIds().stream()
                .map(ToolkitOwnedFlagCatalog::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        for (ToolkitOwnedFlag flag : ToolkitOwnedFlagCatalog.all()) {
            if (options.mode() == ToolkitCleanerMode.WIPE_PROGRESS && !excluded.contains(ToolkitOwnedFlagCatalog.normalize(flag.id()))) {
                clear.add(flag);
            } else {
                preserve.add(flag);
            }
        }
        return new ToolkitCleanerPlan(options.mode(), clear, preserve);
    }

    /* Map of SB1 byte offset -> AND mask used to clear selected bits while preserving all others. */
    Map<Integer, Integer> flagAndMasks() {
        Map<Integer, Integer> masks = new LinkedHashMap<>();
        for (ToolkitOwnedFlag flag : flagsToClear) {
            int byteOffset = FLAGS_SB1_OFFSET + (flag.eventFlag() >>> 3);
            int bit = flag.eventFlag() & 7;
            int mask = masks.getOrDefault(byteOffset, 0xFF);
            mask &= ~(1 << bit);
            masks.put(byteOffset, mask & 0xFF);
        }
        return Map.copyOf(masks);
    }

    String report() {
        StringBuilder s = new StringBuilder();
        s.append("Cleaner plan\n");
        s.append("  mode: ").append(mode).append('\n');
        s.append("  persistent toolkit storage: clear\n");
        s.append("  installation manifest: clear\n");
        s.append("  progress flags to clear: ").append(flagsToClear.size()).append('\n');
        for (ToolkitOwnedFlag flag : flagsToClear) {
            s.append("    - ").append(flag.id()).append(" (0x").append(Integer.toHexString(flag.eventFlag()).toUpperCase()).append(")\n");
        }
        s.append("  progress flags preserved: ").append(flagsToPreserve.size()).append('\n');
        for (ToolkitOwnedFlag flag : flagsToPreserve) s.append("    - ").append(flag.id()).append('\n');
        return s.toString();
    }
}
