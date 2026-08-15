/*
   Experimental phase-3 candidate 5.

   Purpose:
     prove that the persistent normal-context dispatcher can CALL a ROM function,
     receive a return value, and inspect/store that return value.

   Candidate 4 already validated:
     - VBlank supervisor auto-rearm;
     - R + SELECT detection in normal main-loop context;
     - native ROM function dispatch via PlaySE.

   Candidate 5 keeps the validated supervisor / callback strategy, but the
   trigger now calls:

       GetSavedRamScriptIfValid()

   FR10:
     GetSavedRamScriptIfValid = 08069E48 -> Thumb 08069E49

   On success, r0 is the current address of:
       gSaveBlock1Ptr->ramScript.data.script

   That pointer is copied to:
       03003FA4

   Expected relation:
       result == *gSaveBlock1Ptr + 0x3624

   Why +0x3624:
       SaveBlock1.ramScript       = +0x361C
       RamScript.checksum         = +0x0000 (4 bytes)
       RamScriptData header       = +0x0004 (magic/group/map/object = 4 bytes)
       script[]                   = +0x0008 relative to RamScript
       total                      = +0x3624

   Trigger frame behavior:
     - incoming callback LR is pushed;
     - LR is temporarily changed so the ROM function returns to our continuation;
     - returned r0 is stored;
     - incoming LR is restored with pop {pc};
     - CB1_Overworld is therefore skipped only on the trigger frame.

   Offline-only experimental layout (FR10):

     03005310..0300532F  VBlank supervisor
     03003F70..03003F7F  hotkey detector
     03003F80..03003F93  call/return trampoline
     03003F94..03003F97  heldKeysRaw pointer
     03003F9C..03003F9F  GetSavedRamScriptIfValid Thumb pointer
     03003FA4..03003FA7  returned script pointer
     03003EB4..03003EB7  no-trigger CB1 tail
     03003EC0..03003EC3  CB1_Overworld Thumb pointer

   This intentionally occupies link-test/link-state globals and is NOT approved
   for cable/wireless/link use.
*/
final class NormalContextHotkeyCandidate5 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long FR10_GET_SAVED_RAM_SCRIPT_THUMB = 0x08069E49L;
    private static final long FR10_GSAVE_BLOCK1_PTR = 0x03005008L;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long CALL_TRAMPOLINE = 0x03003F80L;
    private static final long HOTKEY_DATA = 0x03003F94L;
    private static final long FUNCTION_LITERAL = 0x03003F9CL;
    private static final long RESULT_SLOT = 0x03003FA4L;

    private NormalContextHotkeyCandidate5() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] trampoline = buildCallTrampoline();
        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] functionLiteral = littleEndian32(FR10_GET_SAVED_RAM_SCRIPT_THUMB);
        byte[] zeroResult = littleEndian32(0);
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
                .writeBytes(RESULT_SLOT, zeroResult)

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
    static long hotkeyDetectorAddress() { return HOTKEY_DETECTOR; }
    static long trampolineAddress() { return CALL_TRAMPOLINE; }
    static long hotkeyDataAddress() { return HOTKEY_DATA; }
    static long functionLiteralAddress() { return FUNCTION_LITERAL; }
    static long resultSlotAddress() { return RESULT_SLOT; }
    static long getSavedRamScriptThumb() { return FR10_GET_SAVED_RAM_SCRIPT_THUMB; }
    static long gSaveBlock1PtrAddress() { return FR10_GSAVE_BLOCK1_PTR; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] trampolineBytesForTest() { return buildCallTrampoline(); }
    static byte[] callbackTailBytesForTest() { return buildCallbackTailStub(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

    /* Same proven supervisor design as C3/C4. */
    private static byte[] buildSupervisor(RomProfile rom) {
        byte[] code = new byte[] {
            0x03, (byte)0xA3,             // adr r3, literals at +0x10
            0x07, (byte)0xCB,             // ldmia r3!, {r0,r1,r2}
            0x03, 0x68,                   // ldr r3,[r0]
            (byte)0x8B, 0x42,             // cmp r3,r1
            0x00, (byte)0xD1,             // bne skip
            0x02, 0x60,                   // str r2,[r0]
            0x03, 0x4B,                   // ldr r3, original VBlank literal
            0x18, 0x47,                   // bx r3

            0,0,0,0,                      // &callback1
            0,0,0,0,                      // CB1_Overworld|1
            0,0,0,0,                      // detector|1
            0,0,0,0                       // original VBlank|1
        };

        putU32(code, 0x10, FR10_GMAIN_CALLBACK1);
        putU32(code, 0x14, FR10_CB1_OVERWORLD_THUMB);
        putU32(code, 0x18, hotkeyDetectorThumb());
        putU32(code, 0x1C, rom.originalVBlankThumb);
        return code;
    }

    /*
       03003F70 detector.

       LDR still reads the held/new raw key word from the literal at 03003F94.

       Difference from C3/C4:
         trigger branch at 03003F7C now targets 03003F80 instead of 03003F98.
    */
    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 5 assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,                   // ldr r0, [pc,#0x20] -> 03003F94
            0x00, 0x68,                   // ldr r0, [r0]
            0x01, 0x06,                   // lsls r1,r0,#24   (R test through carry)
            0x02, (byte)0xD3,             // bcc no-trigger
            (byte)0x81, 0x03,             // lsls r1,r0,#14   (new SELECT test)
            0x00, (byte)0xD3,             // bcc no-trigger
            0x00, (byte)0xE0,             // b 03003F80 trigger trampoline
            (byte)0x99, (byte)0xE7        // b 03003EB4 normal CB1 tail
        };
    }

    /*
       Call/return trampoline:

         03003F80  push {lr}
         03003F82  ldr  r3, [pc,#0x18] -> 03003F9C function literal
         03003F84  adr  r2, 03003F8C
         03003F86  adds r2,#1
         03003F88  mov  lr,r2
         03003F8A  bx   r3

       GetSavedRamScriptIfValid returns to:

         03003F8C  adr  r1,03003FA4
         03003F8E  str  r0,[r1]
         03003F90  pop  {pc}
         03003F92  nop

       push/pop preserve the original callback return address.
    */
    private static byte[] buildCallTrampoline() {
        return new byte[] {
            0x00, (byte)0xB5,             // push {lr}
            0x06, 0x4B,                   // ldr r3, [pc,#24] -> 03003F9C
            0x01, (byte)0xA2,             // adr r2, 03003F8C
            0x01, 0x32,                   // adds r2,#1
            (byte)0x96, 0x46,             // mov lr,r2
            0x18, 0x47,                   // bx r3
            0x05, (byte)0xA1,             // adr r1, 03003FA4
            0x08, 0x60,                   // str r0,[r1]
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
        putU32(result,0,value);
        return result;
    }

    private static void requireFr10(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Dispatcher Candidate 5 is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
