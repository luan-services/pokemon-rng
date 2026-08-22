record PresetPayload(String id, PresetPayloadKind kind, byte[] bytes) {
    PresetPayload {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("preset payload id must not be blank");
        if (kind == null) throw new IllegalArgumentException("preset payload kind must not be null");
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("preset payload bytes must not be empty");
        bytes = bytes.clone();
    }

    @Override public byte[] bytes() { return bytes.clone(); }
}
