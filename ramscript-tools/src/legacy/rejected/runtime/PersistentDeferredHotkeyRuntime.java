/* Build 13: deferred persistent hotkey bridge.

   Keep the validated MultiHotkeyRuntimeV1 callback model unchanged:
   hotkey -> ScriptContext_SetupScript(payload) -> return from callback.

   The deferred Field Script temporarily repurposes the already-validated
   14-byte STAGE1 IWRAM allocation as a SaveBlock2 resolver, calls it only from
   normal script context, then restores the original MultiHotkey STAGE1 bytes.
   No new IWRAM region and no executable gStringVar4 scratch are used.
*/
final class PersistentDeferredHotkeyRuntime {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final int VAR_RESULT = 0x800D;
    private static final int VAR_8004 = 0x8004;
    private static final int VAR_8005 = 0x8005;
    private static final int STRING_VAR_1 = 0;

    private PersistentDeferredHotkeyRuntime() {}

    static RamScript buildInstaller(RomProfile rom, int desiredSeed) {
        // Build 11a/12 storage format was independently validated with the
        // Build 10a launcher. Reuse that installer; only the runtime bridge changes.
        return PersistentHotkeyRuntimePrototype.buildInstaller(rom, desiredSeed);
    }

    static TriggerBuildResult buildRuntime(RomProfile rom, int desiredSeed) {
        // Keep both hotkey entry payloads tiny. The first sets SID and jumps into
        // the shared body stored after the second entry; the second sets SEED and
        // falls through into that same body. This preserves the validated u8
        // payload-offset scheme while paying for the deferred bridge only once.
        final int firstOffset = MultiHotkeyRuntimeV1.FIRST_PAYLOAD_OFFSET;

        // IMPORTANT: a hotkey starts execution directly at the payload offset.
        // It does NOT re-run the RamScript header's setvaddress. Every deferred
        // payload that uses v* opcodes must therefore establish sAddressOffset
        // itself, exactly like the validated SeedModifierPreset does.
        //
        // first:  setvaddress (5) + setvar (5) + vgoto common (5)
        // second: setvaddress (5) + setvar (5) + common body
        byte[] first = new byte[15];
        first[0]=(byte)0xB8; putU32(first,1,VIRTUAL_BASE + firstOffset);
        first[5]=0x16; putU16(first,6,VAR_8005); putU16(first,8,PersistentShowSecretIdModule.MODULE_ID);
        first[10]=(byte)0xB9;

        int secondOffset = firstOffset + first.length;
        int commonOffset = secondOffset + 10;
        putU32(first,11,VIRTUAL_BASE + commonOffset);

        byte[] common = commonBridge(rom, desiredSeed, commonOffset);
        byte[] second = new byte[10 + common.length];
        second[0]=(byte)0xB8; putU32(second,1,VIRTUAL_BASE + secondOffset);
        second[5]=0x16; putU16(second,6,VAR_8005); putU16(second,8,PersistentSeedModifierModule.MODULE_ID);
        System.arraycopy(common,0,second,10,common.length);

        return MultiHotkeyRuntimeV1.compose(
                rom,
                new HotkeyPayload(new Hotkey(HotkeyButton.R, HotkeyButton.B), first),
                new HotkeyPayload(new Hotkey(HotkeyButton.R, HotkeyButton.SELECT), second)
        );
    }

    static byte[] resolverBytesForTest(RomProfile rom) { return resolver(rom); }
    static byte[] restoredStage2BytesForTest() { return originalMultiStage2(); }

    private static byte[] commonBridge(RomProfile rom, int desiredSeed, int commonOffset) {
        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE + commonOffset);
        return b.lockAll()
                // Only Seed needs the precision prompt. SID goes straight through.
                .compareVarToValue(VAR_8005,PersistentSeedModifierModule.MODULE_ID).vGotoIfNotEqual("dispatch")
                .vMessage("prompt").waitMessage().waitButtonPress()
                .label("dispatch")
                .setVar(VAR_RESULT,0).setVar(VAR_8004,0)
                // Callback has already returned. Temporarily reuse the exact
                // validated STAGE2 block and its existing literal slot.
                .writeBytes(0x0300504CL, le32(rom.saveBlock2Ptr))
                .writeBytes(RuntimeV1ResidentBlocks.STAGE2, resolver(rom))
                .callNative(RuntimeV1ResidentBlocks.STAGE2 | 1L)
                // Restore runtime before any message/wait/end path.
                .writeBytes(RuntimeV1ResidentBlocks.STAGE2, originalMultiStage2())
                .writeBytes(0x0300504CL, le32(rom.scriptContextSetupThumb))
                .compareVarToValue(VAR_RESULT,1).vGotoIfNotEqual("bad")
                .compareVarToValue(VAR_8005,PersistentShowSecretIdModule.MODULE_ID).vGotoIfNotEqual("done")
                .bufferNumberString(STRING_VAR_1,VAR_8004)
                .vMessage("sid").waitMessage().waitButtonPress()
                .label("done").releaseAll().end()
                .label("bad").vMessage("badmsg").waitMessage().waitButtonPress().releaseAll().end()
                .text("prompt",SeedModifierPreset.message(desiredSeed))
                .text("sid","SID: {STR_VAR_1}.")
                .text("badmsg","Persistent module invalid.")
                .buildScript();
    }

    private static byte[] resolver(RomProfile rom) {
        // Exactly 14 bytes in STAGE2 (03005032). Its PC-relative literal at
        // 0300504C is temporarily changed from ScriptContext_SetupScript to
        // &gSaveBlock2Ptr by the already-deferred Field Script.
        return new byte[] {
                0x06,0x48,                   // ldr  r0,0300504C (&gSaveBlock2Ptr)
                0x00,0x68,                   // ldr  r0,[r0] current SaveBlock2
                (byte)0xD2,0x21,             // movs r1,#0xD2
                0x09,0x01,                   // lsls r1,r1,#4 = 0xD20
                0x40,0x18,                   // adds r0,r0,r1
                0x01,0x30,                   // adds r0,#1 Thumb bit
                0x00,0x47                    // bx   r0
        };
    }

    private static byte[] originalMultiStage2() {
        return new byte[] {
                0x00,(byte)0x99, 0x40,0x18, 0x05,0x4C,
                (byte)0xFE,(byte)0xF7,(byte)0xAF,(byte)0xFF,
                0x27,(byte)0xE0, (byte)0xC0,0x46
        };
    }

    private static byte[] le32(long v) {
        return new byte[]{(byte)v,(byte)(v>>>8),(byte)(v>>>16),(byte)(v>>>24)};
    }
    private static void putU16(byte[] d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[] d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}

}
