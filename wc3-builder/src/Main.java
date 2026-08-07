import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        try {
            if (args.length < 2) {
                printUsage();
                return;
            }

            switch (args[0].toLowerCase()) { /* check's wether it is a read or edit instruction to a .wc3 file */
                case "inspect" -> inspect(args);
                case "edit" -> edit(args);
                default -> {
                    System.err.println("Unknown command: " + args[0]);
                    printUsage();
                }
            }
        } catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    /* inspect method only loads the .wc3 file read it and print its data */
    private static void inspect(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "inspect requires exactly one WC3 file"
            );
        }

        Wc3File wc3 = Wc3File.load(Path.of(args[1]));
        printCard(wc3);
    }

    private static void edit(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "edit requires input.wc3, output.wc3, and at least one --option value"
            );
        }

        Path input = Path.of(args[1]);
        Path output = Path.of(args[2]);
        Map<String, String> options = parseOptions(args, 3);

        Wc3File wc3 = Wc3File.load(input);
        WonderCard card = wc3.wonderCard();

        for (Map.Entry<String, String> option : options.entrySet()) { /* check for all options set and tries to update the wc */
            String name = option.getKey();
            String value = option.getValue();

            switch (name) {
                case "title" -> card.setTitle(value);
                case "subtitle" -> card.setSubtitle(value);
                case "body1" -> card.setBodyLine(0, value);
                case "body2" -> card.setBodyLine(1, value);
                case "body3" -> card.setBodyLine(2, value);
                case "body4" -> card.setBodyLine(3, value);
                case "footer1" -> card.setFooterLine1(value);
                case "footer2" -> card.setFooterLine2(value);
                case "flag" -> card.setFlagId(parseNumber(value));

                /*use Wc3File, not WonderCard directly. this keeps WonderCard.iconSpecies and WonderCardMetadata.iconSpecies synchronized. */
                case "icon" -> wc3.setIconSpecies(parseNumber(value));

                case "id" -> card.setIdNumber(parseLong(value));
                case "type" -> card.setType(parseNumber(value));
                case "bg" -> card.setBackgroundType(parseNumber(value));
                case "send" -> card.setSendType(parseNumber(value));
                case "stamps" -> card.setMaxStamps(parseNumber(value));
                default -> throw new IllegalArgumentException(
                        "Unknown edit option: --" + name
                );
            }
        }

        wc3.updateCardCrc(); /* calls updateCardCrc to ensure the wc keep valid */
        wc3.write(output); /* write data on the outputfile */

        System.out.println("WC3 written successfully: " + output.toAbsolutePath());
        System.out.printf("New card CRC: 0x%04X%n%n", wc3.storedCardCrc());
        printCard(wc3);
    }

    private static Map<String, String> parseOptions(String[] args, int start) {
        Map<String, String> result = new LinkedHashMap<>();

        for (int i = start; i < args.length; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException(
                        "Expected an option beginning with --, got: " + args[i]
                );
            }

            if (i + 1 >= args.length) {
                throw new IllegalArgumentException(
                        "Missing value for option: " + args[i]
                );
            }

            result.put(
                    args[i].substring(2).toLowerCase(),
                    args[i + 1]
            );
        }

        return result;
    }

    private static int parseNumber(String value) {
        long parsed = parseLong(value);

        if (parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Number is too large: " + value
            );
        }

        return (int) parsed;
    }

    private static long parseLong(String value) {
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }

            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid number: " + value
            );
        }
    }

    private static void printCard(Wc3File wc3) {
        WonderCard card = wc3.wonderCard();

        System.out.println("=== WC3 Wonder Card ===");
        System.out.printf("Stored CRC:      0x%04X%n", wc3.storedCardCrc());
        System.out.printf("Calculated CRC:  0x%04X%n", wc3.calculatedCardCrc());
        System.out.println("CRC valid:       " + wc3.isCardCrcValid());
        System.out.println();

        System.out.println("Flag ID:         " + card.flagId());
        System.out.printf(
                "Icon species:    %d (0x%04X)%n",
                card.iconSpecies(),
                card.iconSpecies()
        );
        System.out.printf(
                "Metadata icon:   %d (0x%04X)%n",
                wc3.metadataIconSpecies(),
                wc3.metadataIconSpecies()
        );
        System.out.println("Card ID number:  " + card.idNumber());
        System.out.println(
                "Type:            " + card.type()
                        + " (" + card.typeName() + ")"
        );
        System.out.println("Background:      " + card.backgroundType());
        System.out.println(
                "Send type:       " + card.sendType()
                        + " (" + card.sendTypeName() + ")"
        );
        System.out.println("Max stamps:      " + card.maxStamps());
        System.out.println();

        System.out.println("Title:           " + quote(card.title()));
        System.out.println("Subtitle:        " + quote(card.subtitle()));

        for (int i = 0; i < WonderCard.BODY_LINE_COUNT; i++) {
            System.out.println(
                    "Body " + (i + 1) + ":          "
                            + quote(card.bodyLine(i))
            );
        }

        System.out.println("Footer 1:        " + quote(card.footerLine1()));
        System.out.println("Footer 2:        " + quote(card.footerLine2()));
    }

    private static String quote(String value) {
        return '"' + value + '"';
    }

    private static void printUsage() {
        System.out.println("WC3 Builder / Inspector");
        System.out.println();
        System.out.println("Inspect:");
        System.out.println(
                "  java -cp out Main inspect input\\event.wc3"
        );
        System.out.println();
        System.out.println("Edit:");
        System.out.println(
                "  java -cp out Main edit input\\event.wc3 output\\custom.wc3 "
                        + "--title \"CUSTOM EVENT\" --bg 3"
        );
        System.out.println();
        System.out.println("Options:");
        System.out.println(
                "  --title --subtitle --body1 --body2 --body3 --body4"
        );
        System.out.println(
                "  --footer1 --footer2 --flag --icon --id --type --bg --send --stamps"
        );
        System.out.println();
        System.out.println(
                "Numbers can be decimal or hexadecimal, for example 1001 or 0x03E9."
        );
    }
}
