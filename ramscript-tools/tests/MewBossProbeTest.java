final class MewBossProbeTest {
    static void run() {
        int[] moves = MewBossProbeNative.moves();
        require(moves.length == 4, "boss must define four moves");
        require(moves[0] == 94 && moves[1] == 105 && moves[2] == 85 && moves[3] == 58,
                "moves must be Psychic/Recover/Thunderbolt/Ice Beam");

        TriggerBuildResult r=MewBossProbePreset.buildLavenderWorker(RomProfile.FIRE_RED_EN_10);
        System.out.println("P23 payload="+r.payloadBytes()+" limit="+RamScript.SCRIPT_SIZE); require(r.payloadBytes() <= RamScript.SCRIPT_SIZE, "probe must fit RamScript");
        byte[] payload=MewBossProbePreset.buildPayload(RomProfile.FIRE_RED_EN_10);
        require(contains(payload,new byte[]{(byte)0xB6,(byte)0x97,0x00,0x64,0x00,0x00}), "must configure Mew Lv100 wild battle");
        require(!contains(payload,new byte[]{(byte)0xB7}), "Probe 19 must use loss-safe native launcher, not stock dowildbattle opcode");
        require(MewBossBallPocketNative.BACKUP_BYTES == 52, "must preserve all 13 Ball slots");
        require(MewBossBallPocketNative.MAGIC_OFFSET + 4 <= 0x0F00, "backup must stay before manifest tail");
    }
    private static boolean contains(byte[]a,byte[]n){outer:for(int i=0;i<=a.length-n.length;i++){for(int j=0;j<n.length;j++)if(a[i+j]!=n[j])continue outer;return true;}return false;}
    private static void require(boolean c,String m){if(!c)throw new AssertionError(m);}
}
