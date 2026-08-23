/*
   Historical low-level byte placement primitive.

   This class intentionally knows only physical byte capacities and is kept for
   rejected/research native-module code that still depends on the old model.
   Production Field Script composition must use PayloadPlacementPlanner, where
   persistent placement is the SB1-gateway + SB2-body pair.
*/
final class RawPayloadPlacementPlanner {
    private RawPayloadPlacementPlanner() {}

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
