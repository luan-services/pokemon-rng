record Hotkey(HotkeyButton heldButton, HotkeyButton pressedButton) {
    static final Hotkey DEFAULT = new Hotkey(HotkeyButton.R, HotkeyButton.SELECT);

    Hotkey {
        if (heldButton == null || pressedButton == null) {
            throw new IllegalArgumentException("hotkey buttons must not be null");
        }
        if (heldButton == pressedButton) {
            throw new IllegalArgumentException("hotkey must use two different buttons");
        }
    }

    static Hotkey parse(String value) {
        if (value == null || value.isBlank()) return DEFAULT;
        String normalized = value.trim().toLowerCase().replace('+', '-');
        String[] parts = normalized.split("-", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid hotkey: " + value + ". Expected <held>-<pressed>, for example r-select or r-b."
            );
        }
        return new Hotkey(HotkeyButton.fromId(parts[0]), HotkeyButton.fromId(parts[1]));
    }

    String id() {
        return heldButton.id() + "-" + pressedButton.id();
    }

    String displayName() {
        return heldButton.displayName() + " + " + pressedButton.displayName();
    }
}
