record PlacementDiagnostics(
        int ramScriptUsed,
        int ramScriptFree,
        int sb1GatewayBytesUsed,
        int sb1GatewayBytesFree,
        int sb2PayloadBytesUsed,
        int sb2PayloadBytesFree
) {
    PlacementDiagnostics {
        if (ramScriptUsed < 0 || ramScriptFree < 0 || sb1GatewayBytesUsed < 0 ||
                sb1GatewayBytesFree < 0 || sb2PayloadBytesUsed < 0 || sb2PayloadBytesFree < 0) {
            throw new IllegalArgumentException("diagnostic byte counts must not be negative");
        }
    }
}
