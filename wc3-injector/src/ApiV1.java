import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/*
Machine-facing JSON API for desktop/tool integrations.

Contract rules:
- stdout contains exactly one JSON object;
- human CLI text is never parsed or reused;
- content-level WC3 issues remain warnings, matching the permissive core behavior;
- API version 1 is intentionally small and stable.
*/
final class ApiV1 {
    static final String PROTOCOL = "wc3-injector-json";
    static final int API_VERSION = 1;

    private ApiV1() {}

    static int run(String[] args, PrintStream out) {
        String command = args.length == 0 ? null : args[0].toLowerCase(Locale.ROOT);
        try {
            if (command == null) {
                throw new ApiException("INVALID_REQUEST", "Missing API command");
            }

            Map<String, Object> result = switch (command) {
                case "version" -> version(args);
                case "inspect-save" -> inspectSave(args);
                case "verify-wc3" -> verifyWc3(args);
                case "inject" -> inject(args);
                case "extract" -> extract(args);
                default -> throw new ApiException(
                        "UNKNOWN_API_COMMAND",
                        "Unknown API command: " + args[0]
                );
            };

            out.println(Json.stringify(success(command, result)));
            return 0;
        } catch (ApiException exception) {
            out.println(Json.stringify(failure(command, exception.code, exception.getMessage())));
            return 2;
        } catch (NoSuchFileException exception) {
            out.println(Json.stringify(failure(command, "INPUT_FILE_NOT_FOUND", missingPathMessage(exception))));
            return 2;
        } catch (IOException exception) {
            out.println(Json.stringify(failure(command, "IO_ERROR", safeMessage(exception))));
            return 2;
        } catch (IllegalArgumentException exception) {
            out.println(Json.stringify(failure(command, classifyIllegalArgument(command, exception), safeMessage(exception))));
            return 2;
        } catch (Exception exception) {
            out.println(Json.stringify(failure(command, "INTERNAL_ERROR", safeMessage(exception))));
            return 2;
        }
    }

    private static Map<String, Object> version(String[] args) throws ApiException {
        requireExactLength(args, 1, "version");
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("protocol", PROTOCOL);
        result.put("apiVersion", API_VERSION);
        result.put("tool", "wc3-injector");
        result.put("capabilities", List.of(
                "inject-save",
                "extract-wc3",
                "inspect-save",
                "verify-wc3"
        ));
        return result;
    }

    private static Map<String, Object> inspectSave(String[] args) throws Exception {
        Options options = Options.parse(args, 1, Map.of("--input", true));
        Path input = existingFile(options.required("--input"));

        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
        FireRedLeafGreenSave.SaveInspection inspection = save.inspect();

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("input", absolute(input));
        result.put("slots", List.of(slotJson(inspection.slot1()), slotJson(inspection.slot2())));
        result.put("activeSlot", inspection.activeSlot() == null ? null : slotJson(inspection.activeSlot()));
        return result;
    }

    private static Map<String, Object> verifyWc3(String[] args) throws Exception {
        Options options = Options.parse(args, 1, Map.of("--input", true));
        Path input = existingFile(options.required("--input"));
        Wc3File wc3 = Wc3File.load(input);

        LinkedHashMap<String, Object> result = wc3Json(wc3);
        result.put("input", absolute(input));
        result.put("warnings", warningJson(wc3.validationWarnings()));
        return result;
    }

    private static Map<String, Object> inject(String[] args) throws Exception {
        Options options = Options.parse(args, 1, Map.of(
                "--input-save", true,
                "--wc3", true,
                "--output", true
        ));
        Path input = existingFile(options.required("--input-save"));
        Path wc3Path = existingFile(options.required("--wc3"));
        Path output = Path.of(options.required("--output"));

        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
        Wc3File wc3 = Wc3File.load(wc3Path);
        FireRedLeafGreenSave.InjectionResult injection = save.inject(wc3);
        save.write(output);

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("inputSave", absolute(input));
        result.put("wonderCard", absolute(wc3Path));
        result.put("output", artifactJson(output));
        result.put("slotIndex", injection.slotIndex() + 1);
        result.put("saveCounter", Integer.toUnsignedLong(injection.saveCounter()));
        result.put("physicalSector", injection.physicalSector());
        result.put("sectorChecksum", injection.sectorChecksum());
        result.put("sectorChecksumHex", String.format("0x%04X", injection.sectorChecksum()));
        result.put("wonderCardFlagId", injection.wonderCardFlagId());
        result.put("warnings", warningJson(wc3.validationWarnings()));
        return result;
    }

    private static Map<String, Object> extract(String[] args) throws Exception {
        Options options = Options.parse(args, 1, Map.of(
                "--input-save", true,
                "--output", true
        ));
        Path input = existingFile(options.required("--input-save"));
        Path output = Path.of(options.required("--output"));

        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(input);
        FireRedLeafGreenSave.ExtractionResult extraction = save.extractWc3();
        Wc3File wc3 = extraction.wc3();
        wc3.write(output);

        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("inputSave", absolute(input));
        result.put("output", artifactJson(output));
        result.put("slotIndex", extraction.slotIndex() + 1);
        result.put("saveCounter", Integer.toUnsignedLong(extraction.saveCounter()));
        result.put("physicalSector", extraction.physicalSector());
        result.put("wonderCard", wc3Json(wc3));
        result.put("warnings", warningJson(wc3.validationWarnings()));
        return result;
    }

    private static LinkedHashMap<String, Object> wc3Json(Wc3File wc3) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("flagId", wc3.wonderCard().flagId());
        result.put("iconSpecies", wc3.wonderCard().iconSpecies());
        result.put("storedCardCrc", wc3.storedCardCrc());
        result.put("storedCardCrcHex", String.format("0x%04X", wc3.storedCardCrc()));
        result.put("calculatedCardCrc", wc3.calculatedCardCrc());
        result.put("calculatedCardCrcHex", String.format("0x%04X", wc3.calculatedCardCrc()));
        result.put("cardCrcValid", wc3.isCardCrcValid());
        result.put("storedRamScriptChecksum", wc3.storedRamScriptChecksum());
        result.put("storedRamScriptChecksumHex", String.format("0x%04X", wc3.storedRamScriptChecksum()));
        result.put("calculatedRamScriptChecksum", wc3.calculatedRamScriptChecksum());
        result.put("calculatedRamScriptChecksumHex", String.format("0x%04X", wc3.calculatedRamScriptChecksum()));
        result.put("ramScriptChecksumValid", wc3.isRamScriptChecksumValid());
        return result;
    }

    private static Map<String, Object> slotJson(FireRedLeafGreenSave.SlotInfo slot) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("slotIndex", slot.slotIndex() + 1);
        result.put("valid", slot.valid());
        result.put("counter", slot.valid() ? Integer.toUnsignedLong(slot.counter()) : null);
        result.put("status", slot.status());
        return result;
    }

    private static List<Map<String, Object>> warningJson(String[] warnings) {
        ArrayList<Map<String, Object>> result = new ArrayList<>();
        for (String warning : warnings) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("severity", "WARNING");
            item.put("code", warningCode(warning));
            item.put("message", warning);
            result.add(item);
        }
        return result;
    }

    private static String warningCode(String warning) {
        if (warning.equals("flagId is 0")) {
            return "ZERO_FLAG_ID";
        }
        if (warning.startsWith("Wonder Card CRC mismatch")) {
            return "CARD_CRC_MISMATCH";
        }
        if (warning.startsWith("RamScript checksum mismatch")) {
            return "RAMSCRIPT_CHECKSUM_MISMATCH";
        }
        return "WC3_WARNING";
    }

    private static Map<String, Object> artifactJson(Path path) throws IOException {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("path", absolute(path));
        result.put("sizeBytes", Files.size(path));
        return result;
    }

    private static Map<String, Object> success(String command, Map<String, Object> result) {
        LinkedHashMap<String, Object> response = envelope(command, true);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> failure(String command, String code, String message) {
        LinkedHashMap<String, Object> response = envelope(command, false);
        LinkedHashMap<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        response.put("error", error);
        return response;
    }

    private static LinkedHashMap<String, Object> envelope(String command, boolean ok) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("protocol", PROTOCOL);
        response.put("apiVersion", API_VERSION);
        response.put("ok", ok);
        response.put("command", command);
        return response;
    }

    private static Path existingFile(String value) throws ApiException {
        Path path = Path.of(value);
        if (!Files.exists(path)) {
            throw new ApiException("INPUT_FILE_NOT_FOUND", "Input file not found: " + absolute(path));
        }
        if (!Files.isRegularFile(path)) {
            throw new ApiException("INVALID_REQUEST", "Input path is not a regular file: " + absolute(path));
        }
        return path;
    }

    private static String absolute(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private static void requireExactLength(String[] args, int expected, String usage) throws ApiException {
        if (args.length != expected) {
            throw new ApiException("INVALID_REQUEST", "Usage: api " + usage);
        }
    }

    private static String classifyIllegalArgument(String command, IllegalArgumentException exception) {
        String message = safeMessage(exception).toLowerCase(Locale.ROOT);
        if ("verify-wc3".equals(command) || message.contains("wc3 size")) {
            return "INVALID_WC3";
        }
        if ("inspect-save".equals(command) || "extract".equals(command)
                || message.contains("save size") || message.contains("save slot")) {
            return "INVALID_SAVE";
        }
        if ("inject".equals(command)) {
            if (message.contains("wc3")) {
                return "INVALID_WC3";
            }
            if (message.contains("save")) {
                return "INVALID_SAVE";
            }
        }
        return "INVALID_REQUEST";
    }

    private static String missingPathMessage(NoSuchFileException exception) {
        return exception.getFile() == null ? safeMessage(exception) : "Input file not found: " + exception.getFile();
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static final class ApiException extends Exception {
        private final String code;

        private ApiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static final class Options {
        private final Map<String, String> values;

        private Options(Map<String, String> values) {
            this.values = values;
        }

        static Options parse(String[] args, int startIndex, Map<String, Boolean> accepted) throws ApiException {
            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            for (int i = startIndex; i < args.length; i++) {
                String key = args[i];
                if (!accepted.containsKey(key)) {
                    throw new ApiException("INVALID_REQUEST", "Unknown option: " + key);
                }
                if (values.containsKey(key)) {
                    throw new ApiException("INVALID_REQUEST", "Duplicate option: " + key);
                }
                if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                    throw new ApiException("INVALID_REQUEST", "Missing value for option: " + key);
                }
                values.put(key, args[++i]);
            }
            return new Options(values);
        }

        String required(String key) throws ApiException {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                throw new ApiException("INVALID_REQUEST", "Missing required option: " + key);
            }
            return value;
        }
    }
}
