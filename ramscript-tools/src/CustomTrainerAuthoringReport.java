final class CustomTrainerAuthoringReport {
    private CustomTrainerAuthoringReport() {}

    static void print(RomProfile rom, CustomTrainerBattleSpec spec, ObjectEventTarget target,
                      long vanillaScript, TriggerBuildResult result) {
        byte[] descriptor = CustomTrainerBattleDescriptor.encode(spec, target.localId(), 10);
        byte[] compact = CustomTrainerCompactTransport.encode(spec, target.localId(), 10);
        byte[] fieldTexts = CustomTrainerFieldTextStorage.encode(spec);
        int pre = Gen3TextCodec.encodeString(spec.preBattleText()).length;
        int defeat = Gen3TextCodec.encodeString(spec.defeatText()).length;
        int post = Gen3TextCodec.encodeString(spec.postVictoryText()).length;

        System.out.println("  authoring size report:");
        System.out.println("    card: " + result.payloadBytes() + " / " + RamScript.SCRIPT_SIZE + " B");
        System.out.println("    free: " + (RamScript.SCRIPT_SIZE - result.payloadBytes()) + " B");
        System.out.println("    party: " + spec.party().size() + " mon(s), persistent descriptor " + descriptor.length + " B");
        System.out.println("    compact descriptor stream: " + compact.length + " B");
        System.out.println("    SB1 text image: " + fieldTexts.length + " / " + CustomTrainerFieldTextStorage.CAPACITY + " B");
        System.out.println("    pre-battle: " + pre + " encoded B");
        System.out.println("    defeat: " + defeat + " / " + CustomTrainerBattleDescriptor.DEFEAT_TEXT_CAPACITY + " encoded B");
        System.out.println("    post-victory: " + post + " encoded B (also reused for already-completed)");
        System.out.println("    note: encoded B includes the Gen III 0xFF terminator; ordinary chars/newline are usually 1 B each");
    }
}
