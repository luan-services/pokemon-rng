import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
   Experimental SharedHotkeyRuntime integration for the game-validated
   Run Anywhere EWRAM sidecar.

   Deliberately conservative scope for the first shared-cart test:
     - start from an ordinary production shared composition;
     - require no shared native staging service, so all existing persistent
       component bytes/offsets remain exactly as planned by production;
     - add one extra shared binding whose Field Script lives locally in the
       final Runtime RamScript instead of consuming scarce SB2;
     - install the already-validated 02022B08 EWRAM sidecar during the same
       Runtime WC activation;
     - use the production SharedHotkeyRuntime resident blocks, changing only
       the supervisor's post-map callback literal in this experimental build.

   No production catalog/planner metadata is changed by this class.
*/
final class RunAnywhereSharedHotkeyProbe {
    static final Hotkey HOTKEY = new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT);
    static final int SE_TOGGLE_CLICK = 0x0066;

    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int RAMSCRIPT_OFFSET_IN_SB1 = 0x3624;

    private RunAnywhereSharedHotkeyProbe() {}

    static TriggerBuildResult buildRuntime(PresetCompositionPlan basePlan) {
        if (basePlan == null) throw new IllegalArgumentException("base plan must not be null");
        if (!basePlan.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            throw new IllegalArgumentException("base plan must already use SharedHotkeyRuntime");
        }
        if (basePlan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)) {
            throw new IllegalArgumentException(
                    "first Run Anywhere shared probe intentionally rejects plans using shared native staging; "
                            + "test the isolated shared integration before moving native-service offsets"
            );
        }
        if (basePlan.hotkeyBindings() >= 8) {
            throw new IllegalArgumentException("no free SharedHotkeyRuntime binding for Run Anywhere");
        }

        Map<String, ConcretePresetAllocation> allocationById = new HashMap<>();
        for (ConcretePresetAllocation allocation : basePlan.concreteLayout().allocations()) {
            allocationById.put(allocation.presetId(), allocation);
        }

        HotkeyButton modifier = null;
        List<SharedHotkeyDispatcher.Entry> entries = new ArrayList<>();
        for (HotkeyBinding binding : basePlan.concreteLayout().bindingPlan().bindings()) {
            if (binding.hotkey().pressedButton() == HOTKEY.pressedButton()) {
                throw new IllegalArgumentException("Run Anywhere hotkey conflicts with " + binding.presetId());
            }
            if (modifier == null) modifier = binding.hotkey().heldButton();
            if (binding.hotkey().heldButton() != modifier) {
                throw new IllegalArgumentException("base shared composition uses mixed modifiers");
            }
            ConcretePresetAllocation allocation = allocationById.get(binding.presetId());
            if (allocation == null || !allocation.hasGateway()) {
                throw new IllegalArgumentException("shared binding has no persistent gateway: " + binding.presetId());
            }
            entries.add(new SharedHotkeyDispatcher.Entry(
                    binding.hotkey().pressedButton(), gatewayDelta(allocation.sb1GatewayOffset())));
        }
        if (modifier == null) throw new IllegalArgumentException("base shared plan has no bindings");
        if (modifier != HOTKEY.heldButton()) {
            throw new IllegalArgumentException("Run Anywhere probe currently expects the shared R modifier");
        }

        int extendedBindingCount = entries.size() + 1;
        int localOffset = align4(SharedHotkeyRuntime.PAYLOAD_OFFSET
                + SharedHotkeyRuntime.dispatcherSize(extendedBindingCount));
        byte[] localPayload = buildLocalTogglePayload(basePlan.rom(), localOffset);
        entries.add(new SharedHotkeyDispatcher.Entry(HOTKEY.pressedButton(), localOffset));

        SharedHotkeyRuntime.ResidentSidecar sidecar = new SharedHotkeyRuntime.ResidentSidecar(
                RunAnywhereHotkeyRuntimeV1.SIDECAR_ADDRESS,
                RunAnywhereHotkeyRuntimeV1.CALLBACK_THUMB,
                RunAnywhereHotkeyRuntimeV1.sidecar(basePlan.rom())
        );

        return SharedHotkeyRuntime.composeWithResidentSidecar(
                basePlan.rom(), modifier, entries, localPayload, 4, sidecar);
    }

    static byte[] buildLocalTogglePayload(RomProfile rom, int ramScriptOffset) {
        long scriptVirtualBase = VIRTUAL_BASE + Integer.toUnsignedLong(ramScriptOffset);
        long flagsAddress = rom.mapHeader + RunAnywhereNativeHelper.ALLOW_RUNNING_OFFSET;
        RamScriptBuilder b = new RamScriptBuilder(scriptVirtualBase);
        b.setVAddress();

        // Toggle entirely in Field Script. The only stock values we need to
        // preserve are bit1 clear (0) or set (2), so no temporary native helper
        // is necessary here. This keeps the shared Runtime WC small.
        b.comparePtrToValue(RunAnywhereHotkeyRuntimeV1.STATE_ADDRESS, 0)
                .vGotoIfEqual("turn_on");

        // OFF: clear armed state, then restore the stock bit captured for the
        // current map by the resident sidecar.
        b.writeBytes(RunAnywhereHotkeyRuntimeV1.STATE_ADDRESS, new byte[] {0})
                .comparePtrToValue(RunAnywhereHotkeyRuntimeV1.ORIGINAL_ADDRESS, 0)
                .vGotoIfEqual("restore_zero")
                .writeBytes(flagsAddress, new byte[] {2})
                .vGoto("click");

        b.label("restore_zero")
                .writeBytes(flagsAddress, new byte[] {0})
                .vGoto("click");

        // ON: capture the map's current stock bit without a native helper, arm
        // the sidecar and force running immediately.
        b.label("turn_on")
                .comparePtrToValue(flagsAddress, 0)
                .vGotoIfEqual("capture_zero")
                .writeBytes(RunAnywhereHotkeyRuntimeV1.ORIGINAL_ADDRESS, new byte[] {2})
                .vGoto("arm");

        b.label("capture_zero")
                .writeBytes(RunAnywhereHotkeyRuntimeV1.ORIGINAL_ADDRESS, new byte[] {0});

        b.label("arm")
                .writeBytes(RunAnywhereHotkeyRuntimeV1.STATE_ADDRESS, new byte[] {1})
                .writeBytes(flagsAddress, new byte[] {2});

        b.label("click")
                .playSe(SE_TOGGLE_CLICK)
                .waitSe()
                .end();
        return b.buildScript();
    }

    static int localPayloadOffset(PresetCompositionPlan basePlan) {
        return align4(SharedHotkeyRuntime.PAYLOAD_OFFSET
                + SharedHotkeyRuntime.dispatcherSize(basePlan.hotkeyBindings() + 1));
    }

    private static int gatewayDelta(int gatewayOffset) {
        return gatewayOffset - RAMSCRIPT_OFFSET_IN_SB1;
    }

    private static int align4(int value) {
        return (value + 3) & ~3;
    }
}
