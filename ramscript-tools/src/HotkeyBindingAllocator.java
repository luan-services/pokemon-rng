import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
   Architecture-aware hotkey binding selection for the validated runtime paths.

   This class deliberately does NOT invent a generic IWRAM decoder. It chooses
   only among execution paths that already have in-game validation:

   0 bindings -> no hotkey runtime
   1 binding  -> HotkeyRuntimeV1
   2 compatible bindings -> MultiHotkeyRuntimeV1

   More bindings, duplicate chords, or an incompatible two-binding pair are
   rejected with an explicit diagnostic. That keeps the old simple runtime
   paths first-class instead of silently routing everything through an
   experimental generic dispatcher.
*/
final class HotkeyBindingAllocator {
    private HotkeyBindingAllocator() {}

    static HotkeyBindingPlan plan(List<HotkeyBinding> bindings) {
        if (bindings == null) throw new IllegalArgumentException("bindings must not be null");
        List<HotkeyBinding> copy = List.copyOf(bindings);
        validateUnique(copy);

        if (copy.isEmpty()) {
            return new HotkeyBindingPlan(copy, HotkeyBindingRuntime.NONE, "no hotkey runtime required");
        }

        if (copy.size() == 1) {
            return new HotkeyBindingPlan(
                    copy,
                    HotkeyBindingRuntime.SINGLE_HOTKEY_V1,
                    "uses validated HotkeyRuntimeV1"
            );
        }

        if (copy.size() == 2) {
            Hotkey first = copy.get(0).hotkey();
            Hotkey second = copy.get(1).hotkey();
            if (!isMultiV1Compatible(first, second)) {
                throw new IllegalArgumentException(
                        "two bindings are not encodable by validated MultiHotkeyRuntimeV1: "
                                + first.displayName() + " and " + second.displayName()
                                + "; they must share the held button and use adjacent pressed-button bits"
                );
            }
            return new HotkeyBindingPlan(
                    copy,
                    HotkeyBindingRuntime.MULTI_HOTKEY_V1,
                    "uses validated MultiHotkeyRuntimeV1 compact adjacent-bit decoder"
            );
        }

        throw new IllegalArgumentException(
                "validated hotkey runtimes currently support at most two bindings; requested " + copy.size()
                        + ". A generic 3+ binding decoder is a separate runtime milestone."
        );
    }

    static boolean isMultiV1Compatible(Hotkey first, Hotkey second) {
        if (first == null || second == null || first.equals(second)) return false;
        if (first.heldButton() != second.heldButton()) return false;
        return Math.abs(first.pressedButton().bit() - second.pressedButton().bit()) == 1;
    }

    private static void validateUnique(List<HotkeyBinding> bindings) {
        Set<String> presetIds = new HashSet<>();
        Set<Hotkey> hotkeys = new HashSet<>();
        for (HotkeyBinding binding : bindings) {
            if (binding == null) throw new IllegalArgumentException("binding must not be null");
            if (!presetIds.add(binding.presetId())) {
                throw new IllegalArgumentException("preset has more than one hotkey binding: " + binding.presetId());
            }
            if (!hotkeys.add(binding.hotkey())) {
                throw new IllegalArgumentException("duplicate hotkey binding: " + binding.hotkey().displayName());
            }
        }
    }
}
