import java.util.List;

/* Describes the high-level building blocks already implemented by RamScriptBuilder.

   This is intentionally metadata, not another assembler. A future graphical
   interface can use this catalog to show categories and available actions
   while still delegating byte generation to RamScriptBuilder.
*/
final class BuilderCatalog {
    record Entry(String category, String name, String description) {}

    private BuilderCatalog() {}

    static List<Entry> entries() {
        return List.of(
                new Entry("Control flow", "label", "Create a target for relocatable branches/calls"),
                new Entry("Control flow", "vGoto", "Relocatable unconditional jump"),
                new Entry("Control flow", "vCall", "Relocatable script call"),
                new Entry("Control flow", "vGotoIf", "Conditional relocatable jump"),
                new Entry("Control flow", "vCallIf", "Conditional relocatable call"),
                new Entry("Control flow", "returnScript", "Return from a normal script call"),
                new Entry("Control flow", "end", "End the field script"),

                new Entry("Variables", "setVar", "Set a script variable"),
                new Entry("Variables", "addVar", "Add to a script variable"),
                new Entry("Variables", "subVar", "Subtract from a script variable"),
                new Entry("Variables", "copyVar", "Copy one variable to another"),
                new Entry("Variables", "compareVarToValue", "Compare variable against immediate value"),
                new Entry("Variables", "compareVarToVar", "Compare two variables"),

                new Entry("Flags", "setFlag", "Set a game/event flag"),
                new Entry("Flags", "clearFlag", "Clear a game/event flag"),
                new Entry("Flags", "checkFlag", "Check a game/event flag"),

                new Entry("Items", "giveItem", "Game-style obtain-item sequence using STD_OBTAIN_ITEM"),
                new Entry("Items", "addItem", "Add an item without the convenience obtain-item macro"),
                new Entry("Items", "removeItem", "Remove an item"),
                new Entry("Items", "checkItem", "Check whether the player owns an item"),
                new Entry("Items", "checkItemSpace", "Check bag capacity for an item"),

                new Entry("Messages", "vMessage", "Display relocatable embedded text"),
                new Entry("Messages", "waitMessage", "Wait until message printing completes"),
                new Entry("Messages", "waitButtonPress", "Wait for player input"),
                new Entry("Messages", "text", "Embed a Gen III encoded string"),

                new Entry("Field", "lock", "Lock the talking object/player interaction"),
                new Entry("Field", "lockAll", "Lock all event objects"),
                new Entry("Field", "facePlayer", "Make the object face the player"),
                new Entry("Field", "release", "Release the current interaction"),
                new Entry("Field", "releaseAll", "Release all event objects"),
                new Entry("Field", "delay", "Delay script execution by frames"),

                new Entry("Sound", "playSe", "Play a sound effect"),
                new Entry("Sound", "waitSe", "Wait for sound effect"),
                new Entry("Sound", "playFanfare", "Play a fanfare"),
                new Entry("Sound", "waitFanfare", "Wait for fanfare"),

                new Entry("Visual", "fadeScreen", "Run a field screen fade"),
                new Entry("Visual", "setWeather", "Select weather"),
                new Entry("Visual", "doWeather", "Apply selected weather"),

                new Entry("Battle", "setWildBattle", "Configure a scripted wild encounter"),
                new Entry("Battle", "doWildBattle", "Start the configured wild encounter"),

                new Entry("Money", "addMoney", "Add money"),
                new Entry("Money", "removeMoney", "Remove money"),
                new Entry("Money", "checkMoney", "Check money"),

                new Entry("Engine", "special", "Invoke a script-engine special by ID"),
                new Entry("Engine", "specialVar", "Invoke a special and store its result"),
                new Entry("Engine", "callNative", "Call an absolute native function (ROM-build dependent)"),
                new Entry("Engine", "gotoNative", "Jump to an absolute native function (ROM-build dependent)"),

                new Entry("Pokemon / experimental", "giveEgg", "Direct wrapper for the game's giveegg opcode"),
                new Entry("Pokemon / experimental", "giveMon", "Low-level givemon wrapper; legitimacy semantics are not modeled")
        );
    }
}
