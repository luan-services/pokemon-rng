import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* Chooses/validates one of the production hotkey runtimes.
   New builds use HotkeyRuntimeV1 for one binding and SharedHotkeyRuntime for
   two or more bindings. MultiHotkeyRuntimeV1 is retained only as an archived,
   explicitly-invoked legacy runtime and is never selected automatically. */
final class HotkeyBindingAllocator {
    private HotkeyBindingAllocator() {}

    static HotkeyBindingPlan plan(List<HotkeyBinding> bindings) {
        List<HotkeyBinding> copy = validate(bindings);
        if (copy.isEmpty()) return new HotkeyBindingPlan(copy, HotkeyBindingRuntime.NONE, "no hotkey runtime required");
        if (copy.size() == 1) {
            return new HotkeyBindingPlan(copy, HotkeyBindingRuntime.SINGLE_HOTKEY_V1, "uses validated HotkeyRuntimeV1");
        }
        return sharedPlan(copy);
    }

    static HotkeyBindingPlan planShared(List<HotkeyBinding> bindings) {
        List<HotkeyBinding> copy = validate(bindings);
        if (copy.isEmpty()) throw new IllegalArgumentException("shared hotkey runtime needs at least one binding");
        return sharedPlan(copy);
    }

    private static HotkeyBindingPlan sharedPlan(List<HotkeyBinding> copy) {
        if (copy.size() > 8) throw new IllegalArgumentException("SharedHotkeyRuntime supports at most eight bindings");
        HotkeyButton modifier = copy.get(0).hotkey().heldButton();
        if (modifier != HotkeyButton.R && modifier != HotkeyButton.L) {
            throw new IllegalArgumentException("SharedHotkeyRuntime modifier must be R or L");
        }
        for (HotkeyBinding binding : copy) {
            Hotkey hotkey = binding.hotkey();
            if (hotkey.heldButton() != modifier) {
                throw new IllegalArgumentException("shared bindings must use the same held modifier");
            }
            if (hotkey.pressedButton().bit() > 7) {
                throw new IllegalArgumentException("shared runtime pressed button must be A/B/SELECT/START/direction");
            }
        }
        return new HotkeyBindingPlan(copy, HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "uses validated SharedHotkeyRuntime generic low-byte dispatcher");
    }

    static boolean isMultiV1Compatible(Hotkey first, Hotkey second) {
        if (first == null || second == null || first.equals(second)) return false;
        if (first.heldButton() != second.heldButton()) return false;
        return Math.abs(first.pressedButton().bit() - second.pressedButton().bit()) == 1;
    }

    private static List<HotkeyBinding> validate(List<HotkeyBinding> bindings) {
        if (bindings == null) throw new IllegalArgumentException("bindings must not be null");
        List<HotkeyBinding> copy = List.copyOf(bindings);
        Set<String> presetIds = new HashSet<>();
        Set<Hotkey> hotkeys = new HashSet<>();
        for (HotkeyBinding binding : copy) {
            if (binding == null) throw new IllegalArgumentException("binding must not be null");
            if (!presetIds.add(binding.presetId())) {
                throw new IllegalArgumentException("preset has more than one hotkey binding: " + binding.presetId());
            }
            if (!hotkeys.add(binding.hotkey())) {
                throw new IllegalArgumentException("duplicate hotkey binding: " + binding.hotkey().displayName());
            }
        }
        return copy;
    }
}
