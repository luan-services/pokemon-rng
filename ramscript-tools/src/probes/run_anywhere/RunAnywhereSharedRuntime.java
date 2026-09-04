import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
   Experimental Build 37 resident-maintenance integration.

   No new IWRAM block is claimed. The existing Runtime V1 VBlank-tail literal
   (03003EC0) is dynamically repointed, during the temporary bootstrap only, to
   this small EWRAM routine embedded in the installed RamScript. The routine
   sets gMapHeader.allowRunning and then chains to the original stock VBlank.
*/
final class RunAnywhereSharedRuntime {
    private static final long MAP_HEADER_FLAGS_BYTE = 0x02036E15L;
    private static final int NATIVE_STAGING_CAPACITY = 0x140;

    private RunAnywhereSharedRuntime() {}

    static TriggerBuildResult build(PresetCompositionPlan plan) {
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        if (!plan.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            throw new IllegalArgumentException("Run Anywhere integration currently requires SharedHotkeyRuntime");
        }

        List<SharedHotkeyDispatcher.Entry> entries = new ArrayList<>();
        HotkeyButton modifier = null;
        Map<String, ConcretePresetAllocation> byId = new HashMap<>();
        for (ConcretePresetAllocation allocation : plan.concreteLayout().allocations()) {
            byId.put(allocation.presetId(), allocation);
        }

        for (HotkeyBinding binding : plan.concreteLayout().bindingPlan().bindings()) {
            ConcretePresetAllocation allocation = byId.get(binding.presetId());
            if (allocation == null || !allocation.hasGateway()) {
                throw new IllegalArgumentException("shared binding has no gateway: " + binding.presetId());
            }
            if (modifier == null) modifier = binding.hotkey().heldButton();
            if (binding.hotkey().heldButton() != modifier) {
                throw new IllegalArgumentException("all shared bindings must use one common modifier");
            }
            entries.add(new SharedHotkeyDispatcher.Entry(
                    binding.hotkey().pressedButton(),
                    allocation.sb1GatewayOffset() - 0x3624
            ));
        }
        if (modifier == null) throw new IllegalArgumentException("shared runtime has no bindings");

        int serviceOffset = -1;
        byte[] service = new byte[0];
        int supportAlignment = 4;
        if (plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)) {
            serviceOffset = SharedPersistentNativeStagingService.offsetForBindings(plan.hotkeyBindings(), 4);
            SharedPersistentNativeStagingService.Build stage = SharedPersistentNativeStagingService.build(
                    plan.rom(), serviceOffset, plan.rom().stringVar4 + 0x140L, NATIVE_STAGING_CAPACITY);
            service = stage.fieldScript();
            supportAlignment = Math.max(supportAlignment, stage.requiredBaseAlignment());
        } else {
            serviceOffset = align(
                    SharedHotkeyRuntime.PAYLOAD_OFFSET + SharedHotkeyRuntime.dispatcherSize(plan.hotkeyBindings()),
                    supportAlignment);
        }

        byte[] maintenance = maintenanceRoutine(plan.rom());
        int maintenanceOffset = align(serviceOffset + service.length, 4);
        byte[] combined = new byte[maintenanceOffset + maintenance.length - serviceOffset];
        System.arraycopy(service, 0, combined, 0, service.length);
        System.arraycopy(maintenance, 0, combined, maintenanceOffset - serviceOffset, maintenance.length);

        return SharedHotkeyRuntime.compose(
                plan.rom(),
                modifier,
                entries,
                combined,
                supportAlignment,
                new SharedHotkeyRuntime.ResidentMaintenance(maintenance, maintenanceOffset)
        );
    }

    static byte[] maintenanceRoutine(RomProfile rom) {
        byte[] out = new byte[] {
                0x03,0x48,                   // ldr  r0, map flags byte
                0x01,0x78,                   // ldrb r1,[r0]
                0x02,0x22,                   // movs r2,#2
                0x11,0x43,                   // orrs r1,r2
                0x01,0x70,                   // strb r1,[r0]
                0x02,0x4B,                   // ldr  r3, original VBlank Thumb
                0x18,0x47,                   // bx   r3
                (byte)0xC0,0x46,             // nop / literal alignment
                0,0,0,0,
                0,0,0,0
        };
        putU32(out, 0x10, MAP_HEADER_FLAGS_BYTE);
        putU32(out, 0x14, rom.originalVBlankThumb);
        return out;
    }

    static int maintenanceSize() { return 24; }

    private static int align(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
