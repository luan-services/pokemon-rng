final class PersistentEventPackageProbeTest {
    static void run() {
        byte[] pkg = PersistentEventPackageProbePreset.buildPackage();
        require(pkg.length > 10 && pkg.length <= 0xFF, "probe package must fit one installer copy");
        require(PersistentEventPackageProbePreset.PACKAGE_SB2_OFFSET == 0x0B20, "probe must use validated SB2 base");
        require(PersistentEventPackageProbePreset.TEST_FLAG == 0x4AF, "probe must use remaining unused 0x4A7..0x4AF flag");

        long expectedDelta = (0x02024588L + 0x0B20L) - (0x0202552CL + 0x3624L);
        long expectedTarget = (0x08010000L + expectedDelta) & 0xFFFF_FFFFL;
        require(PersistentEventPackageProbePreset.packageVirtualTarget() == expectedTarget,
                "direct object-RamScript -> SB2 target must use invariant cross-SaveBlock delta");

        TriggerBuildResult r = PersistentEventPackageProbePreset.buildLavenderWorker(RomProfile.FIRE_RED_EN_10);
        require(r.payloadBytes() <= RamScript.SCRIPT_SIZE, "probe installer/gateway must fit RamScript");
        require(r.ramScript().isChecksumValid(), "object-bound RamScript checksum must be valid");
        require(r.ramScript().mapGroup() == ObjectEventCatalog.LAVENDER_TOWN_WORKER_M.mapGroup(), "map group binding mismatch");
        require(r.ramScript().mapNum() == ObjectEventCatalog.LAVENDER_TOWN_WORKER_M.mapNum(), "map binding mismatch");
        require(r.ramScript().objectId() == ObjectEventCatalog.LAVENDER_TOWN_WORKER_M.localId(), "object binding mismatch");
    }

    private static void require(boolean c, String m) { if (!c) throw new AssertionError(m); }
}
