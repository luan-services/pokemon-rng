import java.util.Arrays;

/*
   Minimal serializer for FR/LG's stock BattleTowerEReaderTrainer save struct.

   Layout (0xBC bytes):
     0x00  u8  unk0
     0x01  u8  trainerClass (facility class)
     0x02  u16 winStreak
     0x04  u8  name[8]
     0x0C  u8  trainerId[4]
     0x10  u16 greeting[6]
     0x1C  u16 farewellPlayerLost[6]
     0x28  u16 farewellPlayerWon[6]
     0x34  BattleTowerPokemon party[3] (0x2C each)
     0xB8  u32 checksum

   The checksum is the stock additive u32 checksum over every word except the
   final checksum word.
*/
final class EReaderTrainerData {
    static final int SIZE = 0xBC;
    static final int PARTY_OFFSET = 0x34;
    static final int MON_SIZE = 0x2C;
    static final int CHECKSUM_OFFSET = 0xB8;

    // FR/LG constants/trainers.h: FACILITY_CLASS_YOUNGSTER = 88.
    static final int FACILITY_CLASS_YOUNGSTER = 88;
    // FR/LG constants/trainers.h. In the facility-class tables this maps to
    // the male Kanto Gym Leader presentation (Brock front sprite / LEADER class).
    static final int FACILITY_CLASS_LEADER_M = 120;

    record Mon(int species, int level, int heldItem, int[] moves, int ppBonuses,
               int hpEV, int attackEV, int defenseEV, int speedEV, int spAttackEV, int spDefenseEV,
               long otId, int hpIV, int attackIV, int defenseIV, int speedIV, int spAttackIV, int spDefenseIV,
               int abilityNum, long personality, String nickname, int friendship) {
        Mon {
            if (moves == null || moves.length != 4) throw new IllegalArgumentException("moves must have length 4");
        }
    }

    private EReaderTrainerData() {}

    static byte[] probeTrainer() {
        byte[] out = new byte[SIZE];
        out[0x00] = 1;
        out[0x01] = (byte) FACILITY_CLASS_YOUNGSTER;
        putU16(out, 0x02, 1);
        putFixedString(out, 0x04, 8, "CUSTOM");
        putU32(out, 0x0C, 0x12345678L);

        // Easy Chat word ids. Zero is a valid word; the first probe is about
        // party/name/sprite/lifecycle, not authored dialogue yet.
        for (int i = 0; i < 18; i++) putU16(out, 0x10 + i * 2, 0);

        // Three unmistakable Kanto mons. StartSpecialBattle(case 2) always
        // materializes exactly three BattleTowerPokemon entries.
        Mon[] mons = {
                new Mon(151, 5, 0, new int[]{1, 0, 0, 0}, 0,
                        0,0,0,0,0,0, 0x11112222L,
                        31,31,31,31,31,31, 0, 0x12345678L, "MEW", 255),
                new Mon(25, 7, 0, new int[]{84, 45, 0, 0}, 0,
                        0,0,0,0,0,0, 0x33334444L,
                        20,20,20,20,20,20, 0, 0x23456789L, "PIKACHU", 255),
                new Mon(129, 9, 0, new int[]{33, 0, 0, 0}, 0,
                        0,0,0,0,0,0, 0x55556666L,
                        10,10,10,10,10,10, 0, 0x3456789AL, "MAGIKARP", 255)
        };
        for (int i = 0; i < mons.length; i++) putMon(out, PARTY_OFFSET + i * MON_SIZE, mons[i]);

        long checksum = 0;
        for (int i = 0; i < CHECKSUM_OFFSET; i += 4)
            checksum = (checksum + getU32(out, i)) & 0xFFFFFFFFL;
        putU32(out, CHECKSUM_OFFSET, checksum);
        return out;
    }


    static byte[] brockIdentityProbeTrainer() {
        byte[] out = new byte[SIZE];
        out[0x00] = 1;
        out[0x01] = (byte) FACILITY_CLASS_LEADER_M;
        putU16(out, 0x02, 1);
        putFixedString(out, 0x04, 8, "BROCK");
        putU32(out, 0x0C, 0xB20C2026L);

        for (int i = 0; i < 18; i++) putU16(out, 0x10 + i * 2, 0);

        // Still exactly three mons: this probe isolates identity only.
        // Levels/species are deliberately unlike Brock's story battle.
        Mon[] mons = {
                new Mon(76, 50, 0, new int[]{89, 88, 33, 0}, 0,
                        0,0,0,0,0,0, 0xB0010001L,
                        31,31,31,31,31,31, 0, 0x51000001L, "GOLEM", 255),
                new Mon(112, 52, 0, new int[]{89, 30, 33, 0}, 0,
                        0,0,0,0,0,0, 0xB0010002L,
                        31,31,31,31,31,31, 0, 0x52000002L, "RHYDON", 255),
                new Mon(95, 54, 0, new int[]{89, 88, 20, 0}, 0,
                        0,0,0,0,0,0, 0xB0010003L,
                        31,31,31,31,31,31, 0, 0x53000003L, "ONIX", 255)
        };
        for (int i = 0; i < mons.length; i++) putMon(out, PARTY_OFFSET + i * MON_SIZE, mons[i]);

        long checksum = 0;
        for (int i = 0; i < CHECKSUM_OFFSET; i += 4)
            checksum = (checksum + getU32(out, i)) & 0xFFFFFFFFL;
        putU32(out, CHECKSUM_OFFSET, checksum);
        return out;
    }


    static byte[] brockExtraThreeProbeMons() {
        Mon[] mons = {
                new Mon(142, 56, 0, new int[]{17, 44, 89, 0}, 0,
                        0,0,0,0,0,0, 0xB0010004L,
                        31,31,31,31,31,31, 0, 0x54000004L, "AERODACTYL", 255),
                new Mon(139, 58, 0, new int[]{56, 44, 89, 0}, 0,
                        0,0,0,0,0,0, 0xB0010005L,
                        31,31,31,31,31,31, 0, 0x55000005L, "OMASTAR", 255),
                new Mon(141, 60, 0, new int[]{10, 44, 89, 0}, 0,
                        0,0,0,0,0,0, 0xB0010006L,
                        31,31,31,31,31,31, 0, 0x56000006L, "KABUTOPS", 255)
        };
        byte[] out = new byte[mons.length * MON_SIZE];
        for (int i = 0; i < mons.length; i++) putMon(out, i * MON_SIZE, mons[i]);
        return out;
    }

    static byte[] brockOneProbeMon() {
        Mon mon = new Mon(95, 14, 0, new int[]{33, 20, 88, 0}, 0,
                0,0,0,0,0,0, 0xB0010001L,
                20,20,20,20,20,20, 0, 0x51000001L, "ONIX", 255);
        byte[] out = new byte[MON_SIZE];
        putMon(out, 0, mon);
        return out;
    }

    static byte[] brockSixProbeMons() {
        byte[] first = brockIdentityProbeTrainer();
        byte[] extra = brockExtraThreeProbeMons();
        byte[] out = new byte[6 * MON_SIZE];
        System.arraycopy(first, PARTY_OFFSET, out, 0, 3 * MON_SIZE);
        System.arraycopy(extra, 0, out, 3 * MON_SIZE, extra.length);
        return out;
    }

    static byte[] encodeMon(Mon m) {
        byte[] out = new byte[MON_SIZE];
        putMon(out, 0, m);
        return out;
    }

    private static void putMon(byte[] out, int o, Mon m) {
        putU16(out, o + 0x00, m.species());
        putU16(out, o + 0x02, m.heldItem());
        for (int i = 0; i < 4; i++) putU16(out, o + 0x04 + i * 2, m.moves()[i]);
        out[o + 0x0C] = (byte)m.level();
        out[o + 0x0D] = (byte)m.ppBonuses();
        out[o + 0x0E] = (byte)m.hpEV();
        out[o + 0x0F] = (byte)m.attackEV();
        out[o + 0x10] = (byte)m.defenseEV();
        out[o + 0x11] = (byte)m.speedEV();
        out[o + 0x12] = (byte)m.spAttackEV();
        out[o + 0x13] = (byte)m.spDefenseEV();
        putU32(out, o + 0x14, m.otId());
        long ivs = (m.hpIV() & 31L)
                | ((m.attackIV() & 31L) << 5)
                | ((m.defenseIV() & 31L) << 10)
                | ((m.speedIV() & 31L) << 15)
                | ((m.spAttackIV() & 31L) << 20)
                | ((m.spDefenseIV() & 31L) << 25)
                | ((m.abilityNum() & 1L) << 31);
        putU32(out, o + 0x18, ivs);
        putU32(out, o + 0x1C, m.personality());
        putFixedString(out, o + 0x20, 11, m.nickname());
        out[o + 0x2B] = (byte)m.friendship();
    }

    private static void putFixedString(byte[] out, int offset, int size, String text) {
        byte[] encoded = Gen3TextCodec.encodeString(text);
        Arrays.fill(out, offset, offset + size, (byte)0xFF);
        System.arraycopy(encoded, 0, out, offset, Math.min(size, encoded.length));
        out[offset + size - 1] = (byte)0xFF;
    }

    private static long getU32(byte[] d, int o) {
        return (d[o] & 0xFFL) | ((d[o+1] & 0xFFL) << 8) | ((d[o+2] & 0xFFL) << 16) | ((d[o+3] & 0xFFL) << 24);
    }
    private static void putU16(byte[] d, int o, long v) { d[o]=(byte)v; d[o+1]=(byte)(v>>>8); }
    private static void putU32(byte[] d, int o, long v) { d[o]=(byte)v; d[o+1]=(byte)(v>>>8); d[o+2]=(byte)(v>>>16); d[o+3]=(byte)(v>>>24); }
}
