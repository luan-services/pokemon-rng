final class TradeEvolutionBuild43Test {
    public static void main(String[] args) {
        RomProfile rom=RomProfile.FIRE_RED_EN_10;
        long base=rom.stringVar4 + 0x140L;
        byte[] h=TradeEvolutionEventNativeHelper.buildAt(rom,base).codeCopy();

        check16(h,0x04,0x4903);
        check16(h,0x06,0x4B04);
        check16(h,0x0A,0x4804);
        check16(h,0x0E,0x72C1); // strb r1,[r0,#11]

        check32(h,0x14,0x080568E1L);
        check32(h,0x18,0x081277F5L);
        check32(h,0x1C,0x0203B0A0L);

        byte[] payload=TradeEvolutionPreset.buildPayload(rom);
        if (payload.length > RamScript.SCRIPT_SIZE)
            throw new AssertionError("payload overflow: "+payload.length);

        System.out.println("fr10 payload="+payload.length+" helper="+h.length);
        System.out.println("stock ContinueScript selector checks: OK");
    }

    static void check16(byte[] b,int o,int e) {
        int a=(b[o]&255)|((b[o+1]&255)<<8);
        if(a!=e) throw new AssertionError("u16 @"+o+" "+Integer.toHexString(a));
    }
    static void check32(byte[] b,int o,long e) {
        long a=(b[o]&255L)|((b[o+1]&255L)<<8)|((b[o+2]&255L)<<16)|((b[o+3]&255L)<<24);
        if(a!=e) throw new AssertionError("u32 @"+o+" "+Long.toHexString(a));
    }
}
