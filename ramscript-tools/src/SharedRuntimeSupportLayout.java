/* Exact RamScript support packing used by SharedHotkeyRuntime composition.

   Normal Shared builds remain unchanged. When Run Anywhere is selected its
   local toggle Field Script is packed immediately after the dispatcher; the
   native staging service, when required, follows at a 4-byte aligned offset.
*/
record SharedRuntimeSupportLayout(
        byte[] support,
        int alignment,
        int runAnywhereOffset,
        int serviceOffset
) {
    SharedRuntimeSupportLayout {
        support = support.clone();
    }
    @Override public byte[] support() { return support.clone(); }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean nativeService,
            int nativeStagingCapacity
    ) {
        return build(rom, bindingCount, runAnywhere, false, nativeService, nativeStagingCapacity);
    }

    static SharedRuntimeSupportLayout build(
            RomProfile rom,
            int bindingCount,
            boolean runAnywhere,
            boolean runBikeAnywhere,
            boolean nativeService,
            int nativeStagingCapacity
    ) {
        return build(rom, bindingCount, runAnywhere, runBikeAnywhere,
                runAnywhere || runBikeAnywhere, nativeService, nativeStagingCapacity);
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
        if (runAnywhere && runBikeAnywhere) {
            throw new IllegalArgumentException("run-anywhere and run-bike-anywhere share the same fixed EWRAM sidecar and cannot be combined");
        }
        if (bindingCount < 1 || bindingCount > 8) throw new IllegalArgumentException("binding count must be 1..8");
        int dispatcherEnd = SharedHotkeyRuntime.PAYLOAD_OFFSET + SharedHotkeyRuntime.dispatcherSize(bindingCount);

        if (!mobilityLocalPayload) {
            if (!nativeService) return new SharedRuntimeSupportLayout(new byte[0], 1, -1, -1);
            int serviceOffset = SharedPersistentNativeStagingService.offsetForBindings(bindingCount, 4);
            SharedPersistentNativeStagingService.Build service = SharedPersistentNativeStagingService.build(
                    rom, serviceOffset, rom.stringVar4 + 0x140L, nativeStagingCapacity);
            return new SharedRuntimeSupportLayout(service.fieldScript(), service.requiredBaseAlignment(), -1, serviceOffset);
        }

        int supportStart = align(dispatcherEnd, 4);
        int runOffset = supportStart;
        byte[] local = runBikeAnywhere
                ? RunBikeAnywhereSharedPreset.buildLocalTogglePayload(rom, runOffset)
                : RunAnywhereSharedPreset.buildLocalTogglePayload(rom, runOffset);
        int cursor = runOffset + local.length;
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
        System.arraycopy(local, 0, support, runOffset - supportStart, local.length);
        if (nativeService) {
            System.arraycopy(serviceBytes, 0, support, serviceOffset - supportStart, serviceBytes.length);
        }
        return new SharedRuntimeSupportLayout(support, 4, runOffset, serviceOffset);
    }

    private static int align(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }
}
