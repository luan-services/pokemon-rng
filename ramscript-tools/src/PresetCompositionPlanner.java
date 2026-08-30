import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
   Dry-run preset composition planner.

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

        for (int i = 0; i < selections.size(); i++) {
            PresetSelection selection = selections.get(i);
            PresetDefinition preset = presets.get(i);
            PresetDeploymentKind kind;
            if (selection.activation() == PresetActivation.HOTKEY) {
                if (!preset.hotkeyCapable()) throw new IllegalArgumentException(preset.id() + " does not support hotkey activation");
                kind = hotkeyCount == 1
                        ? PresetDeploymentKind.HOTKEY_LOCAL
                        : sharedDeploymentKind(preset);
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

        return evaluate(rom, presets, deployments);
    }

    private static PresetDeploymentKind sharedDeploymentKind(PresetDefinition preset) {
        if (preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE))
            return PresetDeploymentKind.SHARED_PERSISTENT_NATIVE;
        if (preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT))
            return PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT;
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

}
