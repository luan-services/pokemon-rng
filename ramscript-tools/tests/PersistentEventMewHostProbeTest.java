final class PersistentEventMewHostProbeTest {
    static void run() {
        require(PersistentEventMewHostProbePreset.JULIA.mapGroup() == 3 && PersistentEventMewHostProbePreset.JULIA.mapNum() == 26,
                "Route 8 map id must be 3/26");
        require(PersistentEventMewHostProbePreset.JULIA.localId() == 1, "Julia must be localId 1");
        require(PersistentEventMewHostProbePreset.MEW_HOST.localId() == 11, "second Cut Tree must be localId 11");
        require(PersistentEventMewHostProbePreset.MEW_HOST.x() == 47 && PersistentEventMewHostProbePreset.MEW_HOST.y() == 12,
                "Mew host must be Route 8 Cut Tree B at 47,12");
        byte[] pkg = PersistentEventMewHostProbePreset.buildPackage();
        require(pkg.length > 10 && pkg.length <= 0xFF, "probe3 package must fit one installer copy");
        TriggerBuildResult r = PersistentEventMewHostProbePreset.build(RomProfile.FIRE_RED_EN_10);
        require(r.payloadBytes() <= RamScript.SCRIPT_SIZE, "probe3 must fit RamScript");
        require(r.ramScript().isChecksumValid(), "probe3 initial RamScript checksum invalid");
        require(r.ramScript().mapGroup() == 3 && r.ramScript().mapNum() == 26 && r.ramScript().objectId() == 1,
                "probe3 must initially bind Julia");
    }
    private static void require(boolean c,String m){if(!c)throw new AssertionError(m);}
}
