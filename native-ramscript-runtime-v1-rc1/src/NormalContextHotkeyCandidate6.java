/*
   Experimental Candidate 6.

   Goal:
     prove the full bridge:

       R + SELECT
         -> GetSavedRamScriptIfValid()
         -> current relocatable RamScript pointer
         -> ScriptContext_SetupScript(payload)
         -> Field Script engine executes payload on the following overworld tick

   This candidate deliberately uses a tiny Field Script payload:
       setptr 0x66, 0x03003FA1
       end

   If the marker becomes 0x66, the write was performed by the FIELD SCRIPT
   ENGINE, not by native dispatcher code.

   RamScript layout:

       +0x000  setvaddress 0x08010000
       +0x005  vgoto installer
       +0x00A  hotkey payload:
                  setptr 0x66, 0x03003FA1
                  end
       +0x011  installer script

   Therefore GetSavedRamScriptIfValid() returns script+0, while the dispatcher
   passes script+0x0A to ScriptContext_SetupScript().

   Native FR10 layout:

       03003F42..03003F4F  stage 2 in linker padding
       03003F50             gSendCmd starts -- UNTOUCHED

       03003F70..03003F7F  hotkey detector

       03003F80..03003F93  UNTOUCHED

       03003F94..03003F97  heldKeysRaw pointer
       03003F98..03003F9B  primary thunk:
                              ldr r4, GetSaved literal
                              bx r4
       03003F9C..03003F9F  GetSavedRamScriptIfValid|1

       03003FA0             gLastSendQueueCount -- UNTOUCHED
       03003FA1             FIELD SCRIPT marker
       03003FA2..03003FAF  stage 1 wrapper
       03003FB0             gLink starts -- UNTOUCHED

   Why r4:
     the thunk loads GetSavedRamScriptIfValid|1 into r4. r4 is callee-saved,
     so after GetSavedRamScriptIfValid returns, r4 still contains 08069E49.
     ScriptContext_SetupScript|1 is exactly 0x364 bytes lower:

         08069E49 - 0x364 = 08069AE5

     Stage 2 derives that address using:
         movs r1,#0xD9
         lsls r1,#2       ; 0xD9 * 4 = 0x364
         subs r4,r4,r1

   Stage 1:
       push {r4,lr}
       bl primary_thunk
       cmp r0,#0
       beq done
       b stage2
     done:
       pop {r4,pc}

   Stage 2:
       adds r0,#0x0A      ; RamScript payload offset
       movs r1,#0xD9
       lsls r1,#2
       subs r4,r4,r1      ; r4 = ScriptContext_SetupScript|1
       bl bx_r4            ; bx_r4 is the second instruction of primary thunk
       b done

   ScriptContext_SetupScript returns to stage2, then stage2 branches back to the
   stage1 pop, restoring r4 and the callback's original return address.
*/
final class NormalContextHotkeyCandidate6 {
    private static final long FR10_GMAIN_CALLBACK1 = 0x030030F0L;
    private static final long FR10_CB1_OVERWORLD_THUMB = 0x08056535L;

    private static final long FR10_GET_SAVED_RAM_SCRIPT_THUMB = 0x08069E49L;
    private static final long FR10_SCRIPT_CONTEXT_SETUP_THUMB = 0x08069AE5L;

    private static final long HOTKEY_DETECTOR = 0x03003F70L;
    private static final long STAGE2 = 0x03003F42L;
    private static final long HELD_KEYS_LITERAL = 0x03003F94L;
    private static final long PRIMARY_THUNK = 0x03003F98L;
    private static final long FUNCTION_LITERAL = 0x03003F9CL;
    private static final long FIELD_SCRIPT_MARKER = 0x03003FA1L;
    private static final long STAGE1 = 0x03003FA2L;

    private static final int PAYLOAD_OFFSET = 0x0A;
    private static final long VIRTUAL_BASE = 0x08010000L;

    private NormalContextHotkeyCandidate6() {}

    static RamScript build(RomProfile rom) {
        requireFr10(rom);

        byte[] supervisor = buildSupervisor(rom);
        byte[] detector = buildHotkeyDetector(rom);
        byte[] stage1 = buildStage1();
        byte[] stage2 = buildStage2();
        byte[] thunk = buildPrimaryThunk();

        byte[] heldKeysLiteral = littleEndian32(rom.heldKeysRaw);
        byte[] functionLiteral = littleEndian32(FR10_GET_SAVED_RAM_SCRIPT_THUMB);
        byte[] zeroMarker = new byte[] { 0 };

        byte[] callbackTail = buildCallbackTailStub();
        byte[] callbackLiteral = littleEndian32(FR10_CB1_OVERWORLD_THUMB);
        byte[] installer = buildInstaller(rom);

        byte[] installerScript = new FieldScriptWriter()
                .writeBytes(rom.tailStub, callbackTail)
                .writeBytes(rom.originalVBlankLiteral, callbackLiteral)

                .writeBytes(STAGE2, stage2)

                .writeBytes(HOTKEY_DETECTOR, detector)
                .writeBytes(HELD_KEYS_LITERAL, heldKeysLiteral)
                .writeBytes(PRIMARY_THUNK, thunk)
                .writeBytes(FUNCTION_LITERAL, functionLiteral)
                .writeBytes(FIELD_SCRIPT_MARKER, zeroMarker)
                .writeBytes(STAGE1, stage1)

                .writeBytes(rom.mainHook, supervisor)

                .writeBytes(rom.installerStaging, installer)
                .callNative(rom.installerStaging | 1L)
                .returnRam()
                .build();

        byte[] payload = buildFieldScriptPayload();

        // Header is fixed at 10 bytes: setvaddress + vgoto.
        int installerOffset = PAYLOAD_OFFSET + payload.length;

        byte[] script = new byte[installerOffset + installerScript.length];
        int p = 0;

        // setvaddress 0x08010000
        script[p++] = (byte)0xB8;
        putU32(script, p, VIRTUAL_BASE);
        p += 4;

        // vgoto VIRTUAL_BASE + installerOffset
        script[p++] = (byte)0xBA;
        putU32(script, p, VIRTUAL_BASE + installerOffset);
        p += 4;

        if (p != PAYLOAD_OFFSET)
            throw new IllegalStateException("Candidate 6 payload offset mismatch");

        System.arraycopy(payload, 0, script, p, payload.length);
        p += payload.length;

        if (p != installerOffset)
            throw new IllegalStateException("Candidate 6 installer offset mismatch");

        System.arraycopy(installerScript, 0, script, p, installerScript.length);

        return RamScript.wonderCardScript(script);
    }

    static long getSavedRamScriptThumb() { return FR10_GET_SAVED_RAM_SCRIPT_THUMB; }
    static long scriptContextSetupThumb() { return FR10_SCRIPT_CONTEXT_SETUP_THUMB; }
    static long stage1Address() { return STAGE1; }
    static long stage2Address() { return STAGE2; }
    static long primaryThunkAddress() { return PRIMARY_THUNK; }
    static long fieldScriptMarkerAddress() { return FIELD_SCRIPT_MARKER; }
    static int payloadOffset() { return PAYLOAD_OFFSET; }
    static long hotkeyDetectorThumb() { return HOTKEY_DETECTOR | 1L; }
    static long supervisorThumb(RomProfile rom) { return rom.mainHook | 1L; }

    static byte[] supervisorBytesForTest(RomProfile rom) { return buildSupervisor(rom); }
    static byte[] detectorBytesForTest(RomProfile rom) { return buildHotkeyDetector(rom); }
    static byte[] stage1BytesForTest() { return buildStage1(); }
    static byte[] stage2BytesForTest() { return buildStage2(); }
    static byte[] thunkBytesForTest() { return buildPrimaryThunk(); }
    static byte[] payloadBytesForTest() { return buildFieldScriptPayload(); }
    static byte[] installerBytesForTest(RomProfile rom) { return buildInstaller(rom); }

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

    private static byte[] buildHotkeyDetector(RomProfile rom) {
        if (rom.heldKeysRaw != 0x03003118L)
            throw new IllegalStateException("Candidate 6 assumes FR10 heldKeysRaw at 03003118");

        return new byte[] {
            0x08, 0x48,                    // ldr r0,[pc,#0x20] -> 03003F94
            0x00, 0x68,                    // ldr r0,[r0]
            0x01, 0x06,                    // lsls r1,r0,#24
            0x02, (byte)0xD3,              // bcc no-trigger
            (byte)0x81, 0x03,              // lsls r1,r0,#14
            0x00, (byte)0xD3,              // bcc no-trigger
            0x11, (byte)0xE0,              // b 03003FA2
            (byte)0x99, (byte)0xE7         // b 03003EB4
        };
    }

    /*
       03003F98:
         ldr r4,[pc,#0] -> 03003F9C
       03003F9A:
         bx r4

       First BL targets 03003F98.
       Second BL targets 03003F9A after r4 has been derived to SetupScript.
    */
    private static byte[] buildPrimaryThunk() {
        return new byte[] {
            0x00, 0x4C,                    // ldr r4,[pc,#0]
            0x20, 0x47                     // bx r4
        };
    }

    /*
       03003FA2..03003FAF
    */
    private static byte[] buildStage1() {
        return new byte[] {
            0x10, (byte)0xB5,              // push {r4,lr}
            (byte)0xFF, (byte)0xF7,        // BL 03003F98 first half
            (byte)0xF8, (byte)0xFF,        // BL second half
            0x00, 0x28,                    // cmp r0,#0
            0x01, (byte)0xD0,              // beq 03003FAE
            (byte)0xC9, (byte)0xE7,        // b 03003F42
            0x10, (byte)0xBD               // pop {r4,pc}
        };
    }

    /*
       03003F42..03003F4F -- linker padding before gSendCmd at 03003F50.
    */
    private static byte[] buildStage2() {
        return new byte[] {
            0x0A, 0x30,                    // adds r0,#0x0A
            (byte)0xD9, 0x21,              // movs r1,#0xD9
            (byte)0x89, 0x00,              // lsls r1,r1,#2
            0x64, 0x1A,                    // subs r4,r4,r1
            0x00, (byte)0xF0,              // BL 03003F9A first half
            0x26, (byte)0xF8,              // BL second half
            0x2E, (byte)0xE0               // b 03003FAE
        };
    }

    /*
       A real field script, not native code:
         setptr 0x66, 03003FA1
         end
    */
    private static byte[] buildFieldScriptPayload() {
        byte[] payload = new byte[7];
        payload[0] = 0x11;                 // setptr
        payload[1] = 0x66;
        putU32(payload, 2, FIELD_SCRIPT_MARKER);
        payload[6] = 0x02;                 // end
        return payload;
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
            throw new IllegalArgumentException("Dispatcher Candidate 6 is currently FR10-only");
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset+1] = (byte)(value >>> 8);
        data[offset+2] = (byte)(value >>> 16);
        data[offset+3] = (byte)(value >>> 24);
    }
}
