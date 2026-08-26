/* Describes how a RamScript is installed and how the stable continuation bridge
   reacquires its current physical address after SaveBlock relocation. */
interface RamScriptBinding {
    EventTrigger trigger();
    RamScript createRamScript(byte[] payload);
    byte[] continuationCallback(RomProfile rom, int continuationOffset);
    byte[] continuationLiterals(RomProfile rom);
    int continuationImmediateOffset();
    String description();
}
