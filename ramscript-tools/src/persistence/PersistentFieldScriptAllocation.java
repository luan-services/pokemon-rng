record PersistentFieldScriptAllocation(
        String presetId,
        int sb1GatewayOffset,
        int gatewaySize,
        int gatewayDistanceFromRamScript,
        int sb2PayloadOffset,
        int payloadSize,
        boolean requiresHotkey
) {
    PersistentFieldScriptAllocation {
        if (presetId == null || presetId.isBlank()) throw new IllegalArgumentException("presetId must not be blank");
        if (gatewaySize <= 0 || payloadSize <= 0) throw new IllegalArgumentException("allocation sizes must be positive");
    }
}
