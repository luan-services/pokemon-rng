/* Semantic description of an existing map object that can host a RamScript.
   The numeric ids are game data, but presets should depend on this descriptor
   rather than scattering raw mapGroup/mapNum/localId literals. */
record ObjectEventTarget(
        String id,
        String displayName,
        int mapGroup,
        int mapNum,
        int localId,
        int x,
        int y,
        String originalScript,
        ObjectEventSafety safety
) {
    ObjectEventTarget {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Object target id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Object target displayName must not be blank");
        if ((mapGroup & ~0xFF) != 0 || (mapNum & ~0xFF) != 0 || (localId & ~0xFF) != 0)
            throw new IllegalArgumentException("Object event ids must fit in one byte");
    }
}
