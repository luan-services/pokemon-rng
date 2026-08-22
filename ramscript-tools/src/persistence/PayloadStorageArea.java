/* Physical places where toolkit payload bytes may live.

   RAMSCRIPT is the normal 995-byte Wonder Card RamScript area.
   SAVE_BLOCK1 and SAVE_BLOCK2 are the persistent regions validated on FR1.0.

   SaveBlock addresses are intentionally represented as offsets from the live
   gSaveBlock pointers. The pointers move at runtime, so callers must never turn
   these into fixed EWRAM addresses.
*/
enum PayloadStorageArea {
    RAMSCRIPT(0, RamScript.SCRIPT_SIZE, true),
    SAVE_BLOCK1(0x348C, 400, true),
    SAVE_BLOCK2(0x0B20, 0x400, true);

    private final int offset;
    private final int capacity;
    private final boolean persistent;

    PayloadStorageArea(int offset, int capacity, boolean persistent) {
        this.offset = offset;
        this.capacity = capacity;
        this.persistent = persistent;
    }

    int offset() { return offset; }
    int capacity() { return capacity; }
    boolean persistent() { return persistent; }

    long pointerAddress(RomProfile rom) {
        return switch (this) {
            case RAMSCRIPT -> throw new IllegalStateException("RamScript is not addressed through a SaveBlock pointer");
            case SAVE_BLOCK1 -> rom.saveBlock1Ptr;
            case SAVE_BLOCK2 -> rom.saveBlock2Ptr;
        };
    }
}
