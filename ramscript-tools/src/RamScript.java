import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/* Represents the complete RamScript stored at the end of a WC3 file.

   C structure:

   struct RamScript {
       u32 checksum;
       struct RamScriptData {
           u8 magic;
           u8 mapGroup;
           u8 mapNum;
           u8 objectId;
           u8 script[995];
       } data;
   };

   RamScriptData has 999 bytes of fields but sizeof(struct RamScriptData) is 1000 in the game build;
   that final alignment byte is included in the RamScript CRC. The complete RamScript is therefore 1004 bytes.
*/
final class RamScript {
    static final int WC3_FILE_SIZE = 0x58C;
    static final int WC3_OFFSET = 0x1A0;

    static final int SIZE = 0x3EC;       // 1004 bytes in the WC3 file
    static final int CHECKSUM_OFFSET = 0x000;
    static final int DATA_OFFSET = 0x004;
    static final int DATA_SIZE = 1000;   // sizeof(struct RamScriptData), including its final alignment byte

    static final int MAGIC_OFFSET = DATA_OFFSET;
    static final int MAP_GROUP_OFFSET = DATA_OFFSET + 1;
    static final int MAP_NUM_OFFSET = DATA_OFFSET + 2;
    static final int OBJECT_ID_OFFSET = DATA_OFFSET + 3;
    static final int SCRIPT_OFFSET = DATA_OFFSET + 4;
    static final int SCRIPT_SIZE = 995;

    static final int EXPECTED_MAGIC = 51; // RAM_SCRIPT_MAGIC (0x33)

    private final byte[] data;

    private RamScript(byte[] data) {
        this.data = data;
    }

    static RamScript fromWc3(Path path) throws IOException {
        byte[] wc3 = Files.readAllBytes(path);
        if (wc3.length != WC3_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Expected a 0x58C-byte WC3 file, but received " + wc3.length + " bytes"
            );
        }

        return new RamScript(Arrays.copyOfRange(wc3, WC3_OFFSET, WC3_OFFSET + SIZE));
    }

    static RamScript fromBinary(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length != SIZE) {
            throw new IllegalArgumentException(
                    "Expected a 0x3EC-byte RamScript binary, but received " + bytes.length + " bytes"
            );
        }
        return new RamScript(bytes);
    }

    static RamScript createWonderCard(byte[] script) {
        if (script.length > SCRIPT_SIZE) {
            throw new IllegalArgumentException(
                    "RamScript field script is " + script.length + " bytes; maximum is " + SCRIPT_SIZE
            );
        }

        byte[] data = new byte[SIZE];
        data[MAGIC_OFFSET] = (byte) EXPECTED_MAGIC;
        data[MAP_GROUP_OFFSET] = (byte) 0xFF;
        data[MAP_NUM_OFFSET] = (byte) 0xFF;
        data[OBJECT_ID_OFFSET] = (byte) 0xFF;
        System.arraycopy(script, 0, data, SCRIPT_OFFSET, script.length);

        int checksum = Crc16.calculate(data, DATA_OFFSET, DATA_SIZE);
        Binary.putU32(data, CHECKSUM_OFFSET, checksum);
        return new RamScript(data);
    }

    long storedChecksum() {
        return Binary.u32(data, CHECKSUM_OFFSET);
    }

    int calculatedChecksum() {
        return Crc16.calculate(data, DATA_OFFSET, DATA_SIZE);
    }

    boolean isChecksumValid() {
        return storedChecksum() == Integer.toUnsignedLong(calculatedChecksum());
    }

    int magic() {
        return Binary.u8(data, MAGIC_OFFSET);
    }

    int mapGroup() {
        return Binary.u8(data, MAP_GROUP_OFFSET);
    }

    int mapNum() {
        return Binary.u8(data, MAP_NUM_OFFSET);
    }

    int objectId() {
        return Binary.u8(data, OBJECT_ID_OFFSET);
    }

    boolean hasWonderCardHeader() {
        return magic() == EXPECTED_MAGIC
                && mapGroup() == 0xFF
                && mapNum() == 0xFF
                && objectId() == 0xFF;
    }

    byte[] scriptCopy() {
        return Arrays.copyOfRange(data, SCRIPT_OFFSET, SCRIPT_OFFSET + SCRIPT_SIZE);
    }

    byte[] bytesCopy() {
        return Arrays.copyOf(data, data.length);
    }

    int paddingByte() {
        return Binary.u8(data, SIZE - 1);
    }

    void writeBinary(Path output) throws IOException {
        createParentDirectories(output);
        Files.write(output, data);
    }

    void replaceInWc3(Path inputWc3, Path outputWc3) throws IOException {
        byte[] wc3 = Files.readAllBytes(inputWc3);
        if (wc3.length != WC3_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Expected a 0x58C-byte WC3 file, but received " + wc3.length + " bytes"
            );
        }

        System.arraycopy(data, 0, wc3, WC3_OFFSET, SIZE);
        createParentDirectories(outputWc3);
        Files.write(outputWc3, wc3);
    }

    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
    }
}
