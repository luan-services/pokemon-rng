final class FixedRamSeedModifier {
    private FixedRamSeedModifier() {}

    static RamScript build(RomProfile rom, int desiredSeed) {
        validateSeed(desiredSeed);

        byte[] mainHook = buildMainHook(rom);
        byte[] extension = buildRngExtension(rom, desiredSeed);
        byte[] tailStub = buildTailStub();
        byte[] originalVBlank = littleEndian32(rom.originalVBlankThumb);
        byte[] installer = buildInstaller(rom);

        if (mainHook.length != 32) {
            throw new IllegalStateException("Main hook must be exactly 32 bytes");
        }

        if (extension.length != 12) {
            throw new IllegalStateException("RNG extension must be exactly 12 bytes");
        }

        if (tailStub.length != 4) {
            throw new IllegalStateException("Tail stub must be exactly 4 bytes");
        }

        FieldScriptWriter script = new FieldScriptWriter()
                // Runtime payload first.
                .writeBytes(rom.tailStub, tailStub)
                .writeBytes(rom.originalVBlankLiteral, originalVBlank)
                .writeBytes(rom.rngExtension, extension)
                .writeBytes(rom.mainHook, mainHook)

                // VBlank redirect last.
                .writeBytes(rom.installerStaging, installer)
                .callNative(rom.installerStaging | 1L)
                .returnRam();

        return RamScript.wonderCardScript(script.build());
    }

    static long predecessorFor(int desiredSeed) {
        validateSeed(desiredSeed);
        return RngMath.previousState(desiredSeed);
    }

    private static void validateSeed(int desiredSeed) {
        if (desiredSeed < 0 || desiredSeed > 0xFFFF) {
            throw new IllegalArgumentException("Initial seed must fit in 16 bits (0000..FFFF)");
        }
    }

    /*
       Main fixed hook, 32 bytes.

       Runtime behavior:
         - read heldKeysRaw/newKeysRaw together
         - R must be held
         - SELECT must be newly pressed
         - trigger => RNG extension
         - otherwise => original VBlank tail stub

       No SaveBlock pointer is used.
       No code is executed from RamScript after installation.
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

            0x18, 0x47,             // bx    r3 -> extension

            (byte) 0xE0, 0x3B,      // no_trigger: subs r3,#0xE0
            0x18, 0x47,             // bx    r3 -> tail stub

            0, 0, 0, 0,             // &heldKeysRaw
            0, 0, 0, 0,             // &gRngValue
            0, 0, 0, 0              // extension|1
        };

        putU32(code, 0x14, rom.heldKeysRaw);
        putU32(code, 0x18, rom.rngValue);
        putU32(code, 0x1C, extensionThumb);

        // The compact no-trigger path depends on the validated FR10 layout:
        // 0x03003F95 - 0xE0 = 0x03003EB5.
        long expectedTail = extensionThumb - 0xE0;

        if (expectedTail != (rom.tailStub | 1L)) {
            throw new IllegalStateException(
                    "ROM profile does not satisfy compact hook layout: extension|1 - 0xE0 != tail|1"
            );
        }

        return code;
    }

    /*
       12-byte fixed extension:
         ldr r1, predecessor
         str r1, [r2]
         b   tailStub
         nop
         .word predecessor
    */
    private static byte[] buildRngExtension(RomProfile rom, int desiredSeed) {
        long predecessor = RngMath.previousState(desiredSeed);

        // Branch was validated for the FR10 fixed addresses:
        // from 03003F98-ish flow back to 03003EB4.
        if (rom.rngExtension != 0x03003F94L || rom.tailStub != 0x03003EB4L) {
            throw new IllegalStateException(
                    "Current compact extension encoding is validated only for fr10"
            );
        }

        byte[] code = new byte[] {
            0x01, 0x49,                   // ldr r1, predecessor
            0x11, 0x60,                   // str r1,[r2]
            (byte) 0x8C, (byte) 0xE7,    // b 03003EB4
            (byte) 0xC0, 0x46,            // nop
            0, 0, 0, 0                    // predecessor
        };

        putU32(code, 0x08, predecessor);
        return code;
    }

    /*
       4-byte tail:
         ldr r3,[pc,#8] -> literal at 03003EC0
         bx  r3
    */
    private static byte[] buildTailStub() {
        return new byte[] {
            0x02, 0x4B,
            0x18, 0x47
        };
    }

    /*
       Installer executed once from staging RAM.
       It atomically writes the 32-bit VBlank function pointer.
    */
    private static byte[] buildInstaller(RomProfile rom) {
        byte[] code = new byte[] {
            0x01, 0x48,             // ldr r0, =vblankSlot
            0x02, 0x49,             // ldr r1, =mainHook|1
            0x01, 0x60,             // str r1,[r0]
            0x70, 0x47,             // bx lr
            0, 0, 0, 0,             // vblankSlot
            0, 0, 0, 0              // mainHook|1
        };

        putU32(code, 0x08, rom.vblankSlot);
        putU32(code, 0x0C, rom.mainHook | 1L);

        return code;
    }

    private static byte[] littleEndian32(long value) {
        byte[] result = new byte[4];
        putU32(result, 0, value);
        return result;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
