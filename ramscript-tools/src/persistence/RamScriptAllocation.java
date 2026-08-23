record RamScriptAllocation(String presetId, int offset, int size, boolean requiresHotkey) {
    RamScriptAllocation {
        if (presetId == null || presetId.isBlank()) throw new IllegalArgumentException("presetId must not be blank");
        if (offset < 0 || size <= 0 || offset + size > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("invalid RamScript allocation");
        }
    }
}
