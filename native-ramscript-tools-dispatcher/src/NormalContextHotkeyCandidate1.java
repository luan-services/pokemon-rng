/*
   Experimental phase-3 candidate.

   Purpose:
     prove that the same fixed-RAM hotkey mechanism can run as gMain.callback1
     in normal main-loop context instead of inside VBlank/IRQ.

   Expected lifetime:
     the game is allowed to overwrite callback1 during map/menu transitions.
     Candidate 1 does NOT try to reinstall itself yet. Losing the hook is an
     expected result; crashing is not.

   Runtime behavior while installed:
     hold R, then press SELECT
       -> write 0x12345678 to 0x0201C100
       -> tail-chain to the original CB1_Overworld

   This class intentionally does not modify FixedRamSeedModifier.
*/
final class NormalContextHotkeyCandidate1 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;
    private static final long DEBUG_ADDRESS = 0x0201C100L;
    private static final long DEBUG_VALUE = 0x12345678L;

    private NormalContextHotkeyCandidate1() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] mainHook = buildMainHook(rom);
        byte[] extension = buildDebugExtension(rom);
        byte[] tailStub = buildTailStub();
        byte[] originalCallback = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
        byte[] installer = buildInstaller(rom);

        if (mainHook.length != 32)
            throw new IllegalStateException("Main hook must be exactly 32 bytes");
        if (extension.length != 12)
            throw new IllegalStateException("Debug extension must be exactly 12 bytes");
        if (tailStub.length != 4)
            throw new IllegalStateException("Tail stub must be exactly 4 bytes");
        if (installer.length != 16)
            throw new IllegalStateException("Installer must be exactly 16 bytes");

        FieldScriptWriter script = new FieldScriptWriter()
                // Reuse only regions already exercised by the known-good fixed-RAM work.
                .writeBytes(rom.tailStub, tailStub)
                .writeBytes(rom.originalVBlankLiteral, originalCallback)
                .writeBytes(rom.rngExtension, extension)
                .writeBytes(rom.mainHook, mainHook)

                // Redirect callback1 last, after all runtime bytes exist.
                .writeBytes(rom.installerStaging, installer)
                .callNative(rom.installerStaging | 1L)
                .returnRam();

        return RamScript.wonderCardScript(script.build());
    }

    static long debugAddress() {
        return DEBUG_ADDRESS;
    }

    static long callback1Address() {
        return FR10_GMAIN_CALLBACK1;
    }

    static long originalCallbackThumb() {
        return FR10_CB1_OVERWORLD_THUMB;
    }

    /*
       Same compact 32-byte input detector shape as the known-good seed hook.

       r1 = &gMain.heldKeysRaw
       r2 = debug destination
       r3 = extension|1

       no trigger:
         extension|1 - 0xE0 == tailStub|1
         bx tailStub

       trigger:
         bx extension
    */
    private static byte[] buildMainHook(RomProfile rom) {
        long extensionThumb = rom.rngExtension | 1L;

        byte[] code = new byte[] {
            0x04, (byte) 0xA0,      // adr   r0, literal table
            0x0E, (byte) 0xC8,      // ldmia r0!, {r1,r2,r3}
            0x08, 0x68,             // ldr   r0,[r1]

            0x01, 0x06,             // lsls  r1,r0,#24 -> C = held R
            0x02, (byte) 0xD3,      // bcc   no_trigger

            (byte) 0x81, 0x03,      // lsls  r1,r0,#14 -> C = new SELECT
            0x00, (byte) 0xD3,      // bcc   no_trigger

            0x18, 0x47,             // bx    r3 -> debug extension

            (byte) 0xE0, 0x3B,      // no_trigger: subs r3,#0xE0
            0x18, 0x47,             // bx    r3 -> tail stub

            0, 0, 0, 0,             // &heldKeysRaw
            0, 0, 0, 0,             // debug address
            0, 0, 0, 0              // extension|1
        };

        putU32(code, 0x14, rom.heldKeysRaw);
        putU32(code, 0x18, DEBUG_ADDRESS);
        putU32(code, 0x1C, extensionThumb);

        if (extensionThumb - 0xE0 != (rom.tailStub | 1L)) {
            throw new IllegalStateException(
                    "FR10 compact fixed-RAM relationship changed"
            );
        }

        return code;
    }

    /*
       12-byte trigger extension:
         ldr r1, =0x12345678
         str r1, [r2]
         b   tailStub
         nop
         .word 0x12345678
    */
    private static byte[] buildDebugExtension(RomProfile rom) {
        if (rom.rngExtension != 0x03003F94L || rom.tailStub != 0x03003EB4L) {
            throw new IllegalStateException(
                    "Candidate 1 compact extension is currently FR10-only"
            );
        }

        byte[] code = new byte[] {
            0x01, 0x49,                   // ldr r1, debug value
            0x11, 0x60,                   // str r1,[r2]
            (byte) 0x8C, (byte) 0xE7,    // b 03003EB4
            (byte) 0xC0, 0x46,            // nop
            0, 0, 0, 0                    // 0x12345678
        };

        putU32(code, 0x08, DEBUG_VALUE);
        return code;
    }

    /*
       Tail stub. The literal at 03003EC0 contains CB1_Overworld|1 here,
       rather than VBlankIntr|1 as in the seed modifier.
    */
    private static byte[] buildTailStub() {
        return new byte[] {
            0x02, 0x4B,
            0x18, 0x47
        };
    }

    /*
       Runs once from staging RAM and atomically redirects gMain.callback1.
       The main loop has already called callback1 for the current frame by the
       time a deliveryman Field Script reaches this callnative, so the wrapper
       starts on the following frame.
    */
    private static byte[] buildInstaller(RomProfile rom) {
        byte[] code = new byte[] {
            0x01, 0x48,             // ldr r0, =gMain.callback1
            0x02, 0x49,             // ldr r1, =mainHook|1
            0x01, 0x60,             // str r1,[r0]
            0x70, 0x47,             // bx lr
            0, 0, 0, 0,             // callback1 address
            0, 0, 0, 0              // mainHook|1
        };

        putU32(code, 0x08, FR10_GMAIN_CALLBACK1);
        putU32(code, 0x0C, rom.mainHook | 1L);
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
                    "Dispatcher Candidate 1 is currently validated only for fr10"
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
