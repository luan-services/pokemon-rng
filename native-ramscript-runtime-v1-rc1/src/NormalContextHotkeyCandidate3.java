/*
   Experimental phase-3 candidate 3.

   Purpose:
     combine the two behaviors already validated separately:
       - Candidate 1: R + SELECT detection from normal main-loop context;
       - Candidate 2d: automatic re-arming after map/battle/menu callback changes.

   Candidate 3 still does NOT run a real payload. On R + SELECT it writes one
   marker byte (0x77) to 03003FA1, then immediately tail-chains to the original
   CB1_Overworld.

   Runtime layout (FR10 only):

     03005310..0300532F  32-byte VBlank supervisor, fully self-contained
     03003F70..03003F7F  16-byte normal-context hotkey detector
     03003F94..03003F97  heldKeysRaw literal
     03003F98..03003F9F  8-byte trigger extension
     03003FA1             one-byte debug marker (padding after gLastSendQueueCount)
     03003EB4..03003EB7  callback tail stub
     03003EC0..03003EC3  CB1_Overworld Thumb literal

   IMPORTANT:
     03003F70 is gLinkTestBGInfo (16 bytes). The symbol table identifies it as
     link-test storage, not generic free RAM. Candidate 3 is therefore a
     NORMAL/OFFLINE gameplay experiment only. Link/cable/wireless behavior is
     explicitly outside this candidate's safety claim.
*/
final class NormalContextHotkeyCandidate3 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long HOTKEY_DATA = 0x03003F94L;
    private static final long TRIGGER_EXTENSION = 0x03003F98L;
    private static final long MARKER_ADDRESS = 0x03003FA1L;
    private static final int MARKER_VALUE = 0x77;

    private NormalContextHotkeyCandidate3() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] triggerExtension = buildTriggerExtension();
        byte[] callbackTail = buildCallbackTailStub();
        byte[] callbackLiteral = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
        byte[] installer = buildInstaller(rom);

        if (supervisor.length != 32) throw new IllegalStateException("supervisor must be 32 bytes");
        if (detector.length != 16) throw new IllegalStateException("hotkey detector must be 16 bytes");
        if (heldKeysLiteral.length != 4) throw new IllegalStateException("heldKeys literal must be 4 bytes");
        if (triggerExtension.length != 8) throw new IllegalStateException("trigger extension must be 8 bytes");
        if (callbackTail.length != 4) throw new IllegalStateException("callback tail must be 4 bytes");
        if (installer.length != 16) throw new IllegalStateException("installer must be 16 bytes");

        FieldScriptWriter script = new FieldScriptWriter()
                // Callback return path.
                .writeBytes(rom.tailStub, callbackTail)
                .writeBytes(rom.originalVBlankLiteral, callbackLiteral)

                // Normal-context dispatcher pieces.
                .writeBytes(HOTKEY_DETECTOR, detector)
                .writeBytes(HOTKEY_DATA, heldKeysLiteral)
                .writeBytes(TRIGGER_EXTENSION, triggerExtension)

                // Persistent VBlank supervisor. This contains its own original
                // VBlank literal and no longer depends on the callback tail.
                .writeBytes(rom.mainHook, supervisor)

                // Redirect VBlank last.
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
    static long markerAddress() { return MARKER_ADDRESS; }
    static int markerValue() { return MARKER_VALUE; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] triggerBytesForTest() { return buildTriggerExtension(); }
    static byte[] callbackTailBytesForTest() { return buildCallbackTailStub(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

    /*
       32-byte VBlank supervisor.

       It only re-arms callback1 when callback1 is exactly CB1_Overworld.
       All other callback states are left untouched.

       Unlike Candidate 2d, this supervisor is completely self-contained:
       its fourth literal is the original VBlank handler, so its IRQ return
       path shares no code/data with the normal callback path.
    */
    private static byte[] buildSupervisor(RomProfile rom) {
        byte[] code = new byte[] {
            0x03, (byte) 0xA3,            // adr   r3, 03005320 literal table
            0x07, (byte) 0xCB,            // ldmia r3!, {r0,r1,r2}
            0x03, 0x68,                    // ldr   r3,[r0] current callback1
            (byte) 0x8B, 0x42,            // cmp   r3,r1
            0x00, (byte) 0xD1,            // bne   tail
            0x02, 0x60,                    // str   r2,[r0] callback1 = detector
            0x03, 0x4B,                    // tail: ldr r3, original VBlank literal
            0x18, 0x47,                    // bx    r3

            0,0,0,0,                       // 03005320 &gMain.callback1
            0,0,0,0,                       // 03005324 CB1_Overworld|1
            0,0,0,0,                       // 03005328 detector|1
            0,0,0,0                        // 0300532C original VBlank|1
        };

        putU32(code, 0x10, FR10_GMAIN_CALLBACK1);
        putU32(code, 0x14, FR10_CB1_OVERWORLD_THUMB);
        putU32(code, 0x18, hotkeyDetectorThumb());
        putU32(code, 0x1C, rom.originalVBlankThumb);
        return code;
    }

    /*
       16-byte detector in gLinkTestBGInfo (03003F70).

       It reproduces the exact key-test shape already validated in Candidate 1:
         held R + newly pressed SELECT.

       The held/new raw key halves are loaded together as one u32 from
       gMain.heldKeysRaw. Trigger branches to 03003F98. No-trigger branches to
       the callback tail at 03003EB4.
    */
    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 3 detector currently assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,                    // ldr   r0, [pc,#0x20] -> 03003F94
            0x00, 0x68,                    // ldr   r0, [r0]
            0x01, 0x06,                    // lsls  r1,r0,#24 -> C = held R
            0x02, (byte) 0xD3,            // bcc   03003F7E no_trigger
            (byte) 0x81, 0x03,            // lsls  r1,r0,#14 -> C = new SELECT
            0x00, (byte) 0xD3,            // bcc   03003F7E no_trigger
            0x0C, (byte) 0xE0,            // b     03003F98 trigger extension
            (byte) 0x99, (byte) 0xE7      // b     03003EB4 callback tail
        };
    }

    /*
       8-byte trigger extension at 03003F98.

       Uses ADR rather than an absolute literal for the marker. r1 becomes
       03003FA0, then STRB writes 0x77 at +1 (03003FA1). The named byte at
       03003FA0 (gLastSendQueueCount) itself is NOT modified.

       After the marker write it branches to the same CB1 tail as no-trigger.
    */
    private static byte[] buildTriggerExtension() {
        return new byte[] {
            0x01, (byte) 0xA1,            // adr   r1, 03003FA0
            0x77, 0x20,                    // movs  r0,#0x77
            0x48, 0x70,                    // strb  r0,[r1,#1] -> 03003FA1
            (byte) 0x89, (byte) 0xE7      // b     03003EB4 callback tail
        };
    }

    /* Callback-only tail. 03003EC0 contains CB1_Overworld|1. */
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
            throw new IllegalArgumentException("Dispatcher Candidate 3 is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
