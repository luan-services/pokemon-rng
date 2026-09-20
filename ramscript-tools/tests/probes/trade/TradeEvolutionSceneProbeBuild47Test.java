final class TradeEvolutionSceneProbeBuild47Test {
    public static void main(String[] args) {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] h = TradeEvolutionSceneProbeHelper.buildAt(rom, rom.stringVar4 + 0x180L).codeCopy();
        if (h.length != 76) throw new AssertionError("helper size");
        byte[] p = TradeEvolutionSceneProbePreset.buildPayload(rom);
        if (p.length > RamScript.SCRIPT_SIZE) throw new AssertionError("payload too large: " + p.length);
        System.out.println("Build 47 scene probe payload=" + p.length + " helper=" + h.length);
    }
}
