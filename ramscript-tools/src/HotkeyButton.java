enum HotkeyButton {
    A("a", "A", 0),
    B("b", "B", 1),
    SELECT("select", "SELECT", 2),
    START("start", "START", 3),
    RIGHT("right", "RIGHT", 4),
    LEFT("left", "LEFT", 5),
    UP("up", "UP", 6),
    DOWN("down", "DOWN", 7),
    R("r", "R", 8),
    L("l", "L", 9);

    private final String id;
    private final String displayName;
    private final int bit;

    HotkeyButton(String id, String displayName, int bit) {
        this.id = id;
        this.displayName = displayName;
        this.bit = bit;
    }

    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    int bit() {
        return bit;
    }

    static HotkeyButton fromId(String value) {
        if (value == null) throw new IllegalArgumentException("hotkey button must not be null");
        String normalized = value.trim().toLowerCase();
        for (HotkeyButton button : values()) {
            if (button.id.equals(normalized)) return button;
        }
        throw new IllegalArgumentException(
                "Unknown hotkey button: " + value
                        + ". Expected a, b, select, start, right, left, up, down, r, or l."
        );
    }
}
