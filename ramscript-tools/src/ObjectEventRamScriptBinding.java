final class ObjectEventRamScriptBinding implements RamScriptBinding {
    private final ObjectEventTarget target;

    ObjectEventRamScriptBinding(ObjectEventTarget target) {
        if (target == null) throw new IllegalArgumentException("Object-event target must not be null");
        this.target = target;
    }

    ObjectEventTarget target() { return target; }

    @Override public EventTrigger trigger() { return EventTrigger.OBJECT_BOUND_NPC; }
    @Override public RamScript createRamScript(byte[] payload) {
        return RamScript.createObjectBound(payload, target.mapGroup(), target.mapNum(), target.localId());
    }
    @Override public byte[] continuationCallback(RomProfile rom, int continuationOffset) {
        return ObjectEventRamScriptContinuationRuntime.callback(rom, continuationOffset, target.localId());
    }
    @Override public byte[] continuationLiterals(RomProfile rom) {
        return ObjectEventRamScriptContinuationRuntime.callbackLiterals(rom);
    }
    @Override public int continuationImmediateOffset() {
        return ObjectEventRamScriptContinuationRuntime.CONTINUATION_IMMEDIATE_OFFSET;
    }
    @Override public String description() { return target.displayName() + " [" + target.id() + "]"; }
}
