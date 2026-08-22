/* Persistent native core for the Seed Modifier preset.

   This does not replace SeedModifierPreset. The original preset remains the
   simple RamScript + hotkey implementation, including its prompt/timing UI.

   The persistent form stores only the native action: write the LCG predecessor
   of the requested initial seed to gRngValue, then report success through
   VAR_RESULT. On the next normal PRNG advance the requested seed is produced,
   matching the timing model used by the validated simple preset.
*/
final class PersistentSeedModifierModule {
    static final int MODULE_ID = 0x11;
    static final int SUCCESS_VALUE = 1;

    private PersistentSeedModifierModule() {}

    static PresetPayload payload(RomProfile rom, int desiredSeed) {
        if (desiredSeed < 0 || desiredSeed > 0xFFFF) {
            throw new IllegalArgumentException("Initial seed must fit in u16");
        }
        long predecessor = RngMath.previousState(desiredSeed);

        // 0x1C bytes, position-independent apart from the three absolute literals.
        // IMPORTANT: the native module must return before the literal pool. Build 10
        // omitted bx lr and execution fell through into literal data.
        byte[] code = new byte[0x1C];
        put16(code, 0x00, 0x4804); // ldr r0, =gRngValue @ +0x14
        put16(code, 0x02, 0x4905); // ldr r1, =predecessor @ +0x18
        put16(code, 0x04, 0x6001); // str r1, [r0]
        put16(code, 0x06, 0x2001); // movs r0, #1
        put16(code, 0x08, 0x4901); // ldr r1, =gSpecialVar_Result @ +0x10
        put16(code, 0x0A, 0x8008); // strh r0, [r1]
        put16(code, 0x0C, 0x4770); // bx lr
        put16(code, 0x0E, 0x46C0); // nop / align literal pool
        PersistentToolkitStorageV2.putU32(code, 0x10, rom.specialVarResult);
        PersistentToolkitStorageV2.putU32(code, 0x14, rom.rngValue);
        PersistentToolkitStorageV2.putU32(code, 0x18, predecessor);
        return new PresetPayload("seed-modifier-" + String.format("%04X", desiredSeed), PresetPayloadKind.THUMB, code);
    }

    static long predecessor(int desiredSeed) {
        if (desiredSeed < 0 || desiredSeed > 0xFFFF) throw new IllegalArgumentException("Initial seed must fit in u16");
        return RngMath.previousState(desiredSeed);
    }

    private static void put16(byte[] b, int o, int v) {
        b[o] = (byte)v;
        b[o + 1] = (byte)(v >>> 8);
    }
}
