import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Wc3BuilderApi {
    static final String PROTOCOL = "wc3-builder-json";
    static final int API_VERSION = 1;

    private Wc3BuilderApi() {}

    static int run(String[] args) {
        try {
            if (args.length < 2) {
                throw new ApiException("INVALID_REQUEST", "api requires a command");
            }

            Map<String, Object> response = switch (args[1].toLowerCase()) {
                case "version" -> version();
                case "catalog" -> catalog();
                case "inspect" -> inspect(parseOptions(args, 2));
                case "create" -> create(parseOptions(args, 2));
                case "edit" -> edit(parseOptions(args, 2));
                default -> throw new ApiException(
                        "UNKNOWN_API_COMMAND",
                        "Unknown API command: " + args[1]
                );
            };

            System.out.println(Json.stringify(response));
            return 0;
        } catch (ApiException exception) {
            System.out.println(Json.stringify(error(exception.code, exception.getMessage())));
            return 1;
        } catch (IllegalArgumentException exception) {
            System.out.println(Json.stringify(error("INVALID_REQUEST", exception.getMessage())));
            return 1;
        } catch (Exception exception) {
            System.out.println(Json.stringify(error("INTERNAL_ERROR", exception.getMessage())));
            return 1;
        }
    }

    private static Map<String, Object> version() {
        Map<String, Object> result = baseSuccess();
        result.put("tool", "wc3-builder");
        result.put("commands", List.of("version", "catalog", "inspect", "create", "edit"));
        return result;
    }

    private static Map<String, Object> catalog() {
        Map<String, Object> result = baseSuccess();

        result.put("cardTypes", List.of(
                enumItem(0, "GIFT", "Normal Wonder Card"),
                enumItem(1, "STAMP", "Stamp Card"),
                enumItem(2, "LINK_STAT", "Battle Card / link statistics")
        ));
        result.put("sendTypes", List.of(
                enumItem(0, "DISALLOWED", "Card cannot be sent onward"),
                enumItem(1, "ALLOWED", "Card may be sent; retail code can disable further sending after transfer"),
                enumItem(2, "ALLOWED_ALWAYS", "Card may always be sent")
        ));

        List<Object> backgrounds = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            backgrounds.add(Map.of("value", i, "id", "BACKGROUND_" + i));
        }
        result.put("backgrounds", backgrounds);

        List<Object> iconSpecies = new ArrayList<>();
        for (PokemonSpeciesCatalog.Species species : PokemonSpeciesCatalog.all()) {
            iconSpecies.add(speciesJson(species));
        }
        result.put("iconSpecies", iconSpecies);

        // Exact source of truth for the initial UI model. Saving this model with
        // api create produces the same base WC3, including the informational
        // placeholder RamScript.
        Wc3File defaultWc3 = Wc3Factory.createBase();
        result.put("defaultCard", cardJson(defaultWc3));
        result.put("defaultRamScriptBehavior", "DEFAULT_INFORMATIONAL_PLACEHOLDER");

        List<Object> receiveSlots = new ArrayList<>();
        for (WonderCardReceiveSlots.Slot slot : WonderCardReceiveSlots.all()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("receiveId", slot.receiveId());
            item.put("eventFlag", slot.eventFlag());
            item.put("eventFlagHex", String.format("0x%03X", slot.eventFlag()));
            item.put("id", slot.id());
            item.put("label", slot.label());
            item.put("category", slot.category());
            item.put("usedByFrlgRetail", slot.usedByFrlgRetail());
            item.put("note", slot.note());
            receiveSlots.add(item);
        }
        result.put("receiveSlots", receiveSlots);

        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("fileSize", Wc3File.FILE_SIZE);
        limits.put("wonderCardSize", WonderCard.SIZE);
        limits.put("textFieldBytes", WonderCard.TEXT_LENGTH);
        limits.put("bodyLines", WonderCard.BODY_LINE_COUNT);
        limits.put("backgroundMin", 0);
        limits.put("backgroundMax", 7);
        limits.put("maxStampsMin", 0);
        limits.put("maxStampsMax", 7);
        limits.put("receiveIdMin", WonderCardReceiveSlots.MIN_RECEIVE_ID);
        limits.put("receiveIdMax", WonderCardReceiveSlots.MAX_RECEIVE_ID);
        limits.put("iconSpeciesMin", 0);
        limits.put("iconSpeciesMax", 65535);
        limits.put("selectableIconSpeciesCount", PokemonSpeciesCatalog.all().size());
        result.put("limits", limits);

        Map<String, Object> semantics = new LinkedHashMap<>();
        semantics.put("wirelessCardComparison",
                "FR/LG link data exposes the currently saved Wonder Card flagId. MysteryGift_CompareCardFlags returns no-card when it is 0, same-card when the incoming flagId matches, and different-card otherwise.");
        semantics.put("giftReceivedCheck",
                "For normal gift-delivery state, FR/LG only accepts receive IDs 1000-1019. The game subtracts 1000 and maps the result through its 20-entry sReceivedGiftFlags table.");
        semantics.put("generalEventFlags",
                "General unused event flags are not WonderCard.flagId values. They belong to the event/RamScript payload and are intentionally outside wc3-builder's receive-slot catalog.");
        semantics.put("saveInjection",
                "Direct save injection bypasses the wireless server comparison, but the saved card and its RamScript are still interpreted by the game's normal Wonder Card logic afterward.");
        result.put("semantics", semantics);

        return result;
    }

    private static Map<String, Object> inspect(Map<String, String> options) throws Exception {
        requireOnly(options, List.of("input"));
        Path input = requiredPath(options, "input");
        requireExistingFile(input);
        Wc3File wc3 = Wc3File.load(input);

        Map<String, Object> result = baseSuccess();
        result.put("input", input.toAbsolutePath().normalize().toString());
        result.put("card", cardJson(wc3));
        result.put("validation", validationJson(wc3));
        return result;
    }

    private static Map<String, Object> create(Map<String, String> options) throws Exception {
        Path output = requiredPath(options, "output");
        Map<String, String> cardOptions = without(options, "output");
        validateCardOptionNames(cardOptions);

        Wc3File wc3 = Wc3Factory.createBase();
        applyOptions(wc3, cardOptions);
        wc3.updateCardCrc();
        wc3.write(output);

        Map<String, Object> result = baseSuccess();
        result.put("output", output.toAbsolutePath().normalize().toString());
        result.put("card", cardJson(wc3));
        result.put("validation", validationJson(wc3));
        result.put("ramScriptBehavior", "DEFAULT_INFORMATIONAL_PLACEHOLDER");
        return result;
    }

    private static Map<String, Object> edit(Map<String, String> options) throws Exception {
        Path input = requiredPath(options, "input");
        Path output = requiredPath(options, "output");
        requireExistingFile(input);

        Map<String, String> cardOptions = without(options, "input", "output");
        if (cardOptions.isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "edit requires at least one card field option");
        }
        validateCardOptionNames(cardOptions);

        Wc3File wc3 = Wc3File.load(input);
        byte[] beforeRamScript = wc3.ramScriptCopy();
        applyOptions(wc3, cardOptions);
        wc3.updateCardCrc();
        wc3.write(output);

        Map<String, Object> result = baseSuccess();
        result.put("input", input.toAbsolutePath().normalize().toString());
        result.put("output", output.toAbsolutePath().normalize().toString());
        result.put("card", cardJson(wc3));
        result.put("validation", validationJson(wc3));
        result.put("ramScriptPreserved", java.util.Arrays.equals(beforeRamScript, wc3.ramScriptCopy()));
        return result;
    }

    static void applyOptions(Wc3File wc3, Map<String, String> options) {
        WonderCard card = wc3.wonderCard();
        for (Map.Entry<String, String> option : options.entrySet()) {
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
                case "flag" -> {
                    int receiveId = parseNumber(value);
                    WonderCardReceiveSlots.requireValidReceiveId(receiveId);
                    card.setFlagId(receiveId);
                }
                case "icon" -> wc3.setIconSpecies(parseNumber(value));
                case "id" -> card.setIdNumber(parseLong(value));
                case "type" -> card.setType(parseNumber(value));
                case "bg" -> card.setBackgroundType(parseNumber(value));
                case "send" -> card.setSendType(parseNumber(value));
                case "stamps" -> card.setMaxStamps(parseNumber(value));
                default -> throw new ApiException("UNKNOWN_OPTION", "Unknown card option: --" + name);
            }
        }
    }

    private static Map<String, Object> cardJson(Wc3File wc3) {
        WonderCard card = wc3.wonderCard();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flagId", card.flagId());

        WonderCardReceiveSlots.Slot slot = WonderCardReceiveSlots.byReceiveId(card.flagId());
        if (slot != null) {
            Map<String, Object> slotJson = new LinkedHashMap<>();
            slotJson.put("receiveId", slot.receiveId());
            slotJson.put("eventFlag", slot.eventFlag());
            slotJson.put("eventFlagHex", String.format("0x%03X", slot.eventFlag()));
            slotJson.put("id", slot.id());
            slotJson.put("label", slot.label());
            slotJson.put("category", slot.category());
            result.put("receiveSlot", slotJson);
        } else {
            result.put("receiveSlot", null);
        }

        result.put("iconSpecies", card.iconSpecies());
        PokemonSpeciesCatalog.Species species = PokemonSpeciesCatalog.byValue(card.iconSpecies());
        result.put("iconSpeciesInfo", species == null ? null : speciesJson(species));
        result.put("metadataIconSpecies", wc3.metadataIconSpecies());
        result.put("idNumber", card.idNumber());
        result.put("type", enumValue(card.type(), card.typeName()));
        result.put("background", card.backgroundType());
        result.put("sendType", enumValue(card.sendType(), card.sendTypeName()));
        result.put("maxStamps", card.maxStamps());
        result.put("title", card.title());
        result.put("subtitle", card.subtitle());
        result.put("body", List.of(card.bodyLine(0), card.bodyLine(1), card.bodyLine(2), card.bodyLine(3)));
        result.put("footer", List.of(card.footerLine1(), card.footerLine2()));
        return result;
    }

    private static Map<String, Object> validationJson(Wc3File wc3) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cardCrcStored", wc3.storedCardCrc());
        result.put("cardCrcCalculated", wc3.calculatedCardCrc());
        result.put("cardCrcValid", wc3.isCardCrcValid());
        result.put("ramScriptChecksumStored", wc3.storedRamScriptChecksum());
        result.put("ramScriptChecksumCalculated", wc3.calculatedRamScriptChecksum());
        result.put("ramScriptChecksumValid", wc3.isRamScriptChecksumValid());
        result.put("receiveIdInGiftRange", WonderCardReceiveSlots.isValidReceiveId(wc3.wonderCard().flagId()));
        result.put("iconMetadataMatches", wc3.wonderCard().iconSpecies() == wc3.metadataIconSpecies());
        return result;
    }


    private static Map<String, Object> speciesJson(PokemonSpeciesCatalog.Species species) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", species.value());
        result.put("id", species.id());
        result.put("label", species.label());
        result.put("nationalDex", species.nationalDex());
        result.put("kind", species.value() == PokemonSpeciesCatalog.MYSTERY_GIFT_DEFAULT_VALUE ? "SPECIAL" : "POKEMON");
        return result;
    }

    private static Map<String, Object> enumItem(int value, String id, String description) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", value);
        result.put("id", id);
        result.put("description", description);
        return result;
    }

    private static Map<String, Object> enumValue(int value, String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", value);
        result.put("id", id);
        return result;
    }

    private static Map<String, Object> baseSuccess() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocol", PROTOCOL);
        result.put("apiVersion", API_VERSION);
        result.put("ok", true);
        return result;
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocol", PROTOCOL);
        result.put("apiVersion", API_VERSION);
        result.put("ok", false);
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message == null ? "Unknown error" : message);
        result.put("error", error);
        return result;
    }

    private static Map<String, String> parseOptions(String[] args, int start) {
        Map<String, String> result = new LinkedHashMap<>();
        if (((args.length - start) & 1) != 0) {
            throw new ApiException("INVALID_REQUEST", "Options must be provided as --name value pairs");
        }
        for (int i = start; i < args.length; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new ApiException("INVALID_REQUEST", "Expected an option beginning with --, got: " + args[i]);
            }
            String name = args[i].substring(2).toLowerCase();
            if (result.containsKey(name)) {
                throw new ApiException("INVALID_REQUEST", "Duplicate option: --" + name);
            }
            result.put(name, args[i + 1]);
        }
        return result;
    }

    private static void validateCardOptionNames(Map<String, String> options) {
        List<String> allowed = List.of(
                "title", "subtitle", "body1", "body2", "body3", "body4",
                "footer1", "footer2", "flag", "icon", "id", "type", "bg", "send", "stamps"
        );
        requireOnly(options, allowed);
    }

    private static void requireOnly(Map<String, String> options, List<String> allowed) {
        for (String key : options.keySet()) {
            if (!allowed.contains(key)) {
                throw new ApiException("UNKNOWN_OPTION", "Unknown option: --" + key);
            }
        }
    }

    private static Path requiredPath(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new ApiException("INVALID_REQUEST", "Missing required option --" + name);
        }
        return Path.of(value);
    }

    private static void requireExistingFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new ApiException("INPUT_FILE_NOT_FOUND", "Input file not found: " + path);
        }
    }

    private static Map<String, String> without(Map<String, String> source, String... names) {
        Map<String, String> result = new LinkedHashMap<>(source);
        for (String name : names) result.remove(name);
        return result;
    }

    static int parseNumber(String value) {
        long parsed = parseLong(value);
        if (parsed > Integer.MAX_VALUE) throw new IllegalArgumentException("Number is too large: " + value);
        return (int) parsed;
    }

    static long parseLong(String value) {
        try {
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number: " + value);
        }
    }

    private static final class ApiException extends RuntimeException {
        private final String code;
        private ApiException(String code, String message) {
            super(message);
            this.code = code;
        }
    }
}
