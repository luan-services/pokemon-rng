/*
   Persistent Event Framework — Probe 1.

   Purpose: validate the event-specific path that does NOT use the Hotkey
   Runtime or its scheduler:

       object-bound RamScript (SB1 RamScript slot)
           -> relocation-safe direct vgoto
           -> persistent Field Script package in SB2

   The first interaction installs the tiny package in the already validated
   SB2 toolkit region. The package itself sets an unused FR/LG event flag.
   Later interactions skip installation when that flag is set, so after a
   save/reset an "ON" result exercises the already-persisted SB2 bytes rather
   than copying them again.

   This is BUILD-TESTED only until exercised in FireRed 1.0.
*/
final class PersistentEventPackageProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final long PACKAGE_VIRTUAL_BASE = 0x08020000L;

    // Static non-ASLR bases used only to calculate the invariant SB1->SB2 delta.
    // These are the same FR1.0 bases already used by PersistentFieldScriptGatewayRuntime.
    private static final long STATIC_SAVE_BLOCK2 = 0x02024588L;
    private static final long STATIC_SAVE_BLOCK1 = 0x0202552CL;
    private static final int RAMSCRIPT_SCRIPT_IN_SB1 = 0x3624;

    static final int PACKAGE_SB2_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset(); // 0x0B20
    static final int TEST_FLAG = 0x4AF; // pret/pokefirered flags.h: unused; 0x4A7..0x4AE are toolkit Gym flags.

    private PersistentEventPackageProbePreset() {}

    static TriggerBuildResult buildLavenderWorker(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Persistent Event Probe 1 is intentionally FR1.0-only");

        byte[] eventPackage = buildPackage();
        long copier = rom.stringVar4 + 0x100L;
        long helperAddress = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper helper = buildPackageInstaller(rom, helperAddress, eventPackage);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("dispatch");

        // prepare() embeds unreachable installer material. Entry skips it.
        NativeHelperInstaller.Plan install = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, helper, copier, "persistent_event_probe1_install",
                NativeHelperInstaller.Mode.AUTO);

        b.label("dispatch")
         .checkFlag(TEST_FLAG)
         .vGotoIfEqual("launch")
         .lockAll();
        install.installAndCall(b);
        b.releaseAll()
         .label("launch")
         .vGotoAddress(packageVirtualTarget());

        byte[] payload = b.buildScript();
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(ObjectEventCatalog.LAVENDER_TOWN_WORKER_M);
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, eventPackage.length,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    static byte[] buildPackage() {
        RamScriptBuilder p = new RamScriptBuilder(PACKAGE_VIRTUAL_BASE);
        p.setVAddress()
         .checkFlag(TEST_FLAG).vGotoIfEqual("on")
         .vMessage("off").waitMessage().waitButtonPress()
         .setFlag(TEST_FLAG)
         .releaseAll().end()
         .label("on")
         .vMessage("onMsg").waitMessage().waitButtonPress()
         .releaseAll().end()
         .text("off", "Persistent package: OFF.\\nFlag is now ON.")
         .text("onMsg", "Persistent package: ON.");
        return p.buildScript();
    }

    static long packageVirtualTarget() {
        long scriptStatic = STATIC_SAVE_BLOCK1 + RAMSCRIPT_SCRIPT_IN_SB1;
        long targetStatic = STATIC_SAVE_BLOCK2 + PACKAGE_SB2_OFFSET;
        long delta = targetStatic - scriptStatic;
        return (VIRTUAL_BASE + delta) & 0xFFFF_FFFFL;
    }

    private static NativeHelper buildPackageInstaller(RomProfile rom, long address, byte[] data) {
        if (data.length == 0 || data.length > 0xFF)
            throw new IllegalArgumentException("Probe package must fit one <=255-byte copy operation");

        // Thumb: resolve live *gSaveBlock2Ptr, add 0x0B20, copy embedded package byte-by-byte.
        final int codeSize = 26;
        final int literalOffset = 28;
        final int dataOffset = 40;
        byte[] code = new byte[dataOffset + data.length];

        putU16(code, 0x00, ldrLiteral(0, 0x00, literalOffset));
        putU16(code, 0x02, 0x6800); // ldr r0,[r0]
        putU16(code, 0x04, ldrLiteral(1, 0x04, literalOffset + 4));
        putU16(code, 0x06, 0x1840); // adds r0,r0,r1
        putU16(code, 0x08, ldrLiteral(1, 0x08, literalOffset + 8));
        putU16(code, 0x0A, 0x2200 | data.length); // movs r2,#len
        putU16(code, 0x0C, 0x780B); // ldrb r3,[r1]
        putU16(code, 0x0E, 0x7003); // strb r3,[r0]
        putU16(code, 0x10, 0x3101); // adds r1,#1
        putU16(code, 0x12, 0x3001); // adds r0,#1
        putU16(code, 0x14, 0x3A01); // subs r2,#1
        putU16(code, 0x16, branchCond(1, 0x16, 0x0C)); // bne copy loop
        putU16(code, 0x18, 0x4770); // bx lr
        putU16(code, 0x1A, 0x46C0); // align

        putU32(code, literalOffset, rom.saveBlock2Ptr);
        putU32(code, literalOffset + 4, PACKAGE_SB2_OFFSET);
        putU32(code, literalOffset + 8, address + dataOffset);
        System.arraycopy(data, 0, code, dataOffset, data.length);
        return new NativeHelper(address, code);
    }

    private static int ldrLiteral(int rt, int insn, int literal) {
        int base = (insn + 4) & ~3;
        int d = literal - base;
        if (d < 0 || (d & 3) != 0 || d / 4 > 255) throw new IllegalArgumentException("literal range");
        return 0x4800 | (rt << 8) | (d / 4);
    }

    private static int branchCond(int cond, int insn, int target) {
        int d = target - (insn + 4);
        if ((d & 1) != 0 || d / 2 < -128 || d / 2 > 127) throw new IllegalArgumentException("branch range");
        return 0xD000 | (cond << 8) | ((d / 2) & 0xFF);
    }

    private static void putU16(byte[] b, int o, int v) { b[o]=(byte)v; b[o+1]=(byte)(v>>>8); }
    private static void putU32(byte[] b, int o, long v) { b[o]=(byte)v; b[o+1]=(byte)(v>>>8); b[o+2]=(byte)(v>>>16); b[o+3]=(byte)(v>>>24); }
}
