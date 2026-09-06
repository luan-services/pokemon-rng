import java.util.List;

/* Concrete ownership view returned by the planner for fixed/session resources. */
record PresetResourceAllocation(
        PresetOwnedResource resource,
        List<String> ownerPresetIds
) {
    PresetResourceAllocation {
        if (resource == null) throw new IllegalArgumentException("resource must not be null");
        ownerPresetIds = List.copyOf(ownerPresetIds);
        if (ownerPresetIds.isEmpty()) throw new IllegalArgumentException("resource allocation needs an owner");
    }
}
