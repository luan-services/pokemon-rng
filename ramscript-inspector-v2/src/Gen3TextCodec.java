import java.util.HashMap;
import java.util.Map;

/* Decoder for the Gen III text encoding used by embedded RamScript messages.
   This version focuses on the international FireRed/LeafGreen characters and
   control sequences that occur in the official Wonder Card scripts.
*/
final class Gen3TextCodec {
    private static final int EOS = 0xFF;
    private static final Map<Integer, Character> DECODE = new HashMap<>();
    private static final Map<Integer, String> PLACEHOLDERS = new HashMap<>();

    static {
        map(' ', 0x00);
        String digits = "0123456789";
        for (int i = 0; i < digits.length(); i++) map(digits.charAt(i), 0xA1 + i);
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < upper.length(); i++) map(upper.charAt(i), 0xBB + i);
        String lower = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < lower.length(); i++) map(lower.charAt(i), 0xD5 + i);

        map('!', 0xAB); map('?', 0xAC); map('.', 0xAD); map('-', 0xAE);
        map('…', 0xB0); map('“', 0xB1); map('”', 0xB2); map('‘', 0xB3); map((char) 0x27, 0xB4);
        map('♂', 0xB5); map('♀', 0xB6); map(',', 0xB8); map('×', 0xB9); map('/', 0xBA);
        map(':', 0xF0); map('%', 0x5B); map('(', 0x5C); map(')', 0x5D); map('&', 0x2D); map('+', 0x2E); map('=', 0x35); map(';', 0x36);
        map('À', 0x01); map('Á', 0x02); map('Â', 0x03); map('Ç', 0x04); map('È', 0x05); map('É', 0x06); map('Ê', 0x07); map('Ë', 0x08);
        map('Ì', 0x09); map('Î', 0x0B); map('Ï', 0x0C); map('Ò', 0x0D); map('Ó', 0x0E); map('Ô', 0x0F); map('Ù', 0x11); map('Ú', 0x12); map('Û', 0x13); map('Ñ', 0x14);
        map('à', 0x16); map('á', 0x17); map('ç', 0x19); map('è', 0x1A); map('é', 0x1B); map('ê', 0x1C); map('ë', 0x1D); map('ì', 0x1E);
        map('î', 0x20); map('ï', 0x21); map('ò', 0x22); map('ó', 0x23); map('ô', 0x24); map('ù', 0x26); map('ú', 0x27); map('û', 0x28); map('ñ', 0x29); map('â', 0x68); map('í', 0x6F);
        map('Ä', 0xF1); map('Ö', 0xF2); map('Ü', 0xF3); map('ä', 0xF4); map('ö', 0xF5); map('ü', 0xF6);

        PLACEHOLDERS.put(0x01, "{PLAYER}");
        PLACEHOLDERS.put(0x02, "{STR_VAR_1}");
        PLACEHOLDERS.put(0x03, "{STR_VAR_2}");
        PLACEHOLDERS.put(0x04, "{STR_VAR_3}");
        PLACEHOLDERS.put(0x05, "{KUN}");
        PLACEHOLDERS.put(0x06, "{RIVAL}");
        PLACEHOLDERS.put(0x07, "{VERSION}");
        PLACEHOLDERS.put(0x08, "{EVIL_TEAM}");
        PLACEHOLDERS.put(0x09, "{GOOD_TEAM}");
        PLACEHOLDERS.put(0x0A, "{EVIL_LEADER}");
        PLACEHOLDERS.put(0x0B, "{GOOD_LEADER}");
        PLACEHOLDERS.put(0x0C, "{EVIL_LEGENDARY}");
    }

    private Gen3TextCodec() {}

    private static void map(char c, int value) {
        DECODE.put(value, c);
    }

    static String decodeString(byte[] data, int offset) {
        if (offset < 0 || offset >= data.length) return null;

        StringBuilder result = new StringBuilder();
        for (int i = offset; i < data.length; i++) {
            int value = Byte.toUnsignedInt(data[i]);
            if (value == EOS) return result.toString();

            if (value == 0xFE) { result.append("\\n"); continue; }
            if (value == 0xFB) { result.append("\\p"); continue; }
            if (value == 0xFA) { result.append("\\l"); continue; }

            if (value == 0xFD) {
                if (i + 1 >= data.length) return result.append("{STRING:?}").toString();
                int id = Byte.toUnsignedInt(data[++i]);
                result.append(PLACEHOLDERS.getOrDefault(id, String.format("{STRING:0x%02X}", id)));
                continue;
            }

            if (value == 0xFC) {
                // Extended control code. We preserve the id instead of guessing its arguments.
                if (i + 1 >= data.length) return result.append("{CTRL:?}").toString();
                int id = Byte.toUnsignedInt(data[++i]);
                result.append(String.format("{CTRL:0x%02X}", id));
                continue;
            }

            Character c = DECODE.get(value);
            result.append(c != null ? c : String.format("{0x%02X}", value));
        }
        return result.toString();
    }
}
