final class RepelHotkeyPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;

    private static final int VAR_REPEL_STEP_COUNT = 0x4020;
    private static final int VAR_RESULT = 0x800D;

    private static final int ITEM_SUPER_REPEL = 0x0053;
    private static final int ITEM_MAX_REPEL = 0x0054;
    private static final int ITEM_REPEL = 0x0056;

    private static final int SUPER_REPEL_STEPS = 200;
    private static final int MAX_REPEL_STEPS = 250;
    private static final int REPEL_STEPS = 100;

    // FireRed/LeafGreen SE_REPEL from constants/songs.h / charmap sound table.
    private static final int SE_REPEL = 0x0029;

    private RepelHotkeyPreset() {}

    static TriggerBuildResult build(RomProfile rom) {
        return build(rom, Hotkey.DEFAULT);
    }

    static TriggerBuildResult build(RomProfile rom, Hotkey hotkey) {
        return TriggerComposer.compose(
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                buildPayload(),
                hotkey
        );
    }

    static byte[] buildPayload() {
        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);

        return builder
                .setVAddress()

                // Match vanilla FR/LG behavior: do not stack a second Repel
                // while VAR_REPEL_STEP_COUNT is still nonzero.
                .compareVarToValue(VAR_REPEL_STEP_COUNT, 0)
                .vGotoIfNotEqual("done")

                // Prefer the longest-duration Repel available.
                .checkItem(ITEM_MAX_REPEL, 1)
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfEqual("use_max")

                .checkItem(ITEM_SUPER_REPEL, 1)
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfEqual("use_super")

                .checkItem(ITEM_REPEL, 1)
                .compareVarToValue(VAR_RESULT, 1)
                .vGotoIfEqual("use_normal")

                .lockAll()
                .vMessage("no_repel")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .vGoto("done")

                .label("use_max")
                .bufferItemName(1, ITEM_MAX_REPEL)
                .playSe(SE_REPEL)
                .waitSe()
                .removeItem(ITEM_MAX_REPEL, 1)
                .setVar(VAR_REPEL_STEP_COUNT, MAX_REPEL_STEPS)
                .vGoto("used")

                .label("use_super")
                .bufferItemName(1, ITEM_SUPER_REPEL)
                .playSe(SE_REPEL)
                .waitSe()
                .removeItem(ITEM_SUPER_REPEL, 1)
                .setVar(VAR_REPEL_STEP_COUNT, SUPER_REPEL_STEPS)
                .vGoto("used")

                .label("use_normal")
                .bufferItemName(1, ITEM_REPEL)
                .playSe(SE_REPEL)
                .waitSe()
                .removeItem(ITEM_REPEL, 1)
                .setVar(VAR_REPEL_STEP_COUNT, REPEL_STEPS)

                .label("used")
                .lockAll()
                .vMessage("used_repel")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()

                .label("done")
                .end()

                .text("no_repel", "No Repels.")
                .text("used_repel", "{PLAYER} used the\\n{STR_VAR_2}.")
                .buildScript();
    }
}
