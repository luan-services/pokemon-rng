/* Machine/UI-facing build parameter metadata owned by the preset definition.
   The UI renders these fields; the toolkit remains the validation authority. */
record PresetParameterDefinition(
        String id,
        PresetParameterType type,
        boolean required,
        String label,
        String description,
        String example
) {
    PresetParameterDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("parameter id must not be blank");
        if (type == null) throw new IllegalArgumentException("parameter type must not be null");
        label = label == null || label.isBlank() ? id : label;
        description = description == null ? "" : description;
        example = example == null ? "" : example;
    }

    int parseHexU32(String raw) {
        if (type != PresetParameterType.HEX_U32) throw new IllegalStateException("parameter is not HEX_U32: " + id);
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException(id + " must not be blank");
        String value = raw.startsWith("0x") || raw.startsWith("0X") ? raw.substring(2) : raw;
        if (!value.matches("[0-9a-fA-F]{1,8}")) {
            throw new IllegalArgumentException(id + " must be 1..8 hexadecimal digits");
        }
        return (int) Long.parseUnsignedLong(value, 16);
    }
}
