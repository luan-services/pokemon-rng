/*
   Tiny temporary IWRAM trampoline for calling a larger native routine that is
   embedded inside the CURRENT object-bound RamScript.

   This is deliberately transient: the caller may overwrite the same 32-byte
   continuation slot with the normal ReturnToField bridge before entering a
   scene that relocates the SaveBlocks.
*/
final class ObjectEventRamScriptNativeCallRuntime {
    private ObjectEventRamScriptNativeCallRuntime() {}

    static byte[] build(RomProfile rom, int localId, int nativeOffset) {
        if (localId < 0 || localId > 0xFF) throw new IllegalArgumentException("localId must fit one byte");
        if (nativeOffset < 0 || nativeOffset > 0xFE) throw new IllegalArgumentException("nativeOffset must fit adds immediate and leave room for Thumb bit");
        byte[] out = new byte[TradeEvolutionContinuationRuntime.CALLBACK_SIZE];
        putU16(out, 0x00, 0xB500);              // push {lr}
        putU16(out, 0x02, 0x2000 | localId);    // movs r0,#localId
        putU16(out, 0x04, 0x2100);              // movs r1,#0
        putU16(out, 0x06, 0x4B04);              // ldr r3, GetRamScript @ +0x18
        putU16(out, 0x08, 0x4798);              // blx r3
        putU16(out, 0x0A, 0x2800);              // cmp r0,#0
        putU16(out, 0x0C, 0xD004);              // beq done
        putU16(out, 0x0E, 0x3000 | nativeOffset);// adds r0,#nativeOffset
        putU16(out, 0x10, 0x3001);              // adds r0,#1 (Thumb)
        putU16(out, 0x12, 0x4780);              // blx r0
        putU16(out, 0x14, 0xBD00);              // done: pop {pc}
        putU16(out, 0x16, 0x46C0);              // nop / align
        putU32(out, 0x18, rom.getSavedRamScriptThumb - 0xBCL); // GetRamScript
        // remaining 4 bytes intentionally zero
        return out;
    }

    private static void putU16(byte[] d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}    
    private static void putU32(byte[] d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}    
}
