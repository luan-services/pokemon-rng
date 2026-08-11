/* Rebuilds the official Aurora Ticket and Mystic Ticket delivery scripts from
   field-script commands instead of copying their original RamScript bytes.

   The numeric constants below are the values used by the FireRed/LeafGreen
   decompilation. They are kept here next to the event template so the flow is
   easy to compare with the disassembler output.
*/
final class OfficialGiftScripts {
    private static final int VAR_RESULT = 0x800D;

    private static final int ITEM_MYSTIC_TICKET = 0x0172;
    private static final int ITEM_AURORA_TICKET = 0x0173;

    private static final int FLAG_RECEIVED_AURORA_TICKET = 0x02A7;
    private static final int FLAG_RECEIVED_MYSTIC_TICKET = 0x02A8;
    private static final int FLAG_FOUGHT_DEOXYS = 0x02E4;
    private static final int FLAG_FOUGHT_LUGIA = 0x02F2;
    private static final int FLAG_FOUGHT_HO_OH = 0x02F3;
    private static final int FLAG_ENABLE_SHIP_NAVEL_ROCK = 0x084A;
    private static final int FLAG_ENABLE_SHIP_BIRTH_ISLAND = 0x084B;

    // Same virtual bases used by the official English WC3 files we inspected.
    private static final long AURORA_VIRTUAL_BASE = 0x08012728L;
    private static final long MYSTIC_VIRTUAL_BASE = 0x0801297CL;

    private static final String INTRO_TEXT =
            "Thank you for using the MYSTERY\\n" +
            "GIFT System.\\p" +
            "You must be {PLAYER}.\\n" +
            "There is a ticket here for you.";

    private static final String SUCCESS_TEXT =
            "It appears to be for use at the\\n" +
            "VERMILION CITY port.\\p" +
            "Why not give it a try and see what\\n" +
            "it is about?";

    private static final String ALREADY_RECEIVED_TEXT =
            "Thank you for using the MYSTERY\\n" +
            "GIFT System.";

    private static final String BAG_FULL_TEXT =
            "Oh, I'm sorry, {PLAYER}. Your BAG's\\n" +
            "KEY ITEMS POCKET is full.\\p" +
            "Please store something on your PC,\\n" +
            "then come back for this.";

    private OfficialGiftScripts() {}

    static RamScript buildAuroraTicket() {
        RamScriptBuilder script = new RamScriptBuilder(AURORA_VIRTUAL_BASE);

        script.setVAddress()
                .lock()
                .facePlayer()
                .checkFlag(FLAG_RECEIVED_AURORA_TICKET)
                .vGotoIfEqual("alreadyReceived")
                .checkFlag(FLAG_FOUGHT_DEOXYS)
                .vGotoIfEqual("alreadyReceived")
                .checkItem(ITEM_AURORA_TICKET, 1)
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfEqual("alreadyReceived")
                .vMessage("introText")
                .waitMessage()
                .waitButtonPress()
                .checkItemSpace(ITEM_AURORA_TICKET, 1)
                .compareVarToValue(VAR_RESULT, 0)
                .vGotoIfEqual("bagFull")
                .giveItem(ITEM_AURORA_TICKET, 1)
                .setFlag(FLAG_ENABLE_SHIP_BIRTH_ISLAND)
                .setFlag(FLAG_RECEIVED_AURORA_TICKET)
                .vMessage("successText")
                .waitMessage()
                .waitButtonPress()
                .release()
                .end()

                .label("bagFull")
                .vMessage("bagFullText")
                .waitMessage()
                .waitButtonPress()
                .release()
                .end()

                .label("alreadyReceived")
                .vMessage("alreadyReceivedText")
                .waitMessage()
                .waitButtonPress()
                .release()
                .end()

                // The official cards store their message data after the code.
                .text("introText", INTRO_TEXT)
                .text("successText", SUCCESS_TEXT)
                .text("alreadyReceivedText", ALREADY_RECEIVED_TEXT)
                .text("bagFullText", BAG_FULL_TEXT);

        return script.buildWonderCardRamScript();
    }

    static RamScript buildMysticTicket() {
        RamScriptBuilder script = new RamScriptBuilder(MYSTIC_VIRTUAL_BASE);

        script.setVAddress()
                .lock()
                .facePlayer()
                .checkFlag(FLAG_RECEIVED_MYSTIC_TICKET)
                .vGotoIfEqual("alreadyReceived")
                .checkFlag(FLAG_FOUGHT_LUGIA)
                .vGotoIfEqual("alreadyReceived")
                .checkFlag(FLAG_FOUGHT_HO_OH)
                .vGotoIfEqual("alreadyReceived")
                .checkItem(ITEM_MYSTIC_TICKET, 1)
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfEqual("alreadyReceived")
                .vMessage("introText")
                .waitMessage()
                .waitButtonPress()
                .checkItemSpace(ITEM_MYSTIC_TICKET, 1)
                .compareVarToValue(VAR_RESULT, 0)
                .vGotoIfEqual("bagFull")
                .giveItem(ITEM_MYSTIC_TICKET, 1)
                .setFlag(FLAG_ENABLE_SHIP_NAVEL_ROCK)
                .setFlag(FLAG_RECEIVED_MYSTIC_TICKET)
                .vMessage("successText")
                .waitMessage()
                .waitButtonPress()
                .release()
                .end()

                .label("bagFull")
                .vMessage("bagFullText")
                .waitMessage()
                .waitButtonPress()
                .release()
                .end()

                .label("alreadyReceived")
                .vMessage("alreadyReceivedText")
                .waitMessage()
                .waitButtonPress()
                .release()
                .end()

                .text("introText", INTRO_TEXT)
                .text("successText", SUCCESS_TEXT)
                .text("alreadyReceivedText", ALREADY_RECEIVED_TEXT)
                .text("bagFullText", BAG_FULL_TEXT);

        return script.buildWonderCardRamScript();
    }
}
