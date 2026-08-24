/* A <=255-byte operation accepted by the validated batch-copy installer helper. */
record InstallationChunk(
        String componentId,
        InstallationTarget target,
        int offset,
        int size
) {
    InstallationChunk {
        if (componentId == null || componentId.isBlank()) throw new IllegalArgumentException("component id must not be blank");
        if (target == null) throw new IllegalArgumentException("target must not be null");
        if (offset < 0 || size <= 0 || size > 0xFF) throw new IllegalArgumentException("invalid installation chunk");
    }
}
