import java.util.ArrayList;
import java.util.List;

final class RuntimeV1ResidentBlocks {
    static final long ORIGINAL_VBLANK_TAIL = 0x03003EB4L;
    static final long ORIGINAL_VBLANK_LITERAL = 0x03003EC0L;
    static final long SUPERVISOR = 0x03003F42L;
    static final long PRIMARY_THUNK = 0x03003F98L;
    static final long FUNCTION_LITERAL = 0x03003F9CL;
    static final long MARKER = 0x03003FA1L;
    static final long SUPERVISOR_LITERALS = 0x03003FA4L;
    static final long STAGE2 = 0x03005032L;
    static final long STAGE1 = 0x03005082L;
    static final long WRAPPER = 0x03005310L;
    static final long FORMAT_VALIDATOR = 0x030053A8L;
    static final long SAFETY_GATE = 0x03005434L;

    private static final long GMAIN_CALLBACK1 = 0x030030F0L;

    record Block(long address, byte[] data) {}

    private RuntimeV1ResidentBlocks() {}

    static List<Block> build(RomProfile rom) {
        List<Block> blocks = new ArrayList<>();

        // Order is intentional. WRAPPER is last because Candidate 2 initially
        // stages its temporary bootstrap at the same address.
        blocks.add(new Block(ORIGINAL_VBLANK_TAIL,
                new byte[] { 0x02, 0x4B, 0x18, 0x47 }));
        blocks.add(new Block(ORIGINAL_VBLANK_LITERAL, le32(rom.originalVBlankThumb)));
        blocks.add(new Block(SUPERVISOR, supervisor()));
        blocks.add(new Block(SUPERVISOR_LITERALS, supervisorLiterals(rom)));
        blocks.add(new Block(STAGE2, stage2()));
        blocks.add(new Block(STAGE1, stage1()));
        blocks.add(new Block(PRIMARY_THUNK,
                new byte[] { 0x00, 0x4C, 0x20, 0x47 }));
        blocks.add(new Block(FUNCTION_LITERAL, le32(rom.getSavedRamScriptThumb)));
        blocks.add(new Block(MARKER, new byte[] { 0x00 }));
        blocks.add(new Block(SAFETY_GATE, safetyGate()));
        blocks.add(new Block(FORMAT_VALIDATOR, validator()));
        blocks.add(new Block(WRAPPER, wrapper(rom)));

        return List.copyOf(blocks);
    }

    static int totalResidentBytes(RomProfile rom) {
        int total = 0;
        for (Block block : build(rom)) total += block.data().length;
        return total;
    }

    private static byte[] supervisor() {
        return new byte[] {
                0x18,(byte)0xA3, 0x07,(byte)0xCB, 0x03,0x68, (byte)0x8B,0x42,
                (byte)0xB3,(byte)0xD1, 0x02,0x60, (byte)0xB1,(byte)0xE7
        };
    }

    private static byte[] supervisorLiterals(RomProfile rom) {
        byte[] out = new byte[12];
        putU32(out, 0, GMAIN_CALLBACK1);
        putU32(out, 4, rom.cb1OverworldThumb);
        putU32(out, 8, WRAPPER | 1L);
        return out;
    }

    private static byte[] stage2() {
        return new byte[] {
                0x0C,0x30, (byte)0xD9,0x21, (byte)0x89,0x00, 0x64,0x1A,
                (byte)0xFE,(byte)0xF7, (byte)0xAE,(byte)0xFF, 0x26,(byte)0xE0
        };
    }

    private static byte[] stage1() {
        return new byte[] {
                0x10,(byte)0xB5, (byte)0xFE,(byte)0xF7, (byte)0x88,(byte)0xFF,
                0x00,0x28, 0x00,(byte)0xD0, (byte)0x8C,(byte)0xE1, 0x10,(byte)0xBD
        };
    }

    private static byte[] safetyGate() {
        return new byte[] {
                0x10,0x78, 0x00,0x28, 0x00,(byte)0xD0, 0x73,(byte)0xE7,
                0x21,(byte)0xE6, 0x10,(byte)0xBD
        };
    }

    private static byte[] validator() {
        return new byte[] {
                0x41,(byte)0x89, (byte)0xA7,0x29,
                0x47,(byte)0xD1, 0x40,(byte)0xE6
        };
    }

    private static byte[] wrapper(RomProfile rom) {
        byte[] out = new byte[32];
        byte[] code = new byte[] {
                0x03,0x48, 0x00,0x68, 0x01,0x06, 0x05,(byte)0xD3,
                (byte)0x81,0x03, 0x03,(byte)0xD3, 0x03,0x4A, (byte)0x89,(byte)0xE0
        };
        System.arraycopy(code, 0, out, 0, code.length);
        putU32(out, 0x10, rom.heldKeysRaw);
        out[0x14] = 0x00;
        out[0x15] = 0x4B;
        out[0x16] = 0x18;
        out[0x17] = 0x47;
        putU32(out, 0x18, rom.cb1OverworldThumb);
        putU32(out, 0x1C, rom.lockFieldControls);
        return out;
    }

    private static byte[] le32(long value) {
        byte[] out = new byte[4];
        putU32(out, 0, value);
        return out;
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte)value;
        data[offset + 1] = (byte)(value >>> 8);
        data[offset + 2] = (byte)(value >>> 16);
        data[offset + 3] = (byte)(value >>> 24);
    }
}
