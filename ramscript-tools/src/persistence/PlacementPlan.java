import java.util.List;

record PlacementPlan(
        List<RamScriptAllocation> ramScriptAllocations,
        List<PersistentFieldScriptAllocation> persistentAllocations,
        PlacementDiagnostics diagnostics
) {
    PlacementPlan {
        if (ramScriptAllocations == null || persistentAllocations == null || diagnostics == null) {
            throw new IllegalArgumentException("placement plan fields must not be null");
        }
        ramScriptAllocations = List.copyOf(ramScriptAllocations);
        persistentAllocations = List.copyOf(persistentAllocations);
    }

    String report() {
        StringBuilder out = new StringBuilder();
        for (RamScriptAllocation allocation : ramScriptAllocations) {
            out.append(allocation.presetId()).append('\n')
                    .append(String.format("  -> RAMSCRIPT + 0x%03X%n", allocation.offset()))
                    .append(String.format("  -> %d B%n", allocation.size()));
        }
        for (PersistentFieldScriptAllocation allocation : persistentAllocations) {
            out.append(allocation.presetId()).append('\n')
                    .append(String.format("  -> SB2 + 0x%04X%n", allocation.sb2PayloadOffset()))
                    .append(String.format("  -> gateway SB1 + 0x%04X%n", allocation.sb1GatewayOffset()))
                    .append(String.format("  -> %d B payload + %d B gateway%n", allocation.payloadSize(), allocation.gatewaySize()));
        }
        out.append(String.format("RamScript free: %d B%n", diagnostics.ramScriptFree()));
        out.append(String.format("SB1 gateway free: %d B%n", diagnostics.sb1GatewayBytesFree()));
        out.append(String.format("SB2 free: %d B%n", diagnostics.sb2PayloadBytesFree()));
        return out.toString();
    }
}
