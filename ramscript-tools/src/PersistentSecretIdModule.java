/* Persistent-native form of the validated Secret ID helper.

   The helper bytes are position-independent with respect to their execution
   address: they only embed gSaveBlock2Ptr and gSpecialVar_Result. In shared
   compositions the module is stored in SB2, staged into common EWRAM scratch,
   and entered through stock callnative.
*/
final class PersistentSecretIdModule {
    static final int MODULE_ID = 0x21;

    private PersistentSecretIdModule() {}

    static byte[] payload(RomProfile rom) {
        return SecretIdNativeHelper.build(rom).codeCopy();
    }
}
