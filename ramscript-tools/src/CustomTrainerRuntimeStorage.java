/* Fixed layout inside the already validated 1024-byte SaveBlock2 auxiliary region.

   SB2+0x0B20  runtime header (16 B)
   SB2+0x0B30  shared native runtime (max 0x230 B)
   SB2+0x0D40  active trainer descriptor (max 0x1A0 B)
   SB2+0x0F00  existing toolkit manifest reservation (32 B)
*/
final class CustomTrainerRuntimeStorage {
    static final int HEADER_OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset(); // 0x0B20
    static final int HEADER_SIZE = 0x10;
    static final int RUNTIME_OFFSET = HEADER_OFFSET + HEADER_SIZE;            // 0x0B30
    static final int DESCRIPTOR_OFFSET = 0x0D60;
    static final int RUNTIME_CAPACITY = DESCRIPTOR_OFFSET - RUNTIME_OFFSET;   // 0x210
    static final int DESCRIPTOR_CAPACITY = InstallationManifest.OFFSET - DESCRIPTOR_OFFSET; // 0x1C0
    static final int MAGIC = 0x42525443; // "CTRB"
    static final int VERSION = 2;

    private CustomTrainerRuntimeStorage() {}

    static byte[] runtimeImage(byte[] runtime) {
        if (runtime.length > RUNTIME_CAPACITY) throw new IllegalArgumentException("shared trainer runtime too large: " + runtime.length + "/" + RUNTIME_CAPACITY);
        byte[] out = new byte[HEADER_SIZE + runtime.length];
        putU32(out, 0, MAGIC);
        putU16(out, 4, VERSION);
        putU16(out, 6, HEADER_SIZE);
        putU16(out, 8, runtime.length);
        putU16(out, 10, checksum16(runtime));
        putU16(out, 12, CustomTrainerBattleDescriptor.VERSION);
        putU16(out, 14, DESCRIPTOR_OFFSET);
        System.arraycopy(runtime, 0, out, HEADER_SIZE, runtime.length);
        return out;
    }

    static int checksum16(byte[] data) {
        int s=0; for(byte b:data) s=(s+Byte.toUnsignedInt(b))&0xFFFF; return s;
    }
    private static void putU16(byte[]d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[]d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}
}
