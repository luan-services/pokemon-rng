import java.util.List;

/* Registry of semantic object-event targets suitable for UI / preset binding.
   Keep raw map/object ids centralized here instead of inside feature presets. */
final class ObjectEventCatalog {
    private ObjectEventCatalog() {}

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

    static List<ObjectEventTarget> all() {
        return List.of(LAVENDER_TOWN_WORKER_M);
    }

    static ObjectEventTarget byId(String id) {
        return all().stream()
                .filter(target -> target.id().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown object-event target: " + id));
    }
}
