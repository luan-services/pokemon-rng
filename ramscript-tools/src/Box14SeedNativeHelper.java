/*
   BOX 14 seed decoder used by the production Seed Modifier variant.

   Reads exactly eight Gen III uppercase hexadecimal characters from the name
   of BOX 14 (PokemonStorage.boxNames[13]), converts them to a u32 desired RNG
   seed, computes the predecessor for the FR/LG LCG, and writes that predecessor
   to gRngValue. The next normal Random() therefore reaches the requested seed.

   Accepted characters only:
     0-9 = 0xA1..0xAA
     A-F = 0xBB..0xC0

   Invalid input returns without touching gRngValue.

   The helper deliberately uses gPokemonStoragePtr rather than the backing
   gPokemonStorage address so the normal save-block offset remains respected.
   FR/LG English 1.0/1.1 use gPokemonStoragePtr = 0x03005010.

   Thumb body is 72 bytes including its literal pool. It uses only r0-r3 and
   does not keep state across frames.
*/
final class Box14SeedNativeHelper {
    static final long POKEMON_STORAGE_PTR = 0x03005010L;
    static final int BOX_14_NAME_OFFSET = 0x83B9;
    static final int CODE_SIZE = 72;

    private Box14SeedNativeHelper() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] code = new byte[] {
                0x0C,0x48,             // ldr  r0, =gPokemonStoragePtr
                0x00,0x68,             // ldr  r0, [r0]
                0x0C,0x4B,             // ldr  r3, =BOX14_NAME_OFFSET
                (byte)0xC0,0x18,       // adds r0, r0, r3
                0x00,0x21,             // movs r1, #0       ; parsed seed
                0x08,0x22,             // movs r2, #8       ; exactly 8 chars
                0x03,0x78,             // loop: ldrb r3, [r0]
                0x01,0x30,             // adds r0, #1
                (byte)0xA1,0x3B,       // subs r3, #0xA1
                0x09,0x2B,             // cmp  r3, #9
                0x02,(byte)0xD9,       // bls  digit
                0x10,0x3B,             // subs r3, #0x10    ; A-F -> 10..15
                0x0F,0x2B,             // cmp  r3, #15
                0x09,(byte)0xD8,       // bhi  invalid
                0x09,0x01,             // digit: lsls r1, r1, #4
                0x19,0x43,             // orrs r1, r3
                0x01,0x3A,             // subs r2, #1
                (byte)0xF3,(byte)0xD1, // bne  loop
                0x05,0x48,             // ldr  r0, =0x6073
                0x09,0x1A,             // subs r1, r1, r0
                0x05,0x48,             // ldr  r0, =0xEEB9EB65
                0x41,0x43,             // muls r1, r0
                0x05,0x48,             // ldr  r0, =gRngValue
                0x01,0x60,             // str  r1, [r0]
                0x70,0x47,             // invalid/success: bx lr
                (byte)0xC0,0x46,       // alignment nop
                0,0,0,0,               // gPokemonStoragePtr
                0,0,0,0,               // BOX14_NAME_OFFSET
                0x73,0x60,0x00,0x00,   // LCG increment 0x6073
                0x65,(byte)0xEB,(byte)0xB9,(byte)0xEE, // multiplier inverse
                0,0,0,0                // gRngValue
        };
        putU32(code, 0x34, POKEMON_STORAGE_PTR);
        putU32(code, 0x38, BOX_14_NAME_OFFSET);
        putU32(code, 0x44, rom.rngValue);
        if (code.length != CODE_SIZE) throw new IllegalStateException("BOX 14 helper size mismatch");
        return new NativeHelper(stagingAddress, code);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
