import java.util.List;

final class CustomTrainerGymLeaderPresetsTest {
    private CustomTrainerGymLeaderPresetsTest() {}

    static void run() {
        List<Case> cases = List.of(
                new Case("Brock", CustomTrainerGymLeaderPresets.brock(), ObjectEventCatalog.PEWTER_GYM_BROCK, CustomTrainerVanillaScripts::brock),
                new Case("Misty", CustomTrainerGymLeaderPresets.misty(), ObjectEventCatalog.CERULEAN_GYM_MISTY, CustomTrainerVanillaScripts::misty),
                new Case("Lt. Surge", CustomTrainerGymLeaderPresets.ltSurge(), ObjectEventCatalog.VERMILION_GYM_LT_SURGE, CustomTrainerVanillaScripts::ltSurge),
                new Case("Erika", CustomTrainerGymLeaderPresets.erika(), ObjectEventCatalog.CELADON_GYM_ERIKA, CustomTrainerVanillaScripts::erika),
                new Case("Koga", CustomTrainerGymLeaderPresets.koga(), ObjectEventCatalog.FUCHSIA_GYM_KOGA, CustomTrainerVanillaScripts::koga),
                new Case("Sabrina", CustomTrainerGymLeaderPresets.sabrina(), ObjectEventCatalog.SAFFRON_GYM_SABRINA, CustomTrainerVanillaScripts::sabrina),
                new Case("Blaine", CustomTrainerGymLeaderPresets.blaine(), ObjectEventCatalog.CINNABAR_GYM_BLAINE, CustomTrainerVanillaScripts::blaine)
        );

        for (Case c : cases) {
            check(c.spec.party().size() == 6, c.name + ": production Gym Leader party must contain six Pokemon");
            byte[] fieldText = CustomTrainerFieldTextStorage.encode(c.spec);
            check(fieldText.length <= CustomTrainerFieldTextStorage.CAPACITY,
                    c.name + ": field text exceeds persistent SB1 capacity");
            check(Gen3TextCodec.encodeString(c.spec.defeatText()).length <= CustomTrainerBattleDescriptor.DEFEAT_TEXT_CAPACITY,
                    c.name + ": defeat text exceeds fixed CTD1 capacity");

            byte[] descriptor = CustomTrainerBattleDescriptor.encode(c.spec, c.target.localId(), 0x1234);
            byte[] compact = CustomTrainerCompactTransport.encode(c.spec, c.target.localId(), 0x1234);
            check(java.util.Arrays.equals(descriptor, CustomTrainerCompactTransport.expandForTest(compact)),
                    c.name + ": compact stream must expand byte-for-byte to CTD1");

            for (RomProfile rom : RomProfile.values()) {
                TriggerBuildResult card = CustomTrainerBattleSharedRuntimePreset.buildGymLeader(
                        rom, c.target, c.spec, c.vanilla.address(rom));
                check(card.payloadBytes() <= RamScript.SCRIPT_SIZE,
                        rom.id() + "/" + c.name + ": production preset exceeds 995-byte RamScript capacity: " + card.payloadBytes());
                check(card.ramScript().mapGroup() == c.target.mapGroup()
                                && card.ramScript().mapNum() == c.target.mapNum()
                                && card.ramScript().objectId() == c.target.localId(),
                        rom.id() + "/" + c.name + ": object binding drifted");
            }
        }

        CustomTrainerBattleSpec giovanni = CustomTrainerGymLeaderPresets.giovanni();
        ObjectEventTarget giovanniHost = ObjectEventCatalog.FIVE_ISLAND_FISHER_GIOVANNI_HOST;
        check(giovanni.party().size() == 6, "Giovanni: production party must contain six Pokemon");
        byte[] gioDescriptor = CustomTrainerBattleDescriptor.encode(giovanni, giovanniHost.localId(), 0x1234);
        byte[] gioCompact = CustomTrainerCompactTransport.encode(giovanni, giovanniHost.localId(), 0x1234);
        check(java.util.Arrays.equals(gioDescriptor, CustomTrainerCompactTransport.expandForTest(gioCompact)),
                "Giovanni: compact stream must expand byte-for-byte to CTD1");
        for (RomProfile rom : RomProfile.values()) {
            TriggerBuildResult card = CustomTrainerBattleSharedRuntimePreset.buildGiovanniFiveIsland(rom, giovanniHost, giovanni);
            check(card.payloadBytes() == 985,
                    rom.id() + "/Giovanni: Five Island production card size drifted: " + card.payloadBytes());
            check(card.ramScript().mapGroup() == 3 && card.ramScript().mapNum() == 16 && card.ramScript().objectId() == 1,
                    rom.id() + "/Giovanni: Five Island Fisher host binding drifted");
        }

        check(find(CustomTrainerGymLeaderPresets.misty(), "AZUMARILL").abilityNum() == 1,
                "Misty Azumarill must use Huge Power ability slot");
        check(find(CustomTrainerGymLeaderPresets.giovanni(), "DUGTRIO").abilityNum() == 1,
                "Giovanni Dugtrio must use Arena Trap ability slot");
        check(find(CustomTrainerGymLeaderPresets.sabrina(), "GARDEVOIR").abilityNum() == 1,
                "Sabrina Gardevoir must use Trace ability slot");
    }

    private static EReaderTrainerData.Mon find(CustomTrainerBattleSpec spec, String nickname) {
        return spec.party().stream().filter(m -> nickname.equals(m.nickname())).findFirst().orElseThrow();
    }

    private record Case(String name, CustomTrainerBattleSpec spec, ObjectEventTarget target, Vanilla vanilla) {}
    @FunctionalInterface private interface Vanilla { long address(RomProfile rom); }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
