/*
   Native RamScript Runtime v1aa (FireRed English 1.0)

   This is the first cleanup pass after Candidate 6.

   Functional goal remains intentionally unchanged:
       R + SELECT
         -> GetSavedRamScriptIfValid()
         -> ScriptContext_SetupScript(script + 0x0A)
         -> Field Script writes 0x66 to 03003FA1

   Cleanup goals:
     1. do NOT persist code in gLinkTestBGInfo (03003F70..03003F7F);
     2. do NOT use gStringVar4 as installer staging;
     3. do NOT touch the live 03003F80..03003F93 link globals;
     4. preserve an atomic 32-bit VBlank hook installation.

   Key installation trick
   ----------------------

   The real unused 32-byte block 03005310..0300532F is reused in two phases.

   During installation:
       03005310..0300531F = temporary 16-byte installer

   Before calling that installer, callback1 is temporarily changed to 0.
   The installer atomically sets the VBlank callback to the persistent
   supervisor.

   While callback1 is 0, the supervisor's re-arm condition cannot match
   CB1_Overworld, so it cannot install a half-written wrapper.

   After the installer returns:
       03005310..0300532F = final 32-byte callback wrapper

   Then CB1_Overworld is restored byte-by-byte. On a later VBlank, after the
   wrapper is already complete, the supervisor atomically changes callback1 to
   03005311.

   Persistent FR10 layout
   ----------------------

     03003EB4..03003EB7  original-VBlank tail stub      (gLinkFiller1)
     03003EC0..03003EC3  original VBlank Thumb pointer (gLinkFiller2)

     03003F42..03003F4F  VBlank supervisor code        (linker padding)
     03003F98..03003F9B  primary ROM-call thunk        (gLinkFiller4)
     03003F9C..03003F9F  GetSavedRamScriptIfValid|1    (gLinkFiller5)

     03003FA1             field-script test marker      (linker padding)
     03003FA4..03003FAF  supervisor literals           (linker padding)

     03005032..0300503F  native stage 2                (linker padding)
     03005082..0300508F  native stage 1                (linker padding)

     03005310..0300532F  final callback wrapper        (UnusedVarNeededToMatch)

   Explicitly untouched:
     03003F70..03003F7F  gLinkTestBGInfo
     03003F80..03003F93  live link globals
     02021D18..          gStringVar4

   Link/wireless behavior is NOT yet validated even though this RC no longer
   overwrites the live link globals that broke Candidate 5.
*/
final class NativeRamScriptRuntimeV1 {
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

    // FR10 script.c static: bool8 sLockFieldControls

    // 12-byte alignment padding after sIsInSaveFailedScreen and before
    // gHostRfuGameData. v1 uses 10 bytes for the hotkey safety gate.
    private static final long SAFETY_GATE = 0x03005434L;

    // Two-byte runtime format signature stored at script+0x0A.
    // 0x00A7 fits an immediate CMP after a halfword load.
    private static final int RUNTIME_FORMAT_SIGNATURE = 0x00A7;
    private static final int SIGNATURE_OFFSET = 0x0A;
    private static final int PAYLOAD_OFFSET = 0x0C;

    // Exact 8-byte linker padding after gGameContinueCallback and before
    // gRamSaveSectorLocations.
    private static final long FORMAT_VALIDATOR = 0x030053A8L;
    private static final long VIRTUAL_BASE = 0x08010000L;

    private NativeRamScriptRuntimeV1() {}

    static RamScript build(RomProfile rom) {
        validateProfile(rom);

        byte[] vblankTail = buildOriginalVBlankTail();
        byte[] vblankLiteral = littleEndian32(rom.originalVBlankThumb);

        byte[] supervisor = buildSupervisor(rom);
        byte[] supervisorLiterals = buildSupervisorLiterals(rom);

        byte[] stage1 = buildStage1();
        byte[] stage2 = buildStage2(rom);
        byte[] thunk = buildPrimaryThunk();
        byte[] functionLiteral = littleEndian32(rom.getSavedRamScriptThumb);

        byte[] installer = buildInstaller(rom);
        byte[] finalWrapper = buildFinalWrapper(rom);
        byte[] safetyGate = buildSafetyGate();
        byte[] formatValidator = buildFormatValidator();

        byte[] zeroCallback = littleEndian32(0);
        byte[] originalCallback = littleEndian32(rom.cb1OverworldThumb);
        byte[] zeroMarker = new byte[] { 0 };

        FieldScriptWriter installScript = new FieldScriptWriter()
                // persistent supervisor and native bridge
                .writeBytes(ORIGINAL_VBLANK_TAIL, vblankTail)
                .writeBytes(ORIGINAL_VBLANK_LITERAL, vblankLiteral)
                .writeBytes(SUPERVISOR, supervisor)
                .writeBytes(SUPERVISOR_LITERALS, supervisorLiterals)
                .writeBytes(STAGE2, stage2)
                .writeBytes(STAGE1, stage1)
                .writeBytes(PRIMARY_THUNK, thunk)
                .writeBytes(FUNCTION_LITERAL, functionLiteral)
                .writeBytes(FIELD_SCRIPT_MARKER, zeroMarker)
                .writeBytes(SAFETY_GATE, safetyGate)
                .writeBytes(FORMAT_VALIDATOR, formatValidator)

                // installation guard:
                // while callback1 != CB1_Overworld the VBlank supervisor will
                // refuse to arm our final callback wrapper.
                .writeBytes(GMAIN_CALLBACK1, zeroCallback)

                // temporary installer lives in the same truly-unused block that
                // will become the final wrapper immediately afterwards.
                .writeBytes(INSTALLER_STAGING, installer)
                .callNative(INSTALLER_STAGING | 1L)

                // VBlank is now hooked, but callback1 is still 0, so the
                // supervisor cannot install a partially-written wrapper.
                .writeBytes(WRAPPER, finalWrapper)

                // Once CB1_Overworld is fully restored, a later VBlank may
                // safely replace it with WRAPPER|1.
                .writeBytes(GMAIN_CALLBACK1, originalCallback)

                .returnRam();

        byte[] payload = buildFieldScriptPayload();

        int installerOffset = PAYLOAD_OFFSET + payload.length;
        byte[] installerScript = installScript.build();
        byte[] script = new byte[installerOffset + installerScript.length];

        int p = 0;

        // setvaddress 0x08010000
        script[p++] = (byte)0xB8;
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        // deliveryman entry skips metadata + hotkey payload and enters installer
        script[p++] = (byte)0xBA; // vgoto
        putU32(script, p, VIRTUAL_BASE + installerOffset);
        p += 4;

        if (p != SIGNATURE_OFFSET)
            throw new IllegalStateException("Runtime v1aa signature offset mismatch");

        // Metadata bytes are skipped by the deliveryman vgoto.
        script[p++] = (byte)(RUNTIME_FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((RUNTIME_FORMAT_SIGNATURE >>> 8) & 0xFF);

        if (p != PAYLOAD_OFFSET)
            throw new IllegalStateException("Runtime v1aa payload offset mismatch");

        System.arraycopy(payload, 0, script, p, payload.length);
        p += payload.length;

        if (p != installerOffset)
            throw new IllegalStateException("Runtime v1aa installer offset mismatch");

        System.arraycopy(installerScript, 0, script, p, installerScript.length);

        return RamScript.wonderCardScript(script);
    }

    static long supervisorAddress() { return SUPERVISOR; }
    static long supervisorThumb() { return SUPERVISOR | 1L; }
    static long wrapperAddress() { return WRAPPER; }
    static long wrapperThumb() { return WRAPPER | 1L; }
    static long safetyGateAddress() { return SAFETY_GATE; }
    static long formatValidatorAddress() { return FORMAT_VALIDATOR; }
    static int runtimeFormatSignature() { return RUNTIME_FORMAT_SIGNATURE; }
    static int signatureOffset() { return SIGNATURE_OFFSET; }
    static long lockFieldControlsAddress(RomProfile rom) { return rom.lockFieldControls; }
    static long stage1Address() { return STAGE1; }
    static long stage2Address() { return STAGE2; }
    static long markerAddress() { return FIELD_SCRIPT_MARKER; }
    static long getSavedThumb(RomProfile rom) { return rom.getSavedRamScriptThumb; }
    static long setupScriptThumb(RomProfile rom) { return rom.scriptContextSetupThumb; }
    static int payloadOffset() { return PAYLOAD_OFFSET; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] supervisorLiteralBytesForTest(RomProfile rom) { return buildSupervisorLiterals(rom); }
    static byte[] wrapperBytesForTest(RomProfile rom) { return buildFinalWrapper(rom); }
    static byte[] safetyGateBytesForTest() { return buildSafetyGate(); }
    static byte[] formatValidatorBytesForTest() { return buildFormatValidator(); }
    static byte[] stage1BytesForTest() { return buildStage1(); }
    static byte[] stage2BytesForTest(RomProfile rom) { return buildStage2(rom); }
    static byte[] thunkBytesForTest() { return buildPrimaryThunk(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }
    static byte[] payloadBytesForTest() { return buildFieldScriptPayload(); }

    /*
       03003F42..03003F4F (14 bytes)

       Literals at 03003FA4:
         r0 = &gMain.callback1
         r1 = CB1_Overworld|1
         r2 = final wrapper|1

       If callback1 is exactly CB1_Overworld, replace it with the wrapper.
       Both paths branch to the known original-VBlank tail at 03003EB4.
    */
    private static byte[] buildSupervisor(RomProfile rom) {
        return new byte[] {
            0x18, (byte)0xA3,              // adr r3,03003FA4
            0x07, (byte)0xCB,              // ldmia r3!,{r0,r1,r2}
            0x03, 0x68,                    // ldr r3,[r0]
            (byte)0x8B, 0x42,              // cmp r3,r1
            (byte)0xB3, (byte)0xD1,        // bne 03003EB4
            0x02, 0x60,                    // str r2,[r0]
            (byte)0xB1, (byte)0xE7         // b 03003EB4
        };
    }

    private static byte[] buildSupervisorLiterals(RomProfile rom) {
        byte[] out = new byte[12];
        putU32(out, 0, GMAIN_CALLBACK1);
        putU32(out, 4, rom.cb1OverworldThumb);
        putU32(out, 8, wrapperThumb());
        return out;
    }

    /*
       03005310..0300532F, exactly 32 bytes.

       03005310  ldr r0,[pc,#0x0C] -> heldKeysRaw pointer at 03005320
       03005312  ldr r0,[r0]
       03005314  lsls r1,r0,#24
       03005316  bcc local CB1 tail
       03005318  lsls r1,r0,#14
       0300531A  bcc local CB1 tail
       0300531C  b 03005082
       0300531E  nop

       03005320  heldKeysRaw pointer
       03005324  ldr r3,[pc,#0]
       03005326  bx r3
       03005328  CB1_Overworld|1
       0300532C  reserved zero
    */
    private static byte[] buildFinalWrapper(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Runtime v1aa assumes FR10 heldKeysRaw at 03003118");

        byte[] out = new byte[32];

        /*
           03005310..0300532F

           03005310  ldr r0,[pc,#0x0C] -> heldKeysRaw pointer at 03005320
           03005312  ldr r0,[r0]
           03005314  lsls r1,r0,#24     ; SELECT
           03005316  bcc 03005324
           03005318  lsls r1,r0,#14     ; R
           0300531A  bcc 03005324
           0300531C  ldr r2,[pc,#0x0C] -> &sLockFieldControls at 0300532C
           0300531E  b   03005434       ; safety gate

           03005320  heldKeysRaw pointer
           03005324  ldr r3,[pc,#0]
           03005326  bx r3
           03005328  CB1_Overworld|1
           0300532C  &sLockFieldControls
        */
        byte[] code = new byte[] {
            0x03, 0x48,                    // ldr r0,[pc,#12] -> 03005320
            0x00, 0x68,                    // ldr r0,[r0]
            0x01, 0x06,                    // lsls r1,r0,#24
            0x05, (byte)0xD3,              // bcc 03005324
            (byte)0x81, 0x03,              // lsls r1,r0,#14
            0x03, (byte)0xD3,              // bcc 03005324
            0x03, 0x4A,                    // ldr r2,[pc,#12] -> 0300532C
            (byte)0x89, (byte)0xE0         // b 03005434
        };
        System.arraycopy(code, 0, out, 0, code.length);

        putU32(out, 0x10, rom.heldKeysRaw);

        out[0x14] = 0x00;                  // ldr r3,[pc,#0] -> 03005328
        out[0x15] = 0x4B;
        out[0x16] = 0x18;                  // bx r3
        out[0x17] = 0x47;
        putU32(out, 0x18, rom.cb1OverworldThumb);
        putU32(out, 0x1C, rom.lockFieldControls);

        return out;
    }

    /*
       Safety gate: 03005434..0300543D (10 bytes), leaving 0300543E..3F unused.

       The gate runs only after the R+SELECT chord has been detected.

       sLockFieldControls != 0:
           a field script/dialogue/event still owns player controls
           -> ignore hotkey and tail-call CB1_Overworld.

       sLockFieldControls == 0:
           overworld is free
           -> enter the already validated RC2 bridge at 03005082.

       r2 arrives preloaded with &sLockFieldControls by the wrapper.
    */
    private static byte[] buildSafetyGate() {
        /*
           03005434..0300543F, full 12-byte padding.

           Existing RC3 logic:
             5434 ldrb r0,[r2]
             5436 cmp  r0,#0
             5438 beq  543C
             543A b    5324
             543C b    5082

           v1 uses the final two bytes as a local "reject tail" for the
           format validator:
             543E pop {r4,pc}

           Stage1 pushed {r4,lr}, so the validator can reject safely by
           branching here instead of attempting an out-of-range conditional
           branch all the way back to 0300508E.
        */
        return new byte[] {
            0x10, 0x78,                    // 5434 ldrb r0,[r2]
            0x00, 0x28,                    // 5436 cmp r0,#0
            0x00, (byte)0xD0,              // 5438 beq 543C
            0x73, (byte)0xE7,              // 543A b 5324 (locked)
            0x21, (byte)0xE6,              // 543C b 5082 (unlocked)
            0x10, (byte)0xBD               // 543E pop {r4,pc} (format reject)
        };
    }

    /*
       Stage 1: 03005082..0300508F
    */
    private static byte[] buildStage1() {
        return new byte[] {
            0x10, (byte)0xB5,              // push {r4,lr}
            (byte)0xFE, (byte)0xF7,        // BL 03003F98 first half
            (byte)0x88, (byte)0xFF,        // BL second half
            0x00, 0x28,                    // cmp r0,#0
            0x00, (byte)0xD0,              // beq 0300508E
            (byte)0x8C, (byte)0xE1,        // b 030053A8 format validator
            0x10, (byte)0xBD               // pop {r4,pc}
        };
    }

    /*
       Stage 2: 03005032..0300503F
    */
    private static byte[] buildStage2(RomProfile rom) {
        return new byte[] {
            0x0C, 0x30,                    // adds r0,#0x0C
            (byte)0xD9, 0x21,              // movs r1,#0xD9
            (byte)0x89, 0x00,              // lsls r1,r1,#2 = 0x364
            0x64, 0x1A,                    // subs r4,r4,r1
            (byte)0xFE, (byte)0xF7,        // BL 03003F9A first half
            (byte)0xAE, (byte)0xFF,        // BL second half
            0x26, (byte)0xE0               // b 0300508E
        };
    }

    /*
       gLinkFiller4 / gLinkFiller5.
       Search of link.c shows these as filler declarations and no functional use.
    */
    private static byte[] buildPrimaryThunk() {
        return new byte[] {
            0x00, 0x4C,                    // ldr r4,[pc,#0] -> 03003F9C
            0x20, 0x47                     // bx r4
        };
    }

    private static byte[] buildOriginalVBlankTail() {
        return new byte[] {
            0x02, 0x4B,                    // ldr r3,[pc,#8] -> 03003EC0
            0x18, 0x47                     // bx r3
        };
    }

    /*
       Temporary installer placed at 03005310 before the final wrapper replaces it.

       It performs the one write that must remain atomic:
           gMain.vblankCallback = supervisor|1
    */
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
        putU32(code, 0x0C, supervisorThumb());
        return code;
    }

    /*
       v1 visual Field Script payload.

       The hotkey enters at script+0x0A, so the payload establishes its own
       virtual-address base instead of depending on the deliveryman entry.

       Layout relative to payload start:

         +00  B8 00010008       setvaddress 0x08010000
         +05  69                lockall
         +06  BD 0F010008       vmessage 0x0801000F
         +0B  66                waitmessage
         +0C  6D                waitbuttonpress
         +0D  6B                releaseall
         +0E  02                end
         +0F  text              "Hello from the Wonder Card!"
    */
    /*
       Runtime-format validator: 030053A8..030053AF, exactly 8 bytes.

       Stage1 has already verified r0 != NULL.

         ldrh r1,[r0,#10]   ; script+0x0A
         cmp  r1,#0xA7      ; expects bytes A7 00
         bne  stage1_done
         b    stage2

       An old resident runtime therefore becomes inert if the currently saved
       valid RamScript does not explicitly use our v1 runtime format.
    */
    private static byte[] buildFormatValidator() {
        /*
           030053A8..030053AF, exactly 8 bytes.

           53A8  ldrh r1,[r0,#0x0A]
           53AA  cmp  r1,#0xA7
           53AC  bne  0300543E
           53AE  b    03005032

           Important fixes over rejected RC4:
             - 0x8941 is the correct Thumb encoding for
               ldrh r1,[r0,#10].
             - conditional Thumb branches only have an 8-bit signed offset.
               The rejected RC4 tried to branch directly from 53AC to 508E,
               which is far outside that range.
             - invalid format now branches to the nearby 543E pop tail.
        */
        return new byte[] {
            0x41, (byte)0x89,              // ldrh r1,[r0,#10]
            (byte)0xA7, 0x29,              // cmp r1,#0xA7
            0x47, (byte)0xD1,              // bne 0300543E
            0x40, (byte)0xE6               // b 03005032
        };
    }

    private static byte[] buildFieldScriptPayload() {
        byte[] text = encodeHelloMessage();
        byte[] payload = new byte[0x0F + text.length];

        int p = 0;
        payload[p++] = (byte)0xB8;          // setvaddress
        putU32(payload, p, VIRTUAL_BASE);
        p += 4;

        payload[p++] = 0x69;                // lockall

        payload[p++] = (byte)0xBD;          // vmessage
        putU32(payload, p, VIRTUAL_BASE + 0x0F);
        p += 4;

        payload[p++] = 0x66;                // waitmessage
        payload[p++] = 0x6D;                // waitbuttonpress
        payload[p++] = 0x6B;                // releaseall
        payload[p++] = 0x02;                // end

        if (p != 0x0F)
            throw new IllegalStateException("Runtime v1aa text offset mismatch");

        System.arraycopy(text, 0, payload, p, text.length);
        return payload;
    }

    private static byte[] encodeHelloMessage() {
        // Pokémon Gen III English character encoding.
        return new byte[] {
            (byte)0xC2,                         // H
            (byte)0xD9,                         // e
            (byte)0xE0,                         // l
            (byte)0xE0,                         // l
            (byte)0xE3,                         // o
            0x00,                               // space
            (byte)0xDA,                         // f
            (byte)0xE6,                         // r
            (byte)0xE3,                         // o
            (byte)0xE1,                         // m
            0x00,                               // space
            (byte)0xE8,                         // t
            (byte)0xDC,                         // h
            (byte)0xD9,                         // e
            0x00,                               // space
            (byte)0xD1,                         // W
            (byte)0xE3,                         // o
            (byte)0xE2,                         // n
            (byte)0xD8,                         // d
            (byte)0xD9,                         // e
            (byte)0xE6,                         // r
            0x00,                               // space
            (byte)0xBD,                         // C
            (byte)0xD5,                         // a
            (byte)0xE6,                         // r
            (byte)0xD8,                         // d
            (byte)0xAB,                         // !
            (byte)0xFF                          // EOS
        };
    }

    private static byte[] littleEndian32(long value) {
        byte[] out = new byte[4];
        putU32(out, 0, value);
        return out;
    }

    private static void validateProfile(RomProfile rom) {
        // All four supplied symbol maps agree on this runtime IWRAM layout.
        if (rom.vblankSlot != 0x03003550L
                || rom.heldKeysRaw != 0x03003118L
                || rom.lockFieldControls != 0x03000F9CL) {
            throw new IllegalArgumentException(
                    "Runtime v1 profile has an unexpected RAM layout: " + rom.id()
            );
        }

        // The compact stage2 derives ScriptContext_SetupScript from
        // GetSavedRamScriptIfValid by subtracting 0x364. The supplied symbols
        // confirm this for FR/LG 1.0 and 1.1.
        long delta = rom.getSavedRamScriptThumb - rom.scriptContextSetupThumb;
        if (delta != 0x364L) {
            throw new IllegalArgumentException(
                    "Runtime v1 requires GetSavedRamScriptIfValid - "
                            + "ScriptContext_SetupScript == 0x364; got 0x"
                            + Long.toHexString(delta)
            );
        }
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
