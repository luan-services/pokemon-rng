final class TradeEvolutionSceneProbeBuild47cTest {
    public static void main(String[] args) {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] p = TradeEvolutionSceneProbe47cPreset.buildPayload(rom);
        if (p.length > RamScript.SCRIPT_SIZE)
            throw new AssertionError("payload too large: " + p.length);
        int setvCount = 0;
        for (byte b : p) if ((b & 0xFF) == 0xB8) setvCount++;
        if (setvCount < 3)
            throw new AssertionError("expected initial + two continuation setvaddress opcodes, got " + setvCount);
        TriggerBuildResult r = TradeEvolutionSceneProbe47cPreset.buildDeliveryman(rom);
        if (!r.ramScript().isChecksumValid())
            throw new AssertionError("invalid RamScript checksum");
        System.out.println("Build 47c scene probe payload=" + p.length + " setvaddress-count=" + setvCount);
    }
}
