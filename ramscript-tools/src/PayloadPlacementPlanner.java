/* Build-time placement policy.

   The planner deliberately knows only byte budgets. It does not know whether a
   preset needs a hotkey runtime, helper installer, or other RamScript overhead.
   Callers pass the number of RamScript bytes currently available after such
   infrastructure is accounted for.

   AUTO preference:
     1. RamScript, when the payload fits the caller's remaining script budget.
     2. SaveBlock1, for a small persistent module.
     3. SaveBlock2, for a larger persistent module.
*/
final class PayloadPlacementPlanner {
    private PayloadPlacementPlanner() {}

    static PlacedPayload place(byte[] payload, PayloadPlacement requested, int availableRamScriptBytes) {
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("payload must not be empty");
        if (requested == null) throw new IllegalArgumentException("requested placement must not be null");
        if (availableRamScriptBytes < 0 || availableRamScriptBytes > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("invalid RamScript byte budget");
        }

        if (requested != PayloadPlacement.AUTO) {
            PayloadStorageArea area = requested.area();
            int available = area == PayloadStorageArea.RAMSCRIPT ? availableRamScriptBytes : area.capacity();
            if (payload.length > available) {
                throw new IllegalArgumentException("payload does not fit requested " + requested + ": " + payload.length + " > " + available);
            }
            return new PlacedPayload(area, payload);
        }

        if (payload.length <= availableRamScriptBytes) {
            return new PlacedPayload(PayloadStorageArea.RAMSCRIPT, payload);
        }
        if (payload.length <= PayloadStorageArea.SAVE_BLOCK1.capacity()) {
            return new PlacedPayload(PayloadStorageArea.SAVE_BLOCK1, payload);
        }
        if (payload.length <= PayloadStorageArea.SAVE_BLOCK2.capacity()) {
            return new PlacedPayload(PayloadStorageArea.SAVE_BLOCK2, payload);
        }

        throw new IllegalArgumentException("payload exceeds every single storage area: " + payload.length + " bytes");
    }
}
