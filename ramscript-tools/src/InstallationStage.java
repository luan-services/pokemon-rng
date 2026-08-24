import java.util.List;

/* One temporary Wonder Card installer stage. This is a dry-run plan only. */
record InstallationStage(
        int index,
        List<InstallationChunk> chunks,
        int persistentBytes,
        int encodedInstallerBytes
) {
    InstallationStage {
        if (index < 1) throw new IllegalArgumentException("stage index must be positive");
        chunks = List.copyOf(chunks);
        if (chunks.isEmpty()) throw new IllegalArgumentException("installer stage must contain writes");
        if (persistentBytes <= 0 || encodedInstallerBytes <= 0 || encodedInstallerBytes > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("invalid installer stage size");
        }
    }
}
