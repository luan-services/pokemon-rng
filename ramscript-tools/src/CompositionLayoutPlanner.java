import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Turns an already-selected deployment plan into concrete hotkeys and storage
   offsets. It does not generate bytes or WC3s yet. */
final class CompositionLayoutPlanner {
    private static final int SB1_GATEWAY_SIZE = PayloadPlacementPlanner.GATEWAY_SIZE;
    private static final int FIRST_GATEWAY_OFFSET = PayloadStorageArea.SAVE_BLOCK1.offset()
            + PayloadStorageArea.SAVE_BLOCK1.capacity() - SB1_GATEWAY_SIZE;

    private CompositionLayoutPlanner() {}

    static ConcreteCompositionLayout layout(List<PresetCompositionPlan.SelectedPresetDeployment> selected) {
        if (selected == null || selected.isEmpty()) throw new IllegalArgumentException("selected deployments must not be empty");

        List<HotkeyBinding> bindings = new ArrayList<>();
        for (var item : selected) {
            if (requiresHotkey(item.deployment().kind())) {
                Hotkey hotkey = item.preset().defaultHotkey();
                if (hotkey == null) throw new IllegalArgumentException("missing default hotkey for " + item.preset().id());
                bindings.add(new HotkeyBinding(item.preset().id(), hotkey));
            }
        }

        boolean shared = selected.stream().anyMatch(item ->
                item.deployment().infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME));
        HotkeyBindingPlan bindingPlan = shared
                ? HotkeyBindingAllocator.planShared(bindings)
                : HotkeyBindingAllocator.plan(bindings);

        Map<String, Integer> nativeOffsets = new HashMap<>();
        int nativeCount = 0;
        for (var item : selected) {
            if (item.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE) nativeCount++;
        }

        int sb2Start = PayloadStorageArea.SAVE_BLOCK2.offset();
        int sb2Cursor = sb2Start;
        int nativeCatalogOffset = -1;
        int nativeCatalogSize = 0;
        if (nativeCount > 0) {
            nativeCatalogOffset = sb2Start;
            int tableEnd = PersistentNativeCatalogFormat.HEADER_SIZE
                    + PersistentNativeCatalogFormat.ENTRY_SIZE * nativeCount;
            int relativeCursor = align(tableEnd, 4);
            for (var item : selected) {
                if (item.deployment().kind() != PresetDeploymentKind.SHARED_PERSISTENT_NATIVE) continue;
                nativeOffsets.put(item.preset().id(), sb2Start + relativeCursor);
                relativeCursor = align(relativeCursor + item.cost().sb2NativeModuleBytes(), 4);
            }
            nativeCatalogSize = relativeCursor;
            sb2Cursor = sb2Start + nativeCatalogSize;
        }

        int nextGateway = FIRST_GATEWAY_OFFSET;
        Map<String, HotkeyBinding> bindingByPreset = new HashMap<>();
        for (HotkeyBinding binding : bindings) bindingByPreset.put(binding.presetId(), binding);

        List<ConcretePresetAllocation> allocations = new ArrayList<>();
        for (var item : selected) {
            PresetDeploymentCost cost = item.cost();
            int gateway = -1;
            if (cost.sb1GatewayBytes() > 0) {
                if (cost.sb1GatewayBytes() != SB1_GATEWAY_SIZE) {
                    throw new IllegalArgumentException("unsupported gateway size for " + item.preset().id());
                }
                gateway = nextGateway;
                nextGateway -= SB1_GATEWAY_SIZE;
            }

            int fieldOffset = -1;
            if (cost.sb2FieldScriptBytes() > 0) {
                sb2Cursor = align(sb2Cursor, cost.requiredBaseAlignment());
                fieldOffset = sb2Cursor;
                sb2Cursor += cost.sb2FieldScriptBytes();
            }

            int nativeOffset = nativeOffsets.getOrDefault(item.preset().id(), -1);
            allocations.add(new ConcretePresetAllocation(
                    item.preset().id(), item.deployment().kind(), bindingByPreset.get(item.preset().id()),
                    gateway, fieldOffset, cost.sb2FieldScriptBytes(), nativeOffset, cost.sb2NativeModuleBytes()));
        }

        int sb1Start = PayloadStorageArea.SAVE_BLOCK1.offset();
        if (nextGateway + SB1_GATEWAY_SIZE < sb1Start) throw new IllegalArgumentException("SB1 gateway capacity exceeded");
        int sb2End = InstallationManifest.OFFSET;
        if (sb2Cursor > sb2End) throw new IllegalArgumentException("SB2 payload capacity exceeded after reserving installation manifest");

        return new ConcreteCompositionLayout(allocations, bindingPlan, nativeCatalogOffset, nativeCatalogSize);
    }

    private static boolean requiresHotkey(PresetDeploymentKind kind) {
        return kind == PresetDeploymentKind.HOTKEY_LOCAL
                || kind == PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT
                || kind == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE
                || kind == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT;
    }

    private static int align(int value, int alignment) {
        if (alignment <= 1) return value;
        return (value + alignment - 1) & ~(alignment - 1);
    }
}
