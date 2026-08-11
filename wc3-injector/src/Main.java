import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }

            /* backwards compatibility with the original CLI:
               java -cp out Main input.sav event.wc3 output.sav */
            if (args.length == 3 && !isKnownCommand(args[0])) {
                System.err.println(
                        "Warning: legacy syntax detected. Prefer: "
                        + "java -cp out Main inject <input.sav> <event.wc3> <output.sav>"
                );
                inject(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]));
                return;
            }

            switch (args[0].toLowerCase()) {
                case "inject" -> {
                    requireArgCount(args, 4, "inject <input.sav> <event.wc3> <output.sav>");
                    inject(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
                }
                case "extract" -> {
                    requireArgCount(args, 3, "extract <input.sav> <output.wc3>");
                    extract(Path.of(args[1]), Path.of(args[2]));
                }
                case "inspect-save" -> {
                    requireArgCount(args, 2, "inspect-save <input.sav>");
                    inspectSave(Path.of(args[1]));
                }
                case "verify-wc3" -> {
                    requireArgCount(args, 2, "verify-wc3 <event.wc3>");
                    verifyWc3(Path.of(args[1]));
                }
                case "help", "--help", "-h" -> printUsage();
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception exception) {
            System.err.println("Command failed: " + exception.getMessage());
            System.exit(2);
        }
    }

    private static void inject(Path input, Path wc3Path, Path output) throws Exception {
        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
        Wc3File wc3 = Wc3File.load(wc3Path);
        printWc3Warnings(wc3);
        FireRedLeafGreenSave.InjectionResult result = save.inject(wc3);
        save.write(output);

        System.out.printf("WC3 injected successfully.%n");
        System.out.printf("Flag ID: %d%n", result.wonderCardFlagId());
        System.out.printf("Active slot: %d%n", result.slotIndex() + 1);
        System.out.printf("Save counter: %s%n", Integer.toUnsignedString(result.saveCounter()));
        System.out.printf("Physical sector: %d%n", result.physicalSector());
        System.out.printf("New sector checksum: 0x%04X%n", result.sectorChecksum());
        System.out.printf("Output: %s%n", output.toAbsolutePath());
    }

    private static void extract(Path input, Path output) throws Exception {
        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
        FireRedLeafGreenSave.ExtractionResult result = save.extractWc3();
        Wc3File wc3 = result.wc3();
        wc3.write(output);

        System.out.println("WC3 extracted successfully.");
        System.out.printf("Active slot: %d%n", result.slotIndex() + 1);
        System.out.printf("Save counter: %s%n", Integer.toUnsignedString(result.saveCounter()));
        System.out.printf("Physical sector: %d%n", result.physicalSector());
        System.out.printf("Flag ID: %d%n", wc3.wonderCard().flagId());
        System.out.printf("Card CRC valid: %s%n", wc3.isCardCrcValid());
        System.out.printf("RamScript checksum valid: %s%n", wc3.isRamScriptChecksumValid());
        printWc3Warnings(wc3);
        System.out.printf("Output: %s%n", output.toAbsolutePath());
    }

    private static void inspectSave(Path input) throws Exception {
        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
        FireRedLeafGreenSave.SaveInspection inspection = save.inspect();

        printSlot(inspection.slot1());
        printSlot(inspection.slot2());

        if (inspection.activeSlot() == null) {
            System.out.println("Active slot: none (no fully valid save slot)");
        } else {
            System.out.printf(
                    "Active slot: %d (counter %s)%n",
                    inspection.activeSlot().slotIndex() + 1,
                    Integer.toUnsignedString(inspection.activeSlot().counter())
            );
        }
    }

    private static void verifyWc3(Path input) throws Exception {
        Wc3File wc3 = Wc3File.load(input);

        System.out.printf("Flag ID:                  %d%n", wc3.wonderCard().flagId());
        System.out.printf("Icon species:             %d%n", wc3.wonderCard().iconSpecies());
        System.out.printf("Stored card CRC:          0x%04X%n", wc3.storedCardCrc());
        System.out.printf("Calculated card CRC:      0x%04X%n", wc3.calculatedCardCrc());
        System.out.printf("Card CRC valid:           %s%n", wc3.isCardCrcValid());
        System.out.printf("Stored RamScript checksum:     0x%04X%n", wc3.storedRamScriptChecksum());
        System.out.printf("Calculated RamScript checksum: 0x%04X%n", wc3.calculatedRamScriptChecksum());
        System.out.printf("RamScript checksum valid:      %s%n", wc3.isRamScriptChecksumValid());
        printWc3Warnings(wc3);
    }

    private static void printWc3Warnings(Wc3File wc3) {
        String[] warnings = wc3.validationWarnings();

        if (warnings.length == 0) {
            return;
        }

        System.out.println("Warnings:");
        for (String warning : warnings) {
            System.out.println("  - " + warning);
        }
    }

    private static void printSlot(FireRedLeafGreenSave.SlotInfo slot) {
        System.out.printf(
                "Slot %d: %s",
                slot.slotIndex() + 1,
                slot.valid() ? "VALID" : "INVALID"
        );

        if (slot.valid()) {
            System.out.printf(
                    " (counter %s)",
                    Integer.toUnsignedString(slot.counter())
            );
        }

        System.out.println(" - " + slot.status());
    }

    private static boolean isKnownCommand(String value) {
        return switch (value.toLowerCase()) {
            case "inject", "extract", "inspect-save", "verify-wc3", "help", "--help", "-h" -> true;
            default -> false;
        };
    }

    private static void requireArgCount(String[] args, int expected, String usage) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Usage: java -cp out Main " + usage);
        }
    }

    private static void printUsage() {
        System.out.println("wc3-injector");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  java -cp out Main inject <input.sav> <event.wc3> <output.sav>");
        System.out.println("  java -cp out Main extract <input.sav> <output.wc3>");
        System.out.println("  java -cp out Main inspect-save <input.sav>");
        System.out.println("  java -cp out Main verify-wc3 <event.wc3>");
        System.out.println();
        System.out.println("Legacy inject syntax is still accepted:");
        System.out.println("  java -cp out Main <input.sav> <event.wc3> <output.sav>");
    }
}
