final class TradeEvolutionSceneProbeBuild47bTest {
    public static void main(String[] args) {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] p = TradeEvolutionSceneProbe47bPreset.buildPayload(rom);
        if (p.length > RamScript.SCRIPT_SIZE)
            throw new AssertionError("payload too large: " + p.length);

        // Build once more through the public trigger path as a smoke test.
        TriggerBuildResult r = TradeEvolutionSceneProbe47bPreset.buildDeliveryman(rom);
        if (!r.ramScript().isChecksumValid())
            throw new AssertionError("invalid RamScript checksum");

        System.out.println("Build 47b scene probe payload=" + p.length);
    }
}
