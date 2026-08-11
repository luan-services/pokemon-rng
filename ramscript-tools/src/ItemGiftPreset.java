/* High-level reusable preset for the common "deliveryman gives an item once" event.

   This is the kind of object a future GUI can build from form controls:
     item, amount, receipt flag, and four messages.

   The actual low-level opcodes remain centralized in RamScriptBuilder and
   SimpleGiftScripts.
*/
record ItemGiftPreset(
        int item,
        int amount,
        int receivedFlag,
        String introText,
        String successText,
        String alreadyReceivedText,
        String bagFullText
) {
    private static final long DEFAULT_VIRTUAL_BASE = 0x08010000L;

    RamScript build() {
        return SimpleGiftScripts.buildOneTimeItemGift(
                DEFAULT_VIRTUAL_BASE,
                item,
                amount,
                receivedFlag,
                introText,
                successText,
                alreadyReceivedText,
                bagFullText
        );
    }

    static ItemGiftPreset defaults(int item, int amount, int receivedFlag) {
        String displayName = itemDisplayName(item);

        return new ItemGiftPreset(
                item,
                amount,
                receivedFlag,
                "Hello, {PLAYER}!\\nI have something for you.\\pPlease accept this " + displayName + ".",
                "You received the " + displayName + "!",
                "You already received this gift.",
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
