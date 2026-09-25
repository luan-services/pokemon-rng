/*
   Persistent Event Framework — Probe 6M.

   GAME-VALIDATED Probe 6J established the safe visual primitive: teleport the
   active host away with setobjectxy, then spawn/remove a separate temporary Mew.

   Probe 6M applies that primitive back to Stan and adds the missing exclamation
   mark plus a deferred stage transfer to Route 24 Shane (corrected mapNum 43, localId 7).
   Stan's graphics are never mutated and Stan is never removed while active.
*/
final class PersistentEventStanTeleportProbePreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int VAR_HELPER_MODE = 0x8000;

    static final ObjectEventTarget STAN = new ObjectEventTarget(
            "route8-stan-probe6m", "Route 8 — Stan / stage 1",
            3, 26, 8, 21, 3,
            "Route8_EventScript_Stan", ObjectEventSafety.SAFE_SIMPLE_DIALOG);

    static final ObjectEventTarget SHANE = new ObjectEventTarget(
            "route24-shane-probe6m", "Route 24 — Camper Shane / stage 2",
            3, 43, 7, 5, 21,
            "Route24_EventScript_Shane", ObjectEventSafety.SAFE_SIMPLE_DIALOG);

    private PersistentEventStanTeleportProbePreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Persistent Event Probe 6M is intentionally FR1.0-only");

        long copier = rom.stringVar4 + 0x100L;
        long helperAddr = CpuSetNativeHelperInstaller.helperDestination(copier);
        NativeHelper stanHelper = new NativeHelper(helperAddr, helperBytes(21, 3));
        NativeHelper shaneHelper = new NativeHelper(helperAddr, helperBytes(5, 21));

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);
        b.setVAddress().vGoto("dispatch");
        NativeHelperInstaller.Plan stanPlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, stanHelper, copier,
                "persistent_event_probe6m_stan_scene", NativeHelperInstaller.Mode.AUTO);
        NativeHelperInstaller.Plan shanePlan = NativeHelperInstaller.prepare(
                b, VIRTUAL_BASE, shaneHelper, copier,
                "persistent_event_probe6m_shane_scene", NativeHelperInstaller.Mode.AUTO);

        b.label("dispatch")
         .compareVarToValue(0x800F, SHANE.localId()).vGotoIfEqual("stageShane")
         .lockAll().facePlayer()
         .applyMovement(STAN.localId(), commonMovementExclamationMark(rom)).waitMovement(STAN.localId())
         .vMessage("myuu").waitMessage().waitButtonPress().closeMessage()
         .setObjectXY(STAN.localId(), 1, 1)
         .setVar(VAR_HELPER_MODE, 0);

        stanPlan.installAndCall(b);
        b.delay(30)
         .releaseAll()
         .setVar(VAR_HELPER_MODE, 1)
         .callNative(stanHelper.thumbEntryAddress())
         .end();

        b.label("stageShane")
         .lockAll().facePlayer()
         .applyMovement(SHANE.localId(), commonMovementExclamationMark(rom)).waitMovement(SHANE.localId())
         .vMessage("myuu").waitMessage().waitButtonPress().closeMessage()
         .setObjectXY(SHANE.localId(), 1, 1)
         .setVar(VAR_HELPER_MODE, 0);

        shanePlan.installAndCall(b);
        b.delay(30)
         .releaseAll()
         .setVar(VAR_HELPER_MODE, 1)
         .callNative(shaneHelper.thumbEntryAddress())
         .end();

        b.text("myuu", "Myuu!");

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
       mode 1: remove temporary Mew + rebind RamScript to Shane (3,43,7).
    */
    private static long commonMovementExclamationMark(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10 -> 0x081A75DBL;
            case FIRE_RED_EN_11 -> 0x081A7653L;
            case LEAF_GREEN_EN_10 -> 0x081A75B7L;
            case LEAF_GREEN_EN_11 -> 0x081A762FL;
        };
    }

    private static byte[] helperBytes(int mewX, int mewY) {
        byte[] code = new byte[] {
            (byte)0xF0,(byte)0xB5,0x21,0x4C,0x20,(byte)0x88,0x00,0x28,0x12,(byte)0xD1,0x21,0x4C,0x20,0x78,0x01,0x00,
            0x49,0x01,(byte)0x80,0x00,0x09,0x18,0x1F,0x4C,0x64,0x18,0x60,0x78,0x20,0x21,0x08,0x43,
            0x60,0x70,0x13,(byte)0xA0,0x1C,0x4F,0x00,(byte)0xF0,0x1F,(byte)0xF8,0x18,0x4C,0x20,(byte)0x80,(byte)0xF0,(byte)0xBD,
            0x16,0x4C,0x20,(byte)0x88,0x10,0x28,0x08,(byte)0xD2,0x01,0x00,0x49,0x01,(byte)0x80,0x00,0x40,0x18,
            0x14,0x4C,0x00,0x19,0x15,0x4F,0x00,(byte)0xF0,0x10,(byte)0xF8,0x15,0x4C,0x24,0x68,0x15,0x48,
            0x24,0x18,0x03,0x20,0x60,0x71,0x2B,0x20,(byte)0xA0,0x71,0x07,0x20,(byte)0xE0,0x71,0x12,0x4F,
            0x00,(byte)0xF0,0x04,(byte)0xF8,0x20,0x60,(byte)0xF0,(byte)0xBD,0x38,0x47,0x38,0x47,0x38,0x47,(byte)0xC0,0x46,
            (byte)0xFA,(byte)0x8C,0x00,0x00,0x15,0x00,0x03,0x00,0x03,0x08,0x00,0x00,0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,(byte)0xB8,0x70,0x03,0x02,(byte)0xBA,0x70,0x03,0x02,
            0x74,0x50,0x00,0x03,0x38,0x6E,0x03,0x02,(byte)0xF5,(byte)0xE7,0x05,0x08,(byte)0xB5,(byte)0xE4,0x05,0x08,
            0x08,0x50,0x00,0x03,0x1C,0x36,0x00,0x00,(byte)0xB1,(byte)0x9C,0x06,0x08
        };
        // Embedded temporary ObjectEventTemplate x/y fields.
        code[116] = (byte)(mewX & 0xFF);
        code[117] = (byte)((mewX >>> 8) & 0xFF);
        code[118] = (byte)(mewY & 0xFF);
        code[119] = (byte)((mewY >>> 8) & 0xFF);
        return code;
    }
}
