/*
   Persistent Event Framework — Probe 6P.

   GAME-VALIDATED Probe 6J established the safe visual primitive: teleport the
   active host away with setobjectxy, then spawn/remove a separate temporary Mew.

   Probe 6P integrates the GAME-VALIDATED chain with a deferred transfer
   from Route 24 Shane back to Route 8 Julia. Stan/Shane keep the validated
   teleport-host + temporary-Mew presentation; Julia creates/rebinds the final Mew host at (40,13). This integration probe keeps the final host interaction local to the main RamScript; Probe 5 remains the GAME-VALIDATED recovery baseline after reload.
*/
final class PersistentEventStanTeleportProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_HELPER_MODE = 0x8000;

    static final ObjectEventTarget STAN = new ObjectEventTarget(
            "route8-stan-probe6p", "Route 8 — Stan / stage 1",
            3, 26, 8, 21, 3,
            "Route8_EventScript_Stan", ObjectEventSafety.SAFE_SIMPLE_DIALOG);

    static final ObjectEventTarget SHANE = new ObjectEventTarget(
            "route24-shane-probe6p", "Route 24 — Camper Shane / stage 2",
            3, 43, 7, 5, 21,
            "Route24_EventScript_Shane", ObjectEventSafety.SAFE_SIMPLE_DIALOG);

    static final ObjectEventTarget JULIA = new ObjectEventTarget(
            "route8-julia-probe6p", "Route 8 — Julia / stage 3",
            3, 26, 1, 62, 14,
            "Route8_EventScript_Julia", ObjectEventSafety.SAFE_SIMPLE_DIALOG);

    private PersistentEventStanTeleportProbePreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Persistent Event Probe 6P is intentionally FR1.0-only");

        long copier = rom.stringVar4 + 0x100L;
        long helperAddr = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper stanHelper = new NativeHelper(helperAddr, helperBytes(21, 3, SHANE));
        NativeHelper transformAndRebind = PersistentEventMewRecoveryProbePreset.buildTransformAndRebindHelper(rom, helperAddr);

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("dispatch");
        NativeHelperInstaller.Plan stanPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, stanHelper, copier,
                "persistent_event_probe6p_shared_scene", NativeHelperInstaller.Mode.AUTO);
        NativeHelperInstaller.Plan transformPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, transformAndRebind, copier,
                "persistent_event_probe6p_transform_rebind", NativeHelperInstaller.Mode.AUTO);

        b.label("dispatch")
         .compareVarToValue(0x800F, PersistentEventMewRecoveryProbePreset.MEW_HOST.localId()).vGotoIfEqual("stageMew")
         .compareVarToValue(0x800F, JULIA.localId()).vGotoIfEqual("stageJulia")
         .compareVarToValue(0x800F, SHANE.localId()).vGotoIfEqual("stageShane")
         .setVar(0x8001, STAN.localId()).vCall("sharedMyuu")
         .setObjectXY(0x8001, 1, 1)
         .setVar(VAR_HELPER_MODE, 0);

        stanPlan.installAndCall(b);
        b.delay(30)
         .releaseAll()
         .setVar(VAR_HELPER_MODE, 1)
         .callNative(stanHelper.thumbEntryAddress())
         .end();

        b.label("stageShane")
         .setVar(0x8001, SHANE.localId()).vCall("sharedMyuu")
         .setObjectXY(0x8001, 1, 1)
         // Probe 6P: reuse the already-installed native helper. Only four
         // bytes differ between Stan->Shane and Shane->Julia: destination
         // map/localId and temporary-Mew x/y. mapGroup remains 3.
         .setPtr(JULIA.mapNum(), helperAddr + 86)
         .setPtr(JULIA.localId(), helperAddr + 90)
         .setPtr(SHANE.x(), helperAddr + 116)
         .setPtr(SHANE.y(), helperAddr + 118)
         .setVar(VAR_HELPER_MODE, 0)
         .callNative(stanHelper.thumbEntryAddress())
         .delay(30)
         .releaseAll()
         .setVar(VAR_HELPER_MODE, 1)
         .callNative(stanHelper.thumbEntryAddress())
         .end();

        b.label("sharedMyuu")
         .lockAll().facePlayer()
         .applyMovement(0x8001, commonMovementExclamationMark(rom)).waitMovement(0x8001)
         .vMessage("myuu").waitMessage().waitButtonPress().closeMessage()
         .returnScript();

        b.label("stageJulia")
         .lockAll().facePlayer()
         .vMessage("juliaMsg").waitMessage().waitButtonPress().closeMessage()
         .setObjectXY(PersistentEventMewRecoveryProbePreset.MEW_HOST.localId(), 40, 13)
         .setObjectXYPermanent(PersistentEventMewRecoveryProbePreset.MEW_HOST.localId(), 40, 13)
         .releaseAll();
        transformPlan.installAndCall(b);
        b.end();

        b.label("stageMew")
         .lockAll()
         .applyMovement(PersistentEventMewRecoveryProbePreset.MEW_HOST.localId(), commonMovementExclamationMark(rom))
         .waitMovement(PersistentEventMewRecoveryProbePreset.MEW_HOST.localId())
         .vMessage("myuu").waitMessage().waitButtonPress().closeMessage()
         .releaseAll().end();

        b.text("myuu", "Myuu!");
        b.text("juliaMsg", "I saw a weird Pokemon nearby...");

        byte[] payload = b.buildScript();
        ObjectEventRamScriptBinding binding = new ObjectEventRamScriptBinding(STAN);
        RamScript script = binding.createRamScript(payload);
        return new TriggerBuildResult(script, binding.trigger(), rom, payload.length, 0,
                payload.length, RamScript.SCRIPT_SIZE - payload.length);
    }

    /*
       Derived from the Probe-6C/6J temporary-Mew helper.
       mode 0: hide gSelectedObjectEvent (Stan, already teleported) + spawn Mew
               at Stan's original (21,3).
       mode 1: remove temporary Mew + rebind RamScript to the supplied destination.
    */
    private static long commonMovementExclamationMark(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x081A75DBL;
            case FIRE_RED_EN_11 -> 0x081A7653L;
            case LEAF_GREEN_EN_10 -> 0x081A75B7L;
            case LEAF_GREEN_EN_11 -> 0x081A762FL;
        };
    }

    private static byte[] helperBytes(int mewX, int mewY, ObjectEventTarget destination) {
        byte[] code = new byte[] {
            (byte)0xF0,(byte)0xB5,0x21,0x4C,0x20,(byte)0x88,0x00,0x28,0x12,(byte)0xD1,0x21,0x4C,0x20,0x78,0x01,0x00,
            0x49,0x01,(byte)0x80,0x00,0x09,0x18,0x1F,0x4C,0x64,0x18,0x60,0x78,0x20,0x21,0x08,0x43,
            0x60,0x70,0x13,(byte)0xA0,0x1C,0x4F,0x00,(byte)0xF0,0x1F,(byte)0xF8,0x18,0x4C,0x20,(byte)0x80,(byte)0xF0,(byte)0xBD,
            0x16,0x4C,0x20,(byte)0x88,0x10,0x28,0x08,(byte)0xD2,0x01,0x00,0x49,0x01,(byte)0x80,0x00,0x40,0x18,
            0x14,0x4C,0x00,0x19,0x15,0x4F,0x00,(byte)0xF0,0x10,(byte)0xF8,0x15,0x4C,0x24,0x68,0x15,0x48,
            0x24,0x18,0x00,0x20,0x60,0x71,0x00,0x20,(byte)0xA0,0x71,0x00,0x20,(byte)0xE0,0x71,0x12,0x4F,
            0x00,(byte)0xF0,0x04,(byte)0xF8,0x20,0x60,(byte)0xF0,(byte)0xBD,0x38,0x47,0x38,0x47,0x38,0x47,(byte)0xC0,0x46,
            (byte)0xFA,(byte)0x8C,0x00,0x00,0x15,0x00,0x03,0x00,0x03,0x08,0x00,0x00,0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,(byte)0xB8,0x70,0x03,0x02,(byte)0xBA,0x70,0x03,0x02,
            0x74,0x50,0x00,0x03,0x38,0x6E,0x03,0x02,(byte)0xF5,(byte)0xE7,0x05,0x08,(byte)0xB5,(byte)0xE4,0x05,0x08,
            0x08,0x50,0x00,0x03,0x1C,0x36,0x00,0x00,(byte)0xB1,(byte)0x9C,0x06,0x08
        };
        // Destination RamScript identity immediates (MOVS r0,#imm).
        code[82] = (byte)(destination.mapGroup() & 0xFF);
        code[86] = (byte)(destination.mapNum() & 0xFF);
        code[90] = (byte)(destination.localId() & 0xFF);

        // Embedded temporary ObjectEventTemplate x/y fields.
        code[116] = (byte)(mewX & 0xFF);
        code[117] = (byte)((mewX >>> 8) & 0xFF);
        code[118] = (byte)(mewY & 0xFF);
        code[119] = (byte)((mewY >>> 8) & 0xFF);
        return code;
    }
}
