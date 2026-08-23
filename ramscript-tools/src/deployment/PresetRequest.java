/*
   Input to the production placement planner.

   requiresHotkey is intentionally metadata only at this layer. Trigger and
   payload placement remain separate concerns: a hotkey payload may still live
   locally in RamScript.
*/
record PresetRequest(
        String id,
        PresetPayloadKind kind,
        byte[] bytes,
        PresetPlacementPreference preference,
        boolean requiresHotkey
) {
    PresetRequest {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("preset id must not be blank");
        if (kind == null) throw new IllegalArgumentException("preset kind must not be null");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("preset bytes must not be empty");
        if (preference == null) throw new IllegalArgumentException("placement preference must not be null");
        bytes = bytes.clone();
    }

    @Override public byte[] bytes() { return bytes.clone(); }

    static PresetRequest fieldScript(String id, byte[] bytes, PresetPlacementPreference preference, boolean requiresHotkey) {
        return new PresetRequest(id, PresetPayloadKind.FIELD_SCRIPT, bytes, preference, requiresHotkey);
    }
}
