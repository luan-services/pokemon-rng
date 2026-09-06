final class SeedModifierPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;
    private static final int SPECIAL_CLOSE_LINK = 0x001F;
    // GAME-VALIDATED LG1.0 RNG normalization: CloseLink shuts RFU down while
    // this is still non-zero, then clear the selector so the main link loop
    // no longer dispatches RfuMain1. The address comes from RomProfile so
    // ports do not rely on a feature-local hardcode.

    private SeedModifierPreset() {}

    static TriggerBuildResult build(RomProfile rom, int desiredSeed) {
        return build(rom, desiredSeed, Hotkey.DEFAULT);
    }

    static TriggerBuildResult build(RomProfile rom, int desiredSeed, Hotkey hotkey) {
        return TriggerComposer.compose(
                EventTrigger.HOTKEY_RUNTIME,
                rom,
                buildPayload(rom, desiredSeed),
                hotkey
        );
    }

    static TriggerBuildResult buildDeliveryman(RomProfile rom, int desiredSeed) {
        return TriggerComposer.compose(EventTrigger.DELIVERYMAN, rom, buildPayload(rom, desiredSeed));
    }

    static byte[] buildPayload(RomProfile rom, int desiredSeed) {
        return buildPayloadAtOffset(rom, desiredSeed, 0);
    }

    static byte[] buildPayloadAtOffset(RomProfile rom, int desiredSeed, int ramScriptOffset) {
        if (ramScriptOffset < 0) throw new IllegalArgumentException("ramScriptOffset must be >= 0");
        long predecessor = RngMath.previousState(Integer.toUnsignedLong(desiredSeed));
        byte[] predecessorBytes = new byte[] {
                (byte) predecessor,
                (byte) (predecessor >>> 8),
                (byte) (predecessor >>> 16),
                (byte) (predecessor >>> 24)
        };

        long virtualBase = (VIRTUAL_BASE + Integer.toUnsignedLong(ramScriptOffset)) & 0xFFFF_FFFFL;
        RamScriptBuilder builder = new RamScriptBuilder(virtualBase);
        return builder
                .setVAddress()
                .lockAll()
                .special(SPECIAL_CLOSE_LINK)
                .writeBytes(rom.wirelessCommType, new byte[] { 0 })
                .vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .writeBytes(rom.rngValue, predecessorBytes)
                .releaseAll()
                .end()
                .text("message", message(desiredSeed))
                .buildScript();
    }

    static int payloadSize(RomProfile rom, int desiredSeed) {
        return buildPayload(rom, desiredSeed).length;
    }

    static int sharedLocalPayloadSize(RomProfile rom) {
        return buildPayloadAtOffset(rom, 0x1234, 0).length;
    }

    static long predecessor(int desiredSeed) {
        return RngMath.previousState(Integer.toUnsignedLong(desiredSeed));
    }

    static String message(int desiredSeed) {
        return String.format("Press A to set %08X as seed.", desiredSeed);
    }

}
