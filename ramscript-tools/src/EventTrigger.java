enum EventTrigger {
    DELIVERYMAN,
    OBJECT_BOUND_NPC,
    HOTKEY_RUNTIME;

    static EventTrigger fromId(String value) {
        return switch (value.toLowerCase()) {
            case "deliveryman", "delivery" -> DELIVERYMAN;
            case "npc", "object", "object-bound" -> OBJECT_BOUND_NPC;
            case "hotkey", "hotkey-runtime", "r-select", "r+select" -> HOTKEY_RUNTIME;
            default -> throw new IllegalArgumentException(
                    "Unknown trigger: " + value + ". Supported: deliveryman, npc, hotkey"
            );
        };
    }
}
