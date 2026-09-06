import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Exact RamScript support packing used by SharedHotkeyRuntime composition.

   Local Shared payloads are explicitly opt-in per preset. They are packed
   after the dispatcher, followed by the shared native staging service when
   one is still required. This is deliberately not a generic "copy anything
   anywhere" allocator: each local payload must have a relocation-safe
   materializer here before the planner may select it.
*/
record SharedRuntimeSupportLayout(
        byte[] support,
        int alignment,
        Map<String, Integer> localPayloadOffsets,
        int serviceOffset
) {
    SharedRuntimeSupportLayout {
        support = support.clone();
        localPayloadOffsets = Map.copyOf(localPayloadOffsets);
    }
    @Override public byte[] support() { return support.clone(); }
    @Override public Map<String, Integer> localPayloadOffsets() { return localPayloadOffsets; }

    int localPayloadOffset(String presetId) {
        return localPayloadOffsets.getOrDefault(presetId, -1);
    }

    /* Compatibility accessor for the existing mobility probes/tests. */
    int runAnywhereOffset() {
        int offset = localPayloadOffset("run-anywhere");
        return offset >= 0 ? offset : localPayloadOffset("run-bike-anywhere");
    }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean nativeService,
            int nativeStagingCapacity
    ) {
        return build(rom, bindingCount, runAnywhere, false,
                runAnywhere ? List.of("run-anywhere") : List.of(), nativeService, nativeStagingCapacity);
    }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean runBikeAnywhere,
            boolean nativeService,
            int nativeStagingCapacity
    ) {
        List<String> locals = runBikeAnywhere ? List.of("run-bike-anywhere")
                : (runAnywhere ? List.of("run-anywhere") : List.of());
        return build(rom, bindingCount, runAnywhere, runBikeAnywhere, locals, nativeService, nativeStagingCapacity);
    }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean runBikeAnywhere,
            boolean mobilityLocalPayload,
            boolean nativeService,
            int nativeStagingCapacity
    ) {
        List<String> locals = mobilityLocalPayload
                ? (runBikeAnywhere ? List.of("run-bike-anywhere") : (runAnywhere ? List.of("run-anywhere") : List.of()))
                : List.of();
        return build(rom, bindingCount, runAnywhere, runBikeAnywhere, locals, nativeService, nativeStagingCapacity);
    }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean runBikeAnywhere,
            List<String> localPresetIds,
            boolean nativeService,
            int nativeStagingCapacity
    ) {
        return build(rom, bindingCount, runAnywhere, runBikeAnywhere, localPresetIds,
                nativeService, nativeStagingCapacity, 0x1234);
    }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean runBikeAnywhere,
            List<String> localPresetIds,
            boolean nativeService,
            int nativeStagingCapacity,
            int seed
    ) {
        if (runAnywhere && runBikeAnywhere) {
            throw new IllegalArgumentException("run-anywhere and run-bike-anywhere share the same fixed EWRAM sidecar and cannot be combined");
        }
        if (bindingCount < 1 || bindingCount > 8) throw new IllegalArgumentException("binding count must be 1..8");
        if (localPresetIds == null) throw new IllegalArgumentException("localPresetIds must not be null");

        int dispatcherEnd = SharedHotkeyRuntime.PAYLOAD_OFFSET + SharedHotkeyRuntime.dispatcherSize(bindingCount);
        if (localPresetIds.isEmpty()) {
            if (!nativeService) return new SharedRuntimeSupportLayout(new byte[0], 1, Map.of(), -1);
            int serviceOffset = SharedPersistentNativeStagingService.offsetForBindings(bindingCount, 4);
            SharedPersistentNativeStagingService.Build service = SharedPersistentNativeStagingService.build(
                    rom, serviceOffset, rom.stringVar4 + 0x140L, nativeStagingCapacity);
            return new SharedRuntimeSupportLayout(service.fieldScript(), service.requiredBaseAlignment(), Map.of(), serviceOffset);
        }

        int supportStart = align(dispatcherEnd, 4);
        int cursor = supportStart;
        Map<String, Integer> offsets = new LinkedHashMap<>();
        Map<String, byte[]> payloads = new LinkedHashMap<>();
        for (String presetId : localPresetIds) {
            cursor = align(cursor, 4);
            int offset = cursor;
            byte[] payload = buildLocalPayload(presetId, rom, offset, seed);
            offsets.put(presetId, offset);
            payloads.put(presetId, payload);
            cursor += payload.length;
        }

        int serviceOffset = -1;
        byte[] serviceBytes = new byte[0];
        if (nativeService) {
            serviceOffset = align(cursor, 4);
            SharedPersistentNativeStagingService.Build service = SharedPersistentNativeStagingService.build(
                    rom, serviceOffset, rom.stringVar4 + 0x140L, nativeStagingCapacity);
            serviceBytes = service.fieldScript();
            cursor = serviceOffset + serviceBytes.length;
        }

        byte[] support = new byte[cursor - supportStart];
        for (String presetId : localPresetIds) {
            int offset = offsets.get(presetId);
            byte[] payload = payloads.get(presetId);
            System.arraycopy(payload, 0, support, offset - supportStart, payload.length);
        }
        if (nativeService) {
            System.arraycopy(serviceBytes, 0, support, serviceOffset - supportStart, serviceBytes.length);
        }
        return new SharedRuntimeSupportLayout(support, 4, offsets, serviceOffset);
    }

    private static byte[] buildLocalPayload(String presetId, RomProfile rom, int offset, int seed) {
        return switch (presetId) {
            case "run-anywhere" -> RunAnywhereSharedPreset.buildLocalTogglePayload(rom, offset);
            case "run-bike-anywhere" -> RunBikeAnywhereSharedPreset.buildLocalTogglePayload(rom, offset);
            case "show-secret-id" -> ShowSecretIdPreset.buildScriptAtOffset(rom, offset);
            case "seed-modifier" -> SeedModifierPreset.buildPayloadAtOffset(rom, seed, offset);
            case "repel" -> RepelHotkeyPreset.buildPayloadAtOffset(offset);
            case "mute-music" -> MuteMusicPreset.buildPayloadAtOffset(rom, offset);
            default -> throw new IllegalArgumentException("no relocation-safe Shared-local materializer for " + presetId);
        };
    }

    private static int align(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }
}
