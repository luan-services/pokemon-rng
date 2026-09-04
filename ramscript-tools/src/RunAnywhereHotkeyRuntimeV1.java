import java.util.List;

/*
   Experimental standalone Run Anywhere + HotkeyRuntimeV1 toggle probe.

   The frozen HotkeyRuntimeV1 implementation is not modified. This specialized
   build reuses its validated native installer blob and patches only the copy
   of the supervisor callback1 target so map-load returns visit a fixed EWRAM
   sidecar before returning to the ordinary Runtime V1 wrapper.

   Residency:
     - HotkeyRuntimeV1 remains in its ordinary fixed IWRAM blocks.
     - A 54-byte callback/state sidecar is installed inside the game-validated
       68-byte sFlickerArray tail at 02022B08..02022B4B.
     - Toggle logic is NOT resident. R+SELECT stages a 64-byte native helper in
       gStringVar4+0x140 using the existing CpuSet helper-installer mechanism,
       calls it once, then the scratch helper may be overwritten normally.

   Sidecar state:
     +0x34: armed byte (0/1)
     +0x35: captured stock allowRunning bit for the current map (0/2)

   This lets OFF immediately restore the map's original running permission,
   while the resident callback captures a fresh stock bit after each map load.
   Both Runtime V1 and the EWRAM sidecar remain session-only and vanish on reset.
*/
final class RunAnywhereHotkeyRuntimeV1 {
    static final long SIDECAR_ADDRESS = 0x02022B08L;
    static final int SIDECAR_RESERVED_SIZE = 68;
    static final int SIDECAR_CODE_SIZE = 54;
    static final int STATE_OFFSET = 0x34;
    static final int ORIGINAL_OFFSET = 0x35;
    static final long STATE_ADDRESS = SIDECAR_ADDRESS + STATE_OFFSET;
    static final long ORIGINAL_ADDRESS = SIDECAR_ADDRESS + ORIGINAL_OFFSET;
    static final long CALLBACK_THUMB = SIDECAR_ADDRESS + 1L;
    static final Hotkey HOTKEY = Hotkey.DEFAULT;

    private static final int VAR_RESULT = 0x800D;
    private static final int SE_TOGGLE_CLICK = 0x0066;
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    private RunAnywhereHotkeyRuntimeV1() {}

    static RamScript build(RomProfile rom) {
        byte[] payload = buildPayload(rom);
        byte[] nativeBlob = patchedNativeInstallerBlob(rom);
        byte[] sidecar = sidecar(rom);

        int afterPayload = HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length;
        int nativeBlobOffset = align4(afterPayload);
        int alignmentPadding = nativeBlobOffset - afterPayload;
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(SIDECAR_ADDRESS, sidecar)
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "Run Anywhere Hotkey Runtime V1 toggle requires " + total
                            + " bytes; maximum is " + RamScript.SCRIPT_SIZE
            );
        }

        byte[] script = new byte[total];
        int p = 0;
        script[p++] = (byte)0xB8; // setvaddress
        putU32(script, p, VIRTUAL_BASE);
        p += 4;
        script[p++] = (byte)0xB9; // vgoto installer
        putU32(script, p, VIRTUAL_BASE + fieldInstallerOffset);
        p += 4;
        script[p++] = (byte)(HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte)((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);

        if (p != HotkeyRuntimeV1.PAYLOAD_OFFSET) {
            throw new IllegalStateException("run-anywhere v1 payload offset mismatch");
        }

        System.arraycopy(payload, 0, script, p, payload.length);
        p += payload.length;
        for (int i = 0; i < alignmentPadding; i++) script[p++] = 0;
        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length);
        p += nativeBlob.length;
        System.arraycopy(fieldInstaller, 0, script, p, fieldInstaller.length);

        return RamScript.createWonderCard(script);
    }

    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = new NativeHelper(helperAddress, toggleHelper(rom));

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b,
                VIRTUAL_BASE,
                helper,
                copierAddress,
                "run_anywhere_toggle",
                NativeHelperInstaller.Mode.AUTO
        );
        install.installAndCall(b);

        return b
                .comparePtrToValue(STATE_ADDRESS, 0)
                .vGotoIfEqual("disabled")
                .playSe(SE_TOGGLE_CLICK)
                .waitSe()
                .end()
                .label("disabled")
                .playSe(SE_TOGGLE_CLICK)
                .waitSe()
                .end()
                .buildScript();
    }

    /*
       Fixed EWRAM callback, ARM7TDMI Thumb-1.

       callback:
         if armed:
             capture the stock allowRunning bit loaded for this map
             set allowRunning
         restore gMain.callback1 = Runtime V1 wrapper
         tail-call wrapper

       Layout:
         +00..+27  callback code
         +28       gMapHeader+0x19 literal
         +2C       gMain.callback1 literal
         +30       wrapper|1 literal
         +34       armed byte (0/1)
         +35       captured stock allowRunning bit (0/2)

       Only 54 of the validated 68 bytes are written by this build.
    */
    static byte[] sidecar(RomProfile rom) {
        byte[] out = new byte[] {
                0x0C,(byte)0xA0, 0x01,0x78, 0x00,0x29, 0x0A,(byte)0xD0,
                0x07,0x48, 0x01,0x78, 0x02,0x22, 0x0B,0x00,
                0x13,0x40, 0x08,(byte)0xA2, 0x01,0x32, 0x13,0x70,
                0x02,0x22, 0x11,0x43, 0x01,0x70, 0x03,0x48,
                0x03,0x49, 0x01,0x60, 0x08,0x47, (byte)0xC0,0x46,
                0,0,0,0, // +28 flags byte
                0,0,0,0, // +2C gMain.callback1
                0,0,0,0, // +30 wrapper|1
                0,0       // +34 state, +35 reserved mirror
        };
        if (out.length != SIDECAR_CODE_SIZE) {
            throw new IllegalStateException("sidecar code size mismatch: " + out.length);
        }
        putU32(out, 0x28, rom.mapHeader + RunAnywhereNativeHelper.ALLOW_RUNNING_OFFSET);
        putU32(out, 0x2C, 0x030030F0L);
        putU32(out, 0x30, RuntimeV1ResidentBlocks.WRAPPER | 1L);
        return out;
    }

    /*
       One-shot toggle helper staged at gStringVar4+0x140.

       ON:
         capture current stock allowRunning bit into state bit 1,
         set armed bit 0, and force allowRunning on immediately.

       OFF:
         restore allowRunning from captured bit 1 and clear the state byte.

       It uses only r0-r3 and returns normally to ScrCmd_callnative.
    */
    static byte[] toggleHelper(RomProfile rom) {
        byte[] out = new byte[] {
                0x0D,0x48, 0x01,0x78, 0x00,0x29, 0x0A,(byte)0xD1,
                0x0C,0x4A, 0x11,0x78, 0x02,0x23, 0x0B,0x40,
                0x43,0x70, 0x01,0x23, 0x03,0x70, 0x02,0x23,
                0x19,0x43, 0x11,0x70, 0x70,0x47, 0x43,0x78,
                0x00,0x21, 0x01,0x70, 0x41,0x70, 0x05,0x4A,
                0x11,0x78, 0x02,0x20, (byte)0x81,0x43, 0x03,0x40,
                0x19,0x43, 0x11,0x70, 0x70,0x47, (byte)0xC0,0x46,
                0,0,0,0, // +38 state pointer
                0,0,0,0  // +3C flags pointer
        };
        if (out.length != 64) throw new IllegalStateException("toggle helper size mismatch");
        putU32(out, 0x38, STATE_ADDRESS);
        putU32(out, 0x3C, rom.mapHeader + RunAnywhereNativeHelper.ALLOW_RUNNING_OFFSET);
        return out;
    }

    static NativeHelperInstaller.Mode selectedToggleInstallerMode(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = new NativeHelper(helperAddress, toggleHelper(rom));
        // buildPayload calls prepare immediately after setvaddress, i.e. at position 5.
        return NativeHelperInstaller.chooseMode(5, helper, copierAddress);
    }

    static byte[] patchedNativeInstallerBlob(RomProfile rom) {
        byte[] blob = HotkeyRuntimeV1.nativeInstallerBlob(rom, HOTKEY).clone();
        int residentOffset = HotkeyRuntimeV1.NATIVE_CODE_AND_LITERALS_SIZE + HotkeyRuntimeV1.TABLE_SIZE;
        int cursor = residentOffset;
        boolean patched = false;
        List<RuntimeV1ResidentBlocks.Block> blocks = RuntimeV1ResidentBlocks.build(rom, HOTKEY);
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            if (block.address() == RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS) {
                if (block.data().length != 12) throw new IllegalStateException("supervisor literal block size mismatch");
                putU32(blob, cursor + 8, CALLBACK_THUMB);
                patched = true;
                break;
            }
            cursor += block.data().length;
        }
        if (!patched) throw new IllegalStateException("supervisor literal block not found");
        return blob;
    }

    static int scriptSize(RomProfile rom) {
        byte[] payload = buildPayload(rom);
        int nativeBlobOffset = align4(HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length);
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int installerSize = new FieldScriptWriter()
                .writeBytes(SIDECAR_ADDRESS, sidecar(rom))
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build().length;
        return nativeBlobOffset + HotkeyRuntimeV1.NATIVE_BLOB_SIZE + installerSize;
    }

    private static int align4(int value) { return (value + 3) & ~3; }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
