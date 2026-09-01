/* Persistent-native form of the GAME-VALIDATED standalone BOX 14 seed helper.

   The 72-byte Thumb body is position-independent with respect to its execution
   address. In shared compositions it lives in the SB2 native catalog and is
   staged into the existing shared EWRAM native buffer before the prompt.
*/
final class PersistentBox14SeedModule {
    static final int MODULE_ID = 0x25;

    private PersistentBox14SeedModule() {}

    static byte[] payload(RomProfile rom) {
        return Box14SeedNativeHelper.buildAt(rom, 0x02000000L).codeCopy();
    }
}
