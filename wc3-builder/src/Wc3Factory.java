/* Creates the default base WC3 used by the "create" command.

   All values can be changed later through the normal edit command.
   The RamScript is only a harmless placeholder message and should be replaced
   by ramscript-tools/native-ramscript-tools when a real event is desired.
*/
final class Wc3Factory {
    private static final int DEFAULT_FLAG_ID = 1003;
    private static final int DEFAULT_ICON_SPECIES = 0xFFFF;

    private Wc3Factory() {}

    static Wc3File createBase() {
        Wc3File wc3 = Wc3File.createEmpty();
        WonderCard card = wc3.wonderCard();

        card.setFlagId(DEFAULT_FLAG_ID);
        wc3.setIconSpecies(DEFAULT_ICON_SPECIES);
        card.setIdNumber(0);
        card.setType(0);             // GIFT
        card.setBackgroundType(0);
        card.setSendType(0);         // DISALLOWED
        card.setMaxStamps(0);

        card.setTitle("MYSTERY EVENT");
        card.setSubtitle("CHOPPY'S CUSTOM EVENT");
        card.setBodyLine(0, "A special event is available!");
        card.setBodyLine(1, "Talk to the deliveryman");
        card.setBodyLine(2, "inside the POKEMON CENTER.");
        card.setBodyLine(3, "");
        card.setFooterLine1("Custom Wonder Card");
        card.setFooterLine2("");

        wc3.setRamScript(DefaultDeliveryScript.build());
        wc3.updateCardCrc();

        return wc3;
    }
}
