/*
   Experimental Candidate 5c.

   Goal:
     keep the clean C5b memory layout and switch the action back to
     GetSavedRamScriptIfValid(), while avoiding every live global that caused
     the C5/C5a2 regression.

   Because the safe padding around 03003FA0 is very tight, Candidate 5c does
   not try to reserve a permanent 32-bit debug result slot there. Instead it
   stores the LOW BYTE of the returned pointer at 03003FA1.

   This is enough to verify the dynamic relation:

       low8(result) == low8(*gSaveBlock1Ptr + 0x3624)

   and it can be triggered repeatedly after map transitions.

   FR10 layout:

     03003F70..03003F7F  hotkey detector

     03003F94..03003F97  gLinkFiller3 -> heldKeysRaw pointer
     03003F98..03003F9B  gLinkFiller4 -> tiny BX thunk
     03003F9C..03003F9F  gLinkFiller5 -> GetSavedRamScriptIfValid|1

     03003FA0             gLastSendQueueCount (UNTOUCHED)
     03003FA1             returned pointer low byte
     03003FA2..03003FAF  call wrapper
     03003FB0             gLink starts here (UNTOUCHED)

   03003F80..03003F93 remain completely untouched.

   The call wrapper uses a normal nearby Thumb BL to the thunk:
     - push {r4,lr}
     - calculate address 03003FA1 in r4
     - BL thunk
     - thunk BX'es GetSavedRamScriptIfValid
     - ROM function returns to the wrapper
     - low byte of r0 is stored at [r4]
     - pop {r4,pc}

   FR10:
     GetSavedRamScriptIfValid = 08069E48 -> Thumb 08069E49
*/
final class NormalContextHotkeyCandidate5c {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long FR10_GET_SAVED_RAM_SCRIPT_THUMB = 0x08069E49L;
    private static final long FR10_GSAVE_BLOCK1_PTR = 0x03005008L;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long HELD_KEYS_LITERAL = 0x03003F94L;
    private static final long FUNCTION_THUNK = 0x03003F98L;
    private static final long FUNCTION_LITERAL = 0x03003F9CL;
    private static final long RESULT_LOW_BYTE = 0x03003FA1L;
    private static final long CALL_WRAPPER = 0x03003FA2L;

    private NormalContextHotkeyCandidate5c() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] thunk = buildFunctionThunk();
        byte[] wrapper = buildCallWrapper();
        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] functionLiteral = littleEndian32(FR10_GET_SAVED_RAM_SCRIPT_THUMB);
        byte[] zeroMarker = new byte[] { 0 };
        byte[] callbackTail = buildCallbackTailStub();
        byte[] callbackLiteral = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
        byte[] installer = buildInstaller(rom);

        FieldScriptWriter script = new FieldScriptWriter()
                .writeBytes(rom.tailStub, callbackTail)
                .writeBytes(rom.originalVBlankLiteral, callbackLiteral)

                .writeBytes(HOTKEY_DETECTOR, detector)
                .writeBytes(HELD_KEYS_LITERAL, heldKeysLiteral)
                .writeBytes(FUNCTION_THUNK, thunk)
                .writeBytes(FUNCTION_LITERAL, functionLiteral)
                .writeBytes(RESULT_LOW_BYTE, zeroMarker)
                .writeBytes(CALL_WRAPPER, wrapper)

                .writeBytes(rom.mainHook, supervisor)

                .writeBytes(rom.installerStaging, installer)
                .callNative(rom.installerStaging | 1L)
                .returnRam();

        return RamScript.wonderCardScript(script.build());
    }

    static long callback1Address() { return FR10_GMAIN_CALLBACK1; }
    static long cb1OverworldThumb() { return FR10_CB1_OVERWORLD_THUMB; }
    static long supervisorThumb(RomProfile rom) { return rom.mainHook | 1L; }
    static long hotkeyDetectorThumb() { return HOTKEY_DETECTOR | 1L; }
    static long functionThunkAddress() { return FUNCTION_THUNK; }
    static long functionLiteralAddress() { return FUNCTION_LITERAL; }
    static long resultLowByteAddress() { return RESULT_LOW_BYTE; }
    static long callWrapperAddress() { return CALL_WRAPPER; }
    static long getSavedRamScriptThumb() { return FR10_GET_SAVED_RAM_SCRIPT_THUMB; }
    static long gSaveBlock1PtrAddress() { return FR10_GSAVE_BLOCK1_PTR; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] thunkBytesForTest() { return buildFunctionThunk(); }
    static byte[] wrapperBytesForTest() { return buildCallWrapper(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

    /* byte-for-byte same supervisor used by C5b */
    private static byte[] buildSupervisor(RomProfile rom) {
        byte[] code = new byte[] {
            0x03, (byte)0xA3,
            0x07, (byte)0xCB,
            0x03, 0x68,
            (byte)0x8B, 0x42,
            0x00, (byte)0xD1,
            0x02, 0x60,
            0x03, 0x4B,
            0x18, 0x47,

            0,0,0,0,
            0,0,0,0,
            0,0,0,0,
            0,0,0,0
        };

        putU32(code, 0x10, FR10_GMAIN_CALLBACK1);
        putU32(code, 0x14, FR10_CB1_OVERWORLD_THUMB);
        putU32(code, 0x18, hotkeyDetectorThumb());
        putU32(code, 0x1C, rom.originalVBlankThumb);
        return code;
    }

    /*
       Same detector logic as C5b, but trigger target is now 03003FA2.
    */
    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 5c assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,                    // ldr r0,[pc,#0x20] -> 03003F94
            0x00, 0x68,                    // ldr r0,[r0]
            0x01, 0x06,                    // lsls r1,r0,#24
            0x02, (byte)0xD3,              // bcc no-trigger
            (byte)0x81, 0x03,              // lsls r1,r0,#14
            0x00, (byte)0xD3,              // bcc no-trigger
            0x11, (byte)0xE0,              // b 03003FA2
            (byte)0x99, (byte)0xE7         // b 03003EB4
        };
    }

    /*
       03003F98:
         ldr r3,[pc,#0] -> 03003F9C
         bx  r3

       BL from the wrapper sets LR. BX preserves it, so the ROM function
       returns directly to the instruction after BL.
    */
    private static byte[] buildFunctionThunk() {
        return new byte[] {
            0x00, 0x4B,
            0x18, 0x47
        };
    }

    /*
       03003FA2..03003FAF:

         03003FA2  push {r4,lr}
         03003FA4  mov  r4,pc       ; r4 = 03003FA8
         03003FA6  subs r4,#7       ; r4 = 03003FA1
         03003FA8  bl   03003F98
         03003FAC  strb r0,[r4]
         03003FAE  pop  {r4,pc}

       BL encoding for 03003FA8 -> 03003F98:
         FF F7 F6 FF

       The marker can be updated repeatedly because the function literal and
       detector data remain intact.
    */
    private static byte[] buildCallWrapper() {
        return new byte[] {
            0x10, (byte)0xB5,              // push {r4,lr}
            0x7C, 0x46,                    // mov r4,pc
            0x07, 0x3C,                    // subs r4,#7
            (byte)0xFF, (byte)0xF7,        // BL first half
            (byte)0xF6, (byte)0xFF,        // BL second half -> 03003F98
            0x20, 0x70,                    // strb r0,[r4]
            0x10, (byte)0xBD               // pop {r4,pc}
        };
    }

    private static byte[] buildCallbackTailStub() {
        return new byte[] { 0x02,0x4B, 0x18,0x47 };
    }

    private static byte[] buildInstaller(RomProfile rom) {
        byte[] code = new byte[] {
            0x01, 0x48,
            0x02, 0x49,
            0x01, 0x60,
            0x70, 0x47,
            0,0,0,0,
            0,0,0,0
        };
        putU32(code, 0x08, rom.vblankSlot);
        putU32(code, 0x0C, supervisorThumb(rom));
        return code;
    }

    private static byte[] littleEndian32(long value) {
        byte[] result = new byte[4];
        putU32(result,0,value);
        return result;
    }

    private static void requireFr10(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Dispatcher Candidate 5c is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
