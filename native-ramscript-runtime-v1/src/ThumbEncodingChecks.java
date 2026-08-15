final class ThumbEncodingChecks {
    private ThumbEncodingChecks() {}

    static long decodeAdrTarget(long instructionAddress, int lowByte, int highByte) {
        int opcode = (highByte << 8) | lowByte;
        if ((opcode & 0xF800) != 0xA000)
            throw new IllegalArgumentException("Not a Thumb ADR instruction");

        int imm8 = opcode & 0xFF;
        long pc = (instructionAddress + 4) & ~3L;
        return pc + ((long) imm8 << 2);
    }

    static long decodeLdrLiteralTarget(long instructionAddress, int lowByte, int highByte) {
        int opcode = (highByte << 8) | lowByte;
        if ((opcode & 0xF800) != 0x4800)
            throw new IllegalArgumentException("Not a Thumb LDR literal instruction");

        int imm8 = opcode & 0xFF;
        long pc = (instructionAddress + 4) & ~3L;
        return pc + ((long) imm8 << 2);
    }

    static long decodeUnconditionalBranchTarget(long instructionAddress, int lowByte, int highByte) {
        int opcode = (highByte << 8) | lowByte;
        if ((opcode & 0xF800) != 0xE000)
            throw new IllegalArgumentException("Not a Thumb unconditional B instruction");

        int imm11 = opcode & 0x7FF;
        int signed = (imm11 & 0x400) != 0 ? imm11 - 0x800 : imm11;
        return instructionAddress + 4 + ((long) signed << 1);
    }

    static long decodeConditionalBranchTarget(long instructionAddress, int lowByte, int highByte) {
        int opcode = (highByte << 8) | lowByte;
        if ((opcode & 0xF000) != 0xD000 || (opcode & 0x0F00) == 0x0F00)
            throw new IllegalArgumentException("Not a Thumb conditional B instruction");

        int imm8 = opcode & 0xFF;
        int signed = (imm8 & 0x80) != 0 ? imm8 - 0x100 : imm8;
        return instructionAddress + 4 + ((long) signed << 1);
    }


    static long decodeLongBranchWithLinkTarget(
            long instructionAddress,
            int firstLowByte,
            int firstHighByte,
            int secondLowByte,
            int secondHighByte) {
        int first = (firstHighByte << 8) | firstLowByte;
        int second = (secondHighByte << 8) | secondLowByte;

        if ((first & 0xF800) != 0xF000)
            throw new IllegalArgumentException("Not a Thumb BL first half");
        if ((second & 0xF800) != 0xF800)
            throw new IllegalArgumentException("Not a Thumb BL second half");

        int high11 = first & 0x7FF;
        int signedHigh = (high11 & 0x400) != 0 ? high11 - 0x800 : high11;
        int low11 = second & 0x7FF;

        long offset = ((long)signedHigh << 12) | ((long)low11 << 1);
        return instructionAddress + 4 + offset;
    }

    static long u32(byte[] data, int offset) {
        return Integer.toUnsignedLong(
                (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24)
        );
    }
}
