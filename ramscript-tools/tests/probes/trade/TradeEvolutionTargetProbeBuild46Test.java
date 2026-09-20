final class TradeEvolutionTargetProbeBuild46Test {
    public static void main(String[] args) {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        long addr = rom.stringVar4 + 0x180L;
        byte[] h = TradeEvolutionTargetProbeHelper.buildAt(rom, addr).codeCopy();
        if (h.length != 56) throw new AssertionError("helper size " + h.length);
        check32(h, 0x28, rom.specialVar8004);
        check32(h, 0x2C, rom.playerParty);
        check32(h, 0x30, rom.getEvolutionTargetSpeciesThumb);
        check32(h, 0x34, rom.specialVarResult);
        byte[] payload = TradeEvolutionTargetProbePreset.buildPayload(rom);
        if (payload.length > RamScript.SCRIPT_SIZE) throw new AssertionError("payload overflow " + payload.length);
        System.out.println("Build 46 target probe payload=" + payload.length + " helper=" + h.length);
    }

    private static void check32(byte[] b, int o, long e) {
        long a=(b[o]&255L)|((b[o+1]&255L)<<8)|((b[o+2]&255L)<<16)|((b[o+3]&255L)<<24);
        if (a != (e & 0xFFFFFFFFL)) throw new AssertionError("u32 @"+o+" got="+Long.toHexString(a)+" expected="+Long.toHexString(e));
    }
}
