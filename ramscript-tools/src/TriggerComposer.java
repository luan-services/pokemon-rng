/* Chooses how a Field Script payload is entered.

   DELIVERYMAN keeps the classic behavior: the payload itself is the RamScript.
   HOTKEY_RUNTIME wraps the exact same payload in Hotkey Runtime v1.
*/
final class TriggerComposer {
    private TriggerComposer() {}

    static TriggerBuildResult compose(EventTrigger trigger, RomProfile rom, byte[] payload) {
        return compose(trigger, rom, payload, Hotkey.DEFAULT);
    }

    static TriggerBuildResult composeObjectEvent(RomProfile rom, byte[] payload, ObjectEventTarget target) {
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("payload must not be empty");
        if (target == null) throw new IllegalArgumentException("object-event target must not be null");
        if (payload.length > RamScript.SCRIPT_SIZE) throw new IllegalArgumentException("payload exceeds RamScript capacity");
        RamScript script = RamScript.createObjectBound(payload, target.mapGroup(), target.mapNum(), target.localId());
        return new TriggerBuildResult(script, EventTrigger.OBJECT_BOUND_NPC, rom, payload.length, 0, payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    static TriggerBuildResult compose(EventTrigger trigger, RomProfile rom, byte[] payload, Hotkey hotkey) {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("payload must not be empty");
        }

        return switch (trigger) {
            case DELIVERYMAN -> {
                if (payload.length > RamScript.SCRIPT_SIZE) {
                    throw new IllegalArgumentException("payload exceeds RamScript capacity");
                }
                RamScript script = RamScript.createWonderCard(payload);
                yield new TriggerBuildResult(
                        script, trigger, rom, payload.length, 0,
                        payload.length, RamScript.SCRIPT_SIZE - payload.length
                );
            }
            case OBJECT_BOUND_NPC -> throw new IllegalArgumentException("Object-bound trigger requires map/object metadata");
            case HOTKEY_RUNTIME -> HotkeyRuntimeV1.compose(rom, payload, hotkey);
        };
    }
}
