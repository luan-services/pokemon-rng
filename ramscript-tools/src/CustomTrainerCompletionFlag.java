/* Persistent completion flags reserved for authored custom trainer rematches.
   FR/LG flags.h explicitly labels 0x4A7..0x4AF as unused flags, immediately
   before the stock boss-clear flag range. */
enum CustomTrainerCompletionFlag {
    BROCK(0x4A7),
    MISTY(0x4A8),
    LT_SURGE(0x4A9),
    ERIKA(0x4AA),
    KOGA(0x4AB),
    SABRINA(0x4AC),
    BLAINE(0x4AD),
    GIOVANNI(0x4AE);

    private final int eventFlag;
    CustomTrainerCompletionFlag(int eventFlag) { this.eventFlag = eventFlag; }
    int eventFlag() { return eventFlag; }
}
