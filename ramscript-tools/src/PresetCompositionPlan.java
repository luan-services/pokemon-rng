import java.util.List;
import java.util.Set;

/* Planner result. This is still a dry-run description: it does not emit WC3s. */
record PresetCompositionPlan(
        RomProfile rom,
        List<SelectedPresetDeployment> selections,
        Set<PresetInfrastructure> infrastructure,
        int ramScriptBytes,
        int sb1Bytes,
        int sb2Bytes,
        int ramScriptFree,
        int sb1Free,
        int sb2Free,
        int hotkeyBindings,
        boolean usesSharedNativeStaging,
        ConcreteCompositionLayout concreteLayout,
        List<String> diagnostics
) {
    PresetCompositionPlan {
        selections = List.copyOf(selections);
        infrastructure = Set.copyOf(infrastructure);
        if (concreteLayout == null) throw new IllegalArgumentException("concrete layout must not be null");
        diagnostics = List.copyOf(diagnostics);
    }

    record SelectedPresetDeployment(
            PresetDefinition preset,
            PresetDeploymentDefinition deployment,
            PresetDeploymentCost cost
    ) {}

    String report() {
        StringBuilder out = new StringBuilder();
        out.append("Composition plan for ").append(rom.displayName()).append("\n");
        out.append("\nPresets\n");
        for (SelectedPresetDeployment selected : selections) {
            PresetDeploymentCost c = selected.cost();
            out.append("  ").append(selected.preset().displayName())
                    .append(" -> ").append(selected.deployment().kind())
                    .append(selected.deployment().isValidatedOn(rom) ? " [validated]" : " [not runtime-validated]")
                    .append("\n")
                    .append("    RAM payload ").append(c.ramScriptPayloadBytes()).append(" B")
                    .append(", SB1 ").append(c.sb1GatewayBytes()).append(" B")
                    .append(", SB2 field/bridge ").append(c.sb2FieldScriptBytes()).append(" B")
                    .append(", native body ").append(c.sb2NativeModuleBytes()).append(" B")
                    .append("\n");
        }
        out.append("\nInfrastructure\n");
        if (infrastructure.isEmpty()) out.append("  none\n");
        else for (PresetInfrastructure item : infrastructure) out.append("  ").append(item).append("\n");
        out.append("\nMemory\n")
                .append("  RamScript ").append(ramScriptBytes).append(" / ").append(RamScript.SCRIPT_SIZE)
                .append(" B (free ").append(ramScriptFree).append(")\n")
                .append("  SB1       ").append(sb1Bytes).append(" / ").append(PayloadStorageArea.SAVE_BLOCK1.capacity())
                .append(" B (free ").append(sb1Free).append(")\n")
                .append("  SB2       ").append(sb2Bytes).append(" / ").append(PayloadStorageArea.SAVE_BLOCK2.capacity())
                .append(" B (free ").append(sb2Free).append(")\n")
                .append("  hotkey bindings: ").append(hotkeyBindings).append("\n")
                .append("  shared native staging: ").append(usesSharedNativeStaging ? "yes" : "no").append("\n");

        out.append("\nConcrete layout\n");
        out.append("  hotkey runtime: ").append(concreteLayout.bindingPlan().runtime()).append("\n");
        if (concreteLayout.hasNativeCatalog()) {
            out.append(String.format("  native catalog SB2+0x%04X, %d B%n",
                    concreteLayout.nativeCatalogOffset(), concreteLayout.nativeCatalogSize()));
        }
        for (ConcretePresetAllocation allocation : concreteLayout.allocations()) {
            out.append("  ").append(allocation.presetId()).append("\n");
            if (allocation.hotkeyBinding() != null) {
                out.append("    hotkey ").append(allocation.hotkeyBinding().hotkey().displayName()).append("\n");
            }
            if (allocation.hasGateway()) {
                out.append(String.format("    gateway SB1+0x%04X%n", allocation.sb1GatewayOffset()));
            }
            if (allocation.hasNativeModule()) {
                out.append(String.format("    native  SB2+0x%04X, %d B%n",
                        allocation.sb2NativeModuleOffset(), allocation.sb2NativeModuleSize()));
            }
            if (allocation.hasSb2FieldScript()) {
                out.append(String.format("    field   SB2+0x%04X, %d B%n",
                        allocation.sb2FieldScriptOffset(), allocation.sb2FieldScriptSize()));
            }
            if (!allocation.hasGateway() && !allocation.hasNativeModule() && !allocation.hasSb2FieldScript()) {
                out.append("    local RamScript payload\n");
            }
        }
        if (!diagnostics.isEmpty()) {
            out.append("\nDiagnostics\n");
            for (String diagnostic : diagnostics) out.append("  - ").append(diagnostic).append("\n");
        }
        return out.toString();
    }
}
