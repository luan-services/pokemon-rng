final class TradeEvolutionLavenderNpcTest {
    static void run() {
        for (RomProfile rom : RomProfile.values()) {
            TriggerBuildResult r = TradeEvolutionPreset.buildLavenderWorker(rom);
            RamScript rs = r.ramScript();
            ObjectEventTarget target = ObjectEventCatalog.LAVENDER_TOWN_WORKER_M;
            if (rs.mapGroup() != target.mapGroup() || rs.mapNum() != target.mapNum() || rs.objectId() != target.localId())
                throw new AssertionError("Lavender Worker M binding mismatch for " + rom);
            if (!rs.isChecksumValid())
                throw new AssertionError("Object-bound RamScript checksum invalid for " + rom);
            if (r.payloadBytes() > RamScript.SCRIPT_SIZE)
                throw new AssertionError("Object-bound Trade payload too large for " + rom);

            NativeHelper helper = TradeEvolutionRuntimeHelper.buildAt(
                    rom, rom.stringVar4 + 0x180L, 0x20,
                    ObjectEventRamScriptContinuationRuntime.CONTINUATION_IMMEDIATE_OFFSET);
            byte[] code = helper.codeCopy();
            int patchInsn = (code[0x24] & 0xFF) | ((code[0x25] & 0xFF) << 8);
            if (patchInsn != 0x7393)
                throw new AssertionError("Object-bound helper must patch callback +0x0E, got 0x" + Integer.toHexString(patchInsn));
        }
    }
}
