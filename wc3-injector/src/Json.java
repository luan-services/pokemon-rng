import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* tiny dependency-free JSON encoder used by the machine-facing API. */
final class Json {
    private Json() {}

    static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            appendString(out, text);
        } else if (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long || value instanceof Float
                || value instanceof Double) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                appendString(out, String.valueOf(entry.getKey()));
                out.append(':');
                append(out, entry.getValue());
                if (iterator.hasNext()) {
                    out.append(',');
                }
            }
            out.append('}');
        } else if (value instanceof Iterable<?> iterable) {
            out.append('[');
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                append(out, iterator.next());
                if (iterator.hasNext()) {
                    out.append(',');
                }
            }
            out.append(']');
        } else if (value.getClass().isArray()) {
            if (value instanceof Object[] objects) {
                append(out, List.of(objects));
            } else {
                throw new IllegalArgumentException("Unsupported primitive array type in JSON encoder");
            }
        } else {
            throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
        }
    }

    private static void appendString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04X", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }
}
