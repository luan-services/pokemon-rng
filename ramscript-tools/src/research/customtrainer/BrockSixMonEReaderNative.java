/*
   Probe 3B combined staged helper.

   Entry 0 installs the stock 0xBC e-Reader trainer descriptor in SaveBlock2.
   Entry EXTRA_ENTRY_OFFSET is called immediately after StartSpecialBattle(2)
   and materializes slots 3..5 of gEnemyParty with the stock
   CreateBattleTowerMon routine.

   This deliberately keeps BATTLE_TYPE_EREADER_TRAINER. Probe 3B asks only
   whether the battle engine accepts six custom mons when the final three are
   supplied after the stock e-Reader launcher has zeroed/created slots 0..2.
*/
final class BrockSixMonEReaderNative {
    static final int INSTALL_HEADER_SIZE = 0x28;
    static final int EXTRA_ENTRY_OFFSET = INSTALL_HEADER_SIZE + EReaderTrainerData.SIZE; // 0xE4
    static final int EXTRA_CODE_SIZE = 0x28;
    static final int EXTRA_DATA_OFFSET = EXTRA_ENTRY_OFFSET + EXTRA_CODE_SIZE; // 0x10C

    private BrockSixMonEReaderNative() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        byte[] trainer = EReaderTrainerData.brockIdentityProbeTrainer();
        byte[] extra = EReaderTrainerData.brockExtraThreeProbeMons();
        byte[] out = new byte[EXTRA_DATA_OFFSET + extra.length];

        // Entry 0: same validated installer used by Probe 3A.
        putU16(out, 0x00, 0xB570); // push {r4,r5,r6,lr}
        putU16(out, 0x02, 0x4C08); // ldr r4, =gSaveBlock2Ptr @ +0x24
        putU16(out, 0x04, 0x6824); // ldr r4, [r4]
        putU16(out, 0x06, 0x254A); // movs r5,#0x4A
        putU16(out, 0x08, 0x012D); // lsls r5,#4 => 0x4A0
        putU16(out, 0x0A, 0x1964); // adds r4,r4,r5
        putU16(out, 0x0C, 0xA506); // adr r5, trainer @ +0x28
        putU16(out, 0x0E, 0x26BC); // movs r6,#188
        putU16(out, 0x10, 0x7828); // copy loop
        putU16(out, 0x12, 0x7020);
        putU16(out, 0x14, 0x3501);
        putU16(out, 0x16, 0x3401);
        putU16(out, 0x18, 0x3E01);
        putU16(out, 0x1A, 0xD1F9);
        putU16(out, 0x1C, 0x3410); // SB2+0x56C
        putU16(out, 0x1E, 0x20C8); // e-reader trainer id 200
        putU16(out, 0x20, 0x7020);
        putU16(out, 0x22, 0xBD70);
        putU32(out, 0x24, rom.saveBlock2Ptr);
        System.arraycopy(trainer, 0, out, INSTALL_HEADER_SIZE, trainer.length);

        // Entry +0xE4: create gEnemyParty[3], [4], [5].
        int b = EXTRA_ENTRY_OFFSET;
        int literalParty = b + 0x20;
        int literalCreate = b + 0x24;
        int data = EXTRA_DATA_OFFSET;

        putU16(out, b + 0x00, 0xB5F0); // push {r4-r7,lr}
        putU16(out, b + 0x02, ldrLiteral(4, b + 0x02, literalParty));
        putU16(out, b + 0x04, ldrLiteral(7, b + 0x04, literalCreate));
        putU16(out, b + 0x06, adr(5, b + 0x06, data));
        putU16(out, b + 0x08, 0x2603); // movs r6,#3
        int loop = b + 0x0A;
        putU16(out, b + 0x0A, 0x1C20); // adds r0,r4,#0
        putU16(out, b + 0x0C, 0x1C29); // adds r1,r5,#0
        putThumbBl(out, b + 0x0E, b + 0x1E); // bl thunk
        putU16(out, b + 0x12, 0x3464); // sizeof(struct Pokemon)=0x64
        putU16(out, b + 0x14, 0x352C); // sizeof(BattleTowerPokemon)=0x2C
        putU16(out, b + 0x16, 0x3E01);
        putU16(out, b + 0x18, branchCond(1, b + 0x18, loop)); // bne
        putU16(out, b + 0x1A, 0xBDF0); // pop {r4-r7,pc}
        putU16(out, b + 0x1C, 0x46C0); // nop/alignment
        putU16(out, b + 0x1E, 0x4738); // bx r7
        putU32(out, literalParty, 0x0202402CL + 3L * 0x64L); // gEnemyParty slot 3
        putU32(out, literalCreate, createBattleTowerMonThumb(rom));
        System.arraycopy(extra, 0, out, data, extra.length);

        return new NativeHelper(stagingAddress, out);
    }

    private static long createBattleTowerMonThumb(RomProfile rom) {
        return switch (rom) {
            case FIRE_RED_EN_10, LEAF_GREEN_EN_10 -> 0x0803E0A5L;
            case FIRE_RED_EN_11, LEAF_GREEN_EN_11 -> 0x0803E0B9L;
        };
    }

    private static int ldrLiteral(int rt, int insn, int literal) {
        int base = (insn + 4) & ~3;
        int delta = literal - base;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) throw new IllegalArgumentException("literal range");
        return 0x4800 | (rt << 8) | (delta / 4);
    }

    private static int adr(int rd, int insn, int target) {
        int base = (insn + 4) & ~3;
        int delta = target - base;
        if (delta < 0 || (delta & 3) != 0 || delta / 4 > 255) throw new IllegalArgumentException("adr range");
        return 0xA000 | (rd << 8) | (delta / 4);
    }

    private static int branchCond(int cond, int insn, int target) {
        int delta = target - (insn + 4);
        if ((delta & 1) != 0 || delta / 2 < -128 || delta / 2 > 127) throw new IllegalArgumentException("branch range");
        return 0xD000 | (cond << 8) | ((delta / 2) & 0xFF);
    }

    private static void putThumbBl(byte[] data, int sourceOffset, int targetOffset) {
        int pc = sourceOffset + 4;
        int delta = targetOffset - pc;
        if ((delta & 1) != 0 || delta < -(1 << 22) || delta >= (1 << 22)) throw new IllegalArgumentException("Thumb BL range");
        putU16(data, sourceOffset, 0xF000 | ((delta >> 12) & 0x7FF));
        putU16(data, sourceOffset + 2, 0xF800 | ((delta >> 1) & 0x7FF));
    }

    private static void putU16(byte[] d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}
    private static void putU32(byte[] d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}
}
