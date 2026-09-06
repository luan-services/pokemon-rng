import java.util.List;

/* Structured planner rejection that machine-facing clients can surface
   without parsing human prose. */
final class CompositionPlanningException extends IllegalArgumentException {
    private final String code;
    private final List<String> affectedPresetIds;
    private final String field;

    CompositionPlanningException(String code, String message, List<String> affectedPresetIds) {
        this(code, message, affectedPresetIds, null);
    }

    CompositionPlanningException(String code, String message, List<String> affectedPresetIds, String field) {
        super(message);
        if (code == null || code.isBlank()) throw new IllegalArgumentException("planning diagnostic code must not be blank");
        this.code = code;
        this.affectedPresetIds = List.copyOf(affectedPresetIds == null ? List.of() : affectedPresetIds);
        this.field = field;
    }

    String code() { return code; }
    List<String> affectedPresetIds() { return affectedPresetIds; }
    String field() { return field; }
}
