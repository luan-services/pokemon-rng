import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
   Dry-run automatic composition planner.

   It intentionally consumes PresetCatalog metadata rather than knowing preset
   classes by name. Multi-preset local packing is NOT claimed yet: for more
   than one selected preset, the planner only considers the already-validated
   shared persistent deployment families.
*/
final class PresetCompositionPlanner {
    private static final int SHARED_NATIVE_STAGING_CAPACITY = 0x140; // Build-26..28 validated service profile.
    private static final List<HotkeyButton> DUMMY_BUTTONS = List.of(
            HotkeyButton.A, HotkeyButton.B, HotkeyButton.SELECT, HotkeyButton.START,
            HotkeyButton.RIGHT, HotkeyButton.LEFT, HotkeyButton.UP, HotkeyButton.DOWN
    );

    private PresetCompositionPlanner() {}

    static PresetCompositionPlan plan(RomProfile rom, List<String> presetIds) {
        if (rom == null) throw new IllegalArgumentException("ROM profile must not be null");
        if (presetIds == null || presetIds.isEmpty()) throw new IllegalArgumentException("select at least one preset");

        List<PresetDefinition> presets = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : presetIds) {
            PresetDefinition preset = PresetCatalog.byId(id);
            if (!seen.add(preset.id())) throw new IllegalArgumentException("duplicate preset selection: " + preset.id());
            if (!preset.supports(rom)) throw new IllegalArgumentException(preset.id() + " does not support " + rom.displayName());
            presets.add(preset);
        }

        List<List<PresetDeploymentDefinition>> options = new ArrayList<>();
        boolean multi = presets.size() > 1;
        for (PresetDefinition preset : presets) {
            List<PresetDeploymentDefinition> choices = new ArrayList<>();
            for (PresetDeploymentDefinition deployment : preset.deployments()) {
                if (!multi || isSharedPersistent(deployment.kind())) choices.add(deployment);
            }
            if (choices.isEmpty()) {
                throw new IllegalArgumentException("no composable deployment for " + preset.id() +
                        (multi ? " in a multi-preset plan" : ""));
            }
            options.add(choices);
        }

        Candidate best = search(rom, presets, options, 0, new ArrayList<>(), null);
        if (best == null) throw new IllegalArgumentException("no valid composition fits current memory/runtime constraints");
        return best.plan;
    }

    private static Candidate search(
            RomProfile rom,
            List<PresetDefinition> presets,
            List<List<PresetDeploymentDefinition>> options,
            int index,
            List<PresetDeploymentDefinition> selected,
            Candidate best
    ) {
        if (index == presets.size()) {
            try {
                PresetCompositionPlan plan = evaluate(rom, presets, selected);
                int score = plan.ramScriptBytes() + plan.sb1Bytes() + plan.sb2Bytes();
                int complexity = plan.infrastructure().size();
                Candidate candidate = new Candidate(plan, score, complexity);
                if (best == null || candidate.betterThan(best)) return candidate;
            } catch (IllegalArgumentException ignored) {
                // Candidate does not fit or combines incompatible infrastructure.
            }
            return best;
        }
        for (PresetDeploymentDefinition choice : options.get(index)) {
            selected.add(choice);
            best = search(rom, presets, options, index + 1, selected, best);
            selected.remove(selected.size() - 1);
        }
        return best;
    }

    private static PresetCompositionPlan evaluate(
            RomProfile rom,
            List<PresetDefinition> presets,
            List<PresetDeploymentDefinition> deployments
    ) {
        EnumSet<PresetInfrastructure> infrastructure = EnumSet.noneOf(PresetInfrastructure.class);
        List<PresetCompositionPlan.SelectedPresetDeployment> chosen = new ArrayList<>();
        int sb1 = 0;
        int localPayload = 0;
        int hotkeyBindings = 0;
        int nativeCount = 0;
        int maxNativeSize = 0;

        for (int i = 0; i < presets.size(); i++) {
            PresetDefinition preset = presets.get(i);
            PresetDeploymentDefinition deployment = deployments.get(i);
            PresetDeploymentCost cost = deployment.cost(rom);
            infrastructure.addAll(deployment.infrastructure());
            chosen.add(new PresetCompositionPlan.SelectedPresetDeployment(preset, deployment, cost));
            sb1 += cost.sb1GatewayBytes();
            localPayload += cost.ramScriptPayloadBytes();
            if (deployment.kind() == PresetDeploymentKind.HOTKEY_LOCAL || isSharedPersistent(deployment.kind())) {
                hotkeyBindings++;
            }
            if (deployment.kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE) {
                nativeCount++;
                maxNativeSize = Math.max(maxNativeSize, cost.sb2NativeModuleBytes());
            }
        }

        if (infrastructure.contains(PresetInfrastructure.HOTKEY_RUNTIME)
                && infrastructure.contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            throw new IllegalArgumentException("cannot combine local and shared hotkey runtimes");
        }
        if (hotkeyBindings > 8) throw new IllegalArgumentException("shared runtime supports at most eight bindings");
        if (sb1 > PayloadStorageArea.SAVE_BLOCK1.capacity()) throw new IllegalArgumentException("SB1 capacity exceeded");
        if (maxNativeSize > SHARED_NATIVE_STAGING_CAPACITY) {
            throw new IllegalArgumentException("native module exceeds validated shared staging capacity");
        }

        int ramScript = localPayload;
        if (infrastructure.contains(PresetInfrastructure.HOTKEY_RUNTIME)) {
            if (presets.size() != 1 || hotkeyBindings != 1) {
                throw new IllegalArgumentException("local HotkeyRuntimeV1 is only modeled for one-preset plans");
            }
            int payloadBytes = chosen.get(0).cost().ramScriptPayloadBytes();
            ramScript = HotkeyRuntimeV1.scriptSize(rom, new byte[payloadBytes]);
        } else if (infrastructure.contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            if (hotkeyBindings < 1) throw new IllegalArgumentException("shared runtime needs bindings");
            List<SharedHotkeyDispatcher.Entry> entries = new ArrayList<>();
            for (int i = 0; i < hotkeyBindings; i++) {
                entries.add(new SharedHotkeyDispatcher.Entry(DUMMY_BUTTONS.get(i), 0));
            }
            byte[] support = new byte[0];
            int supportAlignment = 1;
            if (infrastructure.contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)) {
                int serviceOffset = SharedPersistentNativeStagingService.offsetForBindings(hotkeyBindings, 4);
                SharedPersistentNativeStagingService.Build service = SharedPersistentNativeStagingService.build(
                        rom, serviceOffset, rom.stringVar4 + 0x140L, SHARED_NATIVE_STAGING_CAPACITY);
                support = service.fieldScript();
                supportAlignment = service.requiredBaseAlignment();
            }
            ramScript = SharedHotkeyRuntime.compose(
                    rom, HotkeyButton.R, entries, support, supportAlignment).totalScriptBytes();
        }

        int sb2Payload = calculateSb2(chosen, nativeCount);
        boolean persistentInstall = chosen.stream().anyMatch(item -> isSharedPersistent(item.deployment().kind()));
        if (persistentInstall && sb2Payload > InstallationManifest.OFFSET - PayloadStorageArea.SAVE_BLOCK2.offset()) {
            throw new IllegalArgumentException("SB2 payload capacity exceeded after reserving installation manifest");
        }
        int sb2 = sb2Payload + (persistentInstall ? InstallationManifest.SIZE : 0);
        if (ramScript > RamScript.SCRIPT_SIZE) throw new IllegalArgumentException("RamScript capacity exceeded");
        if (sb2 > PayloadStorageArea.SAVE_BLOCK2.capacity()) throw new IllegalArgumentException("SB2 capacity exceeded");

        List<String> diagnostics = new ArrayList<>();
        boolean allValidated = true;
        for (var item : chosen) allValidated &= item.deployment().isValidatedOn(rom);
        if (!allValidated) diagnostics.add("one or more selected deployment modes are not runtime-validated on this ROM");
        if (chosen.stream().anyMatch(item -> item.deployment().kind() == PresetDeploymentKind.DEDICATED_LOCAL))
            diagnostics.add("selected preset is exclusive/dedicated and currently cannot participate in a multi-preset composition");
        if (presets.size() > 1) diagnostics.add("multi-preset local packing is intentionally not modeled yet; shared deployments were selected");
        if (nativeCount > 0) diagnostics.add("native catalog overhead/padding is included once for " + nativeCount + " module(s)");

        ConcreteCompositionLayout concreteLayout = CompositionLayoutPlanner.layout(chosen);

        return new PresetCompositionPlan(
                rom, chosen, infrastructure,
                ramScript, sb1, sb2,
                RamScript.SCRIPT_SIZE - ramScript,
                PayloadStorageArea.SAVE_BLOCK1.capacity() - sb1,
                PayloadStorageArea.SAVE_BLOCK2.capacity() - sb2,
                hotkeyBindings,
                infrastructure.contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                concreteLayout,
                diagnostics
        );
    }

    private static int calculateSb2(
            List<PresetCompositionPlan.SelectedPresetDeployment> selected,
            int nativeCount
    ) {
        int cursor = 0;
        if (nativeCount > 0) {
            int tableEnd = PersistentNativeCatalogFormat.HEADER_SIZE
                    + PersistentNativeCatalogFormat.ENTRY_SIZE * nativeCount;
            cursor = align(tableEnd, 4);
            for (var item : selected) {
                if (item.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE) {
                    cursor = align(cursor + item.cost().sb2NativeModuleBytes(), 4);
                }
            }
        }
        for (var item : selected) {
            int fieldBytes = item.cost().sb2FieldScriptBytes();
            if (fieldBytes == 0) continue;
            cursor = align(cursor, item.cost().requiredBaseAlignment());
            cursor += fieldBytes;
        }
        return cursor;
    }

    private static boolean isSharedPersistent(PresetDeploymentKind kind) {
        return kind == PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT
                || kind == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE;
    }

    private static int align(int value, int alignment) {
        if (alignment <= 1) return value;
        return (value + alignment - 1) & ~(alignment - 1);
    }

    private record Candidate(PresetCompositionPlan plan, int totalBytes, int complexity) {
        boolean betterThan(Candidate other) {
            if (totalBytes != other.totalBytes) return totalBytes < other.totalBytes;
            return complexity < other.complexity;
        }
    }
}
