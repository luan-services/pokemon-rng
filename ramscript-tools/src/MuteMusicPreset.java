/* Production FR/LG Mute Music preset.

   The native helper is temporary EWRAM staging only; no resident IWRAM is added.

   R+DOWN:
   - OFF -> ON: sets stock gDisableMusic and sets only the BGM player's track
     volume to 0 with stock m4aMPlayVolumeControl.
   - ON -> OFF: clears gDisableMusic and restores BGM track volume to 256.
   - After either toggle, the field script resets the logical map-music id with
     playbgm MUS_DUMMY and immediately calls stock Overworld_PlaySpecialMapMusic.
     On ON, gDisableMusic keeps the re-resolved request silent; on OFF, this makes
     the correct map/surf/saved BGM start immediately.

   The map-music restart deliberately uses stock Field Script commands instead of
   hardcoding StopMapMusic. The local BL -> bx r3 veneer is part of the temporary
   helper itself and does not consume the frozen resident IWRAM wrapper.
*/
final class MuteMusicPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    static final Hotkey HOTKEY = new Hotkey(HotkeyButton.R, HotkeyButton.DOWN);

    private MuteMusicPreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.HOTKEY_RUNTIME, rom, buildPayload(rom), HOTKEY);
    }

    static byte[] buildPayload(RomProfile rom) {
        long copierAddress = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copierAddress);
        NativeHelper helper = buildHelper(rom, helperAddress);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress();
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copierAddress, "mute_music_volume_control",
                NativeHelperInstaller.Mode.AUTO);
        install.install(b);
        return b.callNative(helper.thumbEntryAddress())
                .playBgm(0, false)
                .special(153) // SPECIAL_Overworld_PlaySpecialMapMusic
                .end()
                .buildScript();
    }

    static NativeHelper buildHelper(RomProfile rom, long stagingAddress) {
        // ARM7TDMI / Thumb-1. Calls the stock M4A routine through a local
        // BL -> bx r3 veneer. Do not replace this with `mov lr, pc; bx r3`:
        // that sequence does not preserve Thumb state in LR on ARMv4T.
        byte[] code = new byte[] {
                0x10,(byte)0xB5, 0x0D,0x4C, 0x23,0x78, 0x00,0x2B,
                0x0A,(byte)0xD0, 0x00,0x23, 0x23,0x70, 0x0B,0x48,
                0x01,0x21, 0x49,0x42, (byte)0x80,0x22, 0x52,0x00,
                0x09,0x4B, 0x00,(byte)0xF0, 0x0B,(byte)0xF8, 0x10,(byte)0xBD,
                0x01,0x23, 0x23,0x70, 0x05,0x48, 0x01,0x21,
                0x49,0x42, 0x00,0x22, 0x04,0x4B, 0x00,(byte)0xF0,
                0x01,(byte)0xF8, 0x10,(byte)0xBD, 0x18,0x47, (byte)0xC0,0x46,
                0x00,0x00,0x00,0x00, // gDisableMusic
                0x00,0x00,0x00,0x00, // gMPlayInfo_BGM
                0x00,0x00,0x00,0x00  // m4aMPlayVolumeControl | 1
        };
        putU32(code, 0x38, rom.gDisableMusic);
        putU32(code, 0x3C, rom.gMPlayInfoBgm);
        putU32(code, 0x40, rom.m4aMPlayVolumeControlThumb | 1L);
        return new NativeHelper(stagingAddress, code);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
