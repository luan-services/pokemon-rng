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
            if callback2 == CB2_Overworld:
                callback1 = 03003F95
            tail-chain to original VBlankIntr

     gMain.callback1 (only while normal overworld is active)
       -> 03003F95 tiny marker wrapper
            *(u8 *)0201C100 = 0x77
            tail-chain to CB1_Overworld

   During transitions/battles callback2 is not CB2_Overworld, so the supervisor
   leaves callback1 completely alone. Once callback2 becomes CB2_Overworld
   again, the next VBlank re-installs the wrapper.

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
        byte[] originalCallback = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
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
                .writeBytes(rom.originalVBlankLiteral, originalCallback)
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

    /*
       32-byte VBlank supervisor.

       Literal loading is compacted with ADR + LDMIA:

         r0 = &gMain.callback1
         r1 = CB2_Overworld|1
         r2 = tiny callback wrapper|1
         r3 = original VBlankIntr|1

       It uses callback2 as the exact safety gate. Only the normal overworld
       value 080565B5 is accepted. In every other game mode callback1 is left
       untouched.
    */
    private static byte[] buildSupervisor(RomProfile rom) {
        byte[] code = new byte[] {
            0x03, (byte) 0xA4,            // adr   r4, literals at +0x10
            0x0F, (byte) 0xCC,            // ldmia r4!, {r0,r1,r2,r3}
            0x44, 0x68,                    // ldr   r4,[r0,#4] (callback2)
            (byte) 0x8C, 0x42,            // cmp   r4,r1
            0x00, (byte) 0xD1,            // bne   tail
            0x02, 0x60,                    // str   r2,[r0] (callback1 = wrapper)
            0x18, 0x47,                    // tail: bx r3 (original VBlank)
            (byte) 0xC0, 0x46,            // nop / alignment

            0, 0, 0, 0,                    // &gMain.callback1
            0, 0, 0, 0,                    // CB2_Overworld|1
            0, 0, 0, 0,                    // callback wrapper|1
            0, 0, 0, 0                     // original VBlankIntr|1
        };

        putU32(code, 0x10, FR10_GMAIN_CALLBACK1);
        putU32(code, 0x14, FR10_CB2_OVERWORLD_THUMB);
        putU32(code, 0x18, callbackWrapperThumb(rom));
        putU32(code, 0x1C, rom.originalVBlankThumb);
        return code;
    }

    /*
       12-byte normal-context wrapper at 03003F94.

       It intentionally has no hotkey logic. Every frame in which the wrapper
       is installed it writes marker 0x77 to one byte at 0201C100, then branches
       to the callback tail stub at 03003EB4.

       Layout:
         ldr  r0, =0201C100
         movs r1, #0x77
         strb r1, [r0]
         b    03003EB4
         .word 0201C100
    */
    private static byte[] buildCallbackWrapper(RomProfile rom) {
        if (rom.rngExtension != 0x03003F94L || rom.tailStub != 0x03003EB4L) {
            throw new IllegalStateException(
                    "Candidate 2 compact callback wrapper is currently FR10-only"
            );
        }

        byte[] code = new byte[] {
            0x01, 0x48,                    // ldr r0, debug address literal
            0x77, 0x21,                    // movs r1,#0x77
            0x01, 0x70,                    // strb r1,[r0]
            (byte) 0x8A, (byte) 0xE7,      // b 03003EB4
            0, 0, 0, 0                     // 0201C100
        };

        putU32(code, 0x08, DEBUG_ADDRESS);
        return code;
    }

    /*
       03003EB4:
         ldr r3,[pc,#8] -> literal at 03003EC0
         bx  r3

       03003EC0 stores CB1_Overworld|1.
    */
    private static byte[] buildCallbackTailStub() {
        return new byte[] {
            0x02, 0x4B,
            0x18, 0x47
        };
    }

    /*
       Installer only redirects the VBlank table slot. callback1 is NOT touched
       here: the supervisor installs it on the following VBlank if callback2 is
       exactly CB2_Overworld.
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
                    "Dispatcher Candidate 2 is currently validated only for fr10"
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
