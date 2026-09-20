final class ObjectEventBindingTest {
    static void run() {
        ObjectEventTarget target = ObjectEventCatalog.LAVENDER_TOWN_WORKER_M;
        if (!target.id().equals("lavender-town-worker-m") || target.mapGroup() != 3 || target.mapNum() != 4 || target.localId() != 2)
            throw new AssertionError("Lavender target registry mismatch");
        if (target.safety() != ObjectEventSafety.SAFE_SIMPLE_DIALOG)
            throw new AssertionError("Lavender worker should be classified as safe simple dialogue");

        ObjectEventTarget oakAide = ObjectEventCatalog.OAKS_LAB_AIDE1_EARLY_INSTALLER;
        if (oakAide.mapGroup() != 4 || oakAide.mapNum() != 3 || oakAide.localId() != 1
                || oakAide.x() != 3 || oakAide.y() != 11)
            throw new AssertionError("Oak Lab early-installer target mismatch");
        if (oakAide.safety() != ObjectEventSafety.SAFE_SIMPLE_DIALOG)
            throw new AssertionError("Oak Lab aide should be classified as safe simple dialogue");

        for (RomProfile rom : RomProfile.values()) {
            RamScript sid = RamScript.rebindObject(ShowSecretIdPreset.build(rom), oakAide);
            if (sid.mapGroup() != oakAide.mapGroup() || sid.mapNum() != oakAide.mapNum() || sid.objectId() != oakAide.localId())
                throw new AssertionError("Oak Lab SID binding mismatch for " + rom);
            if (!sid.isChecksumValid())
                throw new AssertionError("Oak Lab SID checksum mismatch for " + rom);
        }

        for (RomProfile rom : RomProfile.values()) {
            TriggerBuildResult seed = SeedModifierPreset.build(rom, 0x1234);
            RamScript boundSeed = RamScript.rebindObject(seed.ramScript(), oakAide);
            if (boundSeed.mapGroup() != 4 || boundSeed.mapNum() != 3 || boundSeed.objectId() != 1)
                throw new AssertionError("Oak Lab seed installer binding mismatch for " + rom);
            if (!boundSeed.isChecksumValid())
                throw new AssertionError("Oak Lab seed installer checksum mismatch for " + rom);
        }

        for (RomProfile rom : RomProfile.values()) {
            RamScriptBinding binding = new ObjectEventRamScriptBinding(target);
            TriggerBuildResult result = TradeEvolutionPreset.build(rom, binding);
            RamScript rs = result.ramScript();
            if (rs.mapGroup() != target.mapGroup() || rs.mapNum() != target.mapNum() || rs.objectId() != target.localId())
                throw new AssertionError("Generic object binding header mismatch for " + rom);
            if (binding.continuationImmediateOffset() != ObjectEventRamScriptContinuationRuntime.CONTINUATION_IMMEDIATE_OFFSET)
                throw new AssertionError("Generic object continuation patch layout mismatch");
        }
    }
}
