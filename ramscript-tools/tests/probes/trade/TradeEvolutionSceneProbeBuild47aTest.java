final class TradeEvolutionSceneProbeBuild47aTest {
    public static void main(String[] args) {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] h = TradeEvolutionSceneProbe47aHelper.buildAt(rom, rom.stringVar4 + 0x180L).codeCopy();
        if (h.length != 76) throw new AssertionError("helper size");
        // BeginEvolutionScene thunk must be: ldr r4,[pc,#20] ; bx r4.
        if ((h[0x30] & 0xFF) != 0x05 || (h[0x31] & 0xFF) != 0x4C ||
            (h[0x32] & 0xFF) != 0x20 || (h[0x33] & 0xFF) != 0x47)
            throw new AssertionError("Build 47a must preserve r3 in BeginEvolutionScene thunk");
        byte[] p = TradeEvolutionSceneProbe47aPreset.buildPayload(rom);
        if (p.length > RamScript.SCRIPT_SIZE) throw new AssertionError("payload too large: " + p.length);
        System.out.println("Build 47a scene probe payload=" + p.length + " helper=" + h.length);
    }
}
