import java.util.List;

record HotkeyBindingPlan(
        List<HotkeyBinding> bindings,
        HotkeyBindingRuntime runtime,
        String diagnostic
) {
    HotkeyBindingPlan {
        if (bindings == null) throw new IllegalArgumentException("bindings must not be null");
        bindings = List.copyOf(bindings);
        if (runtime == null) throw new IllegalArgumentException("runtime must not be null");
        if (diagnostic == null) diagnostic = "";
    }
}
