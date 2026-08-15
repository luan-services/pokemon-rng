/*
   Experimental phase-3 candidate 4.

   Purpose:
     prove that the persistent normal-context dispatcher can invoke an existing
     FireRed ROM function with an argument.

   Candidate 3 already validated:
     - VBlank supervisor auto-rearm;
     - R + SELECT detection in normal main-loop context.

   Candidate 4 keeps those pieces unchanged and replaces the debug marker with:

       PlaySE(SE_SELECT)

   FR10 symbols/constants:
     PlaySE          = 080722CC  -> Thumb pointer 080722CD
     SE_SELECT       = 0x0005

   IMPORTANT CALLING DETAIL:
     The trigger path TAIL-CALLS PlaySE with BX. It deliberately does not call
     CB1_Overworld on the trigger frame. The callback's incoming LR is preserved,
     so PlaySE returns directly to CallCallbacks/main-loop context. On the next
     frame the normal callback path runs again.

   Runtime layout (FR10 only):

     03005310..0300532F  32-byte VBlank supervisor (same bytes as Candidate 3)
     03003F70..03003F7F  16-byte hotkey detector (same bytes as Candidate 3)
     03003F94..03003F97  heldKeysRaw literal
     03003F98..03003F9F  8-byte PlaySE tail-call trigger
     03003FA4..03003FA7  PlaySE Thumb function literal (alignment padding)
     03003EB4..03003EB7  no-trigger callback tail stub
     03003EC0..03003EC3  CB1_Overworld Thumb literal

   Like Candidate 3, 03003F70 is gLinkTestBGInfo. This remains an OFFLINE-only
   experiment. Cable/wireless/link modes are outside the safety claim.
*/
final class NormalContextHotkeyCandidate4 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long FR10_PLAY_SE_THUMB = 0x080722CDL;
    private static final int SE_SELECT = 0x0005;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long HOTKEY_DATA = 0x03003F94L;
    private static final long TRIGGER_EXTENSION = 0x03003F98L;
    private static final long ACTION_LITERAL = 0x03003FA4L;

    private NormalContextHotkeyCandidate4() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] triggerExtension = buildTriggerExtension();
        byte[] actionLiteral = littleEndian32(FR10_PLAY_SE_THUMB);
        byte[] callbackTail = buildCallbackTailStub();
        byte[] callbackLiteral = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
        byte[] installer = buildInstaller(rom);

        if (supervisor.length != 32) throw new IllegalStateException("supervisor must be 32 bytes");
        if (detector.length != 16) throw new IllegalStateException("hotkey detector must be 16 bytes");
        if (heldKeysLiteral.length != 4) throw new IllegalStateException("heldKeys literal must be 4 bytes");
        if (triggerExtension.length != 8) throw new IllegalStateException("trigger extension must be 8 bytes");
        if (actionLiteral.length != 4) throw new IllegalStateException("action literal must be 4 bytes");
        if (callbackTail.length != 4) throw new IllegalStateException("callback tail must be 4 bytes");
        if (installer.length != 16) throw new IllegalStateException("installer must be 16 bytes");

        FieldScriptWriter script = new FieldScriptWriter()
                // Normal no-trigger callback return path.
                .writeBytes(rom.tailStub, callbackTail)
                .writeBytes(rom.originalVBlankLiteral, callbackLiteral)

                // Normal-context detector/action pieces.
                .writeBytes(HOTKEY_DETECTOR, detector)
                .writeBytes(HOTKEY_DATA, heldKeysLiteral)
                .writeBytes(TRIGGER_EXTENSION, triggerExtension)
                .writeBytes(ACTION_LITERAL, actionLiteral)

                // Persistent VBlank supervisor. Self-contained, same design as C3.
                .writeBytes(rom.mainHook, supervisor)

                // Redirect VBlank only after all runtime bytes exist.
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
    static long hotkeyDataAddress() { return HOTKEY_DATA; }
    static long triggerExtensionAddress() { return TRIGGER_EXTENSION; }
    static long actionLiteralAddress() { return ACTION_LITERAL; }
    static long playSeThumb() { return FR10_PLAY_SE_THUMB; }
    static int soundEffect() { return SE_SELECT; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] triggerBytesForTest() { return buildTriggerExtension(); }
    static byte[] callbackTailBytesForTest() { return buildCallbackTailStub(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

    /* Byte-for-byte identical supervisor logic to Candidate 3. */
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

    /* Byte-for-byte identical R-held + new-SELECT detector to Candidate 3. */
    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 4 detector currently assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,
            0x00, 0x68,
            0x01, 0x06,
            0x02, (byte)0xD3,
            (byte)0x81, 0x03,
            0x00, (byte)0xD3,
            0x0C, (byte)0xE0,
            (byte)0x99, (byte)0xE7
        };
    }

    /*
       Trigger path at 03003F98:

         03003F98  movs r0,#5             ; SE_SELECT
         03003F9A  ldr  r3,[pc,#8]        ; literal 03003FA4
         03003F9C  bx   r3                 ; tail-call PlaySE
         03003F9E  nop

       The callback's incoming LR is intentionally untouched. PlaySE therefore
       returns to the original caller of callback1. CB1_Overworld is skipped
       only on the trigger frame.
    */
    private static byte[] buildTriggerExtension() {
        return new byte[] {
            0x05, 0x20,                    // movs r0,#SE_SELECT
            0x02, 0x4B,                    // ldr r3, 03003FA4 PlaySE|1
            0x18, 0x47,                    // bx r3
            (byte)0xC0, 0x46               // nop
        };
    }

    /* No-trigger path only: tail-call normal CB1_Overworld. */
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
            throw new IllegalArgumentException("Dispatcher Candidate 4 is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
