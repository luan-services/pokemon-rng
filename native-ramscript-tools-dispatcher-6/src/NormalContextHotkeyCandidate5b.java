/*
   Experimental isolation candidate 5b.

   Purpose:
     keep the C5/C5a2 call/return idea while removing the known-bad overlap with
     live globals at 03003F80..03003F93.

   Candidate 5b DOES NOT write gLinkCallback or its neighboring live globals.

   FR10 layout used by this diagnostic:

     03003F70..03003F7F  hotkey detector (same link-test-only area proven by C3/C4)

     03003F94..03003F97  gLinkFiller3 -> heldKeysRaw pointer
     03003F98..03003F9B  gLinkFiller4 -> tiny BX thunk
     03003F9C..03003F9F  gLinkFiller5 -> PlaySE Thumb pointer

     03003FA0             gLastSendQueueCount (UNTOUCHED)
     03003FA1             alignment padding (unused)
     03003FA2..03003FA3  local continuation: pop {pc}
     03003FA4..03003FAF  trigger wrapper in linker padding
     03003FB0             gLink starts here (UNTOUCHED)

   The wrapper deliberately preserves the same conceptual mechanism under test:

     push incoming LR
     set argument
     fabricate a local Thumb return address in LR
     branch to a tiny thunk
     thunk BX'es PlaySE
     PlaySE returns to local continuation
     pop {pc} restores caller's original return address

   No code or data is written to 03003F80..03003F93.

   FR10:
     PlaySE = 080722CC -> Thumb 080722CD
     SE_SELECT = 0x0005
*/
final class NormalContextHotkeyCandidate5b {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long FR10_PLAY_SE_THUMB = 0x080722CDL;
    private static final int SE_SELECT = 0x0005;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long HELD_KEYS_LITERAL = 0x03003F94L;
    private static final long FUNCTION_THUNK = 0x03003F98L;
    private static final long FUNCTION_LITERAL = 0x03003F9CL;
    private static final long CONTINUATION = 0x03003FA2L;
    private static final long CALL_WRAPPER = 0x03003FA4L;

    private NormalContextHotkeyCandidate5b() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] thunk = buildFunctionThunk();
        byte[] wrapper = buildCallWrapper();
        byte[] continuation = buildContinuation();
        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] functionLiteral = littleEndian32(FR10_PLAY_SE_THUMB);
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
                .writeBytes(CONTINUATION, continuation)
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
    static long continuationAddress() { return CONTINUATION; }
    static long callWrapperAddress() { return CALL_WRAPPER; }
    static long playSeThumb() { return FR10_PLAY_SE_THUMB; }
    static int seSelect() { return SE_SELECT; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] thunkBytesForTest() { return buildFunctionThunk(); }
    static byte[] continuationBytesForTest() { return buildContinuation(); }
    static byte[] wrapperBytesForTest() { return buildCallWrapper(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

    /* preserve the already runtime-tested C4/C5 supervisor */
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
       Detector remains at 03003F70.

       Only change from C5a2:
         trigger branch now goes to 03003FA4 instead of 03003F80.
    */
    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 5b assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,                    // ldr r0,[pc,#0x20] -> 03003F94
            0x00, 0x68,                    // ldr r0,[r0]
            0x01, 0x06,                    // lsls r1,r0,#24
            0x02, (byte)0xD3,              // bcc no-trigger
            (byte)0x81, 0x03,              // lsls r1,r0,#14
            0x00, (byte)0xD3,              // bcc no-trigger
            0x12, (byte)0xE0,              // b 03003FA4
            (byte)0x99, (byte)0xE7         // b 03003EB4
        };
    }

    /*
       03003F98:
         ldr r3,[pc,#0] -> literal at 03003F9C
         bx  r3

       The called ROM function inherits LR fabricated by the wrapper.
    */
    private static byte[] buildFunctionThunk() {
        return new byte[] {
            0x00, 0x4B,
            0x18, 0x47
        };
    }

    /*
       03003FA2:
         pop {pc}

       PlaySE returns here using LR = 03003FA3.
       pop {pc} restores the original callback return address pushed by wrapper.
    */
    private static byte[] buildContinuation() {
        return new byte[] {
            0x00, (byte)0xBD
        };
    }

    /*
       03003FA4..03003FAF (12 bytes of linker padding):

         03003FA4  push {lr}
         03003FA6  movs r0,#5
         03003FA8  mov  r2,pc       ; r2 = 03003FAC
         03003FAA  subs r2,#9       ; r2 = 03003FA3
         03003FAC  mov  lr,r2
         03003FAE  b    03003F98

       Because bit 0 of 03003FA3 is set, PlaySE returns in Thumb state to
       03003FA2, where pop {pc} resumes the main loop.
    */
    private static byte[] buildCallWrapper() {
        return new byte[] {
            0x00, (byte)0xB5,              // push {lr}
            0x05, 0x20,                    // movs r0,#5
            0x7A, 0x46,                    // mov r2,pc
            0x09, 0x3A,                    // subs r2,#9
            (byte)0x96, 0x46,              // mov lr,r2
            (byte)0xF3, (byte)0xE7         // b 03003F98
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
            throw new IllegalArgumentException("Dispatcher Candidate 5b is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
