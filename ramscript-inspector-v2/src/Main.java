import java.nio.file.Path;
import java.util.List;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                printUsage();
                return;
            }

            String command = args[0].toLowerCase();
            Path input = Path.of(args[1]);

            switch (command) {
                case "inspect" -> inspect(RamScript.fromWc3(input));
                case "inspect-bin" -> inspect(RamScript.fromBinary(input));
                default -> printUsage();
            }
        } catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void inspect(RamScript ramScript) {
        System.out.println("=== RamScript ===");
        System.out.printf("Stored checksum:     0x%08X%n", ramScript.storedChecksum());
        System.out.printf("Calculated checksum: 0x%04X%n", ramScript.calculatedChecksum());
        System.out.println("Checksum valid:      " + ramScript.isChecksumValid());
        System.out.println();

        System.out.printf("Magic:               %d (0x%02X)%n", ramScript.magic(), ramScript.magic());
        System.out.printf("Map group:           %d (0x%02X)%n", ramScript.mapGroup(), ramScript.mapGroup());
        System.out.printf("Map number:          %d (0x%02X)%n", ramScript.mapNum(), ramScript.mapNum());
        System.out.printf("Object ID:           %d (0x%02X)%n", ramScript.objectId(), ramScript.objectId());
        System.out.println("Wonder Card header:  " + ramScript.hasWonderCardHeader());
        System.out.printf("Final padding byte:  0x%02X%n", ramScript.paddingByte());
        System.out.println();

        byte[] script = ramScript.scriptCopy();
        ScriptDisassembler disassembler = new ScriptDisassembler(script);
        List<ScriptInstruction> instructions = disassembler.disassemble();

        System.out.println("=== Field Script Disassembly ===");
        for (ScriptInstruction instruction : instructions) {
            String raw = bytesToHex(instruction.rawBytes());
            String text = instruction.name();
            if (!instruction.operands().isEmpty()) {
                text += " " + instruction.operands();
            }

            System.out.printf("%04X  %-28s  %-48s", instruction.offset(), raw, text);
            if (!instruction.annotation().isEmpty()) {
                System.out.print(" ; " + instruction.annotation());
            }
            System.out.println();
        }

        if (disassembler.virtualBase() != null) {
            System.out.printf("%nVirtual base: 0x%08X%n", disassembler.virtualBase());
        }

        System.out.println();
        System.out.println("Note: only known control-flow paths are disassembled. Embedded text/data is intentionally not treated as bytecode.");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i != 0) result.append(' ');
            result.append(String.format("%02X", Byte.toUnsignedInt(bytes[i])));
        }
        return result.toString();
    }

    private static void printUsage() {
        System.out.println("RamScript Tools");
        System.out.println();
        System.out.println("Inspect a complete WC3:");
        System.out.println("  java -cp out Main inspect path\\event.wc3");
        System.out.println();
        System.out.println("Inspect an extracted 0x3EC-byte RamScript:");
        System.out.println("  java -cp out Main inspect-bin path\\ramscript.bin");
    }
}
