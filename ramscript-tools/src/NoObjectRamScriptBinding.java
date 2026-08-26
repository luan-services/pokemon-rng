final class NoObjectRamScriptBinding implements RamScriptBinding {
    static final NoObjectRamScriptBinding INSTANCE = new NoObjectRamScriptBinding();
    private NoObjectRamScriptBinding() {}

    @Override public EventTrigger trigger() { return EventTrigger.DELIVERYMAN; }
    @Override public RamScript createRamScript(byte[] payload) { return RamScript.createWonderCard(payload); }
    @Override public byte[] continuationCallback(RomProfile rom, int continuationOffset) {
        return TradeEvolutionContinuationRuntime.callback(rom, continuationOffset);
    }
    @Override public byte[] continuationLiterals(RomProfile rom) {
        return TradeEvolutionContinuationRuntime.callbackLiterals(rom);
    }
    @Override public int continuationImmediateOffset() {
        return TradeEvolutionContinuationRuntime.CONTINUATION_IMMEDIATE_OFFSET;
    }
    @Override public String description() { return "unbound Wonder Card / Deliveryman"; }
}
