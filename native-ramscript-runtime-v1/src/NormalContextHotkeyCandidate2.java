/*
   Experimental phase-3 candidate 2.

   Purpose:
     prove automatic re-arming of a normal-context callback wrapper after the
     game temporarily replaces callback1/callback2 for map transitions,
     battles, menus, and other modes.

   This candidate deliberately does NOT contain the R+SELECT action. Candidate
   1 already proved hotkey execution in normal main-loop context. Candidate 2
   isolates only the persistence/re-arm problem.

   Architecture:

     gIntrTable[VBLANK]
       -> 03005311 supervisor
            if callback1 == CB1_Overworld:
                callback1 = 03003F95
            otherwise leave callback1 untouched
            tail-chain to original VBlankIntr

     gMain.callback1 (only while normal overworld is active)
       -> 03003F95 tiny marker wrapper
            *(u8 *)0201C100 = 0x77
            tail-chain to CB1_Overworld

   During transitions/battles callback1 is NULL or another callback, so the
   supervisor leaves it completely alone. Only after the game itself restores
   CB1_Overworld does the next VBlank install the wrapper again.

   Fixed runtime regions are the same regions already exercised by the
   fixed-RAM stability tests. No new RAM region is introduced here.
*/
final class NormalContextHotkeyCandidate2 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long FR10_CB2_OVERWORLD_THUMB = 0x080565B5L;

    private static final long DEBUG_ADDRESS = 0x0201C100L;
    private static final int DEBUG_MARKER = 0x77;

    private NormalContextHotkeyCandidate2() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] callbackWrapper = buildCallbackWrapper(rom);
        byte[] callbackTailStub = buildCallbackTailStub();
        byte[] originalVBlank = littleEndian32(rom.originalVBlankThumb);
        byte[] installer = buildInstaller(rom);

        if (supervisor.length != 32)
            throw new IllegalStateException("Supervisor must be exactly 32 bytes");
        if (callbackWrapper.length != 12)
            throw new IllegalStateException("Callback wrapper must be exactly 12 bytes");
        if (callbackTailStub.length != 4)
            throw new IllegalStateException("Callback tail stub must be exactly 4 bytes");
        if (installer.length != 16)
            throw new IllegalStateException("Installer must be exactly 16 bytes");

        FieldScriptWriter script = new FieldScriptWriter()
                // Install every runtime byte before redirecting VBlank.
                .writeBytes(rom.tailStub, callbackTailStub)
                .writeBytes(rom.originalVBlankLiteral, originalVBlank)
                .writeBytes(rom.rngExtension, callbackWrapper)
                .writeBytes(rom.mainHook, supervisor)

                // Atomic VBlank redirect is performed last from staging RAM.
                .writeBytes(rom.installerStaging, installer)
                .callNative(rom.installerStaging | 1L)
                .returnRam();

        return RamScript.wonderCardScript(script.build());
    }

    static long callback1Address() {
        return FR10_GMAIN_CALLBACK1;
    }

    static long callback2Address() {
        return FR10_GMAIN_CALLBACK1 + 4;
    }

    static long cb1OverworldThumb() {
        return FR10_CB1_OVERWORLD_THUMB;
    }

    static long cb2OverworldThumb() {
        return FR10_CB2_OVERWORLD_THUMB;
    }

    static long debugAddress() {
        return DEBUG_ADDRESS;
    }

    static int debugMarker() {
        return DEBUG_MARKER;
    }

    static long callbackWrapperThumb(RomProfile rom) {
        return rom.rngExtension | 1L;
    }

    static long supervisorThumb(RomProfile rom) {
        return rom.mainHook | 1L;
    }


    static byte[] supervisorBytesForTest(RomProfile rom) {
        return buildSupervisor(rom);
    }

    static byte[] callbackWrapperBytesForTest(RomProfile rom) {
        return buildCallbackWrapper(rom);
    }

    static byte[] vblankTailBytesForTest() {
        return buildCallbackTailStub();
    }

    static byte[] installerBytesForTest(RomProfile rom) {
        return buildInstaller(rom);
    }

    /*
       32-byte VBlank supervisor, revision 2d.

       Candidate 2 had two architectural bugs:
         1. it clobbered r4, a callee-saved register, before tail-chaining;
         2. it gated on callback2 only, so it could reinstall callback1 during
            a transition window where the game intentionally set callback1 NULL.

       Candidate 2b fixed those architectural bugs but had two machine-code
       encoding bugs:
         3. ADR r3 used imm=3, resolving to 03005320 instead of the literal
            table at 03005324;
         4. the callback-wrapper branch encoded 03003EB2 instead of 03003EB4.

       Candidate 2c fixed those four issues but still incorrectly shared the same
       VBlank tail literal with the callback wrapper. Candidate 2d separates
       those control-flow targets completely.

       This revision uses ONLY r0-r3 and considers callback1 itself the safety
       gate. It installs the wrapper only when callback1 is EXACTLY the normal
       CB1_Overworld Thumb pointer (08056535).

       Therefore:
         callback1 == NULL / battle / menu / transition -> do nothing
         callback1 == wrapper                          -> do nothing
         callback1 == CB1_Overworld                   -> install wrapper

       Literal loading:
         r0 = &gMain.callback1
         r1 = CB1_Overworld|1
         r2 = callback wrapper|1

       r2 is also used to derive the already-known fixed VBlank tail stub:
         03003F95 - 0xE0 = 03003EB5
    */
    private static byte[] buildSupervisor(RomProfile rom) {
        if ((callbackWrapperThumb(rom) - 0xE0L) != (rom.tailStub | 1L)) {
            throw new IllegalStateException(
                    "Candidate 2d compact supervisor requires wrapper|1 - 0xE0 == tailStub|1"
            );
        }

        byte[] code = new byte[] {
            0x04, (byte) 0xA3,            // adr   r3, literals at 03005324
            0x07, (byte) 0xCB,            // ldmia r3!, {r0,r1,r2}
            0x03, 0x68,                    // ldr   r3,[r0]       current callback1
            (byte) 0x8B, 0x42,            // cmp   r3,r1         == CB1_Overworld?
            0x00, (byte) 0xD1,            // bne   tail
            0x02, 0x60,                    // str   r2,[r0]       callback1 = wrapper
            (byte) 0xE0, 0x3A,            // tail: subs r2,#0xE0 -> tailStub|1
            0x10, 0x47,                    // bx    r2
            (byte) 0xC0, 0x46,            // padding/nop
            (byte) 0xC0, 0x46,            // padding/nop

            0, 0, 0, 0,                    // &gMain.callback1
            0, 0, 0, 0,                    // CB1_Overworld|1
            0, 0, 0, 0                     // callback wrapper|1
        };

        putU32(code, 0x14, FR10_GMAIN_CALLBACK1);
        putU32(code, 0x18, FR10_CB1_OVERWORLD_THUMB);
        putU32(code, 0x1C, callbackWrapperThumb(rom));
        return code;
    }

    /*
       12-byte normal-context wrapper at 03003F94.

       Candidate 2c incorrectly reused the VBlank tail stub/literal for this
       callback. That made the VBlank supervisor and callback wrapper require
       different functions in the same literal slot.

       Candidate 2d gives the callback wrapper its OWN CB1_Overworld literal:

         03003F94  ldr r3, [pc, #4]  -> literal at 03003F9C
         03003F96  bx  r3
         03003F98  nop
         03003F9A  nop
         03003F9C  .word 08056535

       The VBlank tail stub at 03003EB4 therefore remains dedicated to the
       original VBlank handler (08000725).

       There is deliberately no debug write in this candidate. callback1 itself
       is the observable marker: 03003F95 means the wrapper is armed.
    */
    private static byte[] buildCallbackWrapper(RomProfile rom) {
        if (rom.rngExtension != 0x03003F94L) {
            throw new IllegalStateException(
                    "Candidate 2d compact callback wrapper is currently FR10-only"
            );
        }

        byte[] code = new byte[] {
            0x01, 0x4B,                    // ldr r3, CB1 literal at 03003F9C
            0x18, 0x47,                    // bx  r3
            (byte) 0xC0, 0x46,             // nop
            (byte) 0xC0, 0x46,             // nop
            0, 0, 0, 0                     // CB1_Overworld|1
        };

        putU32(code, 0x08, FR10_CB1_OVERWORLD_THUMB);
        return code;
    }

    /*
       03003EB4:
         ldr r3,[pc,#8] -> literal at 03003EC0
         bx  r3

       03003EC0 stores the ORIGINAL VBlank handler|1 (08000725).

       This tail is now used only by the VBlank supervisor.
    */
    private static byte[] buildCallbackTailStub() {
        return new byte[] {
            0x02, 0x4B,
            0x18, 0x47
        };
    }

    /*
       Installer only redirects the VBlank table slot. callback1 is NOT touched
       here: the supervisor installs it on a later VBlank only when callback1 is
       exactly CB1_Overworld.
    */
    private static byte[] buildInstaller(RomProfile rom) {
        byte[] code = new byte[] {
            0x01, 0x48,                    // ldr r0, =VBlank slot
            0x02, 0x49,                    // ldr r1, =supervisor|1
            0x01, 0x60,                    // str r1,[r0]
            0x70, 0x47,                    // bx lr
            0, 0, 0, 0,                    // VBlank slot
            0, 0, 0, 0                     // supervisor|1
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
        if (rom != RomProfile.FIRE_RED_EN_10) {
            throw new IllegalArgumentException(
                    "Dispatcher Candidate 2d is currently validated only for fr10"
            );
        }
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
