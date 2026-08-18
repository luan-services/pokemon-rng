/*
   Composition entry point for user-created Field Script payloads.

   Basic gift presets do not need to use this class. It exists for custom
   scripts and advanced presets whose execution mode is configurable.
*/
final class CustomPayloadComposer {
    private CustomPayloadComposer() {}

    static TriggerBuildResult deliveryman(byte[] payload) {
        // Pure deliveryman Field Scripts do not use ROM-specific native code.
        // FR10 is used only because TriggerBuildResult currently carries a
        // RomProfile for uniform reporting.
        return TriggerComposer.compose(
                EventTrigger.DELIVERYMAN,
                RomProfile.FIRE_RED_EN_10,
                payload
        );
    }

    static TriggerBuildResult hotkey(RomProfile rom, byte[] payload) {
        return TriggerComposer.compose(EventTrigger.HOTKEY_RUNTIME, rom, payload);
    }

    static TriggerBuildResult compose(EventTrigger trigger, RomProfile rom, byte[] payload) {
        return TriggerComposer.compose(trigger, rom, payload);
    }
}
