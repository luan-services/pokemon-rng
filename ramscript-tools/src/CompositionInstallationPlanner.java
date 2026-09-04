import java.util.ArrayList;
import java.util.List;

/*
   Converts a concrete placement into temporary installer stages.

   This planner models the validated batch-copy installer contract and packs
   <=255-byte copy chunks under the 995-byte temporary RamScript ceiling.
   InstallationEmitter materializes the resulting stages.
*/
final class CompositionInstallationPlanner {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final String STAGE_MESSAGE = "Installation stage complete.\\nSave, then continue.";

    private CompositionInstallationPlanner() {}

    static InstallationPlan plan(PresetCompositionPlan composition) {
        if (composition == null) throw new IllegalArgumentException("composition must not be null");

        boolean hasPersistent = composition.selections().stream().anyMatch(item ->
                item.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT
                        || item.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE);

        if (!hasPersistent) {
            return new InstallationPlan(
                    composition, List.of(), List.of(), true, composition.ramScriptBytes(),
                    false, 0,
                    List.of("local-only composition needs no persistent installer stages")
            );
        }

        List<InstallationWrite> writes = collectWrites(composition);
        List<InstallationChunk> chunks = chunk(writes);
        List<InstallationStage> stages = pack(composition.rom(), chunks);

        boolean runtimeRequired = composition.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME);
        int runtimeBytes = runtimeRequired ? composition.ramScriptBytes() : 0;
        List<String> diagnostics = new ArrayList<>();
        diagnostics.add("persistent data is split at the validated 0xFF-byte copy-operation limit");
        diagnostics.add("stages are greedily packed against the exact 995-byte temporary RamScript ceiling");
        if (runtimeRequired) diagnostics.add("shared runtime/service is installed after persistent stages");

        return new InstallationPlan(
                composition, writes, stages, false, 0,
                runtimeRequired, runtimeBytes, diagnostics
        );
    }

    private static List<InstallationWrite> collectWrites(PresetCompositionPlan composition) {
        ConcreteCompositionLayout layout = composition.concreteLayout();
        List<InstallationWrite> out = new ArrayList<>();

        // Manifest is written last logically so a partial installation is never
        // mistaken for a complete toolkit installation by Cleaner.

        if (layout.hasNativeCatalog()) {
            out.add(new InstallationWrite("native-catalog", InstallationTarget.SAVE_BLOCK2,
                    layout.nativeCatalogOffset(), layout.nativeCatalogSize()));
        }

        // Gateways are installed before their target Field Scripts. They are tiny,
        // but keeping them as explicit writes lets incremental installation reason
        // about them independently later.
        for (ConcretePresetAllocation allocation : layout.allocations()) {
            if (allocation.hasGateway()) {
                out.add(new InstallationWrite(allocation.presetId() + ":gateway", InstallationTarget.SAVE_BLOCK1,
                        allocation.sb1GatewayOffset(), PayloadPlacementPlanner.GATEWAY_SIZE));
            }
        }

        for (ConcretePresetAllocation allocation : layout.allocations()) {
            if (allocation.hasSb2FieldScript()) {
                out.add(new InstallationWrite(allocation.presetId() + ":field", InstallationTarget.SAVE_BLOCK2,
                        allocation.sb2FieldScriptOffset(), allocation.sb2FieldScriptSize()));
            }
        }
        out.add(new InstallationWrite("installation-manifest", InstallationTarget.SAVE_BLOCK2,
                InstallationManifest.OFFSET, InstallationManifest.SIZE));
        return List.copyOf(out);
    }

    private static List<InstallationChunk> chunk(List<InstallationWrite> writes) {
        List<InstallationChunk> out = new ArrayList<>();
        for (InstallationWrite write : writes) {
            int consumed = 0;
            while (consumed < write.size()) {
                int n = Math.min(0xFF, write.size() - consumed);
                out.add(new InstallationChunk(write.componentId(), write.target(), write.offset() + consumed, n));
                consumed += n;
            }
        }
        return out;
    }

    private static List<InstallationStage> pack(RomProfile rom, List<InstallationChunk> chunks) {
        List<InstallationStage> out = new ArrayList<>();
        List<InstallationChunk> current = new ArrayList<>();

        for (InstallationChunk chunk : chunks) {
            List<InstallationChunk> candidate = new ArrayList<>(current);
            candidate.add(chunk);
            int candidateSize = estimateInstallerBytes(rom, candidate);
            if (candidateSize <= RamScript.SCRIPT_SIZE) {
                current.add(chunk);
                continue;
            }

            if (current.isEmpty()) {
                throw new IllegalArgumentException("single persistent copy chunk cannot fit temporary installer: " + chunk.componentId());
            }
            out.add(stage(out.size() + 1, rom, current));
            current = new ArrayList<>();
            current.add(chunk);
            if (estimateInstallerBytes(rom, current) > RamScript.SCRIPT_SIZE) {
                throw new IllegalArgumentException("single persistent copy chunk cannot fit temporary installer: " + chunk.componentId());
            }
        }
        if (!current.isEmpty()) out.add(stage(out.size() + 1, rom, current));
        return List.copyOf(out);
    }

    private static InstallationStage stage(int index, RomProfile rom, List<InstallationChunk> chunks) {
        int persistent = chunks.stream().mapToInt(InstallationChunk::size).sum();
        return new InstallationStage(index, chunks, persistent, estimateInstallerBytes(rom, chunks));
    }

    /* Exact encoded-size dry run using the same NativeHelperInstaller decisions as
       production. Helper contents do not affect encoding size, only helper length. */
    static int estimateInstallerBytes(RomProfile rom, List<InstallationChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) throw new IllegalArgumentException("chunks must not be empty");
        int helperSize = batchHelperSize(chunks);
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = new NativeHelper(helperAddress, new byte[helperSize]);
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        builder.setVAddress();
        try {
            NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                    builder, VIRTUAL_BASE, helper, copier, "install_plan", NativeHelperInstaller.Mode.AUTO);
            builder.lockAll();
            install.installAndCall(builder);
            builder.vMessage("ok").waitMessage().waitButtonPress().releaseAll().end().text("ok", STAGE_MESSAGE);
            return builder.buildScript().length;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Integer.MAX_VALUE;
        }
    }

    private static int batchHelperSize(List<InstallationChunk> chunks) {
        int count = chunks.size();
        int codeEnd = count * 24;
        int bxOffset = codeEnd;
        int literalOffset = align4(bxOffset + 2);
        int dataOffset = align4(literalOffset + count * 12);
        for (InstallationChunk chunk : chunks) dataOffset = align4(dataOffset + chunk.size());
        return dataOffset;
    }

    private static int align4(int value) { return (value + 3) & ~3; }
}
