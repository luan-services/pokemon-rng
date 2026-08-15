/*
   Experimental isolation candidate 5a2.

   Purpose:
     isolate the C5 regression.

   Keep the SAME unsafe trampoline/layout introduced by C5:
     03003F80..03003F93

   But replace GetSavedRamScriptIfValid() with the already-proven PlaySE(SE_SELECT).

   Therefore:
     - if the Button Mode glitch still happens, the problem is strongly tied to
       the C5 call/return trampoline and/or its memory layout;
     - if the glitch disappears, GetSavedRamScriptIfValid() or its interaction
       with the current context becomes the stronger suspect.

   IMPORTANT:
     This candidate deliberately keeps the known-bad experimental occupation of
     gLinkCallback and nearby link globals. It exists only as a diagnostic test.
     It must never be promoted as a safe runtime layout.

   FR10:
     PlaySE = 080722CC -> Thumb 080722CD
     SE_SELECT = 0x0005
*/
final class NormalContextHotkeyCandidate5a2 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long FR10_PLAY_SE_THUMB = 0x080722CDL;
    private static final int SE_SELECT = 0x0005;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long CALL_TRAMPOLINE = 0x03003F80L;
    private static final long HOTKEY_DATA = 0x03003F94L;
    private static final long FUNCTION_LITERAL = 0x03003F9CL;

    private NormalContextHotkeyCandidate5a2() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] trampoline = buildCallTrampoline();
        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] functionLiteral = littleEndian32(FR10_PLAY_SE_THUMB);
        byte[] callbackTail = buildCallbackTailStub();
        byte[] callbackLiteral = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
        byte[] installer = buildInstaller(rom);

        FieldScriptWriter script = new FieldScriptWriter()
                .writeBytes(rom.tailStub, callbackTail)
                .writeBytes(rom.originalVBlankLiteral, callbackLiteral)

                .writeBytes(HOTKEY_DETECTOR, detector)
                .writeBytes(CALL_TRAMPOLINE, trampoline)
                .writeBytes(HOTKEY_DATA, heldKeysLiteral)
                .writeBytes(FUNCTION_LITERAL, functionLiteral)

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
    static long trampolineAddress() { return CALL_TRAMPOLINE; }
    static long hotkeyDataAddress() { return HOTKEY_DATA; }
    static long functionLiteralAddress() { return FUNCTION_LITERAL; }
    static long playSeThumb() { return FR10_PLAY_SE_THUMB; }
    static int seSelect() { return SE_SELECT; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] trampolineBytesForTest() { return buildCallTrampoline(); }
    static byte[] callbackTailBytesForTest() { return buildCallbackTailStub(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

    /* byte-for-byte same supervisor design as C4/C5 */
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

    /* same C5 detector: trigger goes to 03003F80 */
    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 5a2 assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,
            0x00, 0x68,
            0x01, 0x06,
            0x02, (byte)0xD3,
            (byte)0x81, 0x03,
            0x00, (byte)0xD3,
            0x00, (byte)0xE0,             // trigger -> 03003F80
            (byte)0x99, (byte)0xE7        // no-trigger -> 03003EB4
        };
    }

    /*
       SAME call/return mechanism class as C5, but with PlaySE argument setup.

         03003F80  push {lr}
         03003F82  movs r0,#5          ; SE_SELECT
         03003F84  ldr  r3,[pc,#0x14]  ; 03003F9C = PlaySE|1
         03003F86  adr  r2,03003F90
         03003F88  adds r2,#1
         03003F8A  mov  lr,r2
         03003F8C  bx   r3
         03003F8E  nop

       PlaySE returns to:
         03003F90  pop {pc}
         03003F92  nop

       The important diagnostic properties are preserved:
         - push/pop LR
         - fabricated local LR continuation
         - BX to ROM function
         - same 03003F80..03003F93 occupied region
         - same function literal slot at 03003F9C
    */
    private static byte[] buildCallTrampoline() {
        return new byte[] {
            0x00, (byte)0xB5,             // push {lr}
            0x05, 0x20,                   // movs r0,#5
            0x05, 0x4B,                   // ldr r3,[pc,#20] -> 03003F9C
            0x02, (byte)0xA2,             // adr r2,03003F90
            0x01, 0x32,                   // adds r2,#1
            (byte)0x96, 0x46,             // mov lr,r2
            0x18, 0x47,                   // bx r3
            (byte)0xC0, 0x46,             // nop
            0x00, (byte)0xBD,             // pop {pc}
            (byte)0xC0, 0x46              // nop
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
        putU32(result, 0, value);
        return result;
    }

    private static void requireFr10(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Dispatcher Candidate 5a2 is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
