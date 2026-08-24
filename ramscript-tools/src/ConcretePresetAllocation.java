/* Concrete dry-run placement for one selected preset.
   Offsets are relative to the validated SaveBlock regions. A value of -1 means
   that component is not present for this deployment. */
record ConcretePresetAllocation(
        String presetId,
        PresetDeploymentKind deploymentKind,
        HotkeyBinding hotkeyBinding,
        int sb1GatewayOffset,
        int sb2FieldScriptOffset,
        int sb2FieldScriptSize,
        int sb2NativeModuleOffset,
        int sb2NativeModuleSize
) {
    boolean hasGateway() { return sb1GatewayOffset >= 0; }
    boolean hasSb2FieldScript() { return sb2FieldScriptOffset >= 0; }
    boolean hasNativeModule() { return sb2NativeModuleOffset >= 0; }
}
