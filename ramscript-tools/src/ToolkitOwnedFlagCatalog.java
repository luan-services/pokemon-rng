import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* UI-facing catalog of persistent progress flags authored by this toolkit.
   The Cleaner consumes IDs from this catalog instead of knowing feature-specific
   flag numbers itself. Add future event flags here (Mew event, custom quests, etc.). */
final class ToolkitOwnedFlagCatalog {
    private static final List<ToolkitOwnedFlag> FLAGS = List.of(
            flag("GYM_BROCK_COMPLETED", "Brock rematch completed", CustomTrainerCompletionFlag.BROCK, "gym-leader-rematches"),
            flag("GYM_MISTY_COMPLETED", "Misty rematch completed", CustomTrainerCompletionFlag.MISTY, "gym-leader-rematches"),
            flag("GYM_LT_SURGE_COMPLETED", "Lt. Surge rematch completed", CustomTrainerCompletionFlag.LT_SURGE, "gym-leader-rematches"),
            flag("GYM_ERIKA_COMPLETED", "Erika rematch completed", CustomTrainerCompletionFlag.ERIKA, "gym-leader-rematches"),
            flag("GYM_KOGA_COMPLETED", "Koga rematch completed", CustomTrainerCompletionFlag.KOGA, "gym-leader-rematches"),
            flag("GYM_SABRINA_COMPLETED", "Sabrina rematch completed", CustomTrainerCompletionFlag.SABRINA, "gym-leader-rematches"),
            flag("GYM_BLAINE_COMPLETED", "Blaine rematch completed", CustomTrainerCompletionFlag.BLAINE, "gym-leader-rematches"),
            flag("GYM_GIOVANNI_COMPLETED", "Giovanni rematch completed", CustomTrainerCompletionFlag.GIOVANNI, "gym-leader-rematches")
    );
    private static final Map<String, ToolkitOwnedFlag> BY_ID = index();

    private ToolkitOwnedFlagCatalog() {}

    static List<ToolkitOwnedFlag> all() {
        return FLAGS;
    }

    static ToolkitOwnedFlag require(String id) {
        ToolkitOwnedFlag flag = BY_ID.get(normalize(id));
        if (flag == null) throw new IllegalArgumentException("Unknown toolkit flag id: " + id);
        return flag;
    }

    static String normalize(String id) {
        return id == null ? "" : id.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private static ToolkitOwnedFlag flag(String id, String displayName, CustomTrainerCompletionFlag flag, String owner) {
        return new ToolkitOwnedFlag(id, displayName, flag.eventFlag(), owner);
    }

    private static Map<String, ToolkitOwnedFlag> index() {
        Map<String, ToolkitOwnedFlag> out = new LinkedHashMap<>();
        for (ToolkitOwnedFlag flag : FLAGS) {
            String key = normalize(flag.id());
            if (out.put(key, flag) != null) throw new IllegalStateException("duplicate toolkit flag id: " + flag.id());
        }
        return Map.copyOf(out);
    }
}
