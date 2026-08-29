import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/*
   Wonder-Card-only compression for the validated CTD1 descriptor.

   The persistent descriptor and CustomTrainerBattleRuntimeV2 are unchanged.
   This stream is expanded by the card's descriptor loader before the runtime
   sees it.

   Commands:
     00..7F : literal run, length = command + 1, followed by bytes
     80..BF : zero run,    length = (command & 3F) + 1
     C0..FD : FF run,      length = (command & 3F) + 1
     FE     : emit production default OT ID + IV/ability word (8 bytes)
     FF     : end of stream

   Production policy validated before encoding:
     ppBonuses  = 0
     OT ID      = DEFAULT_OT_ID
     IVs        = 31/31/31/31/31/31
     abilityNum = 0 by default; abilityNum=1 is transported literally when authored
     friendship = 255
     nickname   = authored as the species' normal name (the descriptor still
                  stores its normal fixed 11-byte field; FF padding compresses)
*/
final class CustomTrainerCompactTransport {
    static final long DEFAULT_OT_ID = 0x43545231L;
    static final long DEFAULT_IV_WORD = 0x3FFFFFFFL;

    private static final byte[] DEFAULT_OT_IV = new byte[] {
            0x31, 0x52, 0x54, 0x43,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, 0x3F
    };

    private CustomTrainerCompactTransport() {}

    static byte[] encode(CustomTrainerBattleSpec spec, int localId, int afterBattleOffset) {
        validateDefaults(spec);
        return compress(CustomTrainerBattleDescriptor.encode(spec, localId, afterBattleOffset));
    }

    static byte[] compress(byte[] descriptor) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int p = 0;
        while (p < descriptor.length) {
            if (matchesDefaultOtIv(descriptor, p)) {
                out.write(0xFE);
                p += DEFAULT_OT_IV.length;
                continue;
            }

            int specialRun = repeatedSpecialRun(descriptor, p);
            if (specialRun >= 2) {
                int value = descriptor[p] & 0xFF;
                int max = value == 0 ? 64 : 62; // FE/FF are reserved commands
                int len = Math.min(specialRun, max);
                out.write((value == 0 ? 0x80 : 0xC0) | (len - 1));
                p += len;
                continue;
            }

            int start = p++;
            while (p < descriptor.length && p - start < 128) {
                if (matchesDefaultOtIv(descriptor, p)) break;
                if (repeatedSpecialRun(descriptor, p) >= 2) break;
                p++;
            }
            int len = p - start;
            out.write(len - 1);
            out.write(descriptor, start, len);
        }
        out.write(0xFF);
        return out.toByteArray();
    }

    /* Java oracle for the native decoder. */
    static byte[] expandForTest(byte[] compact) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int p = 0;
        boolean ended = false;
        while (p < compact.length) {
            int cmd = compact[p++] & 0xFF;
            if (cmd == 0xFF) { ended = true; break; }
            if (cmd == 0xFE) {
                out.writeBytes(DEFAULT_OT_IV);
                continue;
            }
            if (cmd < 0x80) {
                int len = cmd + 1;
                if (p + len > compact.length) throw new IllegalArgumentException("truncated compact literal");
                out.write(compact, p, len);
                p += len;
                continue;
            }
            if (cmd < 0xC0) {
                int len = (cmd & 0x3F) + 1;
                for (int i = 0; i < len; i++) out.write(0);
                continue;
            }
            int len = (cmd & 0x3F) + 1;
            for (int i = 0; i < len; i++) out.write(0xFF);
        }
        if (!ended || p != compact.length) throw new IllegalArgumentException("compact descriptor terminator invalid");
        byte[] descriptor = out.toByteArray();
        if (descriptor.length < CustomTrainerBattleDescriptor.HEADER_SIZE)
            throw new IllegalArgumentException("expanded descriptor too small");
        int declared = u16(descriptor, CustomTrainerBattleDescriptor.OFF_SIZE);
        if (declared != descriptor.length)
            throw new IllegalArgumentException("expanded descriptor size mismatch: " + descriptor.length + "/" + declared);
        return descriptor;
    }

    static void validateDefaults(CustomTrainerBattleSpec spec) {
        for (EReaderTrainerData.Mon m : spec.party()) {
            if (m.ppBonuses() != 0) throw new IllegalArgumentException("Custom Trainer requires ppBonuses=0");
            if (m.otId() != DEFAULT_OT_ID) throw new IllegalArgumentException("Custom Trainer requires default OT ID");
            if (m.hpIV()!=31 || m.attackIV()!=31 || m.defenseIV()!=31 || m.speedIV()!=31 || m.spAttackIV()!=31 || m.spDefenseIV()!=31)
                throw new IllegalArgumentException("Custom Trainer requires all IVs=31");
            if (m.abilityNum() < 0 || m.abilityNum() > 1)
                throw new IllegalArgumentException("Custom Trainer abilityNum must be 0 or 1");
            if (m.friendship() != 255) throw new IllegalArgumentException("Custom Trainer requires friendship=255");
        }
    }

    private static boolean matchesDefaultOtIv(byte[] d, int p) {
        // The native FE token expands with aligned u32 stores. Emit it only at
        // the actual BattleTowerPokemon OT-ID field, never merely because the
        // same byte pattern happened to occur elsewhere in the descriptor.
        if (p < CustomTrainerBattleDescriptor.PARTY_OFFSET) return false;
        int monRelative = (p - CustomTrainerBattleDescriptor.PARTY_OFFSET) % EReaderTrainerData.MON_SIZE;
        if (monRelative != 0x14) return false;
        if (p + DEFAULT_OT_IV.length > d.length) return false;
        for (int i = 0; i < DEFAULT_OT_IV.length; i++) if (d[p+i] != DEFAULT_OT_IV[i]) return false;
        return true;
    }

    private static int repeatedSpecialRun(byte[] d, int p) {
        int v = d[p] & 0xFF;
        if (v != 0 && v != 0xFF) return 0;
        int max = v == 0 ? 64 : 62;
        int q = p + 1;
        while (q < d.length && q - p < max && (d[q] & 0xFF) == v) q++;
        return q - p;
    }

    private static int u16(byte[] d, int o) { return (d[o]&0xFF) | ((d[o+1]&0xFF)<<8); }
}
