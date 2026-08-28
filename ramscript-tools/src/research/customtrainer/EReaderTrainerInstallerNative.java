/*
   Relocation-safe native installer stored inside the RamScript itself.

   It copies the embedded 0xBC-byte BattleTowerEReaderTrainer blob to
   *gSaveBlock2Ptr + 0x4A0, then sets battleTowerTrainerId at SB2 + 0x56C to
   BATTLE_TOWER_EREADER_TRAINER_ID (200).

   The code and its source blob relocate together, so ADR remains valid.
*/
final class EReaderTrainerInstallerNative {
    static final int HEADER_SIZE = 0x28;
    static final int EREADER_TRAINER_SB2_OFFSET = 0x4A0;
    static final int BATTLE_TOWER_TRAINER_ID_SB2_OFFSET = 0x56C;
    static final int EREADER_TRAINER_ID = 200;

    private EReaderTrainerInstallerNative() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress, byte[] trainerData) {
        return new NativeHelper(stagingAddress, buildBytes(rom, trainerData));
    }

    static byte[] build(RomProfile rom, byte[] trainerData) {
        return buildBytes(rom, trainerData);
    }

    private static byte[] buildBytes(RomProfile rom, byte[] trainerData) {
        if (trainerData.length != EReaderTrainerData.SIZE) throw new IllegalArgumentException("e-reader trainer must be 0xBC bytes");
        byte[] out = new byte[HEADER_SIZE + trainerData.length];
        putU16(out, 0x00, 0xB570); // push {r4,r5,r6,lr}
        putU16(out, 0x02, 0x4C08); // ldr r4, =gSaveBlock2Ptr @ +0x24
        putU16(out, 0x04, 0x6824); // ldr r4, [r4]
        putU16(out, 0x06, 0x254A); // movs r5, #0x4A
        putU16(out, 0x08, 0x012D); // lsls r5, r5, #4 -> 0x4A0
        putU16(out, 0x0A, 0x1964); // adds r4, r4, r5
        putU16(out, 0x0C, 0xA506); // adr r5, data @ +0x28
        putU16(out, 0x0E, 0x26BC); // movs r6, #188
        putU16(out, 0x10, 0x7828); // loop: ldrb r0, [r5]
        putU16(out, 0x12, 0x7020); // strb r0, [r4]
        putU16(out, 0x14, 0x3501); // adds r5, #1
        putU16(out, 0x16, 0x3401); // adds r4, #1
        putU16(out, 0x18, 0x3E01); // subs r6, #1
        putU16(out, 0x1A, 0xD1F9); // bne loop
        // r4 == SB2 + 0x55C now; +0x10 == battleTowerTrainerId @ 0x56C
        putU16(out, 0x1C, 0x3410); // adds r4, #0x10
        putU16(out, 0x1E, 0x20C8); // movs r0, #200
        putU16(out, 0x20, 0x7020); // strb r0, [r4]
        putU16(out, 0x22, 0xBD70); // pop {r4,r5,r6,pc}
        putU32(out, 0x24, rom.saveBlock2Ptr);
        System.arraycopy(trainerData, 0, out, HEADER_SIZE, trainerData.length);
        return out;
    }

    private static void putU16(byte[] d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}    
    private static void putU32(byte[] d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}    
}
