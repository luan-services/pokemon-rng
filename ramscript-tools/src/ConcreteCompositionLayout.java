import java.util.List;

/* Concrete addresses/bindings selected by the dry-run composition planner. */
record ConcreteCompositionLayout(
        List<ConcretePresetAllocation> allocations,
        HotkeyBindingPlan bindingPlan,
        int nativeCatalogOffset,
        int nativeCatalogSize
) {
    ConcreteCompositionLayout {
        allocations = List.copyOf(allocations);
        if (bindingPlan == null) throw new IllegalArgumentException("binding plan must not be null");
        if (nativeCatalogOffset < -1 || nativeCatalogSize < 0) {
            throw new IllegalArgumentException("invalid native catalog placement");
        }
    }

    boolean hasNativeCatalog() { return nativeCatalogOffset >= 0 && nativeCatalogSize > 0; }
}
