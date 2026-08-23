import java.util.ArrayList;
import java.util.List;

/*
   Materializes a validated hotkey binding plan.

   entryPayload is intentionally generic. For a local preset it may be the real
   Field Script. For a persistent preset it may be the tiny SB1 gateway. This
   keeps binding/trigger selection independent from payload placement.
*/
final class HotkeyBindingComposer {
    private HotkeyBindingComposer() {}

    static TriggerBuildResult compose(RomProfile rom, List<BoundHotkeyPayload> payloads) {
        if (rom == null) throw new IllegalArgumentException("rom must not be null");
        if (payloads == null || payloads.isEmpty()) {
            throw new IllegalArgumentException("at least one bound hotkey payload is required");
        }

        List<HotkeyBinding> bindings = new ArrayList<>();
        for (BoundHotkeyPayload payload : payloads) {
            if (payload == null) throw new IllegalArgumentException("bound payload must not be null");
            bindings.add(payload.binding());
        }
        HotkeyBindingPlan plan = HotkeyBindingAllocator.plan(bindings);

        return switch (plan.runtime()) {
            case SINGLE_HOTKEY_V1 -> {
                BoundHotkeyPayload only = payloads.get(0);
                yield HotkeyRuntimeV1.compose(rom, only.entryPayload(), only.binding().hotkey());
            }
            case MULTI_HOTKEY_V1 -> {
                BoundHotkeyPayload first = payloads.get(0);
                BoundHotkeyPayload second = payloads.get(1);
                yield MultiHotkeyRuntimeV1.compose(
                        rom,
                        new HotkeyPayload(first.binding().hotkey(), first.entryPayload()),
                        new HotkeyPayload(second.binding().hotkey(), second.entryPayload())
                );
            }
            case NONE -> throw new IllegalStateException("non-empty payload list cannot select NONE runtime");
        };
    }
}
