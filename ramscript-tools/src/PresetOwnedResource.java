/* Fixed/session-resident resources that composition planning must arbitrate.
   This is separate from byte-capacity accounting: an exclusive resource may
   have plenty of free bytes and still be unsafe for two independent owners. */
enum PresetOwnedResource {
    MOBILITY_EWRAM_SIDECAR(
            "mobility-ewram-sidecar",
            "EWRAM",
            0x02022B08L,
            68,
            PresetResourceSharing.EXCLUSIVE,
            "GAME-VALIDATED LG1.0 tail reservation used by the Run Anywhere family"
    );

    private final String id;
    private final String memoryRegion;
    private final long address;
    private final int sizeBytes;
    private final PresetResourceSharing sharing;
    private final String notes;

    PresetOwnedResource(String id, String memoryRegion, long address, int sizeBytes,
                        PresetResourceSharing sharing, String notes) {
        this.id = id;
        this.memoryRegion = memoryRegion;
        this.address = address;
        this.sizeBytes = sizeBytes;
        this.sharing = sharing;
        this.notes = notes;
    }

    String id() { return id; }
    String memoryRegion() { return memoryRegion; }
    long address() { return address; }
    int sizeBytes() { return sizeBytes; }
    PresetResourceSharing sharing() { return sharing; }
    String notes() { return notes; }
}
