import java.util.HashMap;
import java.util.Map;

/* this class acts as a translator / encoder to read the game's char set and to encode strings into it */

final class Gen3TextCodec {
    static final int EOS = 0xFF;
    static final int FIELD_SIZE = 40;

    private static final Map<Character, Integer> ENCODE = new HashMap<>();
    private static final Map<Integer, Character> DECODE = new HashMap<>();

    static {
        map(' ', 0x00);

        String digits = "0123456789";
        for (int i = 0; i < digits.length(); i++) map(digits.charAt(i), 0xA1 + i);

        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < upper.length(); i++) map(upper.charAt(i), 0xBB + i);

        String lower = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < lower.length(); i++) map(lower.charAt(i), 0xD5 + i);

        map('!', 0xAB);
        map('?', 0xAC);
        map('.', 0xAD);
        map('-', 0xAE);
        map('…', 0xB0);
        map('“', 0xB1);
        map('”', 0xB2);
        map('‘', 0xB3);
        map('’', 0xB4);
        map('\'', 0xB4);
        map('♂', 0xB5);
        map('♀', 0xB6);
        map(',', 0xB8);
        map('×', 0xB9);
        map('/', 0xBA);
        map(':', 0xF0);
        map('%', 0x5B);
        map('(', 0x5C);
        map(')', 0x5D);
        map('&', 0x2D);
        map('+', 0x2E);
        map('=', 0x35);
        map(';', 0x36);

        map('À', 0x01); map('Á', 0x02); map('Â', 0x03); map('Ç', 0x04);
        map('È', 0x05); map('É', 0x06); map('Ê', 0x07); map('Ë', 0x08);
        map('Ì', 0x09); map('Î', 0x0B); map('Ï', 0x0C); map('Ò', 0x0D);
        map('Ó', 0x0E); map('Ô', 0x0F); map('Ù', 0x11); map('Ú', 0x12);
        map('Û', 0x13); map('Ñ', 0x14);

        map('à', 0x16); map('á', 0x17); map('ç', 0x19); map('è', 0x1A);
        map('é', 0x1B); map('ê', 0x1C); map('ë', 0x1D); map('ì', 0x1E);
        map('î', 0x20); map('ï', 0x21); map('ò', 0x22); map('ó', 0x23);
        map('ô', 0x24); map('ù', 0x26); map('ú', 0x27); map('û', 0x28);
        map('ñ', 0x29); map('â', 0x68); map('í', 0x6F);
        map('Ä', 0xF1); map('Ö', 0xF2); map('Ü', 0xF3);
        map('ä', 0xF4); map('ö', 0xF5); map('ü', 0xF6);
    }

    private Gen3TextCodec() {}

    private static void map(char c, int value) {
        ENCODE.put(c, value);
        DECODE.put(value, c);
    }

    static String decode(byte[] data, int offset, int length) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int value = Byte.toUnsignedInt(data[offset + i]);
            if (value == EOS) break;

            Character c = DECODE.get(value);
            result.append(c != null ? c : '�');
        }

        return result.toString();
    }

    static void encodeInto(String text, byte[] destination, int offset, int length) {
        if (text.length() > length) {
            throw new IllegalArgumentException(
                    "Text is too long: " + text.length() + " characters; maximum is " + length
            );
        }

        for (int i = 0; i < length; i++) {
            destination[offset + i] = (byte) EOS;
        }

        int out = 0;
        for (char c : text.toCharArray()) {
            Integer value = ENCODE.get(c);
            if (value == null) {
                throw new IllegalArgumentException(
                        "Unsupported Gen III character: '" + c + "' (U+"
                                + String.format("%04X", (int) c) + ")"
                );
            }

            if (out >= length) {
                throw new IllegalArgumentException("Encoded text exceeds field size of " + length);
            }

            destination[offset + out] = (byte) (int) value;
            out++;
        }
    }
}
