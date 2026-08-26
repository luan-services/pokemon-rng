/* Historical compatibility alias. Production object-bound code now uses the
   generic ObjectEventRamScriptContinuationRuntime through RamScriptBinding. */
@Deprecated
final class ObjectBoundTradeEvolutionContinuationRuntime {
    static final int CONTINUATION_IMMEDIATE_OFFSET = ObjectEventRamScriptContinuationRuntime.CONTINUATION_IMMEDIATE_OFFSET;
    private ObjectBoundTradeEvolutionContinuationRuntime() {}
    static byte[] callback(RomProfile rom, int continuationOffset, int localId) {
        return ObjectEventRamScriptContinuationRuntime.callback(rom, continuationOffset, localId);
    }
    static byte[] callbackLiterals(RomProfile rom) {
        return ObjectEventRamScriptContinuationRuntime.callbackLiterals(rom);
    }
}
