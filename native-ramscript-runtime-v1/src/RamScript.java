import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class RamScript {
    static final int FILE_SIZE = 0x3EC;
    static final int DATA_OFFSET = 0x004;
    static final int DATA_SIZE = 0x3E8;
    static final int SCRIPT_OFFSET = 0x008;
    static final int SCRIPT_SIZE = 995;

    private final byte[] data;

    private RamScript(byte[] data) {
        this.data = data;
    }

    static RamScript wonderCardScript(byte[] script) {
        if (script.length > SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "RamScript too large: " + script.length + " bytes; max is " + SCRIPT_SIZE
            );
        }

        byte[] result = new byte[FILE_SIZE];

        result[0x04] = 0x33;
        result[0x05] = (byte) 0xFF;
        result[0x06] = (byte) 0xFF;
        result[0x07] = (byte) 0xFF;

        System.arraycopy(script, 0, result, SCRIPT_OFFSET, script.length);

        int checksum = Crc16.calculate(result, DATA_OFFSET, DATA_SIZE);
        Binary.putU32(result, 0, checksum);

        return new RamScript(result);
    }

    static RamScript read(Path input) throws IOException {
        byte[] bytes = Files.readAllBytes(input);

        if (bytes.length != FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Invalid RamScript size: " + bytes.length + "; expected " + FILE_SIZE
            );
        }

        return new RamScript(bytes);
    }

    int storedChecksum() {
        return (int) (Binary.u32(data, 0) & 0xFFFF);
    }

    int calculatedChecksum() {
        return Crc16.calculate(data, DATA_OFFSET, DATA_SIZE);
    }

    boolean isChecksumValid() {
        return storedChecksum() == calculatedChecksum();
    }

    void write(Path output) throws IOException {
        Files.write(output, data);
    }

    byte[] bytes() {
        return data.clone();
    }

}
