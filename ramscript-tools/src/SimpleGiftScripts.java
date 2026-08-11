/* Builds simple one-time item delivery RamScripts using only the normal
   FireRed/LeafGreen field-script engine.

   This class is intentionally generic: official events, custom events, or a
   future text compiler can all call this method with different parameters.
*/
final class SimpleGiftScripts {
    private static final int VAR_RESULT = 0x800D;

    private SimpleGiftScripts() {}

    static RamScript buildOneTimeItemGift(
            long virtualBase,
            int item,
            int amount,
            int receivedFlag,
            String introText,
            String successText,
            String alreadyReceivedText,
            String bagFullText
    ) {
        RamScriptBuilder script = new RamScriptBuilder(virtualBase);

        script.setVAddress()
                .lock()
                .facePlayer()

                /* Prevent this gift from being received twice. */
                .checkFlag(receivedFlag)
                .vGotoIfEqual("alreadyReceived")

                .vMessage("introText")
                .waitMessage()
                .waitButtonPress()

                /* checkitemspace stores its result in VAR_RESULT. */
                .checkItemSpace(item, amount)
                .compareVarToValue(VAR_RESULT, 0)
                .vGotoIfEqual("bagFull")

                /* giveItem expands to the same 3 commands as event.inc:
                   VAR_0x8000 = item, VAR_0x8001 = amount, callstd STD_OBTAIN_ITEM. */
                .giveItem(item, amount)
                .setFlag(receivedFlag)

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

                /* Embedded data comes after the executable paths. */
                .text("introText", introText)
                .text("successText", successText)
                .text("alreadyReceivedText", alreadyReceivedText)
                .text("bagFullText", bagFullText);

        return script.buildWonderCardRamScript();
    }

    /* Builds a repeatable item delivery.
       Unlike buildOneTimeItemGift(), this script intentionally does NOT call
       checkFlag() or setFlag(). The Wonder Card's normal received-gift flag
       therefore remains unset, allowing the deliveryman to remain available
       as long as the card itself remains installed. */
    static RamScript buildRepeatableItemGift(
            long virtualBase,
            int item,
            int amount,
            String introText,
            String successText,
            String bagFullText
    ) {
        RamScriptBuilder script = new RamScriptBuilder(virtualBase);

        script.setVAddress()
                .lock()
                .facePlayer()

                .vMessage("introText")
                .waitMessage()
                .waitButtonPress()

                .checkItemSpace(item, amount)
                .compareVarToValue(VAR_RESULT, 0)
                .vGotoIfEqual("bagFull")

                .giveItem(item, amount)

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

                .text("introText", introText)
                .text("successText", successText)
                .text("bagFullText", bagFullText);

        return script.buildWonderCardRamScript();
    }

    /* Builds a tiny utility event that clears one normal event flag.
       This does not edit a .sav directly; it runs through the normal
       FireRed/LeafGreen field-script engine when the RamScript is executed. */
    static RamScript buildClearFlag(
            long virtualBase,
            int flag,
            String message
    ) {
        RamScriptBuilder script = new RamScriptBuilder(virtualBase);

        script.setVAddress()
                .lock()
                .facePlayer()
                .clearFlag(flag);

        if (message != null && !message.isBlank()) {
            script.vMessage("message")
                    .waitMessage()
                    .waitButtonPress();
        }

        script.release()
                .end();

        if (message != null && !message.isBlank()) {
            script.text("message", message);
        }

        return script.buildWonderCardRamScript();
    }

}
