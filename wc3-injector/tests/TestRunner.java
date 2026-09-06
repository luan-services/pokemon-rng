import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class TestRunner {
    private static final int SAVE_SIZE = 0x20000;
    private static final int SECTOR_SIZE = 0x1000;
    private static final int SECTORS_PER_SLOT = 14;
    private static final int FOOTER_ID_OFFSET = 0xFF4;
    private static final int FOOTER_CHECKSUM_OFFSET = 0xFF6;
    private static final int FOOTER_SIGNATURE_OFFSET = 0xFF8;
    private static final int FOOTER_COUNTER_OFFSET = 0xFFC;
    private static final long SECTOR_SIGNATURE = 0x08012025L;
    private static final int[] CHECKSUM_SIZES = {
            0xF24, 0xF80, 0xF80, 0xF80, 0xEE8,
            0xF80, 0xF80, 0xF80, 0xF80, 0xF80,
            0xF80, 0xF80, 0xF80, 0x7D0
    };
    private static final int WC3_CARD_DESTINATION = 0x460;
    private static final int QUESTIONNAIRE_START = 0x178;
    private static final int QUESTIONNAIRE_END = 0x180;

    private static int tests;

    public static void main(String[] args) throws Exception {
        injectionExtractionRoundTrip();
        corruptedNewerSlotFallsBack();
        invalidWc3ChecksumIsWarningAndInjectable();
        emptyCardCanStillBeExtracted();
        apiVersionContract();
        apiInspectVerifyInjectExtractContract();
        apiStructuredErrors();
        System.out.printf("PASS: %d tests%n", tests);
    }

    private static void injectionExtractionRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("wc3-injector-test-");
        Path savePath = dir.resolve("input.sav");
        Path wc3Path = dir.resolve("event.wc3");
        Path outputSave = dir.resolve("output.sav");
        Path extractedPath = dir.resolve("extracted.wc3");

        byte[] saveBytes = createValidSave(10, 20);
        int activeSectorOffset = (SECTORS_PER_SLOT + 4) * SECTOR_SIZE;
        for (int i = QUESTIONNAIRE_START; i < QUESTIONNAIRE_END; i++) {
            saveBytes[activeSectorOffset + WC3_CARD_DESTINATION + i] = (byte) (0xA0 + i - QUESTIONNAIRE_START);
        }
        recalcSector(saveBytes, SECTORS_PER_SLOT + 4, 4);
        Files.write(savePath, saveBytes);

        byte[] sourceWc3 = createValidWc3(77, 25);
        Arrays.fill(sourceWc3, QUESTIONNAIRE_START, QUESTIONNAIRE_END, (byte) 0x55);
        // Deliberately make metadata inconsistent; injection should normalize it.
        Binary.putU16(sourceWc3, Wc3File.CARD_METADATA_ICON_SPECIES_OFFSET, 999);
        // Card metadata is outside the WonderCard CRC range, so no CRC update is needed here.
        Files.write(wc3Path, sourceWc3);

        FireRedLeafGreenSave save = FireRedLeafGreenSave.load(savePath);
        Wc3File wc3 = Wc3File.load(wc3Path);
        FireRedLeafGreenSave.InjectionResult injection = save.inject(wc3);
        save.write(outputSave);
        check(injection.slotIndex() == 1, "newest slot is selected for injection");
        check(injection.saveCounter() == 20, "newest slot counter is reported");

        FireRedLeafGreenSave extractedSave = FireRedLeafGreenSave.load(outputSave);
        FireRedLeafGreenSave.ExtractionResult extraction = extractedSave.extractWc3();
        extraction.wc3().write(extractedPath);
        byte[] extracted = Files.readAllBytes(extractedPath);

        check(extraction.slotIndex() == 1, "newest slot is selected for extraction");
        check(Binary.u16(extracted, 4) == 77, "flag id survives injection/extraction");
        check(Binary.u16(extracted, 6) == 25, "icon species survives injection/extraction");
        check(Binary.u16(extracted, Wc3File.CARD_METADATA_ICON_SPECIES_OFFSET) == 25,
                "icon metadata is normalized from WonderCard.iconSpecies");
        for (int i = QUESTIONNAIRE_START; i < QUESTIONNAIRE_END; i++) {
            check(extracted[i] == (byte) (0xA0 + i - QUESTIONNAIRE_START),
                    "questionnaire byte " + i + " is preserved from save");
        }
        check(Arrays.equals(
                        Arrays.copyOfRange(sourceWc3, Wc3File.RAM_SCRIPT_OFFSET, Wc3File.FILE_SIZE),
                        Arrays.copyOfRange(extracted, Wc3File.RAM_SCRIPT_OFFSET, Wc3File.FILE_SIZE)),
                "RamScript block survives round trip");
        check(extraction.wc3().isCardCrcValid(), "card CRC remains valid after normal round trip");
        check(extraction.wc3().isRamScriptChecksumValid(), "RamScript checksum remains valid after normal round trip");
    }

    private static void corruptedNewerSlotFallsBack() throws Exception {
        Path savePath = Files.createTempFile("wc3-fallback-", ".sav");
        byte[] save = createValidSave(5, 9);
        // Corrupt data covered by logical sector 2 checksum in slot 2.
        int physical = SECTORS_PER_SLOT + 2;
        save[physical * SECTOR_SIZE + 0x20] ^= 0x5A;
        Files.write(savePath, save);

        FireRedLeafGreenSave.SaveInspection inspection = FireRedLeafGreenSave.load(savePath).inspect();
        check(inspection.slot1().valid(), "older slot remains valid");
        check(!inspection.slot2().valid(), "corrupted newer slot is invalid");
        check(inspection.activeSlot() != null && inspection.activeSlot().slotIndex() == 0,
                "older valid slot becomes active");
    }

    private static void invalidWc3ChecksumIsWarningAndInjectable() throws Exception {
        Path dir = Files.createTempDirectory("wc3-warning-");
        Path savePath = dir.resolve("input.sav");
        Path wc3Path = dir.resolve("bad.wc3");
        Files.write(savePath, createValidSave(1, 2));
        byte[] wc = createValidWc3(88, 133);
        wc[0x20] ^= 0x01; // covered by card CRC, do not update CRC.
        Files.write(wc3Path, wc);

        Wc3File parsed = Wc3File.load(wc3Path);
        check(!parsed.isCardCrcValid(), "modified WC3 has invalid card CRC");
        check(parsed.validationWarnings().length > 0, "invalid card CRC is exposed as warning");
        FireRedLeafGreenSave.InjectionResult result = FireRedLeafGreenSave.load(savePath).inject(parsed);
        check(result.wonderCardFlagId() == 88, "warning-only WC3 remains injectable");
    }

    private static void emptyCardCanStillBeExtracted() throws Exception {
        Path savePath = Files.createTempFile("wc3-empty-", ".sav");
        Files.write(savePath, createValidSave(2, 3));
        Wc3File wc3 = FireRedLeafGreenSave.load(savePath).extractWc3().wc3();
        check(wc3.wonderCard().flagId() == 0, "empty save card reconstructs flag id zero");
        check(wc3.validationWarnings().length > 0, "empty/nonstandard extraction reports warnings");
    }

    private static void apiVersionContract() throws Exception {
        ApiCall call = api("version");
        check(call.exitCode == 0, "api version succeeds");
        check(call.stdout.contains("\"protocol\":\"wc3-injector-json\""), "api version exposes protocol");
        check(call.stdout.contains("\"apiVersion\":1"), "api version exposes numeric version");
        check(call.stdout.contains("\"inject-save\""), "api version exposes capabilities");
        check(oneNonBlankLine(call.stdout), "api success emits exactly one JSON line");
    }

    private static void apiInspectVerifyInjectExtractContract() throws Exception {
        Path dir = Files.createTempDirectory("wc3-api-");
        Path save = dir.resolve("input.sav");
        Path wc3 = dir.resolve("event.wc3");
        Path outputSave = dir.resolve("output.sav");
        Path outputWc3 = dir.resolve("output.wc3");
        Files.write(save, createValidSave(100, 101));
        Files.write(wc3, createValidWc3(321, 151));

        ApiCall inspect = api("inspect-save", "--input", save.toString());
        check(inspect.exitCode == 0 && inspect.stdout.contains("\"activeSlot\""), "api inspect-save returns active slot");
        check(inspect.stdout.contains("\"slotIndex\":2"), "api inspect-save identifies newest slot");

        ApiCall verify = api("verify-wc3", "--input", wc3.toString());
        check(verify.exitCode == 0 && verify.stdout.contains("\"flagId\":321"), "api verify-wc3 returns card metadata");
        check(verify.stdout.contains("\"cardCrcValid\":true"), "api verify-wc3 returns CRC status");

        ApiCall inject = api(
                "inject", "--input-save", save.toString(), "--wc3", wc3.toString(), "--output", outputSave.toString());
        check(inject.exitCode == 0 && Files.size(outputSave) == SAVE_SIZE, "api inject writes save artifact");
        check(inject.stdout.contains("\"wonderCardFlagId\":321"), "api inject returns flag id");
        check(inject.stdout.contains("\"sizeBytes\":131072"), "api inject returns artifact size");

        ApiCall extract = api("extract", "--input-save", outputSave.toString(), "--output", outputWc3.toString());
        check(extract.exitCode == 0 && Files.size(outputWc3) == Wc3File.FILE_SIZE, "api extract writes WC3 artifact");
        check(extract.stdout.contains("\"flagId\":321"), "api extract returns extracted metadata");
        check(extract.stdout.contains("\"sizeBytes\":1420"), "api extract returns artifact size");
    }

    private static void apiStructuredErrors() throws Exception {
        ApiCall missingCommand = api();
        check(missingCommand.exitCode == 2 && missingCommand.stdout.contains("\"code\":\"INVALID_REQUEST\""),
                "missing api command returns structured INVALID_REQUEST");

        ApiCall unknown = api("wat");
        check(unknown.exitCode == 2 && unknown.stdout.contains("\"code\":\"UNKNOWN_API_COMMAND\""),
                "unknown api command returns structured code");

        ApiCall missingFile = api("verify-wc3", "--input", "definitely-does-not-exist.wc3");
        check(missingFile.exitCode == 2 && missingFile.stdout.contains("\"code\":\"INPUT_FILE_NOT_FOUND\""),
                "missing input returns structured file error");

        Path invalidWc3 = Files.createTempFile("wc3-invalid-size-", ".wc3");
        Files.write(invalidWc3, new byte[10]);
        ApiCall badWc3 = api("verify-wc3", "--input", invalidWc3.toString());
        check(badWc3.exitCode == 2 && badWc3.stdout.contains("\"code\":\"INVALID_WC3\""),
                "invalid WC3 returns structured INVALID_WC3");
        check(oneNonBlankLine(badWc3.stdout), "api failure emits exactly one JSON line");
    }

    private static ApiCall api(String... args) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int code;
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            code = ApiV1.run(args, out);
        }
        return new ApiCall(code, bytes.toString(StandardCharsets.UTF_8).trim());
    }

    private static byte[] createValidWc3(int flagId, int iconSpecies) {
        byte[] wc = new byte[Wc3File.FILE_SIZE];
        for (int i = 0x04; i < Wc3File.CARD_BLOCK_SIZE; i++) {
            wc[i] = (byte) (i * 17 + 3);
        }
        Binary.putU16(wc, 0x04, flagId);
        Binary.putU16(wc, 0x06, iconSpecies);
        Binary.putU16(wc, Wc3File.CARD_METADATA_ICON_SPECIES_OFFSET, iconSpecies);
        for (int i = Wc3File.RAM_SCRIPT_DATA_OFFSET; i < Wc3File.FILE_SIZE; i++) {
            wc[i] = (byte) (i * 29 + 11);
        }
        Binary.putU32(wc, Wc3File.CARD_CRC_OFFSET,
                Crc16.calculate(wc, Wc3File.WONDER_CARD_OFFSET, WonderCard.SIZE));
        Binary.putU32(wc, Wc3File.RAM_SCRIPT_CHECKSUM_OFFSET,
                Crc16.calculate(wc, Wc3File.RAM_SCRIPT_DATA_OFFSET, Wc3File.RAM_SCRIPT_DATA_SIZE));
        return wc;
    }

    private static byte[] createValidSave(int slot1Counter, int slot2Counter) {
        byte[] save = new byte[SAVE_SIZE];
        initializeSlot(save, 0, slot1Counter);
        initializeSlot(save, 1, slot2Counter);
        return save;
    }

    private static void initializeSlot(byte[] save, int slot, int counter) {
        for (int logicalId = 0; logicalId < SECTORS_PER_SLOT; logicalId++) {
            int physical = slot * SECTORS_PER_SLOT + logicalId;
            int offset = physical * SECTOR_SIZE;
            Binary.putU16(save, offset + FOOTER_ID_OFFSET, logicalId);
            Binary.putU32(save, offset + FOOTER_SIGNATURE_OFFSET, SECTOR_SIGNATURE);
            Binary.putU32(save, offset + FOOTER_COUNTER_OFFSET, Integer.toUnsignedLong(counter));
            recalcSector(save, physical, logicalId);
        }
    }

    private static void recalcSector(byte[] save, int physicalSector, int logicalId) {
        int offset = physicalSector * SECTOR_SIZE;
        Binary.putU16(save, offset + FOOTER_CHECKSUM_OFFSET,
                SectorChecksum.calculate(save, offset, CHECKSUM_SIZES[logicalId]));
    }

    private static boolean oneNonBlankLine(String value) {
        return value.lines().filter(line -> !line.isBlank()).count() == 1;
    }

    private static void check(boolean condition, String message) {
        tests++;
        if (!condition) {
            throw new AssertionError("FAIL: " + message);
        }
    }

    private record ApiCall(int exitCode, String stdout) {}
}
