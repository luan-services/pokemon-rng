/*
   Build 45 dedicated continuation bridge for the trade-evolution prototype.

   This intentionally does NOT install the hotkey/VBlank runtime. It only
   reuses IWRAM locations that were already validated by the hotkey work,
   because this build is a dedicated Deliveryman-only experiment.

   Flow:
     RamScript installer -> fade to black -> temporary EWRAM launcher
     -> stock ChoosePartyMonByMenuType(3)
     -> Party menu writes VAR_0x8004 and exits through CB2_ReturnToField
     -> ReturnToField relocates SaveBlock1
     -> gFieldCallback2 invokes the stable IWRAM continuation
     -> GetSavedRamScriptIfValid() resolves the NEW RamScript address
     -> ScriptContext_SetupScript(RamScript + 0x0C)

   No VBlank hook, callback1 hook, supervisor, live-code rewrite, or persistent
   physical SaveBlock pointer is used.
*/
final class PartyContinuationRuntime {
    static final long CALLBACK = RuntimeV1ResidentBlocks.WRAPPER; // 03005310, validated 32-byte slot
    static final long LITERAL_GET_RAM_SCRIPT = 0x03005358L;       // validated shared-runtime literal gap
    static final long LITERAL_SETUP_SCRIPT = 0x0300535CL;

    static final int CONTINUATION_OFFSET = 0x0C;
    static final int CALLBACK_SIZE = 32;
    static final int LAUNCHER_SIZE = 32;

    private PartyContinuationRuntime() {}

    static byte[] callback(RomProfile rom) {
        return callback(rom, CONTINUATION_OFFSET);
    }

    static byte[] callback(RomProfile rom, int continuationOffset) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45 continuation runtime currently supports fr10 only");
        if (continuationOffset < 0 || continuationOffset > 0xFF)
            throw new IllegalArgumentException("Continuation offset must fit Thumb adds immediate: " + continuationOffset);

        byte[] out = new byte[CALLBACK_SIZE];

        // 00 push {lr}
        putU16(out, 0x00, 0xB500);
        // 02 bl thunk_get @ 18
        putThumbBl(out, 0x02, 0x18);
        // 06 cmp r0,#0
        putU16(out, 0x06, 0x2800);
        // 08 beq fail @ 14
        putU16(out, 0x08, 0xD004);
        // 0A adds r0,#continuationOffset
        putU16(out, 0x0A, 0x3000 | continuationOffset);
        // 0C bl thunk_setup @ 1C
        putThumbBl(out, 0x0C, 0x1C);
        // 10 movs r0,#1 ; gFieldCallback2 completed
        putU16(out, 0x10, 0x2001);
        // 12 pop {pc}
        putU16(out, 0x12, 0xBD00);
        // 14 fail: movs r0,#1
        putU16(out, 0x14, 0x2001);
        // 16 pop {pc}
        putU16(out, 0x16, 0xBD00);

        // 18 thunk_get: ldr r3, [pc,#0x2C] -> 03005358; bx r3
        putU16(out, 0x18, 0x4B0B);
        putU16(out, 0x1A, 0x4718);
        // 1C thunk_setup: ldr r3, [pc,#0x2C] -> 0300535C; bx r3
        putU16(out, 0x1C, 0x4B0B);
        putU16(out, 0x1E, 0x4718);

        return out;
    }

    static byte[] callbackLiterals(RomProfile rom) {
        byte[] out = new byte[8];
        putU32(out, 0, rom.getSavedRamScriptThumb);
        putU32(out, 4, rom.scriptContextSetupThumb);
        return out;
    }

    static byte[] launcher(RomProfile rom, long launcherAddress) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 45 launcher currently supports fr10 only");

        // Temporary EWRAM helper. It is only needed until the Party menu starts.
        // ChoosePartyMonByMenuType(3) sets the stock gFieldCallback2 first; we
        // immediately replace it with the stable IWRAM continuation above.
        byte[] out = new byte[LAUNCHER_SIZE];

        putU16(out, 0x00, 0xB500); // push {lr}
        putU16(out, 0x02, 0x2003); // movs r0,#3 (same menuType used by stock ChoosePartyMon)
        putThumbBl(out, 0x04, 0x10); // bl thunk_choose
        putU16(out, 0x08, 0x4903); // ldr r1, =gFieldCallback2 @ 18
        putU16(out, 0x0A, 0x4804); // ldr r0, =CALLBACK|1 @ 1C
        putU16(out, 0x0C, 0x6008); // str r0,[r1]
        putU16(out, 0x0E, 0xBD00); // pop {pc}

        putU16(out, 0x10, 0x4B00); // thunk_choose: ldr r3, =ChoosePartyMonByMenuType|1 @ 14
        putU16(out, 0x12, 0x4718); // bx r3

        // FR1.0 symbols audited from the supplied pret map.
        putU32(out, 0x14, 0x081283A9L);
        putU32(out, 0x18, 0x03005024L); // gFieldCallback2
        putU32(out, 0x1C, CALLBACK | 1L);

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
