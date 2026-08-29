import java.util.List;

/* Stable discovery model for a future UI. A frontend can render choices without
   knowing SaveBlock offsets, bit packing, or native Cleaner implementation. */
record ToolkitCleanerUiModel(
        List<ToolkitCleanerMode> modes,
        ToolkitCleanerMode defaultMode,
        List<ToolkitOwnedFlag> progressFlags,
        boolean supportsExclusions
) {
    ToolkitCleanerUiModel {
        modes = List.copyOf(modes);
        progressFlags = List.copyOf(progressFlags);
    }

    static ToolkitCleanerUiModel current() {
        return new ToolkitCleanerUiModel(
                List.of(ToolkitCleanerMode.INFRASTRUCTURE_ONLY, ToolkitCleanerMode.WIPE_PROGRESS),
                ToolkitCleanerMode.INFRASTRUCTURE_ONLY,
                ToolkitOwnedFlagCatalog.all(),
                true);
    }

    String report() {
        StringBuilder s = new StringBuilder();
        s.append("Toolkit Cleaner metadata\n");
        s.append("  default mode: ").append(defaultMode).append('\n');
        s.append("  modes: ").append(modes).append('\n');
        s.append("  per-flag exclusions: ").append(supportsExclusions ? "supported" : "unsupported").append('\n');
        s.append("  progress flags:\n");
        for (ToolkitOwnedFlag flag : progressFlags) {
            s.append("    - ").append(flag.id())
                    .append(" | ").append(flag.displayName())
                    .append(" | owner=").append(flag.owner())
                    .append(" | flag=0x").append(Integer.toHexString(flag.eventFlag()).toUpperCase())
                    .append('\n');
        }
        return s.toString();
    }
}
