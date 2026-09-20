import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class TestRunner {
    private static int passed;

    public static void main(String[] args) throws Exception {
        testReceiveSlots();
        testPokemonSpeciesCatalog();
        testCreateAndInspectDomain();
        testEditPreservesRamScript();
        testApiVersion();
        testApiCatalog();
        testApiCreateInspectEdit();
        testApiRejectsInvalidReceiveId();
        testApiRejectsUnknownOption();
        System.out.println("PASS " + passed);
    }

    private static void testReceiveSlots() {
        check(WonderCardReceiveSlots.all().size() == 20, "20 receive slots");
        check(WonderCardReceiveSlots.byReceiveId(1000).eventFlag() == 0x2A7, "Aurora mapping");
        check(WonderCardReceiveSlots.byReceiveId(1001).eventFlag() == 0x2A8, "Mystic mapping");
        check(WonderCardReceiveSlots.byReceiveId(1002).eventFlag() == 0x2A9, "Old Sea Map mapping");
        check(WonderCardReceiveSlots.byReceiveId(1003).eventFlag() == 0x2AA, "Unused 1 mapping");
        check(WonderCardReceiveSlots.byReceiveId(1019).eventFlag() == 0x2BA, "Unused 17 mapping");
        check(!WonderCardReceiveSlots.isValidReceiveId(999), "999 invalid");
        check(!WonderCardReceiveSlots.isValidReceiveId(1020), "1020 invalid");
    }

    private static void testPokemonSpeciesCatalog() {
        check(PokemonSpeciesCatalog.all().size() == 387, "386 Pokemon plus Mystery Gift default icon entry");
        check(PokemonSpeciesCatalog.byValue(25).label().equals("Pikachu"), "Pikachu icon label");
        check(PokemonSpeciesCatalog.byValue(250).label().equals("Ho-Oh"), "Ho-Oh icon label");
        check(PokemonSpeciesCatalog.byValue(252) == null, "old Unown placeholder excluded");
        check(PokemonSpeciesCatalog.byValue(277).nationalDex() == 252, "Treecko internal/dex mapping");
        check(PokemonSpeciesCatalog.byValue(392).nationalDex() == 280, "Ralts internal/dex mapping");
        check(PokemonSpeciesCatalog.byValue(410).label().equals("Deoxys"), "Deoxys icon label");
        check(PokemonSpeciesCatalog.byValue(0xFFFF).id().equals("MYSTERY_GIFT_DEFAULT"), "Mystery Gift default icon cataloged");
        check(PokemonSpeciesCatalog.byValue(0xFFFF).label().equals("Mystery Gift (?)"), "Mystery Gift default icon label");
    }

    private static void testCreateAndInspectDomain() {
        Wc3File wc3 = Wc3Factory.createBase();
        check(wc3.copyBytes().length == 0x58C, "base size");
        check(wc3.isCardCrcValid(), "base card crc");
        check(wc3.isRamScriptChecksumValid(), "base ramscript checksum");
        check(wc3.wonderCard().flagId() == 1003, "default receive id");
        check(wc3.wonderCard().iconSpecies() == wc3.metadataIconSpecies(), "icon mirror");
    }

    private static void testEditPreservesRamScript() {
        Wc3File wc3 = Wc3Factory.createBase();
        byte[] before = wc3.ramScriptCopy();
        Wc3BuilderApi.applyOptions(wc3, java.util.Map.of(
                "title", "TEST",
                "flag", "1019",
                "icon", "25"
        ));
        wc3.updateCardCrc();
        check(Arrays.equals(before, wc3.ramScriptCopy()), "edit preserves ramscript");
        check(wc3.wonderCard().flagId() == 1019, "edit receive id");
        check(wc3.wonderCard().iconSpecies() == 25 && wc3.metadataIconSpecies() == 25, "edit icon mirror");
        check(wc3.isCardCrcValid(), "edited crc");
    }

    private static void testApiVersion() {
        ApiResult result = runApi("api", "version");
        check(result.exitCode == 0, "api version exit");
        check(result.stdout.contains("\"protocol\":\"wc3-builder-json\""), "api protocol");
        check(result.stdout.contains("\"apiVersion\":1"), "api version number");
    }

    private static void testApiCatalog() {
        ApiResult result = runApi("api", "catalog");
        check(result.exitCode == 0, "catalog exit");
        check(result.stdout.contains("\"receiveId\":1000"), "catalog receive 1000");
        check(result.stdout.contains("\"eventFlagHex\":\"0x2A7\""), "catalog aurora flag");
        check(result.stdout.contains("\"receiveId\":1019"), "catalog receive 1019");
        check(result.stdout.contains("generalEventFlags"), "catalog semantics");
        check(result.stdout.contains("\"label\":\"Pikachu\""), "catalog Pokemon labels");
        check(result.stdout.contains("\"id\":\"MYSTERY_GIFT_DEFAULT\",\"label\":\"Mystery Gift (?)\""), "catalog Mystery Gift default icon");
        check(result.stdout.contains("\"kind\":\"SPECIAL\""), "catalog special icon kind");
        check(result.stdout.contains("\"value\":277,\"id\":\"TREECKO\",\"label\":\"Treecko\",\"nationalDex\":252"), "catalog internal species mapping");
        check(result.stdout.contains("\"defaultCard\""), "catalog default card");
        check(result.stdout.contains("DEFAULT_INFORMATIONAL_PLACEHOLDER"), "catalog default ramscript behavior");
    }

    private static void testApiCreateInspectEdit() throws Exception {
        Path dir = Files.createTempDirectory("wc3-builder-test-");
        Path created = dir.resolve("created.wc3");
        Path edited = dir.resolve("edited.wc3");

        ApiResult create = runApi("api", "create", "--output", created.toString(),
                "--title", "TEST", "--flag", "1003", "--icon", "25", "--bg", "5");
        check(create.exitCode == 0, "api create exit");
        check(Files.size(created) == 0x58C, "api create size");
        check(create.stdout.contains("\"cardCrcValid\":true"), "api create crc");

        Wc3File before = Wc3File.load(created);
        byte[] ramBefore = before.ramScriptCopy();

        ApiResult edit = runApi("api", "edit", "--input", created.toString(), "--output", edited.toString(),
                "--subtitle", "HELLO", "--flag", "1019");
        check(edit.exitCode == 0, "api edit exit");
        check(edit.stdout.contains("\"ramScriptPreserved\":true"), "api edit preservation response");

        Wc3File after = Wc3File.load(edited);
        check(Arrays.equals(ramBefore, after.ramScriptCopy()), "api edit bytes preserved");
        check(after.wonderCard().flagId() == 1019, "api edit receive id bytes");

        ApiResult inspect = runApi("api", "inspect", "--input", edited.toString());
        check(inspect.exitCode == 0, "api inspect exit");
        check(inspect.stdout.contains("\"id\":\"UNUSED_17\""), "api inspect slot semantics");
        check(inspect.stdout.contains("\"iconSpeciesInfo\":{\"value\":25,\"id\":\"PIKACHU\",\"label\":\"Pikachu\",\"nationalDex\":25,\"kind\":\"POKEMON\"}"), "api inspect icon semantics");
    }

    private static void testApiRejectsInvalidReceiveId() throws Exception {
        Path output = Files.createTempFile("wc3-builder-invalid-", ".wc3");
        Files.delete(output);
        ApiResult result = runApi("api", "create", "--output", output.toString(), "--flag", "1020");
        check(result.exitCode == 1, "invalid flag exit");
        check(result.stdout.contains("\"ok\":false"), "invalid flag json error");
        check(result.stdout.contains("INVALID_REQUEST"), "invalid flag error code");
        check(!Files.exists(output), "invalid flag no output");
    }

    private static void testApiRejectsUnknownOption() throws Exception {
        Path output = Files.createTempFile("wc3-builder-unknown-", ".wc3");
        Files.delete(output);
        ApiResult result = runApi("api", "create", "--output", output.toString(), "--wat", "1");
        check(result.exitCode == 1, "unknown option exit");
        check(result.stdout.contains("UNKNOWN_OPTION"), "unknown option code");
    }

    private static ApiResult runApi(String... args) {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream capture = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capture, true, StandardCharsets.UTF_8));
            int code = Wc3BuilderApi.run(args);
            return new ApiResult(code, capture.toString(StandardCharsets.UTF_8).trim());
        } finally {
            System.setOut(oldOut);
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAIL: " + name);
        passed++;
    }

    private record ApiResult(int exitCode, String stdout) {}
}
