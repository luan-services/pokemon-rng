/* High-level preset for an item gift that can be collected repeatedly.

   There is deliberately no receipt flag field here: the generated script
   contains neither checkFlag nor setFlag.
*/
record RepeatableItemGiftPreset(
        int item,
        int amount,
        String introText,
        String successText,
        String bagFullText
) {
    private static final long DEFAULT_VIRTUAL_BASE = 0x08010000L;

    RamScript build() {
        return SimpleGiftScripts.buildRepeatableItemGift(
                DEFAULT_VIRTUAL_BASE,
                item,
                amount,
                introText,
                successText,
                bagFullText
        );
    }

    static RepeatableItemGiftPreset defaults(int item, int amount) {
        String displayName = itemDisplayName(item);

        return new RepeatableItemGiftPreset(
                item,
                amount,
                "Hello, {PLAYER}!\\nI have something for you.\\pPlease accept this " + displayName + ".",
                "You received the " + displayName + "!",
                "Your BAG does not have enough room.\\pPlease make some space and come back."
        );
    }

    private static String itemDisplayName(int item) {
        String name = Items.name(item);

        if (name == null || name.isBlank()) {
            return String.format("ITEM 0x%04X", item);
        }

        String value = name.startsWith("ITEM_") ? name.substring(5) : name;
        return value.replace('_', ' ');
    }
}
