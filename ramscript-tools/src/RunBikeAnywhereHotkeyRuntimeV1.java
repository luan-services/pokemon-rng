import java.util.List;

/* Run + Bike Anywhere sidecar for HotkeyRuntimeV1.

   The frozen Runtime V1 is not modified. A 63-byte callback/state sidecar is
   copied into the already game-validated 68-byte EWRAM tail at 02022B08.
   Toggle logic is temporary: a 76-byte Thumb helper is staged in gStringVar4
   and called only when the hotkey is pressed.

   State at the tail of the resident sidecar:
     +0x3C armed (0/1)
     +0x3D stock allowRunning bit (0/2)
     +0x3E stock bikingAllowed byte (normally 0/1)
*/
final class RunBikeAnywhereHotkeyRuntimeV1 {
    static final long SIDECAR_ADDRESS = RunAnywhereHotkeyRuntimeV1.SIDECAR_ADDRESS;
    static final int SIDECAR_RESERVED_SIZE = RunAnywhereHotkeyRuntimeV1.SIDECAR_RESERVED_SIZE;
    static final int SIDECAR_CODE_SIZE = 63;
    static final int STATE_OFFSET = 0x3C;
    static final int ORIGINAL_RUNNING_OFFSET = 0x3D;
    static final int ORIGINAL_BIKE_OFFSET = 0x3E;
    static final long STATE_ADDRESS = SIDECAR_ADDRESS + STATE_OFFSET;
    static final long CALLBACK_THUMB = SIDECAR_ADDRESS + 1L;
    static final Hotkey HOTKEY = new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT);
    static final int SE_TOGGLE_CLICK = 0x0066;
    static final int BIKING_ALLOWED_OFFSET = 0x18;

    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;

    private RunBikeAnywhereHotkeyRuntimeV1() {}

    static RamScript build(RomProfile rom) {
        byte[] payload = buildPayload(rom);
        byte[] nativeBlob = compactNativeInstallerBlobWithSidecarCopy(rom);

        int afterPayload = HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length;
        int nativeBlobOffset = align4(afterPayload);
        int alignmentPadding = nativeBlobOffset - afterPayload;
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int fieldInstallerOffset = nativeBlobOffset + nativeBlob.length;

        byte[] fieldInstaller = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam()
                .build();

        int total = fieldInstallerOffset + fieldInstaller.length;
        if (total > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("Run + Bike Anywhere HotkeyRuntimeV1 requires " + total
                    + " bytes; maximum is " + RamScript.SCRIPT_SIZE);
        }

        byte[] script = new byte[total];
        int p = 0;
        script[p++] = (byte) 0xB8;
        putU32(script, p, VIRTUAL_BASE); p += 4;
        script[p++] = (byte) 0xB9;
        putU32(script, p, VIRTUAL_BASE + fieldInstallerOffset); p += 4;
        script[p++] = (byte) (HotkeyRuntimeV1.FORMAT_SIGNATURE & 0xFF);
        script[p++] = (byte) ((HotkeyRuntimeV1.FORMAT_SIGNATURE >>> 8) & 0xFF);
        if (p != HotkeyRuntimeV1.PAYLOAD_OFFSET) throw new IllegalStateException("payload offset mismatch");

        System.arraycopy(payload, 0, script, p, payload.length); p += payload.length;
        for (int i = 0; i < alignmentPadding; i++) script[p++] = 0;
        System.arraycopy(nativeBlob, 0, script, p, nativeBlob.length); p += nativeBlob.length;
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
                b, VIRTUAL_BASE, helper, copierAddress, "run_bike_anywhere_toggle",
                NativeHelperInstaller.Mode.AUTO);
        install.installAndCall(b);
        return b.playSe(SE_TOGGLE_CLICK).waitSe().end().buildScript();
    }

    /* 60 bytes callback/literals + 3 state bytes = 63 bytes total.
       When armed, every stock map-load return captures both stock permissions,
       forces bikingAllowed=1 and sets only the allowRunning bit (bit 1), then
       returns control to the ordinary Runtime V1 wrapper. */
    static byte[] sidecar(RomProfile rom) {
        byte[] out = new byte[] {
                0x0A,0x48, 0x01,0x78, 0x00,0x29, 0x0C,(byte)0xD0,
                0x09,0x4A, 0x11,0x78, (byte)0x81,0x70, 0x01,0x21,
                0x11,0x70, 0x01,0x32, 0x11,0x78, 0x02,0x23,
                0x19,0x40, 0x41,0x70, 0x11,0x78, 0x19,0x43,
                0x11,0x70, 0x04,0x48, 0x04,0x49, 0x01,0x60,
                0x08,0x47, (byte)0xC0,0x46,
                0,0,0,0, // +2C state pointer
                0,0,0,0, // +30 bikingAllowed pointer
                0,0,0,0, // +34 gMain.callback1
                0,0,0,0, // +38 wrapper|1
                0,0,0     // +3C state, +3D run stock, +3E bike stock
        };
        if (out.length != SIDECAR_CODE_SIZE) throw new IllegalStateException("sidecar size mismatch: " + out.length);
        putU32(out, 0x2C, STATE_ADDRESS);
        putU32(out, 0x30, rom.mapHeader + BIKING_ALLOWED_OFFSET);
        putU32(out, 0x34, 0x030030F0L);
        putU32(out, 0x38, RuntimeV1ResidentBlocks.WRAPPER | 1L);
        return out;
    }

    /* Thumb-1, r0-r3 only. ON captures both stock map permissions and forces
       them on immediately. OFF restores bikingAllowed exactly and restores
       only bit 1 of the packed allowRunning byte, preserving the other bits. */
    static byte[] toggleHelper(RomProfile rom) {
        byte[] out = new byte[] {
                0x10,0x48, 0x01,0x78, 0x00,0x29, 0x0F,(byte)0xD1,
                0x01,0x21, 0x01,0x70, 0x0E,0x4A, 0x11,0x78,
                (byte)0x81,0x70, 0x01,0x21, 0x11,0x70, 0x01,0x32,
                0x11,0x78, 0x02,0x23, 0x19,0x40, 0x41,0x70,
                0x11,0x78, 0x19,0x43, 0x11,0x70, 0x70,0x47,
                0x00,0x21, 0x01,0x70, 0x06,0x4A, (byte)0x81,0x78,
                0x11,0x70, 0x01,0x32, 0x11,0x78, 0x02,0x23,
                (byte)0x99,0x43, 0x43,0x78, 0x19,0x43, 0x11,0x70,
                0x70,0x47, (byte)0xC0,0x46,
                0,0,0,0, // +44 state pointer
                0,0,0,0  // +48 bikingAllowed pointer
        };
        if (out.length != 76) throw new IllegalStateException("toggle helper size mismatch: " + out.length);
        putU32(out, 0x44, STATE_ADDRESS);
        putU32(out, 0x48, rom.mapHeader + BIKING_ALLOWED_OFFSET);
        return out;
    }

    static NativeHelperInstaller.Mode selectedToggleInstallerMode(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        return NativeHelperInstaller.chooseMode(5, new NativeHelper(helperAddress, toggleHelper(rom)), copierAddress);
    }

    static byte[] patchedNativeInstallerBlob(RomProfile rom) {
        byte[] blob = HotkeyRuntimeV1.nativeInstallerBlob(rom, HOTKEY).clone();
        int residentOffset = HotkeyRuntimeV1.NATIVE_CODE_AND_LITERALS_SIZE + HotkeyRuntimeV1.TABLE_SIZE;
        int cursor = residentOffset;
        boolean patched = false;
        List<RuntimeV1ResidentBlocks.Block> blocks = RuntimeV1ResidentBlocks.build(rom, HOTKEY);
        for (RuntimeV1ResidentBlocks.Block block : blocks) {
            if (block.address() == RuntimeV1ResidentBlocks.SUPERVISOR_LITERALS) {
                putU32(blob, cursor + 8, CALLBACK_THUMB);
                patched = true;
                break;
            }
            cursor += block.data().length;
        }
        if (!patched) throw new IllegalStateException("supervisor literal block not found");
        return blob;
    }

    static byte[] compactNativeInstallerBlobWithSidecarCopy(RomProfile rom) {
        byte[] patched = patchedNativeInstallerBlob(rom);
        final int oldHead = HotkeyRuntimeV1.NATIVE_CODE_AND_LITERALS_SIZE;
        final int descriptorBytes = HotkeyRuntimeV1.TABLE_SIZE;
        byte[] descriptors = java.util.Arrays.copyOfRange(patched, oldHead, oldHead + descriptorBytes);
        byte[] residentData = java.util.Arrays.copyOfRange(patched, oldHead + descriptorBytes, patched.length);
        byte[] residentSidecar = sidecar(rom);

        final int headSize = 0x4C;
        final int descriptorOffset = headSize;
        final int residentDataOffset = descriptorOffset + descriptorBytes;
        byte[] head = new byte[headSize];
        int[] insn = {
                0xB4F0, 0, 0, 0x2703, 0x063F, 0x250C,
                0x8821, 0x8862, 0x3404, 0x19C9,
                0x7833, 0x700B, 0x3601, 0x3101, 0x3A01, 0xD1F9,
                0x3D01, 0xD1F3,
                0, 0x2200 | residentSidecar.length,
                0x7833, 0x7003, 0x3601, 0x3001, 0x3A01, 0xD1F9,
                0, 0, 0x6001, 0xBCF0, 0x4770, 0x46C0
        };
        for (int i = 0; i < insn.length; i++) putU16(head, i * 2, insn[i]);
        putU16(head, 0x02, thumbAdr(4, 0x02, descriptorOffset));
        putU16(head, 0x04, thumbAdr(6, 0x04, residentDataOffset));
        putU16(head, 0x24, thumbLdrLiteral(0, 0x24, 0x40));
        putU16(head, 0x34, thumbLdrLiteral(0, 0x34, 0x44));
        putU16(head, 0x36, thumbLdrLiteral(1, 0x36, 0x48));
        putU32(head, 0x40, SIDECAR_ADDRESS);
        putU32(head, 0x44, 0x03003550L);
        putU32(head, 0x48, 0x03003F43L);

        byte[] out = new byte[head.length + descriptors.length + residentData.length + residentSidecar.length];
        int p = 0;
        System.arraycopy(head, 0, out, p, head.length); p += head.length;
        System.arraycopy(descriptors, 0, out, p, descriptors.length); p += descriptors.length;
        System.arraycopy(residentData, 0, out, p, residentData.length); p += residentData.length;
        System.arraycopy(residentSidecar, 0, out, p, residentSidecar.length);
        return out;
    }

    private static int thumbAdr(int rd, int insnOffset, int targetOffset) {
        int base = (insnOffset + 4) & ~3;
        int delta = targetOffset - base;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) throw new IllegalArgumentException("ADR target out of range");
        return 0xA000 | (rd << 8) | (delta / 4);
    }

    private static int thumbLdrLiteral(int rd, int insnOffset, int literalOffset) {
        int base = (insnOffset + 4) & ~3;
        int delta = literalOffset - base;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) throw new IllegalArgumentException("LDR literal target out of range");
        return 0x4800 | (rd << 8) | (delta / 4);
    }

    private static void putU16(byte[] b, int o, int v) {
        b[o]=(byte)v; b[o+1]=(byte)(v>>>8);
    }

    static int scriptSize(RomProfile rom) {
        byte[] payload = buildPayload(rom);
        int nativeBlobOffset = align4(HotkeyRuntimeV1.PAYLOAD_OFFSET + payload.length);
        byte[] bootstrap = HotkeyRuntimeV1.bootstrapBytes(rom, nativeBlobOffset);
        int installerSize = new FieldScriptWriter()
                .writeBytes(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS, bootstrap)
                .callNative(HotkeyRuntimeV1.BOOTSTRAP_ADDRESS | 1L)
                .returnRam().build().length;
        return nativeBlobOffset + compactNativeInstallerBlobWithSidecarCopy(rom).length + installerSize;
    }

    private static int align4(int value) { return (value + 3) & ~3; }
    private static void putU32(byte[] b, int o, long v) {
        b[o]=(byte)v; b[o+1]=(byte)(v>>>8); b[o+2]=(byte)(v>>>16); b[o+3]=(byte)(v>>>24);
    }
}
