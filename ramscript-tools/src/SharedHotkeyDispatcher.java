import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
   Field Script dispatcher used by the shared N-hotkey runtime candidate.

   The resident callback stores the low-byte newKeysRaw mask in gSaveUnusedVar.
   This script runs later through ScriptContext_SetupScript, so the N-way fan-out
   is not executed inside callback1.

   The dispatcher begins with setvaddress every time. This is required because
   persistent SB1 gateways also execute setvaddress, changing the global
   sAddressOffset. Resetting it here makes repeated dispatch deterministic.
*/
final class SharedHotkeyDispatcher {
    static final long SELECTED_KEY_BYTE = 0x0300539CL; // low byte of gSaveUnusedVar
    static final int LOCAL_INDEX = 0;

    record Entry(HotkeyButton pressedButton, int targetDeltaFromRamScriptBase) {
        Entry {
            if (pressedButton == null) throw new IllegalArgumentException("pressedButton must not be null");
            if (pressedButton.bit() < 0 || pressedButton.bit() > 7) {
                throw new IllegalArgumentException(
                        "shared runtime candidate only dispatches A/B/SELECT/START/directions"
                );
            }
        }

        int keyMask() {
            return 1 << pressedButton.bit();
        }
    }

    private SharedHotkeyDispatcher() {}

    static byte[] build(List<Entry> entries) {
        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("shared hotkey dispatcher needs at least one entry");
        }
        if (entries.size() > 8) {
            throw new IllegalArgumentException("shared hotkey dispatcher supports at most 8 pressed buttons");
        }

        Set<HotkeyButton> seen = new HashSet<>();
        for (Entry entry : entries) {
            if (!seen.add(entry.pressedButton())) {
                throw new IllegalArgumentException("duplicate shared hotkey button: " + entry.pressedButton());
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Re-establish RamScript relocation on every dispatch. Persistent
        // gateways change sAddressOffset, so relying on the install-time value
        // would break the second persistent hotkey execution.
        u8(out, 0xB8); // setvaddress
        u32(out, HotkeyRuntimeV1.VIRTUAL_BASE + HotkeyRuntimeV1.PAYLOAD_OFFSET);

        // loadbytefromptr local0, gSaveUnusedVar
        u8(out, 0x12);
        u8(out, LOCAL_INDEX);
        u32(out, SELECTED_KEY_BYTE);

        for (Entry entry : entries) {
            // compare_local_to_value local0, one-hot key mask
            u8(out, 0x1C);
            u8(out, LOCAL_INDEX);
            u8(out, entry.keyMask());

            // vgoto_if EQ, virtual target
            u8(out, 0xBB);
            u8(out, 1); // comparison condition '='
            u32(out, HotkeyRuntimeV1.VIRTUAL_BASE + entry.targetDeltaFromRamScriptBase());
        }

        // Unknown / simultaneous low-byte key masks are ignored safely.
        u8(out, 0x02); // end

        return out.toByteArray();
    }

    static int sizeForBindings(int bindingCount) {
        if (bindingCount < 1 || bindingCount > 8) {
            throw new IllegalArgumentException("bindingCount must be 1..8");
        }
        return 12 + 9 * bindingCount;
    }

    private static void u8(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
    }

    private static void u32(ByteArrayOutputStream out, long value) {
        out.write((int)value & 0xFF);
        out.write((int)(value >>> 8) & 0xFF);
        out.write((int)(value >>> 16) & 0xFF);
        out.write((int)(value >>> 24) & 0xFF);
    }
}
