/* Per-preset storage cost for one deployment mode.

   Shared runtime/service bytes intentionally do NOT live here. They are
   represented by PresetInfrastructure and will be accounted for once by the
   future composition planner.
*/
record PresetDeploymentCost(
        int ramScriptPayloadBytes,
        int sb1GatewayBytes,
        int sb2FieldScriptBytes,
        int sb2NativeModuleBytes,
        int requiredBaseAlignment
) {
    PresetDeploymentCost {
        if (ramScriptPayloadBytes < 0 || sb1GatewayBytes < 0 || sb2FieldScriptBytes < 0 ||
                sb2NativeModuleBytes < 0) {
            throw new IllegalArgumentException("preset deployment costs must not be negative");
        }
        if (requiredBaseAlignment <= 0 || (requiredBaseAlignment & (requiredBaseAlignment - 1)) != 0) {
            throw new IllegalArgumentException("required alignment must be a positive power of two");
        }
    }

    int sb2TotalBytes() {
        return sb2FieldScriptBytes + sb2NativeModuleBytes;
    }
}
