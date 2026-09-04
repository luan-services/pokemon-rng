/*
   Build 45b: isolate GetSavedRamScriptIfValid() inside the dedicated
   post-Party gFieldCallback2.

   The callback deliberately does NOT resume ScriptContext and does NOT fade
   or unlock controls. A frozen-looking overworld is therefore expected.

   Marker written to VAR_0x8005:
     0x45B1 -> GetSavedRamScriptIfValid() returned non-NULL
     0x45BF -> GetSavedRamScriptIfValid() returned NULL
*/
final class PartyRamScriptResolveProbeRuntime {
    static final long CALLBACK = RuntimeV1ResidentBlocks.WRAPPER; // 03005310
    static final long LITERAL_GET_RAM_SCRIPT = 0x03005358L;
    static final long LITERAL_VAR_8005 = 0x0300535CL;
    static final int CALLBACK_SIZE = 32;

    private PartyRamScriptResolveProbeRuntime() {}

    static byte[] callback(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45b resolve probe currently supports fr10 only");

        byte[] out = new byte[CALLBACK_SIZE];

        // 00 push {lr}
        putU16(out, 0x00, 0xB500);
        // 02 bl thunk_get @ 1C
        putThumbBl(out, 0x02, 0x1C);
        // 06 movs r1,#0xBF      ; default = NULL marker low byte
        putU16(out, 0x06, 0x21BF);
        // 08 cmp r0,#0
        putU16(out, 0x08, 0x2800);
        // 0A beq mark           ; skip success low byte
        putU16(out, 0x0A, 0xD000);
        // 0C movs r1,#0xB1      ; non-NULL marker low byte
        putU16(out, 0x0C, 0x21B1);
        // 0E movs r0,#0x45
        putU16(out, 0x0E, 0x2045);
        // 10 lsls r0,r0,#8
        putU16(out, 0x10, 0x0200);
        // 12 adds r0,r0,r1      ; 0x45B1 or 0x45BF
        putU16(out, 0x12, 0x1840);
        // 14 ldr r1,[pc,#0x34]  ; literal 0300535C
        putU16(out, 0x14, 0x490D);
        // 16 strh r0,[r1]
        putU16(out, 0x16, 0x8008);
        // 18 movs r0,#1         ; gFieldCallback2 complete
        putU16(out, 0x18, 0x2001);
        // 1A pop {pc}
        putU16(out, 0x1A, 0xBD00);
        // 1C thunk_get: ldr r3,[pc,#0x28] -> 03005358 ; bx r3
        putU16(out, 0x1C, 0x4B0A);
        putU16(out, 0x1E, 0x4718);

        return out;
    }

    static byte[] callbackLiterals(RomProfile rom) {
        byte[] out = new byte[8];
        putU32(out, 0, rom.getSavedRamScriptThumb);
        putU32(out, 4, rom.specialVar8005);
        return out;
    }

    private static void putThumbBl(byte[] data, int sourceOffset, int targetOffset) {
        int pc = sourceOffset + 4;
        int delta = targetOffset - pc;
        if ((delta & 1) != 0 || delta < -(1 << 22) || delta >= (1 << 22))
            throw new IllegalArgumentException("Thumb BL target out of range/alignment");
        int hi = (delta >> 12) & 0x7FF;
        int lo = (delta >> 1) & 0x7FF;
        putU16(data, sourceOffset, 0xF000 | hi);
        putU16(data, sourceOffset + 2, 0xF800 | lo);
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
