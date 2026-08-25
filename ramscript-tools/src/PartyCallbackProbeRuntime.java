/*
   Build 45a: minimal gFieldCallback2/IWRAM probe.

   This deliberately does NOT call GetSavedRamScriptIfValid() or
   ScriptContext_SetupScript(). It only proves that ReturnToField can invoke a
   function at the known dedicated 32-byte IWRAM slot without crashing.

   On invocation it writes 0x45A1 directly to gSpecialVar_0x8005 and returns
   TRUE, allowing RunFieldCallback() to clear gFieldCallback2 normally.
*/
final class PartyCallbackProbeRuntime {
    static final long CALLBACK = RuntimeV1ResidentBlocks.WRAPPER; // 03005310
    static final int CALLBACK_SIZE = 32;

    private PartyCallbackProbeRuntime() {}

    static byte[] callback(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45a callback probe currently supports fr10 only");

        byte[] out = new byte[CALLBACK_SIZE];

        // 00 ldr r1, [pc,#0x0C] -> literal at +0x10
        putU16(out, 0x00, 0x4903);
        // 02 movs r0,#0x45
        putU16(out, 0x02, 0x2045);
        // 04 lsls r0,r0,#8
        putU16(out, 0x04, 0x0200);
        // 06 adds r0,#0xA1  => 0x45A1
        putU16(out, 0x06, 0x30A1);
        // 08 strh r0,[r1]
        putU16(out, 0x08, 0x8008);
        // 0A movs r0,#1 (bool8 TRUE)
        putU16(out, 0x0A, 0x2001);
        // 0C bx lr
        putU16(out, 0x0C, 0x4770);
        // 0E nop / alignment
        putU16(out, 0x0E, 0x46C0);
        // 10 literal: &gSpecialVar_0x8005
        putU32(out, 0x10, rom.specialVar8005);

        return out;
    }

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
