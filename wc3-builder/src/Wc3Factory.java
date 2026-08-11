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

        card.setTitle("CUSTOM WONDER CARD");
        card.setSubtitle("CHOPPY'S WC3 BUILDER");
        card.setBodyLine(0, "This is a custom Wonder Card.");
        card.setBodyLine(1, "No real event is attached yet.");
        card.setBodyLine(2, "Edit the design, then attach");
        card.setBodyLine(3, "a RamScript when you are ready.");
        card.setFooterLine1("Custom Wonder Card");
        card.setFooterLine2("Generated from scratch");

        wc3.setRamScript(DefaultDeliveryScript.build());
        wc3.updateCardCrc();

        return wc3;
    }
}
