
import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java ... <input.sav> <event.wc3> <output.sav>");
            System.exit(1);
        }

        Path input = Path.of(args[0]);
        Path wc3Path = Path.of(args[1]);
        Path output = Path.of(args[2]);

        try {
            FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
            Wc3File wc3 = Wc3File.load(wc3Path);
            FireRedLeafGreenSave.InjectionResult result = save.inject(wc3);
            save.write(output);

            System.out.printf("WC3 injected successfully.%n");
            System.out.printf("Flag ID: %d%n", result.wonderCardFlagId());
            System.out.printf("Active slot: %d%n", result.slotIndex() + 1);
            System.out.printf("Save counter: %s%n", Integer.toUnsignedString(result.saveCounter()));
            System.out.printf("Physical sector: %d%n", result.physicalSector());
            System.out.printf("New sector checksum: 0x%04X%n", result.sectorChecksum());
            System.out.printf("Output: %s%n", output.toAbsolutePath());
        } catch (Exception exception) {
            System.err.println("Injection failed: " + exception.getMessage());
            exception.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
