/* Embeds an arbitrary self-contained Field Script behind the validated Runtime v1 hotkey.

   This class deliberately contains no event/preset logic. Its only job is to
   install the persistent R+SELECT runtime and route the hotkey to the payload.

   The resident native layout is the validated RC4a layout. FR/LG 1.0 share
   the same binary; revision 1.1 uses the symbol-derived ROM pointers from
   RomProfile but remains runtime-untested.
*/
final class NativeRuntimeV1Composer {
    private static final long GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long ORIGINAL_VBLANK_TAIL = 0x03003EB4L;
    private static final long ORIGINAL_VBLANK_LITERAL = 0x03003EC0L;
    private static final long SUPERVISOR = 0x03003F42L;
    private static final long PRIMARY_THUNK = 0x03003F98L;
    private static final long FUNCTION_LITERAL = 0x03003F9CL;
    private static final long FIELD_SCRIPT_MARKER = 0x03003FA1L;
    private static final long SUPERVISOR_LITERALS = 0x03003FA4L;
    private static final long STAGE2 = 0x03005032L;
    private static final long STAGE1 = 0x03005082L;
    private static final long WRAPPER = 0x03005310L;
    private static final long INSTALLER_STAGING = 0x03005310L;
    private static final long FORMAT_VALIDATOR = 0x030053A8L;
    private static final long SAFETY_GATE = 0x03005434L;

    private static final int RUNTIME_FORMAT_SIGNATURE = 0x00A7;
    private static final int SIGNATURE_OFFSET = 0x0A;
    private static final int PAYLOAD_OFFSET = 0x0C;
    private static final long VIRTUAL_BASE = 0x08010000L;

    private NativeRuntimeV1Composer() {}

    static TriggerBuildResult compose(RomProfile rom, byte[] payload) {
        validateProfile(rom);
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("hotkey payload must not be empty");
        }

        byte[] installScript = buildInstallScript(rom);
        int installerOffset = PAYLOAD_OFFSET + payload.length;
        int total = installerOffset + installScript.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "Hotkey event requires " + total + " RamScript bytes (payload "
                            + payload.length + " + runtime overhead "
                            + (total - payload.length) + "); maximum is " + RamScript.SCRIPT_SIZE
            );
        }

        byte[] script = new byte[total];
        int p = 0;

        // Exact validated Runtime v1 header.
        script[p++] = (byte)0xB8; // setvaddress
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        // Keep the exact byte used by the validated runtime. This entry skips
        // metadata/payload and transfers control to the installer.
        script[p++] = (byte)0xBA;
        putU32(script, p, VIRTUAL_BASE + installerOffset);
        p += 4;

        if (p != SIGNATURE_OFFSET) throw new IllegalStateException("signature offset mismatch");
        script[p++] = (byte)(RUNTIME_FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((RUNTIME_FORMAT_SIGNATURE >>> 8) & 0xFF);
        if (p != PAYLOAD_OFFSET) throw new IllegalStateException("payload offset mismatch");

        System.arraycopy(payload, 0, script, p, payload.length);
        p += payload.length;
        System.arraycopy(installScript, 0, script, p, installScript.length);

        RamScript ramScript = RamScript.createWonderCard(script);
        return new TriggerBuildResult(
                ramScript,
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                payload.length,
                total - payload.length,
                total,
                RamScript.SCRIPT_SIZE - total
        );
    }

    static int formatSignature() { return RUNTIME_FORMAT_SIGNATURE; }
    static int payloadOffset() { return PAYLOAD_OFFSET; }

    private static byte[] buildInstallScript(RomProfile rom) {
        byte[] vblankTail = new byte[] { 0x02,0x4B, 0x18,0x47 };
        byte[] vblankLiteral = le32(rom.originalVBlankThumb);
        byte[] supervisor = buildSupervisor();
        byte[] supervisorLiterals = buildSupervisorLiterals(rom);
        byte[] stage1 = buildStage1();
        byte[] stage2 = buildStage2();
        byte[] thunk = new byte[] { 0x00,0x4C, 0x20,0x47 };
        byte[] functionLiteral = le32(rom.getSavedRamScriptThumb);
        byte[] installer = buildInstaller(rom);
        byte[] wrapper = buildWrapper(rom);
        byte[] safetyGate = buildSafetyGate();
        byte[] validator = buildFormatValidator();

        return new FieldScriptWriter()
                .writeBytes(ORIGINAL_VBLANK_TAIL, vblankTail)
                .writeBytes(ORIGINAL_VBLANK_LITERAL, vblankLiteral)
                .writeBytes(SUPERVISOR, supervisor)
                .writeBytes(SUPERVISOR_LITERALS, supervisorLiterals)
                .writeBytes(STAGE2, stage2)
                .writeBytes(STAGE1, stage1)
                .writeBytes(PRIMARY_THUNK, thunk)
                .writeBytes(FUNCTION_LITERAL, functionLiteral)
                // Preserved byte-for-byte from the validated RC4a layout.
                // This marker is not used by generic payloads, but keeping the
                // write here guarantees the first integration does not alter
                // the already runtime-tested installer sequence.
                .writeBytes(FIELD_SCRIPT_MARKER, new byte[] { 0 })
                .writeBytes(SAFETY_GATE, safetyGate)
                .writeBytes(FORMAT_VALIDATOR, validator)
                .writeBytes(GMAIN_CALLBACK1, le32(0))
                .writeBytes(INSTALLER_STAGING, installer)
                .callNative(INSTALLER_STAGING | 1L)
                .writeBytes(WRAPPER, wrapper)
                .writeBytes(GMAIN_CALLBACK1, le32(rom.cb1OverworldThumb))
                .returnRam()
                .build();
    }

    private static byte[] buildSupervisor() {
        return new byte[] {
                0x18,(byte)0xA3, 0x07,(byte)0xCB, 0x03,0x68, (byte)0x8B,0x42,
                (byte)0xB3,(byte)0xD1, 0x02,0x60, (byte)0xB1,(byte)0xE7
        };
    }

    private static byte[] buildSupervisorLiterals(RomProfile rom) {
        byte[] out = new byte[12];
        putU32(out,0,GMAIN_CALLBACK1);
        putU32(out,4,rom.cb1OverworldThumb);
        putU32(out,8,WRAPPER | 1L);
        return out;
    }

    private static byte[] buildWrapper(RomProfile rom) {
        byte[] out = new byte[32];
        byte[] code = new byte[] {
                0x03,0x48, 0x00,0x68, 0x01,0x06, 0x05,(byte)0xD3,
                (byte)0x81,0x03, 0x03,(byte)0xD3, 0x03,0x4A, (byte)0x89,(byte)0xE0
        };
        System.arraycopy(code,0,out,0,code.length);
        putU32(out,0x10,rom.heldKeysRaw);
        out[0x14]=0x00; out[0x15]=0x4B; out[0x16]=0x18; out[0x17]=0x47;
        putU32(out,0x18,rom.cb1OverworldThumb);
        putU32(out,0x1C,rom.lockFieldControls);
        return out;
    }

    private static byte[] buildSafetyGate() {
        return new byte[] {
                0x10,0x78, 0x00,0x28, 0x00,(byte)0xD0, 0x73,(byte)0xE7,
                0x21,(byte)0xE6, 0x10,(byte)0xBD
        };
    }

    private static byte[] buildStage1() {
        return new byte[] {
                0x10,(byte)0xB5, (byte)0xFE,(byte)0xF7, (byte)0x88,(byte)0xFF,
                0x00,0x28, 0x00,(byte)0xD0, (byte)0x8C,(byte)0xE1, 0x10,(byte)0xBD
        };
    }

    private static byte[] buildStage2() {
        return new byte[] {
                0x0C,0x30, (byte)0xD9,0x21, (byte)0x89,0x00, 0x64,0x1A,
                (byte)0xFE,(byte)0xF7, (byte)0xAE,(byte)0xFF, 0x26,(byte)0xE0
        };
    }

    private static byte[] buildFormatValidator() {
        return new byte[] {
                0x41,(byte)0x89, (byte)0xA7,0x29, 0x47,(byte)0xD1, 0x40,(byte)0xE6
        };
    }

    private static byte[] buildInstaller(RomProfile rom) {
        byte[] code = new byte[] {
                0x01,0x48, 0x02,0x49, 0x01,0x60, 0x70,0x47,
                0,0,0,0, 0,0,0,0
        };
        putU32(code,0x08,rom.vblankSlot);
        putU32(code,0x0C,SUPERVISOR | 1L);
        return code;
    }

    private static void validateProfile(RomProfile rom) {
        if (rom.vblankSlot != 0x03003550L
                || rom.heldKeysRaw != 0x03003118L
                || rom.lockFieldControls != 0x03000F9CL) {
            throw new IllegalArgumentException("Unexpected runtime RAM layout for " + rom.id());
        }
        if (rom.getSavedRamScriptThumb - rom.scriptContextSetupThumb != 0x364L) {
            throw new IllegalArgumentException("Unsupported runtime function delta for " + rom.id());
        }
    }

    private static byte[] le32(long value) {
        byte[] out = new byte[4]; putU32(out,0,value); return out;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset]=(byte)value;
        data[offset+1]=(byte)(value>>>8);
        data[offset+2]=(byte)(value>>>16);
        data[offset+3]=(byte)(value>>>24);
    }
}
