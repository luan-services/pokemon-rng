import java.nio.charset.StandardCharsets;

/* Persistent identity for planner-generated toolkit installations.
   Stored at the tail of the validated SB2 region so a cleaner/upgrade tool can
   recognize our data without requiring direct access to the .sav file. */
final class InstallationManifest {
    static final int FORMAT_VERSION = 1;
    static final int SIZE = 32;
    static final int OFFSET = PayloadStorageArea.SAVE_BLOCK2.offset()
            + PayloadStorageArea.SAVE_BLOCK2.capacity() - SIZE; // SB2+0x0F00
    static final int SECONDARY_MARKER = 0x3CC35AA5;

    private static final byte[] MAGIC = "RSPK".getBytes(StandardCharsets.US_ASCII);

    private InstallationManifest() {}

    static byte[] build(PresetCompositionPlan plan) {
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        byte[] out = new byte[SIZE];
        System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
        putU16(out, 4, FORMAT_VERSION);
        putU16(out, 6, SIZE);
        putU32(out, 8, SECONDARY_MARKER);
        putU32(out, 12, fingerprint(plan));
        out[16] = (byte) plan.selections().size();
        out[17] = (byte) plan.hotkeyBindings();
        int flags = 0;
        if (plan.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) flags |= 1;
        if (plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)) flags |= 2;
        putU16(out, 18, flags);
        putU16(out, 20, plan.ramScriptBytes());
        putU16(out, 22, plan.sb1Bytes());
        // sb2Bytes includes this manifest in Build 34+; keep payload bytes separately.
        putU16(out, 24, Math.max(0, plan.sb2Bytes() - SIZE));
        putU16(out, 26, plan.concreteLayout().nativeCatalogSize());
        putU16(out, 28, Crc16.calculate(out, 0, 28));
        putU16(out, 30, 0);
        return out;
    }

    static boolean hasMagic(byte[] data) {
        if (data == null || data.length < SIZE) return false;
        for (int i = 0; i < MAGIC.length; i++) if (data[i] != MAGIC[i]) return false;
        return Binary.u16(data, 4) == FORMAT_VERSION
                && Binary.u16(data, 6) == SIZE
                && Binary.u32(data, 8) == Integer.toUnsignedLong(SECONDARY_MARKER)
                && Binary.u16(data, 28) == Crc16.calculate(data, 0, 28);
    }

    private static int fingerprint(PresetCompositionPlan plan) {
        int hash = 0x811C9DC5;
        for (var selected : plan.selections()) {
            hash = fnv(hash, selected.preset().id());
            hash = fnv(hash, selected.deployment().kind().name());
        }
        for (ConcretePresetAllocation allocation : plan.concreteLayout().allocations()) {
            hash = fnv(hash, allocation.presetId());
            hash = fnvInt(hash, allocation.sb1GatewayOffset());
            hash = fnvInt(hash, allocation.sb2FieldScriptOffset());
            hash = fnvInt(hash, allocation.sb2NativeModuleOffset());
        }
        return hash;
    }

    private static int fnv(int hash, String value) {
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= Byte.toUnsignedInt(b);
            hash *= 0x01000193;
        }
        return hash;
    }

    private static int fnvInt(int hash, int value) {
        for (int shift = 0; shift < 32; shift += 8) {
            hash ^= (value >>> shift) & 0xFF;
            hash *= 0x01000193;
        }
        return hash;
    }

    private static void putU16(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }

    private static void putU32(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }
}
