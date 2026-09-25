final class PersistentEventMewMoveProbeTest {
    static void run() {
        require(PersistentEventMewMoveProbePreset.JULIA.mapGroup() == 3 && PersistentEventMewMoveProbePreset.JULIA.mapNum() == 26, "Route 8 map id must be 3/26");
        require(PersistentEventMewMoveProbePreset.JULIA.localId() == 1, "Julia must be localId 1");
        require(PersistentEventMewMoveProbePreset.MEW_HOST.localId() == 11, "second Cut Tree must be localId 11");
        require(PersistentEventMewMoveProbePreset.MEW_TARGET_X == 47 && PersistentEventMewMoveProbePreset.MEW_TARGET_Y == 8, "probe4 target must be 47,8");
        byte[] pkg = PersistentEventMewMoveProbePreset.buildPackage();
        require(pkg.length > 10 && pkg.length <= 0xFF, "probe4 package must fit one installer copy");
        TriggerBuildResult r = PersistentEventMewMoveProbePreset.build(RomProfile.FIRE_RED_EN_10);
        require(r.payloadBytes() <= RamScript.SCRIPT_SIZE, "probe4 must fit RamScript");
        require(r.ramScript().isChecksumValid(), "probe4 initial RamScript checksum invalid");
        require(r.ramScript().mapGroup() == 3 && r.ramScript().mapNum() == 26 && r.ramScript().objectId() == 1, "probe4 must initially bind Julia");
    }
    private static void require(boolean c,String m){if(!c)throw new AssertionError(m);}
}
