# Wonder Card `flagId` semantics in FR/LG

Reference: `pret/pokefirered` retail-matching source.

Relevant source files:

- `include/constants/mystery_gift.h`
- `include/constants/flags.h`
- `src/mystery_gift.c`

## Two related checks

There are two distinct concepts that are easy to conflate.

### 1. Wireless card comparison

When FR/LG prepares its Mystery Gift link data, it sends the `flagId` of the currently saved Wonder Card, or `0` when there is no valid saved card.

`MysteryGift_CompareCardFlags` then compares the incoming card's `flagId` with that saved value:

```text
saved flagId == 0       -> HAS_NO_CARD
incoming == saved       -> HAS_SAME_CARD
otherwise               -> HAS_DIFF_CARD
```

Therefore two different custom distributions that reuse the same `flagId` are indistinguishable to this particular same-card/different-card comparison.

Direct save injection does not perform this wireless server/client comparison because the card is written directly into the save.

### 2. Has the gift from the saved card already been received?

`IsSavedWonderCardGiftNotReceived` is stricter. The saved `flagId` must be in:

```text
WONDER_CARD_FLAG_OFFSET = 1000
NUM_WONDER_CARD_FLAGS   = 20
valid receive IDs       = 1000..1019
```

The game subtracts 1000 and indexes `sReceivedGiftFlags`.

The FR/LG table is:

```text
1000 -> FLAG_RECEIVED_AURORA_TICKET  (0x2A7)
1001 -> FLAG_RECEIVED_MYSTIC_TICKET  (0x2A8)
1002 -> FLAG_RECEIVED_OLD_SEA_MAP    (0x2A9; source notes unused until Emerald)
1003 -> FLAG_WONDER_CARD_UNUSED_1    (0x2AA)
...
1019 -> FLAG_WONDER_CARD_UNUSED_17   (0x2BA)
```

This is why the builder's historical default `1003` is a sensible custom receive slot: it is the first dedicated unused Wonder Card slot.

## General unused flags are different

Other unused FR/LG event flags can still be useful for custom events (for example event progression/rematch state), but they must not be confused with `WonderCard.flagId`.

Those general flags belong to the RamScript/event implementation. `wc3-builder` intentionally exposes only the dedicated 20 receive IDs in its UI-facing catalog.
