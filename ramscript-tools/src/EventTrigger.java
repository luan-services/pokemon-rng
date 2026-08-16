enum EventTrigger {
    DELIVERYMAN,
    HOTKEY_RUNTIME;

    static EventTrigger fromId(String value) {
        return switch (value.toLowerCase()) {
            case "deliveryman", "delivery" -> DELIVERYMAN;
            case "hotkey", "hotkey-runtime", "r-select", "r+select" -> HOTKEY_RUNTIME;
            default -> throw new IllegalArgumentException(
                    "Unknown trigger: " + value + ". Supported: deliveryman, hotkey"
            );
        };
    }
}
