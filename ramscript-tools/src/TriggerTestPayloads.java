final class TriggerTestPayloads {
    private static final long DEFAULT_VIRTUAL_BASE = 0x08010000L;

    private TriggerTestPayloads() {}

    static byte[] helloWonderCard() {
        RamScriptBuilder builder = new RamScriptBuilder(DEFAULT_VIRTUAL_BASE);
        return builder
                .setVAddress()
                .lockAll()
                .vMessage("message")
                .waitMessage()
                .waitButtonPress()
                .releaseAll()
                .end()
                .text("message", "Hello from the Wonder Card!")
                .buildScript();
    }
}
