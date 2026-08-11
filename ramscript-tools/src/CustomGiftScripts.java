/* Custom examples built from our generic RamScript API.

   Unlike OfficialGiftScripts, this does not reproduce any Nintendo event.
   It is our own test event and exists to prove that the assembler can build
   new behavior from normal FireRed/LeafGreen script commands.
*/
final class CustomGiftScripts {
    private static final int ITEM_RARE_CANDY = 0x0044;

    /* flags.h reserves 0x2AA-0x2BA as unused Wonder Card receipt flags.
       0x2AA is the first unused slot. */
    private static final int FLAG_WONDER_CARD_UNUSED_1 = 0x02AA;

    /* This is only a virtual address namespace for v* pointers. The script is
       relocatable; it is not calling code at this ROM address. */
    private static final long CUSTOM_VIRTUAL_BASE = 0x08010000L;

    private CustomGiftScripts() {}

    static RamScript buildRareCandyTest() {
        return SimpleGiftScripts.buildOneTimeItemGift(
                CUSTOM_VIRTUAL_BASE,
                ITEM_RARE_CANDY,
                1,
                FLAG_WONDER_CARD_UNUSED_1,
                "Hello, {PLAYER}!\\nThis is our first custom RamScript.\\pI have a RARE CANDY for you.",
                "It worked!\\nEnjoy your RARE CANDY!",
                "You already received this custom gift.",
                "Your BAG does not have enough room.\\pPlease make some space and come back."
        );
    }
}
