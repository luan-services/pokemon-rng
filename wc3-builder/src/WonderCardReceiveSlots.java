import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * FR/LG Wonder Card flagId is not an arbitrary event flag number.
 * The retail Mystery Gift code treats it as an index in the 20-entry
 * sReceivedGiftFlags table plus WONDER_CARD_FLAG_OFFSET (1000).
 */
final class WonderCardReceiveSlots {
    static final int OFFSET = 1000;
    static final int COUNT = 20;
    static final int MIN_RECEIVE_ID = OFFSET;
    static final int MAX_RECEIVE_ID = OFFSET + COUNT - 1;

    record Slot(
            int receiveId,
            int eventFlag,
            String id,
            String label,
            String category,
            boolean usedByFrlgRetail,
            String note
    ) {}

    private static final List<Slot> SLOTS = buildSlots();

    private WonderCardReceiveSlots() {}

    static boolean isValidReceiveId(int receiveId) {
        return receiveId >= MIN_RECEIVE_ID && receiveId <= MAX_RECEIVE_ID;
    }

    static void requireValidReceiveId(int receiveId) {
        if (!isValidReceiveId(receiveId)) {
            throw new IllegalArgumentException(
                    "flagId must be a FR/LG Wonder Card receive ID between "
                            + MIN_RECEIVE_ID + " and " + MAX_RECEIVE_ID
            );
        }
    }

    static Slot byReceiveId(int receiveId) {
        if (!isValidReceiveId(receiveId)) {
            return null;
        }
        return SLOTS.get(receiveId - OFFSET);
    }

    static List<Slot> all() {
        return SLOTS;
    }

    private static List<Slot> buildSlots() {
        List<Slot> slots = new ArrayList<>();
        slots.add(new Slot(1000, 0x2A7, "AURORA_TICKET", "Aurora Ticket", "OFFICIAL", true,
                "Maps to FLAG_RECEIVED_AURORA_TICKET."));
        slots.add(new Slot(1001, 0x2A8, "MYSTIC_TICKET", "Mystic Ticket", "OFFICIAL", true,
                "Maps to FLAG_RECEIVED_MYSTIC_TICKET."));
        slots.add(new Slot(1002, 0x2A9, "OLD_SEA_MAP", "Old Sea Map", "RESERVED", false,
                "Present in FR/LG's received-gift table; source notes it is not used until Emerald."));

        for (int i = 1; i <= 17; i++) {
            int receiveId = 1002 + i;
            int eventFlag = 0x2A9 + i;
            slots.add(new Slot(
                    receiveId,
                    eventFlag,
                    "UNUSED_" + i,
                    "Custom Slot " + i,
                    "UNUSED",
                    false,
                    "Maps to FLAG_WONDER_CARD_UNUSED_" + i + "."
            ));
        }

        return Collections.unmodifiableList(slots);
    }
}
