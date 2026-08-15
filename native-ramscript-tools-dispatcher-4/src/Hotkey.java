/* High-level description of an input trigger.

   This does not assemble button-checking code yet. It exists so future native
   effects/GUI work can represent triggers independently from the effect body.

   The known-good seed modifier is currently fixed to:
       hold R, then press SELECT
*/
record Hotkey(String heldButton, String pressedButton) {
    static Hotkey seedModifierDefault() {
        return new Hotkey("R", "SELECT");
    }

    @Override
    public String toString() {
        return "hold " + heldButton + ", then press " + pressedButton;
    }
}
