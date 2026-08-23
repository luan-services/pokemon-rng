import java.util.ArrayList;
import java.util.List;

/*
   Production Build-16-aware Field Script placement planner.

   Physical choices are intentionally architectural rather than raw heaps:

     LOCAL      -> Field Script body in RamScript
     PERSISTENT -> 10-byte gateway in validated SB1 storage + body in SB2

   AUTO prefers local RamScript while it fits, then falls back to the validated
   persistent pair. Native/Thumb persistent deployment is a separate unresolved
   deployment class and is rejected here instead of silently choosing a legacy
   architecture.

   The current Build 15a scheduler stores the gateway distance in one byte. To
   keep every produced gateway reachable by that frozen runtime, gateway slots
   are allocated backwards from the end of the validated SB1 region and must
   remain within 0xFF bytes of the RamScript script start.
*/
final class PayloadPlacementPlanner {
    static final int GATEWAY_SIZE = 10;

    // RamScript script starts at SB1+0x3624 in the validated FR/LG save layout.
    static final int RAMSCRIPT_SCRIPT_IN_SB1 = 0x3624;
    static final int MAX_GATEWAY_DISTANCE = 0xFF;

    private static final int SB1_START = PayloadStorageArea.SAVE_BLOCK1.offset();
    private static final int SB1_END = SB1_START + PayloadStorageArea.SAVE_BLOCK1.capacity();
    private static final int SB2_START = PayloadStorageArea.SAVE_BLOCK2.offset();
    private static final int SB2_END = SB2_START + PayloadStorageArea.SAVE_BLOCK2.capacity();

    // First slot is the same 0x3612 entry already validated by Build 15a.
    private static final int FIRST_GATEWAY_OFFSET = SB1_END - GATEWAY_SIZE;

    private PayloadPlacementPlanner() {}

    static PlacementPlan plan(List<PresetRequest> requests, int availableRamScriptBytes) {
        if (requests == null) throw new IllegalArgumentException("requests must not be null");
        if (availableRamScriptBytes < 0 || availableRamScriptBytes > RamScript.SCRIPT_SIZE) {
            throw new IllegalArgumentException("invalid RamScript byte budget");
        }

        int ramScriptCursor = RamScript.SCRIPT_SIZE - availableRamScriptBytes;
        int sb2Cursor = SB2_START;
        int nextGatewayOffset = FIRST_GATEWAY_OFFSET;

        List<RamScriptAllocation> local = new ArrayList<>();
        List<PersistentFieldScriptAllocation> persistent = new ArrayList<>();

        for (PresetRequest request : requests) {
            if (request == null) throw new IllegalArgumentException("request must not be null");
            if (request.kind() != PresetPayloadKind.FIELD_SCRIPT) {
                throw new IllegalArgumentException(
                        "production automatic placement currently supports FIELD_SCRIPT only: " + request.id()
                );
            }

            int size = request.bytes().length;
            boolean localFits = size <= RamScript.SCRIPT_SIZE - ramScriptCursor;
            boolean persistentFits = persistentFits(size, nextGatewayOffset, sb2Cursor);

            switch (request.preference()) {
                case LOCAL -> {
                    if (!localFits) {
                        throw capacityError(request, "RamScript", size, RamScript.SCRIPT_SIZE - ramScriptCursor);
                    }
                    local.add(new RamScriptAllocation(request.id(), ramScriptCursor, size, request.requiresHotkey()));
                    ramScriptCursor += size;
                }
                case PERSISTENT -> {
                    if (!persistentFits) throw persistentCapacityError(request, size, nextGatewayOffset, sb2Cursor);
                    int distance = RAMSCRIPT_SCRIPT_IN_SB1 - nextGatewayOffset;
                    persistent.add(new PersistentFieldScriptAllocation(
                            request.id(), nextGatewayOffset, GATEWAY_SIZE, distance,
                            sb2Cursor, size, request.requiresHotkey()
                    ));
                    nextGatewayOffset -= GATEWAY_SIZE;
                    sb2Cursor += size;
                }
                case AUTO -> {
                    if (localFits) {
                        local.add(new RamScriptAllocation(request.id(), ramScriptCursor, size, request.requiresHotkey()));
                        ramScriptCursor += size;
                    } else {
                        if (!persistentFits) throw persistentCapacityError(request, size, nextGatewayOffset, sb2Cursor);
                        int distance = RAMSCRIPT_SCRIPT_IN_SB1 - nextGatewayOffset;
                        persistent.add(new PersistentFieldScriptAllocation(
                                request.id(), nextGatewayOffset, GATEWAY_SIZE, distance,
                                sb2Cursor, size, request.requiresHotkey()
                        ));
                        nextGatewayOffset -= GATEWAY_SIZE;
                        sb2Cursor += size;
                    }
                }
            }
        }

        int ramUsed = ramScriptCursor;
        int gatewayCount = persistent.size();
        int gatewayUsed = gatewayCount * GATEWAY_SIZE;
        int reachableGatewayCapacity = reachableGatewayCapacityBytes();
        int sb2Used = sb2Cursor - SB2_START;

        PlacementDiagnostics diagnostics = new PlacementDiagnostics(
                ramUsed,
                RamScript.SCRIPT_SIZE - ramUsed,
                gatewayUsed,
                reachableGatewayCapacity - gatewayUsed,
                sb2Used,
                PayloadStorageArea.SAVE_BLOCK2.capacity() - sb2Used
        );

        return new PlacementPlan(local, persistent, diagnostics);
    }

    static int reachableGatewaySlots() {
        return reachableGatewayCapacityBytes() / GATEWAY_SIZE;
    }

    static byte[] buildGateway(PersistentFieldScriptAllocation allocation) {
        if (allocation == null) throw new IllegalArgumentException("allocation must not be null");
        return PersistentFieldScriptGatewayRuntime.gatewayForTest(
                allocation.sb1GatewayOffset(), allocation.sb2PayloadOffset()
        );
    }

    private static boolean persistentFits(int payloadSize, int gatewayOffset, int sb2Cursor) {
        if (payloadSize <= 0) return false;
        if (gatewayOffset < SB1_START) return false;
        if (gatewayOffset + GATEWAY_SIZE > SB1_END) return false;
        int distance = RAMSCRIPT_SCRIPT_IN_SB1 - gatewayOffset;
        if (distance < 0 || distance > MAX_GATEWAY_DISTANCE) return false;
        return sb2Cursor + payloadSize <= SB2_END;
    }

    private static int reachableGatewayCapacityBytes() {
        int count = 0;
        for (int offset = FIRST_GATEWAY_OFFSET; offset >= SB1_START; offset -= GATEWAY_SIZE) {
            int distance = RAMSCRIPT_SCRIPT_IN_SB1 - offset;
            if (distance > MAX_GATEWAY_DISTANCE) break;
            count++;
        }
        return count * GATEWAY_SIZE;
    }

    private static IllegalArgumentException persistentCapacityError(
            PresetRequest request, int payloadSize, int gatewayOffset, int sb2Cursor
    ) {
        int sb2Free = SB2_END - sb2Cursor;
        int distance = RAMSCRIPT_SCRIPT_IN_SB1 - gatewayOffset;
        if (payloadSize > sb2Free) {
            return capacityError(request, "SaveBlock2 payload storage", payloadSize, sb2Free);
        }
        if (gatewayOffset < SB1_START || distance > MAX_GATEWAY_DISTANCE) {
            return new IllegalArgumentException(
                    "no reachable 10-byte SB1 gateway slot remains for " + request.id() +
                    " under the current Build 15a u8-distance scheduler"
            );
        }
        return new IllegalArgumentException("persistent placement failed for " + request.id());
    }

    private static IllegalArgumentException capacityError(PresetRequest request, String area, int required, int available) {
        return new IllegalArgumentException(
                request.id() + " does not fit " + area + ": requires " + required + " bytes, " + available + " available"
        );
    }
}
