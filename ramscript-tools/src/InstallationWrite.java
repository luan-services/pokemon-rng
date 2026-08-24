/* One logical persistent write required by a concrete composition. */
record InstallationWrite(
        String componentId,
        InstallationTarget target,
        int offset,
        int size
) {
    InstallationWrite {
        if (componentId == null || componentId.isBlank()) throw new IllegalArgumentException("component id must not be blank");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        if (offset < 0 || size <= 0) throw new IllegalArgumentException("invalid installation write");
    }
}
