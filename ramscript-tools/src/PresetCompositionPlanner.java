import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
   Dry-run preset composition planner.

   It consumes PresetCatalog metadata and preserves the historical preferred
   deployment when it fits. For multi-hotkey compositions, an explicit
   Shared-local deployment may be selected as a fallback when the preferred
   persistent layout exceeds a modeled storage capacity. Only presets that
   advertise a relocation-safe SHARED_LOCAL_FIELD_SCRIPT are eligible.
*/
final class PresetCompositionPlanner {
    private static final int SHARED_NATIVE_STAGING_CAPACITY = 0x140; // Build-26..28 validated service profile.
    private static final List<HotkeyButton> DUMMY_BUTTONS = List.of(
            HotkeyButton.A, HotkeyButton.B, HotkeyButton.SELECT, HotkeyButton.START,
            HotkeyButton.RIGHT, HotkeyButton.LEFT, HotkeyButton.UP, HotkeyButton.DOWN
    );

    private PresetCompositionPlanner() {}

    static PresetCompositionPlan planHotkeys(RomProfile rom, List<String> presetIds) {
        List<PresetSelection> selections = new ArrayList<>();
        for (String id : presetIds) selections.add(PresetSelection.hotkey(id));
        return planSelections(rom, selections);
    }

    static PresetCompositionPlan planDeliveryman(RomProfile rom, String presetId) {
        return planSelections(rom, List.of(PresetSelection.deliveryman(presetId)));
    }

    static PresetCompositionPlan planSelections(RomProfile rom, List<PresetSelection> selections) {
        if (rom == null) throw new IllegalArgumentException("ROM profile must not be null");
        if (selections == null || selections.isEmpty()) throw new IllegalArgumentException("select at least one preset");

        List<PresetDefinition> presets = new ArrayList<>();
        List<PresetDeploymentDefinition> deployments = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int hotkeyCount = 0;
        int deliverymanCount = 0;

        for (PresetSelection selection : selections) {
            PresetDefinition preset = PresetCatalog.byId(selection.presetId());
            if (!seen.add(preset.id())) throw new IllegalArgumentException("duplicate preset selection: " + preset.id());
            if (!preset.supports(rom)) throw new IllegalArgumentException(preset.id() + " does not support " + rom.displayName());
            presets.add(preset);
            if (selection.activation() == PresetActivation.HOTKEY) hotkeyCount++;
            else deliverymanCount++;
        }

        if (hotkeyCount > 0 && deliverymanCount > 0) {
            throw new IllegalArgumentException("one composition cannot mix Deliveryman and hotkey activation");
        }
        if (deliverymanCount > 1) {
            throw new IllegalArgumentException("Deliveryman activation currently supports one preset per Wonder Card");
        }

        validatePresetConflicts(presets);

        boolean sharedCompositionNeedsNativeService = hotkeyCount > 1 && presets.stream()
                .filter(p -> !p.id().equals("run-bike-anywhere") && !p.id().equals("run-anywhere"))
                .anyMatch(p -> p.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE));

        for (int i = 0; i < selections.size(); i++) {
            PresetSelection selection = selections.get(i);
            PresetDefinition preset = presets.get(i);
            PresetDeploymentKind kind;
            if (selection.activation() == PresetActivation.HOTKEY) {
                if (!preset.hotkeyCapable()) throw new IllegalArgumentException(preset.id() + " does not support hotkey activation");
                kind = hotkeyCount == 1
                        ? PresetDeploymentKind.HOTKEY_LOCAL
                        : sharedDeploymentKind(preset, sharedCompositionNeedsNativeService);
            } else {
                if (preset.supportsDeployment(PresetDeploymentKind.DELIVERYMAN_LOCAL)) {
                    kind = PresetDeploymentKind.DELIVERYMAN_LOCAL;
                } else if (preset.supportsDeployment(PresetDeploymentKind.DEDICATED_LOCAL)) {
                    kind = PresetDeploymentKind.DEDICATED_LOCAL;
                } else {
                    throw new IllegalArgumentException(preset.id() + " does not support Deliveryman/direct activation");
                }
            }
            if (!preset.supportsDeployment(kind)) {
                throw new IllegalArgumentException(preset.id() + " does not support required deployment " + kind);
            }
            deployments.add(preset.deployment(kind));
        }

        if (hotkeyCount > 1) {
            return evaluateSharedWithFallback(rom, presets, deployments);
        }
        return evaluate(rom, presets, deployments);
    }


    private static void validatePresetConflicts(List<PresetDefinition> presets) {
        Set<String> ids = new HashSet<>();
        for (PresetDefinition preset : presets) ids.add(preset.id());
        rejectRedundantViewerPair(ids, "party-iv-viewer", "lead-iv-viewer", "iv-viewer-scope");
        rejectRedundantViewerPair(ids, "party-ev-viewer", "lead-ev-viewer", "ev-viewer-scope");
    }

    private static void rejectRedundantViewerPair(Set<String> ids, String partyId, String leadId, String field) {
        if (!ids.contains(partyId) || !ids.contains(leadId)) return;
        throw new CompositionPlanningException(
                "PRESET_CONFLICT",
                "redundant viewer presets cannot be selected together: " + partyId + ", " + leadId,
                List.of(partyId, leadId),
                field
        );
    }

    private static PresetCompositionPlan evaluateSharedWithFallback(
            RomProfile rom,
            List<PresetDefinition> presets,
            List<PresetDeploymentDefinition> preferred
    ) {
        try {
            return evaluate(rom, presets, preferred);
        } catch (IllegalArgumentException baselineFailure) {
            if (!isCapacityFailure(baselineFailure)) throw baselineFailure;

            List<PresetCompositionPlan> candidates = new ArrayList<>();
            enumerateSharedAlternatives(rom, presets, preferred, 0, new ArrayList<>(), candidates);
            if (candidates.isEmpty()) throw baselineFailure;

            candidates.sort((a, b) -> {
                int c = Integer.compare(deploymentChanges(a, preferred), deploymentChanges(b, preferred));
                if (c != 0) return c;
                c = Integer.compare(a.sb2Bytes(), b.sb2Bytes());
                if (c != 0) return c;
                c = Integer.compare(a.ramScriptBytes(), b.ramScriptBytes());
                if (c != 0) return c;
                return Integer.compare(a.sb1Bytes(), b.sb1Bytes());
            });
            return candidates.get(0);
        }
    }

    private static void enumerateSharedAlternatives(
            RomProfile rom,
            List<PresetDefinition> presets,
            List<PresetDeploymentDefinition> preferred,
            int index,
            List<PresetDeploymentDefinition> current,
            List<PresetCompositionPlan> out
    ) {
        if (index == presets.size()) {
            boolean changed = false;
            for (int i = 0; i < current.size(); i++) {
                changed |= current.get(i).kind() != preferred.get(i).kind();
            }
            if (!changed) return;
            try {
                out.add(evaluate(rom, presets, List.copyOf(current)));
            } catch (IllegalArgumentException ignored) {
                // Candidate is simply not feasible; the preferred failure is
                // retained if no advertised alternative produces a valid plan.
            }
            return;
        }

        PresetDefinition preset = presets.get(index);
        PresetDeploymentDefinition base = preferred.get(index);
        List<PresetDeploymentKind> kinds = new ArrayList<>();
        kinds.add(base.kind());
        for (PresetDeploymentKind kind : List.of(
                PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT,
                PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)) {
            if (kind != base.kind() && preset.supportsDeployment(kind)) kinds.add(kind);
        }
        for (PresetDeploymentKind kind : kinds) {
            current.add(preset.deployment(kind));
            enumerateSharedAlternatives(rom, presets, preferred, index + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    private static int deploymentChanges(PresetCompositionPlan plan, List<PresetDeploymentDefinition> preferred) {
        int changes = 0;
        for (int i = 0; i < plan.selections().size(); i++) {
            if (plan.selections().get(i).deployment().kind() != preferred.get(i).kind()) changes++;
        }
        return changes;
    }

    private static boolean isCapacityFailure(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null) return false;
        return message.contains("capacity exceeded")
                || message.contains("maximum is " + RamScript.SCRIPT_SIZE)
                || message.contains("requires") && message.contains("bytes; maximum is");
    }

    private static PresetDeploymentKind sharedDeploymentKind(
            PresetDefinition preset, boolean compositionAlreadyNeedsNativeService
    ) {
        // Mobility toggles are cheapest as local Shared payloads when they are
        // the only native-like feature. If another selected preset already
        // requires the shared staging service, reuse it and move the toggle
        // helper to the persistent native catalog instead of spending ~271 B
        // of the final Runtime RamScript.
        if ((preset.id().equals("run-bike-anywhere") || preset.id().equals("run-anywhere"))
                && preset.supportsDeployment(PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)
                && (!compositionAlreadyNeedsNativeService
                    || !preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE))) {
            return PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT;
        }
        if (preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE))
            return PresetDeploymentKind.SHARED_PERSISTENT_NATIVE;
        if (preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT))
            return PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT;
        if (preset.supportsDeployment(PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT))
            return PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT;
        throw new IllegalArgumentException(preset.id() + " does not support SharedHotkeyRuntime deployment");
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
            if (deployment.kind() == PresetDeploymentKind.HOTKEY_LOCAL || isSharedHotkeyDeployment(deployment.kind())) {
                hotkeyBindings++;
            }
            if (deployment.kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE) {
                nativeCount++;
                maxNativeSize = Math.max(maxNativeSize, cost.sb2NativeModuleBytes());
            }
        }

        List<PresetResourceAllocation> resources = validateOwnedResources(chosen);
        validateHotkeyConflicts(chosen);

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
            if (chosen.get(0).preset().id().equals("run-anywhere")) {
                ramScript = RunAnywhereHotkeyRuntimeV1.scriptSize(rom);
            } else if (chosen.get(0).preset().id().equals("run-bike-anywhere")) {
                ramScript = RunBikeAnywhereHotkeyRuntimeV1.scriptSize(rom);
            } else {
                ramScript = HotkeyRuntimeV1.scriptSize(rom, new byte[payloadBytes]);
            }
        } else if (infrastructure.contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            if (hotkeyBindings < 1) throw new IllegalArgumentException("shared runtime needs bindings");
            List<SharedHotkeyDispatcher.Entry> entries = new ArrayList<>();
            for (int i = 0; i < hotkeyBindings; i++) {
                entries.add(new SharedHotkeyDispatcher.Entry(DUMMY_BUTTONS.get(i), 0));
            }
            boolean runAnywhere = infrastructure.contains(PresetInfrastructure.RUN_ANYWHERE_EWRAM_SIDECAR);
            boolean runBikeAnywhere = infrastructure.contains(PresetInfrastructure.RUN_BIKE_ANYWHERE_EWRAM_SIDECAR);
            boolean residentMobilitySidecar = runAnywhere || runBikeAnywhere;
            boolean nativeService = infrastructure.contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE);
            List<String> localPresetIds = chosen.stream()
                    .filter(item -> item.deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)
                    .map(item -> item.preset().id())
                    .toList();
            SharedRuntimeSupportLayout supportLayout = SharedRuntimeSupportLayout.build(
                    rom, hotkeyBindings, runAnywhere, runBikeAnywhere, localPresetIds, nativeService, SHARED_NATIVE_STAGING_CAPACITY);
            for (int i = 0; i < chosen.size(); i++) {
                if (chosen.get(i).deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT) {
                    int target = supportLayout.localPayloadOffset(chosen.get(i).preset().id());
                    if (target < 0) throw new IllegalArgumentException("missing Shared-local payload offset for " + chosen.get(i).preset().id());
                    entries.set(i, new SharedHotkeyDispatcher.Entry(DUMMY_BUTTONS.get(i), target));
                }
            }
            SharedHotkeyRuntime.ResidentSidecar mobilitySidecar = runBikeAnywhere
                    ? RunBikeAnywhereSharedPreset.residentSidecar(rom)
                    : (runAnywhere ? RunAnywhereSharedPreset.residentSidecar(rom) : null);
            ramScript = residentMobilitySidecar
                    ? SharedHotkeyRuntime.composeWithResidentSidecarNativeCopy(
                            rom, HotkeyButton.R, entries, supportLayout.support(), supportLayout.alignment(),
                            mobilitySidecar).totalScriptBytes()
                    : SharedHotkeyRuntime.compose(
                            rom, HotkeyButton.R, entries, supportLayout.support(), supportLayout.alignment()).totalScriptBytes();
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
        if (presets.size() > 1) {
            List<String> localIds = chosen.stream()
                    .filter(item -> item.deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)
                    .map(item -> item.preset().id()).toList();
            if (localIds.isEmpty()) {
                diagnostics.add("preferred shared persistent deployments fit; no Shared-local fallback was needed");
            } else {
                diagnostics.add("resource-aware Shared packing placed local payload(s) in Runtime RamScript: " + String.join(", ", localIds));
            }
        }
        if (nativeCount > 0) diagnostics.add("native catalog overhead/padding is included once for " + nativeCount + " module(s)");

        ConcreteCompositionLayout concreteLayout = CompositionLayoutPlanner.layout(chosen);

        return new PresetCompositionPlan(
                rom, chosen, infrastructure, resources,
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

    private static List<PresetResourceAllocation> validateOwnedResources(
            List<PresetCompositionPlan.SelectedPresetDeployment> chosen
    ) {
        Map<PresetOwnedResource, List<String>> owners = new LinkedHashMap<>();
        for (var item : chosen) {
            for (PresetInfrastructure infrastructure : item.deployment().infrastructure()) {
                for (PresetOwnedResource resource : infrastructure.ownedResources()) {
                    owners.computeIfAbsent(resource, ignored -> new ArrayList<>()).add(item.preset().id());
                }
            }
        }

        List<PresetResourceAllocation> allocations = new ArrayList<>();
        for (var entry : owners.entrySet()) {
            PresetOwnedResource resource = entry.getKey();
            List<String> presetIds = List.copyOf(entry.getValue());
            if (resource.sharing() == PresetResourceSharing.EXCLUSIVE && presetIds.size() > 1) {
                throw new CompositionPlanningException(
                        "RESOURCE_CONFLICT",
                        "exclusive resource " + resource.id() + " cannot have multiple owners: " + String.join(", ", presetIds),
                        presetIds,
                        resource.id()
                );
            }
            allocations.add(new PresetResourceAllocation(resource, presetIds));
        }
        return List.copyOf(allocations);
    }

    private static void validateHotkeyConflicts(List<PresetCompositionPlan.SelectedPresetDeployment> chosen) {
        Map<Hotkey, List<String>> owners = new LinkedHashMap<>();
        for (var item : chosen) {
            if (!isHotkeyDeployment(item.deployment().kind())) continue;
            Hotkey hotkey = item.preset().defaultHotkey();
            if (hotkey == null) continue;
            owners.computeIfAbsent(hotkey, ignored -> new ArrayList<>()).add(item.preset().id());
        }
        for (var entry : owners.entrySet()) {
            if (entry.getValue().size() > 1) {
                throw new CompositionPlanningException(
                        "HOTKEY_CONFLICT",
                        "duplicate hotkey binding " + entry.getKey().displayName() + ": " + String.join(", ", entry.getValue()),
                        entry.getValue(),
                        entry.getKey().id()
                );
            }
        }
    }

    private static boolean isHotkeyDeployment(PresetDeploymentKind kind) {
        return kind == PresetDeploymentKind.HOTKEY_LOCAL || isSharedHotkeyDeployment(kind);
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

    private static boolean isSharedHotkeyDeployment(PresetDeploymentKind kind) {
        return isSharedPersistent(kind) || kind == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT;
    }

    private static int align(int value, int alignment) {
        if (alignment <= 1) return value;
        return (value + alignment - 1) & ~(alignment - 1);
    }

}
