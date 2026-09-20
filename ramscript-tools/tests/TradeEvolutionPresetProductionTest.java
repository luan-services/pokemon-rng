final class TradeEvolutionPresetProductionTest {
    static void run() {
        for (RomProfile rom : RomProfile.values()) {
            byte[] payload = TradeEvolutionPreset.buildPayload(rom);
            if (payload.length <= 0 || payload.length > RamScript.SCRIPT_SIZE)
                throw new AssertionError("Trade Evolution payload must fit on " + rom.displayName() + ": " + payload.length);

            byte[] launcher = TradeEvolutionContinuationRuntime.launcher(rom);
            assertU32(launcher, 0x14, rom.choosePartyMonByMenuTypeThumb, "ChoosePartyMonByMenuType");
            assertU32(launcher, 0x18, rom.fieldCallback2, "gFieldCallback2");

            NativeHelper helper = TradeEvolutionRuntimeHelper.buildAt(rom, rom.stringVar4 + 0x180L, 0x20);
            byte[] code = helper.codeCopy();
            assertU32(code, 0x54, rom.fieldCallback2, "helper gFieldCallback2");
            assertU32(code, 0x5C, rom.cb2ReturnToFieldThumb, "CB2_ReturnToField");
            assertU32(code, 0x60, rom.getEvolutionTargetSpeciesThumb, "GetEvolutionTargetSpecies");
            assertU32(code, 0x64, rom.beginEvolutionSceneThumb, "BeginEvolutionScene");
            assertU16(code, 0x24, 0x7293, "delivery callback patch must target +0x0A");
        }

        PresetDefinition preset = PresetCatalog.byId("trade-evolution");
        if (preset.selectionPolicy() != PresetSelectionPolicy.EXCLUSIVE)
            throw new AssertionError("Trade Evolution must expose EXCLUSIVE UI selection policy for now");
        if (!preset.exclusiveSelection()) throw new AssertionError("Trade Evolution must be UI-exclusive for now");
        if (preset.validationStatus(PresetUsageMode.DELIVERYMAN, RomProfile.FIRE_RED_EN_10)
                != PresetValidationStatus.VALIDATED_IN_GAME)
            throw new AssertionError("FR1.0 production Trade Evolution must be marked VALIDATED_IN_GAME");
        if (preset.validationStatus(PresetUsageMode.OBJECT_EVENT, RomProfile.FIRE_RED_EN_10)
                != PresetValidationStatus.VALIDATED_IN_GAME)
            throw new AssertionError("FR1.0 object-bound Trade Evolution must be marked VALIDATED_IN_GAME");
        if (preset.validationStatus(PresetUsageMode.OBJECT_EVENT, RomProfile.FIRE_RED_EN_11)
                != PresetValidationStatus.SUPPORTED_NOT_TESTED)
            throw new AssertionError("FR1.1 object-bound Trade Evolution must remain supported/not-tested");

        byte[] fr10Payload = TradeEvolutionPreset.buildPayload(RomProfile.FIRE_RED_EN_10);
        assertContains(fr10Payload, Gen3TextCodec.encodeString("Would you lend me your\\nPOKéMON for a while?"), "ask text");
        assertContains(fr10Payload, Gen3TextCodec.encodeString("Okay. Maybe another time..."), "cancel text");
        assertContains(fr10Payload, Gen3TextCodec.encodeString("It doesn't seem special at all."), "no-evolution text");
        assertContains(fr10Payload, Gen3TextCodec.encodeString("Woah! Looks like something happened."), "success text");
        if (!preset.supportsDeployment(PresetDeploymentKind.DEDICATED_LOCAL))
            throw new AssertionError("Trade Evolution must expose dedicated deployment");

        PresetCompositionPlan single = PresetCompositionPlanner.planDeliveryman(
                RomProfile.FIRE_RED_EN_10, "trade-evolution");
        if (single.selections().get(0).deployment().kind() != PresetDeploymentKind.DEDICATED_LOCAL)
            throw new AssertionError("single Trade Evolution plan must choose dedicated deployment");

        boolean rejected = false;
        try {
            PresetCompositionPlanner.planSelections(RomProfile.FIRE_RED_EN_10,
                    java.util.List.of(
                            PresetSelection.deliveryman("trade-evolution"),
                            PresetSelection.deliveryman("repel")
                    ));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        if (!rejected) throw new AssertionError("Trade Evolution must reject multi-preset composition for now");
    }

    private static void assertContains(byte[] haystack, byte[] needle, String label) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return;
        }
        throw new AssertionError(label + " not found in production payload");
    }

    private static void assertU16(byte[] d, int o, int expected, String label) {
        int actual = (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
        if (actual != (expected & 0xFFFF))
            throw new AssertionError(label + " mismatch: expected 0x" + Integer.toHexString(expected) + " got 0x" + Integer.toHexString(actual));
    }

    private static void assertU32(byte[] d, int o, long expected, String label) {
        long actual = (d[o] & 0xFFL) | ((d[o+1] & 0xFFL) << 8) | ((d[o+2] & 0xFFL) << 16) | ((d[o+3] & 0xFFL) << 24);
        if (actual != (expected & 0xFFFFFFFFL))
            throw new AssertionError(label + " mismatch: expected 0x" + Long.toHexString(expected) + " got 0x" + Long.toHexString(actual));
    }
}
