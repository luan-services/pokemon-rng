/*
   Persistent Event Framework — Probe 2A.

   Diagnostic correction after Probe 2 froze in-game.

   Probe 2 mutated the saved RamScript binding before Stage A dialogue had
   finished. Probe 2A deliberately separates the operations:

     1. Stage A installs the SB2 package (same proven idea as Probe 1).
     2. Stage A completes all dialogue while the Worker M binding is unchanged.
     3. Only after the final dialogue/button press, a tiny native helper changes
        mapGroup/mapNum/objectId and refreshes the stock RamScript checksum.
     4. The very next Field Script command is end.

   No progress flag is used. Stage selection is based on VAR_LAST_TALKED, while
   the saved RamScript binding determines which object can enter this script.

   BUILD-TESTED only until exercised in FireRed 1.0.
*/
final class PersistentEventRebindingProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final long PACKAGE_VIRTUAL_BASE = 0x08020000L;
    private static final long STATIC_SAVE_BLOCK2 = 0x02024588L;
    private static final long STATIC_SAVE_BLOCK1 = 0x0202552CL;
    private static final int RAMSCRIPT_STRUCT_IN_SB1 = 0x361C;
    private static final int RAMSCRIPT_SCRIPT_IN_SB1 = 0x3624;
    private static final int VAR_LAST_TALKED = 0x800F;

    static final int PACKAGE_SB2_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset();
    static final ObjectEventTarget STAGE_A = ObjectEventCatalog.LAVENDER_TOWN_WORKER_M;
    static final ObjectEventTarget STAGE_B = ObjectEventCatalog.OAKS_LAB_AIDE1_EARLY_INSTALLER;

    private PersistentEventRebindingProbePreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Persistent Event Probe 2A is intentionally FR1.0-only");

        byte[] eventPackage = buildPackage();
        long copier = rom.stringVar4 + 0x100L;
        long helperAddr = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper packageInstaller = buildPackageInstaller(rom, helperAddr, eventPackage);
        NativeHelper rebind = buildRebindHelper(rom, helperAddr);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("dispatch");

        // Both helpers are embedded as unreachable installer material. They use
        // the same temporary execution destination, but are staged/called at
        // different moments and never need to coexist there.
        NativeHelperInstaller.Plan packagePlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, packageInstaller, copier,
                "persistent_event_probe2a_package", NativeHelperInstaller.Mode.AUTO);
        NativeHelperInstaller.Plan rebindPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, rebind, copier,
                "persistent_event_probe2a_rebind", NativeHelperInstaller.Mode.AUTO);

        b.label("dispatch")
         .compareVarToValue(VAR_LAST_TALKED, STAGE_B.localId()).vGotoIfEqual("stageB")
         .lockAll();

        // Stage A: package install first, while binding remains Worker M.
        packagePlan.installAndCall(b);
        b.vMessage("stageAMsg").waitMessage().waitButtonPress()
         .vMessage("beforeRebindMsg").waitMessage().waitButtonPress()
         .releaseAll();

        // Critical diagnostic difference from rejected Probe 2: mutate the
        // binding only after every Stage A dialogue has completed. After the
        // helper returns, execute nothing except end.
        rebindPlan.installAndCall(b);
        b.end();

        b.label("stageB").vGotoAddress(packageVirtualTarget());

        b.text("stageAMsg", "Stage A: Lavender host.\\nDialogue finished normally.");
        b.text("beforeRebindMsg", "Now passing the event\\nto Oak's aide...");

        byte[] payload = b.buildScript();
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(STAGE_A);
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, eventPackage.length,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    static byte[] buildPackage() {
        RamScriptBuilder p = new RamScriptBuilder(PACKAGE_VIRTUAL_BASE);
        p.setVAddress()
         .lockAll()
         .vMessage("stageBMsg").waitMessage().waitButtonPress()
         .releaseAll().end()
         .text("stageBMsg", "Stage B: Oak's aide.\\nRebinding survived save/reset.");
        return p.buildScript();
    }

    static long packageVirtualTarget() {
        long scriptStatic = STATIC_SAVE_BLOCK1 + RAMSCRIPT_SCRIPT_IN_SB1;
        long targetStatic = STATIC_SAVE_BLOCK2 + PACKAGE_SB2_OFFSET;
        return (VIRTUAL_BASE + (targetStatic - scriptStatic)) & 0xFFFF_FFFFL;
    }

    private static NativeHelper buildRebindHelper(RomProfile rom, long address) {
        // Resolve live SB1, update only RamScript binding metadata, calculate
        // stock CRC, store the returned u32 checksum, return immediately.
        final int literalOffset = 0x28;
        byte[] c = new byte[0x38];
        putU16(c,0x00,0xB510); // push {r4,lr}
        putU16(c,0x02,ldrLiteral(4,0x02,literalOffset));
        putU16(c,0x04,0x6824); // ldr r4,[r4]
        putU16(c,0x06,ldrLiteral(0,0x06,literalOffset+4));
        putU16(c,0x08,0x1824); // adds r4,r4,r0 -> RamScript struct
        putU16(c,0x0A,0x2000 | STAGE_B.mapGroup()); putU16(c,0x0C,0x7160); // +5 mapGroup
        putU16(c,0x0E,0x2000 | STAGE_B.mapNum());   putU16(c,0x10,0x71A0); // +6 mapNum
        putU16(c,0x12,0x2000 | STAGE_B.localId());  putU16(c,0x14,0x71E0); // +7 objectId
        putU16(c,0x16,ldrLiteral(3,0x16,literalOffset+8));
        putU16(c,0x18,0xF000); putU16(c,0x1A,0xF803); // BL thunk @ 0x24
        putU16(c,0x1C,0x6020); // str r0,[r4,#0] checksum u32
        putU16(c,0x1E,0xBD10); // pop {r4,pc}
        putU16(c,0x20,0x46C0); putU16(c,0x22,0x46C0);
        putU16(c,0x24,0x4718); // bx r3
        putU16(c,0x26,0x46C0);
        putU32(c,literalOffset,rom.saveBlock1Ptr);
        putU32(c,literalOffset+4,RAMSCRIPT_STRUCT_IN_SB1);
        putU32(c,literalOffset+8,EarlyObjectBoundHotkeyInstaller.calculateRamScriptChecksumThumb(rom));
        return new NativeHelper(address,c);
    }

    private static NativeHelper buildPackageInstaller(RomProfile rom, long address, byte[] data) {
        if (data.length == 0 || data.length > 0xFF)
            throw new IllegalArgumentException("Probe package must fit one <=255-byte copy operation");
        final int literalOffset = 28;
        final int dataOffset = 40;
        byte[] code = new byte[dataOffset + data.length];
        putU16(code,0x00,ldrLiteral(0,0x00,literalOffset));
        putU16(code,0x02,0x6800);
        putU16(code,0x04,ldrLiteral(1,0x04,literalOffset+4));
        putU16(code,0x06,0x1840);
        putU16(code,0x08,ldrLiteral(1,0x08,literalOffset+8));
        putU16(code,0x0A,0x2200 | data.length);
        putU16(code,0x0C,0x780B); putU16(code,0x0E,0x7003);
        putU16(code,0x10,0x3101); putU16(code,0x12,0x3001); putU16(code,0x14,0x3A01);
        putU16(code,0x16,branchCond(1,0x16,0x0C));
        putU16(code,0x18,0x4770); putU16(code,0x1A,0x46C0);
        putU32(code,literalOffset,rom.saveBlock2Ptr);
        putU32(code,literalOffset+4,PACKAGE_SB2_OFFSET);
        putU32(code,literalOffset+8,address+dataOffset);
        System.arraycopy(data,0,code,dataOffset,data.length);
        return new NativeHelper(address,code);
    }

    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3,d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);}
    private static void putU16(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[] b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}
}
