record ToolkitOwnedFlag(
        String id,
        String displayName,
        int eventFlag,
        String owner
) {
    ToolkitOwnedFlag {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("flag id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("flag display name must not be blank");
        if (eventFlag < 0) throw new IllegalArgumentException("event flag must be non-negative");
        owner = owner == null ? "" : owner;
    }
}
