/* Named FR/LG battle BGM ids used by authored trainer events. */
enum BattleMusic {
    GYM_LEADER(0x0128),
    TRAINER(0x0129),
    CHAMPION(0x012B);

    private final int songId;
    BattleMusic(int songId) { this.songId = songId; }
    int songId() { return songId; }
}
