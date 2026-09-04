
/*
   Build 44 diagnostic harness.

   Purpose: prove only:
       RamScript -> stock FireRed party-selection screen -> RamScript

   Uses ChooseHalfPartyForBattle because FireRed itself uses:
       fadescreen FADE_TO_BLACK
       special ChooseHalfPartyForBattle
       waitstate
       ...continue script...

   The FireRed callback CB2_ReturnFromChooseHalfParty sets VAR_RESULT and then
   switches to CB2_ReturnToFieldContinueScriptPlayMapMusic.

   No native helper, no installer, no custom party-menu code, no post-menu
   relocatable string.
*/
final class PartySelectorHarnessPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;

    // From FireRed's special table / vanilla scripts.
    private static final int SPECIAL_CHOOSE_HALF_PARTY_FOR_BATTLE = 0x0029;
    private static final int VAR_RESULT = 0x800D;

    // Deliberately visible scratch result for save/RAM inspection if desired.
    // VAR_0x8005 is already a standard temporary special variable.
    private static final int VAR_PROBE = 0x8005;
    private static final int PROBE_BEFORE = 0x4401;
    private static final int PROBE_AFTER  = 0x4402;

    private PartySelectorHarnessPreset() {}

    static TriggerBuildResult buildDeliveryman(RomProfile rom) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom));
    }

    static byte[] buildPayload(RomProfile rom) {
        if (rom != RomProfile.FIRE_RED_EN_10)
            throw new IllegalArgumentException("Build 44 selector harness is FR1.0-only");

        RamScriptBuilder b = new RamScriptBuilder(VIRTUAL_BASE);

        // No setvaddress is needed: there are no relocatable pointers/strings.
        // This intentionally mirrors the vanilla cable-club script pattern.
        b.setVar(VAR_PROBE, PROBE_BEFORE)
         .fadeScreen(1)
         .special(SPECIAL_CHOOSE_HALF_PARTY_FOR_BATTLE)
         .waitState()

         // Reaching this instruction proves ScriptContext resumed.
         .setVar(VAR_PROBE, PROBE_AFTER)

         // Copy the stock success/cancel result to another temp var so both
         // facts remain inspectable without invoking any message system.
         .copyVar(0x8006, VAR_RESULT)
         .end();

        return b.buildScript();
    }
}
