/* Stock FR/LG Wonder Card gift-flag range.
   cardFlagId 1000 maps to FLAG_RECEIVED_AURORA_TICKET (0x2A7), so the
   officially unused custom slots begin at cardFlagId 1003 / event flag 0x2AA. */
enum WonderCardGiftFlag {
    UNUSED_1 (0x2AA, 1003),
    UNUSED_2 (0x2AB, 1004),
    UNUSED_3 (0x2AC, 1005),
    UNUSED_4 (0x2AD, 1006),
    UNUSED_5 (0x2AE, 1007),
    UNUSED_6 (0x2AF, 1008),
    UNUSED_7 (0x2B0, 1009),
    UNUSED_8 (0x2B1, 1010),
    UNUSED_9 (0x2B2, 1011),
    UNUSED_10(0x2B3, 1012),
    UNUSED_11(0x2B4, 1013),
    UNUSED_12(0x2B5, 1014),
    UNUSED_13(0x2B6, 1015),
    UNUSED_14(0x2B7, 1016),
    UNUSED_15(0x2B8, 1017),
    UNUSED_16(0x2B9, 1018),
    UNUSED_17(0x2BA, 1019);

    private final int eventFlag;
    private final int cardFlagId;

    WonderCardGiftFlag(int eventFlag, int cardFlagId) {
        this.eventFlag = eventFlag;
        this.cardFlagId = cardFlagId;
    }

    int eventFlag() { return eventFlag; }
    int cardFlagId() { return cardFlagId; }
}
