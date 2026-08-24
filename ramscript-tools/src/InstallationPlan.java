import java.util.List;

/* Dry-run sequence of WC deliveries required by a composition. */
record InstallationPlan(
        PresetCompositionPlan composition,
        List<InstallationWrite> writes,
        List<InstallationStage> persistentStages,
        boolean localOnly,
        int localRamScriptBytes,
        boolean runtimeStageRequired,
        int runtimeStageBytes,
        List<String> diagnostics
) {
    InstallationPlan {
        if (composition == null) throw new IllegalArgumentException("composition must not be null");
        writes = List.copyOf(writes);
        persistentStages = List.copyOf(persistentStages);
        diagnostics = List.copyOf(diagnostics);
    }

    int totalWonderCards() {
        if (localOnly) return 1;
        return persistentStages.size() + (runtimeStageRequired ? 1 : 0);
    }

    String report() {
        StringBuilder out = new StringBuilder();
        out.append("Installation plan for ").append(composition.rom().displayName()).append("\n");
        if (localOnly) {
            out.append("\nLocal deployment\n")
                    .append("  one WC / RamScript: ").append(localRamScriptBytes).append(" / ")
                    .append(RamScript.SCRIPT_SIZE).append(" B\n")
                    .append("  persistent writes: none\n")
                    .append("  runtime installation step: included in local payload when required\n")
                    .append("\nTotal Wonder Cards: 1\n");
            return out.toString();
        }

        out.append("\nPersistent writes\n");
        for (InstallationWrite write : writes) {
            out.append(String.format("  %-24s %s+0x%04X, %d B%n",
                    write.componentId(), write.target() == InstallationTarget.SAVE_BLOCK1 ? "SB1" : "SB2",
                    write.offset(), write.size()));
        }

        out.append("\nInstaller stages\n");
        for (InstallationStage stage : persistentStages) {
            out.append("  WC ").append(stage.index())
                    .append(": ").append(stage.persistentBytes()).append(" B persistent data, ")
                    .append(stage.encodedInstallerBytes()).append(" / ").append(RamScript.SCRIPT_SIZE)
                    .append(" B temporary RamScript\n");
            for (InstallationChunk chunk : stage.chunks()) {
                out.append(String.format("    %-22s %s+0x%04X, %d B%n",
                        chunk.componentId(), chunk.target() == InstallationTarget.SAVE_BLOCK1 ? "SB1" : "SB2",
                        chunk.offset(), chunk.size()));
            }
        }
        if (runtimeStageRequired) {
            out.append("  Runtime WC: ").append(runtimeStageBytes).append(" / ")
                    .append(RamScript.SCRIPT_SIZE).append(" B\n");
        }
        out.append("\nTotal Wonder Cards: ").append(totalWonderCards()).append("\n");
        if (!diagnostics.isEmpty()) {
            out.append("\nDiagnostics\n");
            for (String item : diagnostics) out.append("  - ").append(item).append("\n");
        }
        return out.toString();
    }
}
