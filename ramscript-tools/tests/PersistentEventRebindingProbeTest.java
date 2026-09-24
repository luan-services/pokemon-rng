final class PersistentEventRebindingProbeTest {
    static void run() {
        byte[] pkg = PersistentEventRebindingProbePreset.buildPackage();
        require(pkg.length > 10 && pkg.length <= 0xFF, "probe2 package must fit one installer copy");
        require(PersistentEventRebindingProbePreset.PACKAGE_SB2_OFFSET == 0x0B20, "probe2 must use validated SB2 base");
        require(PersistentEventRebindingProbePreset.STAGE_A.localId() != PersistentEventRebindingProbePreset.STAGE_B.localId(),
                "diagnostic dispatch needs distinct local ids");
        long expectedDelta = (0x02024588L + 0x0B20L) - (0x0202552CL + 0x3624L);
        long expectedTarget = (0x08010000L + expectedDelta) & 0xFFFF_FFFFL;
        require(PersistentEventRebindingProbePreset.packageVirtualTarget() == expectedTarget,
                "probe2 direct RamScript -> SB2 target mismatch");
        TriggerBuildResult r = PersistentEventRebindingProbePreset.build(RomProfile.FIRE_RED_EN_10);
        require(r.payloadBytes() <= RamScript.SCRIPT_SIZE, "probe2 must fit RamScript");
        require(r.ramScript().isChecksumValid(), "probe2 initial RamScript checksum invalid");
        require(r.ramScript().mapGroup() == PersistentEventRebindingProbePreset.STAGE_A.mapGroup(), "initial mapGroup mismatch");
        require(r.ramScript().mapNum() == PersistentEventRebindingProbePreset.STAGE_A.mapNum(), "initial mapNum mismatch");
        require(r.ramScript().objectId() == PersistentEventRebindingProbePreset.STAGE_A.localId(), "initial objectId mismatch");
        require(EarlyObjectBoundHotkeyInstaller.calculateRamScriptChecksumThumb(RomProfile.FIRE_RED_EN_10) == 0x08069CB1L,
                "probe2 CRC helper must use symbol-backed FR1.0 stock checksum function");
    }
    private static void require(boolean c, String m) { if (!c) throw new AssertionError(m); }
}
