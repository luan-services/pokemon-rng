final class SeedModifierPreset {
    private static final long VIRTUAL_BASE = 0x08010000L;

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
        validateSeed(desiredSeed);

        long predecessor = RngMath.previousState(desiredSeed);
        byte[] predecessorBytes = new byte[] {
                (byte) predecessor,
                (byte) (predecessor >>> 8),
                (byte) (predecessor >>> 16),
                (byte) (predecessor >>> 24)
        };

        RamScriptBuilder builder = new RamScriptBuilder(VIRTUAL_BASE);
        return builder
                .setVAddress()
                .lockAll()
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

    static long predecessor(int desiredSeed) {
        validateSeed(desiredSeed);
        return RngMath.previousState(desiredSeed);
    }

    static String message(int desiredSeed) {
        validateSeed(desiredSeed);
        return String.format("Press A to set %04X as seed.", desiredSeed);
    }

    private static void validateSeed(int desiredSeed) {
        if (desiredSeed < 0 || desiredSeed > 0xFFFF) {
            throw new IllegalArgumentException("Initial seed must fit in u16");
        }
    }
}
