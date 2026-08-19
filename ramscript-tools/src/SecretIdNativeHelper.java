/*
   Reads the Secret ID from SaveBlock2 and stores it in gSpecialVar_Result.

   FR/LG SaveBlock2:
     +0x0A playerTrainerId[4]

   The visible Trainer ID is the low 16 bits. The Secret ID is the high
   16 bits, therefore it begins at SaveBlock2 + 0x0C.

   Thumb code (20 bytes):

       ldr  r0, =gSaveBlock2Ptr
       ldr  r0, [r0]
       ldrh r0, [r0, #0x0C]
       ldr  r1, =gSpecialVar_Result
       strh r0, [r1]
       bx   lr
       .word gSaveBlock2Ptr
       .word gSpecialVar_Result

   The helper uses 03005310 as temporary staging. No callback or persistent
   hook points to it in this preset.
*/
final class SecretIdNativeHelper {
    static final long STAGING_ADDRESS = 0x03005310L;

    private SecretIdNativeHelper() {}

    static NativeHelper build(RomProfile rom) {
        byte[] code = new byte[] {
                0x02, 0x48,             // ldr  r0, [pc,#8]  -> literal @ +0x0C
                0x00, 0x68,             // ldr  r0, [r0]
                (byte)0x80, (byte)0x89, // ldrh r0, [r0,#0x0C]
                0x02, 0x49,             // ldr  r1, [pc,#8]  -> literal @ +0x10
                0x08, (byte)0x80,       // strh r0, [r1]
                0x70, 0x47,             // bx   lr
                0, 0, 0, 0,             // gSaveBlock2Ptr
                0, 0, 0, 0              // gSpecialVar_Result
        };

        putU32(code, 0x0C, rom.saveBlock2Ptr);
        putU32(code, 0x10, rom.specialVarResult);
        return new NativeHelper(STAGING_ADDRESS, code);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
