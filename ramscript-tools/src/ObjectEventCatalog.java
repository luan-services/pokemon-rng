import java.util.List;

/* Registry of semantic object-event targets suitable for UI / preset binding.
   Keep raw map/object ids centralized here instead of inside feature presets. */
final class ObjectEventCatalog {
    private ObjectEventCatalog() {}


    // Early-game installer host in Professor Oak's Lab. This aide is permanent
    // (flag 0) and has a simple dialogue script, so binding a RamScript here
    // does not replace Oak's story-critical interaction during starter selection.
    static final ObjectEventTarget OAKS_LAB_AIDE1_EARLY_INSTALLER = new ObjectEventTarget(
            "oaks-lab-aide1-early-installer", "Professor Oak's Lab — Aide 1 (early installer)",
            4, 3, 1, 3, 11,
            "PalletTown_ProfessorOaksLab_EventScript_Aide1", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget LAVENDER_TOWN_WORKER_M = new ObjectEventTarget(
            "lavender-town-worker-m",
            "Lavender Town — Worker M",
            3,
            4,
            2,
            12,
            12,
            "LavenderTown_EventScript_WorkerM",
            ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget PEWTER_GYM_BROCK = new ObjectEventTarget(
            "pewter-gym-brock", "Pewter Gym — Brock", 6, 2, 1, 6, 5,
            "PewterCity_Gym_EventScript_Brock", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget CERULEAN_GYM_MISTY = new ObjectEventTarget(
            "cerulean-gym-misty", "Cerulean Gym — Misty", 7, 5, 3, 8, 6,
            "CeruleanCity_Gym_EventScript_Misty", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );


    static final ObjectEventTarget VERMILION_GYM_LT_SURGE = new ObjectEventTarget(
            "vermilion-gym-lt-surge", "Vermilion Gym — Lt. Surge", 9, 6, 1, 5, 2,
            "VermilionCity_Gym_EventScript_LtSurge", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget CELADON_GYM_ERIKA = new ObjectEventTarget(
            "celadon-gym-erika", "Celadon Gym — Erika", 10, 16, 7, 6, 4,
            "CeladonCity_Gym_EventScript_Erika", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget FUCHSIA_GYM_KOGA = new ObjectEventTarget(
            "fuchsia-gym-koga", "Fuchsia Gym — Koga", 11, 3, 7, 7, 13,
            "FuchsiaCity_Gym_EventScript_Koga", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget SAFFRON_GYM_SABRINA = new ObjectEventTarget(
            "saffron-gym-sabrina", "Saffron Gym — Sabrina", 14, 3, 7, 14, 11,
            "SaffronCity_Gym_EventScript_Sabrina", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static final ObjectEventTarget CINNABAR_GYM_BLAINE = new ObjectEventTarget(
            "cinnabar-gym-blaine", "Cinnabar Gym — Blaine", 12, 0, 8, 5, 4,
            "CinnabarIsland_Gym_EventScript_Blaine", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    // Vanilla story sets FLAG_HIDE_VIRIDIAN_GIOVANNI, so this target remains metadata-only.
    static final ObjectEventTarget VIRIDIAN_GYM_GIOVANNI = new ObjectEventTarget(
            "viridian-gym-giovanni", "Viridian Gym — Giovanni", 5, 1, 8, 2, 2,
            "ViridianCity_Gym_EventScript_Giovanni", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );


    // Five Island's permanent Fisherman, beside the route toward Five Isle Meadow /
    // Rocket Warehouse. Production Giovanni reuses this safe dialog object as its
    // RamScript host and swaps the live sprite under a fade on interaction.
    static final ObjectEventTarget FIVE_ISLAND_FISHER_GIOVANNI_HOST = new ObjectEventTarget(
            "five-island-fisher-giovanni-host", "Five Island — Fisher (Giovanni host)",
            3, 16, 1, 8, 5,
            "FiveIsland_EventScript_Fisher", ObjectEventSafety.SAFE_SIMPLE_DIALOG
    );

    static List<ObjectEventTarget> all() {
        return List.of(OAKS_LAB_AIDE1_EARLY_INSTALLER, LAVENDER_TOWN_WORKER_M, PEWTER_GYM_BROCK, CERULEAN_GYM_MISTY,
                VERMILION_GYM_LT_SURGE, CELADON_GYM_ERIKA, FUCHSIA_GYM_KOGA,
                SAFFRON_GYM_SABRINA, CINNABAR_GYM_BLAINE, VIRIDIAN_GYM_GIOVANNI,
                FIVE_ISLAND_FISHER_GIOVANNI_HOST);
    }

    static ObjectEventTarget byId(String id) {
        return all().stream()
                .filter(target -> target.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown object-event target: " + id));
    }
}
