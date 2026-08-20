/*
   Thumb helper for PartyIvViewerPreset.

   The helper builds the complete party IV viewer text in one pass at
   gStringVar4 + 0x280. For each occupied party slot it writes:

     <nickname> IVs:\nHP <n> ATK <n> DEF <n>
     {PROMPT_CLEAR}
     SPA <n> SPD <n> SPE <n>

   A second PROMPT_CLEAR is inserted before the next Pokemon. Because the whole
   party is one text stream, the stock text printer keeps the same message box
   open and shows its normal down-arrow prompt between pages/Pokemon.

   GetMonData(MON_DATA_IVS) returns all six IVs packed into one u32, so each
   Pokemon needs only three stock calls: species check, nickname, and packed IVs.
*/
final class PartyMonDataNativeHelper {
    static final int CODE_SIZE = 296;
    static final int DYNAMIC_MESSAGE_DELTA = 0x280;
    static final int MAX_MESSAGE_BYTES = 342;

    private static final int PLAYER_PARTY_LITERAL_OFFSET = 0x11C;
    private static final int DYNAMIC_MESSAGE_LITERAL_OFFSET = 0x120;
    private static final int GET_MON_DATA_LITERAL_OFFSET = 0x124;

    private PartyMonDataNativeHelper() {}

    static NativeHelper build(RomProfile rom) {
        return buildAt(rom, rom.stringVar4 + 0x140L);
    }

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] code = hex(
                "F0B5464D464C002628000B21002200F069F800284DD0002E02D0FB202070013428000221220000F05DF8" +
                "2078FF2801D00134FAE72CA000F04DF828004221002200F050F8070038001F21084000F034F828A000F0" +
                "3FF8380040091F21084000F02BF826A000F036F83800800A1F21084000F022F823A000F02DF83800000D" +
                "1F21084000F019F821A000F024F83800400E1F21084000F010F81EA000F01BF83800C00B1F21084000F0" +
                "07F864350136062EAAD3FF202070F0BD0A2807D300210A3801310A28FBD2A13121700134A13020700134" +
                "70470178FF2903D0217001300134F8E770470F4B184700C3D0E7F0FEC2CA00FFC04600BBCEC500FFC046" +
                "00BEBFC000FFC046FBCDCABB00FFC04600CDCABE00FFC04600CDCABF00FFC046111111112222222233333333"
        );

        putU32(code, PLAYER_PARTY_LITERAL_OFFSET, rom.playerParty);
        putU32(code, DYNAMIC_MESSAGE_LITERAL_OFFSET, dynamicMessageAddress(rom));
        putU32(code, GET_MON_DATA_LITERAL_OFFSET, rom.getMonData3Thumb);

        if (code.length != CODE_SIZE) {
            throw new IllegalStateException("party IV message helper size mismatch");
        }
        if (stagingAddress + code.length > dynamicMessageAddress(rom)) {
            throw new IllegalStateException("party IV helper overlaps dynamic message buffer");
        }
        // gStringVar4 is 1000 bytes in stock FR/LG. The source begins at +0x280,
        // leaving 360 bytes; worst-case six 10-character nicknames use 342.
        if (DYNAMIC_MESSAGE_DELTA + MAX_MESSAGE_BYTES > 1000) {
            throw new IllegalStateException("party IV message can exceed gStringVar4");
        }

        return new NativeHelper(stagingAddress, code);
    }

    static long dynamicMessageAddress(RomProfile rom) {
        return rom.stringVar4 + DYNAMIC_MESSAGE_DELTA;
    }

    private static byte[] hex(String value) {
        if ((value.length() & 1) != 0) {
            throw new IllegalArgumentException("hex string length must be even");
        }
        byte[] data = new byte[value.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return data;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
