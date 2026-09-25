final class PersistentEventMewRecoveryProbeTest {
    static void run() {
        require(PersistentEventMewRecoveryProbePreset.JULIA.localId() == 1, "Julia must be localId 1");
        require(PersistentEventMewRecoveryProbePreset.MEW_HOST.localId() == 11, "Cut Tree B must be localId 11");
        byte[] pkg = PersistentEventMewRecoveryProbePreset.buildPackage(RomProfile.FIRE_RED_EN_10);
        require(pkg.length > 10 && pkg.length <= PayloadStorageArea.SAVE_BLOCK2.capacity(), "probe5 package must fit SB2 persistent area");
        TriggerBuildResult r = PersistentEventMewRecoveryProbePreset.build(RomProfile.FIRE_RED_EN_10);
        require(r.payloadBytes() <= RamScript.SCRIPT_SIZE, "probe5 must fit RamScript");
        require(r.ramScript().isChecksumValid(), "probe5 initial RamScript checksum invalid");
        require(r.ramScript().objectId() == 1, "probe5 must initially bind Julia");
    }
    private static void require(boolean c,String m){if(!c)throw new AssertionError(m);}
}
