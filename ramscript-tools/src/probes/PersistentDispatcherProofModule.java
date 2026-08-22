/* Small THUMB module used only to validate cross-area module dispatch.
   Kept under probes so it cannot be mistaken for a production preset. */
final class PersistentDispatcherProofModule {
    static final int MODULE_ID = 2;
    static final int SUCCESS_VALUE = 2;

    private PersistentDispatcherProofModule() {}

    static byte[] build(RomProfile rom) {
        byte[] code = new byte[12];
        put16(code, 0x00, 0x4801); // ldr r0, =gSpecialVar_Result
        put16(code, 0x02, 0x2102); // movs r1, #2
        put16(code, 0x04, 0x8001); // strh r1, [r0]
        put16(code, 0x06, 0x4770); // bx lr
        PersistentToolkitStorageV2.putU32(code, 0x08, rom.specialVarResult);
        return code;
    }

    private static void put16(byte[] b, int o, int v) {
        b[o] = (byte)v;
        b[o + 1] = (byte)(v >>> 8);
    }
}
