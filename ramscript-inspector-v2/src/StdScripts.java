final class StdScripts {
    private static final String[] NAMES = {
        "STD_OBTAIN_ITEM",
        "STD_FIND_ITEM",
        "MSGBOX_NPC",
        "MSGBOX_SIGN",
        "MSGBOX_DEFAULT",
        "MSGBOX_YESNO",
        "MSGBOX_AUTOCLOSE",
        "STD_OBTAIN_DECORATION",
        "STD_PUT_ITEM_AWAY",
        "STD_RECEIVED_ITEM"
    };

    private StdScripts() {}

    static String format(int id) {
        return id >= 0 && id < NAMES.length ? NAMES[id] : String.format("0x%02X", id & 0xFF);
    }
}
