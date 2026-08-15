import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

public final class TestRunner {
    private static final String GOLDEN_SHA256 =
            "7a4d45dddb10288af7cd7eef6bdeec2d578c3f516f08e791f1fcf519279cab0d";

    private TestRunner() {}

    public static void main(String[] args) throws Exception {
        testRngMath();
        testSeedValidation();
        testKnownGoodPayload();
        testRuntimeLayout();
        testCatalog();
        testCandidate2dStaticAudit();
        testDispatcherCandidate1();
        testDispatcherCandidate2();
        testDispatcherCandidate3();
        System.out.println("All tests passed.");
    }

    private static void testRngMath() {
        long predecessor = RngMath.previousState(0x1234);
        long first = RngMath.nextState(predecessor);

        assertEquals(0x9A864C25L, predecessor, "0x1234 predecessor");
        assertEquals(0x00001234L, first, "first RNG state");
    }

    private static void testSeedValidation() {
        new SeedHotkeyEffect(RomProfile.FIRE_RED_EN_10, 0x0000);
        new SeedHotkeyEffect(RomProfile.FIRE_RED_EN_10, 0xFFFF);

        boolean rejected = false;
        try {
            new SeedHotkeyEffect(RomProfile.FIRE_RED_EN_10, 0x10000);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }

        assertTrue(rejected, "seed above 0xFFFF must be rejected");
    }

    private static void testKnownGoodPayload() throws Exception {
        SeedHotkeyEffect effect =
                new SeedHotkeyEffect(RomProfile.FIRE_RED_EN_10, 0x1234);

        RamScript generated = effect.build();

        Path temp = Files.createTempFile("seed-hotkey-generated", ".bin");
        generated.write(temp);
        byte[] actual = Files.readAllBytes(temp);
        Files.deleteIfExists(temp);

        Path goldenPath = Path.of(
                "testdata",
                "seed-hotkey-fr10-1234.golden.bin"
        );
        byte[] golden = Files.readAllBytes(goldenPath);

        assertEquals(RamScript.FILE_SIZE, actual.length, "generated RamScript size");
        assertEquals(RamScript.FILE_SIZE, golden.length, "golden RamScript size");
        assertTrue(generated.isChecksumValid(), "generated checksum");
        assertTrue(Arrays.equals(golden, actual),
                "generated payload must remain byte-identical to known-good golden file");

        String sha = sha256(actual);
        assertTrue(
                GOLDEN_SHA256.equals(sha),
                "generated SHA-256 changed: " + sha
        );
    }

    private static void testRuntimeLayout() {
        var regions = NativeRuntimeLayout.regions(RomProfile.FIRE_RED_EN_10);

        assertTrue(regions.size() == 5, "expected 5 documented runtime/staging regions");

        RuntimeRegion main = regions.get(0);
        assertEquals(0x03005310L, main.start(), "main hook start");
        assertEquals(32L, main.size(), "main hook size");

        RuntimeRegion extension = regions.get(1);
        assertEquals(0x03003F94L, extension.start(), "RNG extension start");
        assertEquals(12L, extension.size(), "RNG extension size");
    }

    private static void testCatalog() {
        var entries = NativeEffectCatalog.entries();

        assertTrue(entries.size() == 1, "v1 should expose only one validated native effect");
        assertTrue(entries.get(0).id().equals("seed-hotkey"), "seed-hotkey effect ID");
    }

    private static void testDispatcherCandidate1() {
        RamScript script = NormalContextHotkeyCandidate1.build(RomProfile.FIRE_RED_EN_10);

        assertTrue(script.isChecksumValid(), "dispatcher candidate checksum");
        assertEquals(0x030030F0L, NormalContextHotkeyCandidate1.callback1Address(), "callback1 slot");
        assertEquals(0x08056535L, NormalContextHotkeyCandidate1.originalCallbackThumb(), "CB1 thumb");
        assertEquals(0x0201C100L, NormalContextHotkeyCandidate1.debugAddress(), "debug address");
    }

    private static void testDispatcherCandidate2() {
        RamScript script = NormalContextHotkeyCandidate2.build(RomProfile.FIRE_RED_EN_10);

        assertTrue(script.isChecksumValid(), "dispatcher candidate 2 checksum");
        assertEquals(0x030030F0L, NormalContextHotkeyCandidate2.callback1Address(), "candidate2 callback1 slot");
        assertEquals(0x08056535L, NormalContextHotkeyCandidate2.cb1OverworldThumb(), "candidate2 CB1 thumb");
        assertEquals(0x080565B5L, NormalContextHotkeyCandidate2.cb2OverworldThumb(), "candidate2 CB2 thumb");
        assertEquals(0x03003F95L, NormalContextHotkeyCandidate2.callbackWrapperThumb(RomProfile.FIRE_RED_EN_10), "candidate2 wrapper thumb");
        assertEquals(0x03005311L, NormalContextHotkeyCandidate2.supervisorThumb(RomProfile.FIRE_RED_EN_10), "candidate2 supervisor thumb");
        assertEquals(0x77L, NormalContextHotkeyCandidate2.debugMarker(), "candidate2 marker");
    }


    private static void testDispatcherCandidate3() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        RamScript script = NormalContextHotkeyCandidate3.build(rom);
        assertTrue(script.isChecksumValid(), "dispatcher candidate 3 checksum");

        byte[] supervisor = NormalContextHotkeyCandidate3.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate3.detectorBytesForTest(rom);
        byte[] trigger = NormalContextHotkeyCandidate3.triggerBytesForTest();
        byte[] tail = NormalContextHotkeyCandidate3.callbackTailBytesForTest();
        byte[] installer = NormalContextHotkeyCandidate3.installerBytesForTest(rom);

        assertEquals(32, supervisor.length, "c3 supervisor size");
        assertEquals(16, detector.length, "c3 detector size");
        assertEquals(8, trigger.length, "c3 trigger size");
        assertEquals(4, tail.length, "c3 callback tail size");
        assertEquals(16, installer.length, "c3 installer size");

        // Supervisor is fully self-contained.
        assertEquals(0x03005320L,
                ThumbEncodingChecks.decodeAdrTarget(0x03005310L, supervisor[0]&0xFF, supervisor[1]&0xFF),
                "c3 supervisor literal table");
        assertEquals(0x0300531CL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(0x03005318L, supervisor[8]&0xFF, supervisor[9]&0xFF),
                "c3 supervisor bne tail");
        assertEquals(0x0300532CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(0x0300531CL, supervisor[12]&0xFF, supervisor[13]&0xFF),
                "c3 supervisor original VBlank literal target");
        assertEquals(0x030030F0L, ThumbEncodingChecks.u32(supervisor,0x10), "c3 callback1 literal");
        assertEquals(0x08056535L, ThumbEncodingChecks.u32(supervisor,0x14), "c3 CB1 literal");
        assertEquals(0x03003F71L, ThumbEncodingChecks.u32(supervisor,0x18), "c3 detector pointer");
        assertEquals(0x08000725L, ThumbEncodingChecks.u32(supervisor,0x1C), "c3 original VBlank literal");

        // Detector literals/branches.
        assertEquals(0x03003F94L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(0x03003F70L, detector[0]&0xFF, detector[1]&0xFF),
                "c3 heldKeys literal target");
        assertEquals(0x03003F7EL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(0x03003F76L, detector[6]&0xFF, detector[7]&0xFF),
                "c3 first BCC no-trigger");
        assertEquals(0x03003F7EL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(0x03003F7AL, detector[10]&0xFF, detector[11]&0xFF),
                "c3 second BCC no-trigger");
        assertEquals(0x03003F98L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(0x03003F7CL, detector[12]&0xFF, detector[13]&0xFF),
                "c3 trigger branch");
        assertEquals(0x03003EB4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(0x03003F7EL, detector[14]&0xFF, detector[15]&0xFF),
                "c3 no-trigger callback tail branch");

        // Trigger extension ADR and callback-tail branch.
        assertEquals(0x03003FA0L,
                ThumbEncodingChecks.decodeAdrTarget(0x03003F98L, trigger[0]&0xFF, trigger[1]&0xFF),
                "c3 trigger marker base");
        assertEquals(0x03003EB4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(0x03003F9EL, trigger[6]&0xFF, trigger[7]&0xFF),
                "c3 trigger callback tail branch");
        assertEquals(0x03003FA1L, NormalContextHotkeyCandidate3.markerAddress(), "c3 marker address");

        // Callback tail remains normal-context only and resolves to 03003EC0.
        assertEquals(0x03003EC0L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(0x03003EB4L, tail[0]&0xFF, tail[1]&0xFF),
                "c3 callback tail literal target");

        // Installer still only redirects VBlank after all runtime bytes are copied.
        assertEquals(0x02021D20L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(0x02021D18L, installer[0]&0xFF, installer[1]&0xFF),
                "c3 installer VBlank literal target");
        assertEquals(0x02021D24L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(0x02021D1AL, installer[2]&0xFF, installer[3]&0xFF),
                "c3 installer supervisor literal target");
        assertEquals(0x03003550L, ThumbEncodingChecks.u32(installer,0x08), "c3 VBlank slot literal");
        assertEquals(0x03005311L, ThumbEncodingChecks.u32(installer,0x0C), "c3 supervisor pointer literal");
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder result = new StringBuilder();

        for (byte value : hash) {
            result.append(String.format("%02x", Byte.toUnsignedInt(value)));
        }

        return result.toString();
    }



    private static void testCandidate2dStaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NormalContextHotkeyCandidate2.supervisorBytesForTest(rom);
        byte[] wrapper = NormalContextHotkeyCandidate2.callbackWrapperBytesForTest(rom);
        byte[] tail = NormalContextHotkeyCandidate2.vblankTailBytesForTest();
        byte[] installer = NormalContextHotkeyCandidate2.installerBytesForTest(rom);

        assertEquals(32, supervisor.length, "2d supervisor size");
        assertEquals(12, wrapper.length, "2d callback wrapper size");
        assertEquals(4, tail.length, "2d VBlank tail size");
        assertEquals(16, installer.length, "2d installer size");

        // Supervisor ADR: 03005310 -> literal table at 03005324.
        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeAdrTarget(
                        0x03005310L,
                        supervisor[0] & 0xFF,
                        supervisor[1] & 0xFF
                ),
                "2d supervisor ADR literal table"
        );

        // Conditional BNE at 03005318 must jump to the tail arithmetic at 0300531C.
        assertEquals(
                0x0300531CL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x03005318L,
                        supervisor[8] & 0xFF,
                        supervisor[9] & 0xFF
                ),
                "2d supervisor BNE tail target"
        );

        assertEquals(0x030030F0L, ThumbEncodingChecks.u32(supervisor, 0x14),
                "2d supervisor callback1 literal");
        assertEquals(0x08056535L, ThumbEncodingChecks.u32(supervisor, 0x18),
                "2d supervisor expected CB1 literal");
        assertEquals(0x03003F95L, ThumbEncodingChecks.u32(supervisor, 0x1C),
                "2d supervisor wrapper literal");

        // wrapper|1 - 0xE0 must be the VBlank tail stub|1.
        assertEquals(
                0x03003EB5L,
                0x03003F95L - 0xE0L,
                "2d supervisor compact VBlank tail computation"
        );

        // Callback wrapper has its own CB1 literal and never uses the VBlank tail.
        assertEquals(
                0x03003F9CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F94L,
                        wrapper[0] & 0xFF,
                        wrapper[1] & 0xFF
                ),
                "2d wrapper CB1 literal target"
        );
        assertEquals(0x08056535L, ThumbEncodingChecks.u32(wrapper, 0x08),
                "2d wrapper CB1 function pointer");

        // VBlank tail's literal remains exclusively the original VBlank handler.
        assertEquals(
                0x03003EC0L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003EB4L,
                        tail[0] & 0xFF,
                        tail[1] & 0xFF
                ),
                "2d VBlank tail literal target"
        );

        // Installer literal loads must resolve to the two words at +8 and +12.
        assertEquals(
                0x02021D20L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x02021D18L,
                        installer[0] & 0xFF,
                        installer[1] & 0xFF
                ),
                "2d installer VBlank-slot literal target"
        );
        assertEquals(
                0x02021D24L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x02021D1AL,
                        installer[2] & 0xFF,
                        installer[3] & 0xFF
                ),
                "2d installer supervisor literal target"
        );
        assertEquals(0x03003550L, ThumbEncodingChecks.u32(installer, 0x08),
                "2d installer VBlank slot");
        assertEquals(0x03005311L, ThumbEncodingChecks.u32(installer, 0x0C),
                "2d installer supervisor pointer");

        // No r4-r7 register opcodes are used by the known supervisor sequence.
        // Exact bytes are pinned here so accidental assembly changes fail tests.
        int[] expectedSupervisor = {
                0x04,0xA3, 0x07,0xCB, 0x03,0x68, 0x8B,0x42,
                0x00,0xD1, 0x02,0x60, 0xE0,0x3A, 0x10,0x47,
                0xC0,0x46, 0xC0,0x46
        };
        for (int i = 0; i < expectedSupervisor.length; i++) {
            assertEquals(expectedSupervisor[i], supervisor[i] & 0xFF,
                    "2d supervisor opcode byte " + i);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(
                    message + ": expected 0x"
                            + Long.toHexString(expected)
                            + ", got 0x"
                            + Long.toHexString(actual)
            );
        }
    }
}
