/*
   Persistent Event Framework — Probe 4.

   Route 8 object-host transformation probe built on the GAME-VALIDATED Probe 2A
   deferred-rebind lifecycle.

   Stage A: Julia (Route 8, x=62 y=14). After dialogue is fully released, a
   native helper changes the CURRENT Route 8 template for Cut Tree B (x=47 y=12)
   to OBJ_EVENT_GFX_MEW and also updates the live object if it is loaded. Before release, stock field-script commands move Cut Tree B to (47,8) and update its current template position. The
   same helper then rebinds the saved RamScript to that Cut Tree localId and
   refreshes the stock RamScript checksum. Nothing runs after the helper except end.

   Stage B: interaction with the transformed host jumps to the persistent SB2
   package and displays an unmistakable diagnostic message.

   No battle, reward, stage flag, map listener, or resident runtime.
   BUILD-TESTED only until exercised in FireRed 1.0.
*/
final class PersistentEventMewMoveProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final long PACKAGE_VIRTUAL_BASE = 0x08020000L;
    private static final long STATIC_SAVE_BLOCK2 = 0x02024588L;
    private static final long STATIC_SAVE_BLOCK1 = 0x0202552CL;
    private static final int RAMSCRIPT_STRUCT_IN_SB1 = 0x361C;
    private static final int RAMSCRIPT_SCRIPT_IN_SB1 = 0x3624;
    private static final int VAR_LAST_TALKED = 0x800F;

    // SaveBlock1::objectEventTemplates starts at 0x08E0; each template is 0x18.
    private static final int OBJECT_TEMPLATES_SB1_OFFSET = 0x08E0;
    private static final int OBJECT_TEMPLATE_SIZE = 0x18;
    private static final int OBJECT_TEMPLATE_COUNT = 64;
    private static final int OBJ_EVENT_GFX_MEW = 140;
    static final int MEW_TARGET_X = 47;
    static final int MEW_TARGET_Y = 8;

    static final int PACKAGE_SB2_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset();

    // gMapGroup_TownsAndRoutes = group 3. Route8 is index 26 in that group.
    // map.json implicit localIds are 1-based in object-event order:
    // Julia is object #1; Cut Tree B (47,12) is object #11.
    static final ObjectEventTarget JULIA = new ObjectEventTarget(
            "route8-julia-probe4", "Route 8 — Julia (Probe 4 host)",
            3, 26, 1, 62, 14,
            "Route8_EventScript_Julia", ObjectEventSafety.SAFE_SIMPLE_DIALOG);
    static final ObjectEventTarget MEW_HOST = new ObjectEventTarget(
            "route8-cut-tree-b-mew-host-probe4", "Route 8 — Cut Tree B / Mew host",
            3, 26, 11, 47, 12,
            "EventScript_CutTree", ObjectEventSafety.SAFE_SIMPLE_DIALOG);

    private PersistentEventMewMoveProbePreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Persistent Event Probe 4 is intentionally FR1.0-only");

        byte[] eventPackage = buildPackage();
        long copier = rom.stringVar4 + 0x100L;
        long helperAddr = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper packageInstaller = buildPackageInstaller(rom, helperAddr, eventPackage);
        NativeHelper transformAndRebind = buildTransformAndRebindHelper(rom, helperAddr);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("dispatch");

        NativeHelperInstaller.Plan packagePlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, packageInstaller, copier,
                "persistent_event_probe4_package", NativeHelperInstaller.Mode.AUTO);
        NativeHelperInstaller.Plan transformPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, transformAndRebind, copier,
                "persistent_event_probe4_transform_rebind", NativeHelperInstaller.Mode.AUTO);

        b.label("dispatch")
         .compareVarToValue(VAR_LAST_TALKED, MEW_HOST.localId()).vGotoIfEqual("stageMew")
         .lockAll();

        packagePlan.installAndCall(b);
        b.facePlayer()
         .vMessage("juliaMsg").waitMessage().waitButtonPress()
         .vMessage("beforeTransformMsg").waitMessage().waitButtonPress()
         .setObjectXY(MEW_HOST.localId(), MEW_TARGET_X, MEW_TARGET_Y)
         .setObjectXYPermanent(MEW_HOST.localId(), MEW_TARGET_X, MEW_TARGET_Y)
         .releaseAll();

        // Probe-2A rule: only mutate the host/binding after the originating
        // dialogue has completely finished. After this helper, only end.
        transformPlan.installAndCall(b);
        b.end();

        b.label("stageMew").vGotoAddress(packageVirtualTarget());

        b.text("juliaMsg", "I saw a weird Pokemon\\nfloating around here...");
        b.text("beforeTransformMsg", "Something moved in the grass...");

        byte[] payload = b.buildScript();
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(JULIA);
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, eventPackage.length,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    static byte[] buildPackage() {
        RamScriptBuilder p = new RamScriptBuilder(PACKAGE_VIRTUAL_BASE);
        p.setVAddress().lockAll().facePlayer()
         .vMessage("mewMsg").waitMessage().waitButtonPress()
         .releaseAll().end()
         .text("mewMsg", "Mew stage reached.\\nPersistent host works.");
        return p.buildScript();
    }

    static long packageVirtualTarget() {
        long scriptStatic = STATIC_SAVE_BLOCK1 + RAMSCRIPT_SCRIPT_IN_SB1;
        long targetStatic = STATIC_SAVE_BLOCK2 + PACKAGE_SB2_OFFSET;
        return (VIRTUAL_BASE + (targetStatic - scriptStatic)) & 0xFFFF_FFFFL;
    }

    private static NativeHelper buildTransformAndRebindHelper(RomProfile rom, long address) {
        // r4 = live SB1. Scan its current ObjectEventTemplate array for localId 11,
        // change graphicsId to Mew, then ask the stock live-object helper to update
        // an already-loaded instance. Finally perform the exact Probe-2A rebind.
        final int literalOffset = 0x50;
        byte[] c = new byte[0x70];
        putU16(c,0x00,0xB5F0); // push {r4-r7,lr}
        putU16(c,0x02,ldrLiteral(4,0x02,literalOffset));
        putU16(c,0x04,0x6824); // ldr r4,[r4] live SB1
        putU16(c,0x06,ldrLiteral(5,0x06,literalOffset+4));
        putU16(c,0x08,0x1965); // adds r5,r4,r5 -> templates
        putU16(c,0x0A,0x263F); // movs r6,#63 (scan 64 entries with index-style countdown)
        // scan @ 0x0C
        putU16(c,0x0C,0x7828); // ldrb r0,[r5,#0] localId
        putU16(c,0x0E,0x2800 | MEW_HOST.localId());
        putU16(c,0x10,branchCond(0,0x10,0x1C)); // beq found
        putU16(c,0x12,0x3518); // adds r5,#0x18
        putU16(c,0x14,0x3E01); // subs r6,#1
        putU16(c,0x16,branchCond(2,0x16,0x0C)); // bcs scan
        putU16(c,0x18,branchUncond(0x18,0x2E)); // no template: still rebind
        putU16(c,0x1A,0x46C0);
        // found @ 0x1C: graphicsId is byte +1
        putU16(c,0x1C,0x208C); // movs r0,#140
        putU16(c,0x1E,0x7068); // strb r0,[r5,#1]

        // Update live object too if currently loaded: (localId,mapNum,mapGroup,gfxId).
        putU16(c,0x20,0x2000 | MEW_HOST.localId());
        putU16(c,0x22,0x2100 | MEW_HOST.mapNum());
        putU16(c,0x24,0x2200 | MEW_HOST.mapGroup());
        putU16(c,0x26,0x238C);
        putU16(c,0x28,ldrLiteral(7,0x28,literalOffset+8));
        putU16(c,0x2A,0xF000); putU16(c,0x2C,0xF80D); // BL thunk_r7 @ 0x4A

        // rebind @ 0x2E
        putU16(c,0x2E,ldrLiteral(0,0x2E,literalOffset+12));
        putU16(c,0x30,0x1824); // adds r4,r4,r0 -> RamScript struct
        putU16(c,0x32,0x2000 | MEW_HOST.mapGroup()); putU16(c,0x34,0x7160);
        putU16(c,0x36,0x2000 | MEW_HOST.mapNum());   putU16(c,0x38,0x71A0);
        putU16(c,0x3A,0x2000 | MEW_HOST.localId());  putU16(c,0x3C,0x71E0);
        putU16(c,0x3E,ldrLiteral(7,0x3E,literalOffset+16));
        putU16(c,0x40,0xF000); putU16(c,0x42,0xF802); // BL thunk_r7 @ 0x48
        putU16(c,0x44,0x6020); // checksum
        putU16(c,0x46,0xBDF0); // pop {r4-r7,pc}
        putU16(c,0x48,0x4738); // thunk_r7: bx r7
        putU16(c,0x4A,0x4738); // thunk_r7: bx r7
        putU16(c,0x4C,0x46C0); putU16(c,0x4E,0x46C0);

        putU32(c,literalOffset,rom.saveBlock1Ptr);
        putU32(c,literalOffset+4,OBJECT_TEMPLATES_SB1_OFFSET);
        putU32(c,literalOffset+8,objectEventSetGraphicsIdByLocalIdAndMapThumb(rom));
        putU32(c,literalOffset+12,RAMSCRIPT_STRUCT_IN_SB1);
        putU32(c,literalOffset+16,EarlyObjectBoundHotkeyInstaller.calculateRamScriptChecksumThumb(rom));
        return new NativeHelper(address,c);
    }

    private static long objectEventSetGraphicsIdByLocalIdAndMapThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10, LEAF_GREEN_EN_10 -> 0x0805F1D9L;
            case FIRE_RED_EN_11, LEAF_GREEN_EN_11 -> 0x0805F1EDL;
        };
    }

    private static NativeHelper buildPackageInstaller(RomProfile rom, long address, byte[] data) {
        if (data.length == 0 || data.length > 0xFF)
            throw new IllegalArgumentException("Probe package must fit one <=255-byte copy operation");
        final int literalOffset = 28, dataOffset = 40;
        byte[] code = new byte[dataOffset + data.length];
        putU16(code,0x00,ldrLiteral(0,0x00,literalOffset)); putU16(code,0x02,0x6800);
        putU16(code,0x04,ldrLiteral(1,0x04,literalOffset+4)); putU16(code,0x06,0x1840);
        putU16(code,0x08,ldrLiteral(1,0x08,literalOffset+8)); putU16(code,0x0A,0x2200 | data.length);
        putU16(code,0x0C,0x780B); putU16(code,0x0E,0x7003);
        putU16(code,0x10,0x3101); putU16(code,0x12,0x3001); putU16(code,0x14,0x3A01);
        putU16(code,0x16,branchCond(1,0x16,0x0C)); putU16(code,0x18,0x4770); putU16(code,0x1A,0x46C0);
        putU32(code,literalOffset,rom.saveBlock2Ptr); putU32(code,literalOffset+4,PACKAGE_SB2_OFFSET);
        putU32(code,literalOffset+8,address+dataOffset); System.arraycopy(data,0,code,dataOffset,data.length);
        return new NativeHelper(address,code);
    }

    private static int ldrLiteral(int rt,int insn,int literal){int base=(insn+4)&~3,d=literal-base;if(d<0||(d&3)!=0||d/4>255)throw new IllegalArgumentException("literal range");return 0x4800|(rt<<8)|(d/4);}
    private static int branchCond(int cond,int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -128||d/2>127)throw new IllegalArgumentException("branch range");return 0xD000|(cond<<8)|((d/2)&0xFF);}
    private static int branchUncond(int insn,int target){int d=target-(insn+4);if((d&1)!=0||d/2< -1024||d/2>1023)throw new IllegalArgumentException("branch range");return 0xE000|((d/2)&0x7FF);}
    private static void putU16(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[] b,int o,long v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);b[o+2]=(byte)(v>>>16);b[o+3]=(byte)(v>>>24);}
}
