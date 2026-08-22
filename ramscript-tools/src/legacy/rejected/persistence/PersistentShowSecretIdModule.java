/* Native persistent form of the Show Secret ID preset.

   This is intentionally separate from ShowSecretIdPreset. The original preset
   remains the simple RamScript implementation and stays the default CLI path.

   The persistent module writes the SID to VAR_8004 and writes 1 to VAR_RESULT
   as an execution-success marker. This avoids treating SID=0 as failure.
*/
final class PersistentShowSecretIdModule {
    static final int MODULE_ID = 0x10;
    static final int SUCCESS_VALUE = 1;

    private PersistentShowSecretIdModule() {}

    static PresetPayload payload(RomProfile rom) {
        byte[] code = new byte[0x20];
        put16(code, 0x00, 0x4804); // ldr r0, =gSaveBlock2Ptr @ +0x14
        put16(code, 0x02, 0x6800); // ldr r0, [r0]
        put16(code, 0x04, 0x8980); // ldrh r0, [r0,#0x0C]
        put16(code, 0x06, 0x4904); // ldr r1, =gSpecialVar_0x8004 @ +0x18
        put16(code, 0x08, 0x8008); // strh r0, [r1]
        put16(code, 0x0A, 0x2001); // movs r0, #1
        put16(code, 0x0C, 0x4903); // ldr r1, =gSpecialVar_Result @ +0x1C
        put16(code, 0x0E, 0x8008); // strh r0, [r1]
        put16(code, 0x10, 0x4770); // bx lr
        put16(code, 0x12, 0x46C0); // nop / align literals
        PersistentToolkitStorageV2.putU32(code, 0x14, rom.saveBlock2Ptr);
        PersistentToolkitStorageV2.putU32(code, 0x18, rom.specialVar8004);
        PersistentToolkitStorageV2.putU32(code, 0x1C, rom.specialVarResult);
        return new PresetPayload("show-secret-id", PresetPayloadKind.THUMB, code);
    }

    private static void put16(byte[] out, int offset, int value) {
        out[offset] = (byte)value;
        out[offset + 1] = (byte)(value >>> 8);
    }
}
