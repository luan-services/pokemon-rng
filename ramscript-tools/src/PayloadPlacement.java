/* Placement requested for a payload.

   AUTO is a build-time policy. The remaining values are physical locations.
*/
enum PayloadPlacement {
    AUTO,
    RAMSCRIPT,
    SAVE_BLOCK1,
    SAVE_BLOCK2;

    PayloadStorageArea area() {
        return switch (this) {
            case RAMSCRIPT -> PayloadStorageArea.RAMSCRIPT;
            case SAVE_BLOCK1 -> PayloadStorageArea.SAVE_BLOCK1;
            case SAVE_BLOCK2 -> PayloadStorageArea.SAVE_BLOCK2;
            case AUTO -> throw new IllegalStateException("AUTO has no physical area until planned");
        };
    }
}
