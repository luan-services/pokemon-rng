import java.util.Set;

/* Stable UI/domain input. Raw event-flag numbers intentionally do not appear here. */
record ToolkitCleanerOptions(
        ToolkitCleanerMode mode,
        Set<String> excludedFlagIds
) {
    ToolkitCleanerOptions {
        if (mode == null) throw new IllegalArgumentException("cleaner mode must not be null");
        excludedFlagIds = excludedFlagIds == null ? Set.of() : Set.copyOf(excludedFlagIds);
        if (mode == ToolkitCleanerMode.INFRASTRUCTURE_ONLY && !excludedFlagIds.isEmpty()) {
            throw new IllegalArgumentException("flag exclusions require WIPE_PROGRESS mode");
        }
        for (String id : excludedFlagIds) ToolkitOwnedFlagCatalog.require(id);
    }

    static ToolkitCleanerOptions infrastructureOnly() {
        return new ToolkitCleanerOptions(ToolkitCleanerMode.INFRASTRUCTURE_ONLY, Set.of());
    }

    static ToolkitCleanerOptions wipeAllProgress() {
        return new ToolkitCleanerOptions(ToolkitCleanerMode.WIPE_PROGRESS, Set.of());
    }
}
