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
        testDispatcherCandidate4();
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


    private static void testDispatcherCandidate4() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        RamScript script = NormalContextHotkeyCandidate4.build(rom);
        assertTrue(script.isChecksumValid(), "dispatcher candidate 4 checksum");

        byte[] supervisor = NormalContextHotkeyCandidate4.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate4.detectorBytesForTest(rom);
        byte[] trigger = NormalContextHotkeyCandidate4.triggerBytesForTest();
        byte[] tail = NormalContextHotkeyCandidate4.callbackTailBytesForTest();
        byte[] installer = NormalContextHotkeyCandidate4.installerBytesForTest(rom);

        // C4 deliberately keeps the C3 supervisor and detector byte-for-byte.
        assertTrue(Arrays.equals(supervisor, NormalContextHotkeyCandidate3.supervisorBytesForTest(rom)),
                "c4 supervisor must equal validated c3 supervisor");
        assertTrue(Arrays.equals(detector, NormalContextHotkeyCandidate3.detectorBytesForTest(rom)),
                "c4 detector must equal validated c3 detector");
        assertTrue(Arrays.equals(installer, NormalContextHotkeyCandidate3.installerBytesForTest(rom)),
                "c4 installer must equal validated c3 installer");

        // Trigger: movs r0,#5; ldr r3, literal; bx r3; nop.
        assertEquals(8, trigger.length, "c4 trigger size");
        assertEquals(0x05, trigger[0] & 0xFF, "c4 SE_SELECT immediate");
        assertEquals(0x20, trigger[1] & 0xFF, "c4 movs r0 opcode");
        assertEquals(0x03003FA4L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F9AL, trigger[2]&0xFF, trigger[3]&0xFF),
                "c4 PlaySE literal target");
        assertEquals(0x18, trigger[4] & 0xFF, "c4 bx r3 low byte");
        assertEquals(0x47, trigger[5] & 0xFF, "c4 bx r3 high byte");

        assertEquals(0x03003FA4L, NormalContextHotkeyCandidate4.actionLiteralAddress(),
                "c4 action literal address");
        assertEquals(0x080722CDL, NormalContextHotkeyCandidate4.playSeThumb(),
                "c4 PlaySE Thumb pointer");
        assertEquals(0x0005L, NormalContextHotkeyCandidate4.soundEffect(),
                "c4 SE_SELECT value");

        // No-trigger callback tail remains unchanged and points to CB1 literal.
        assertTrue(Arrays.equals(tail, NormalContextHotkeyCandidate3.callbackTailBytesForTest()),
                "c4 no-trigger tail must equal c3 tail");
        assertEquals(0x03003EC0L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003EB4L, tail[0]&0xFF, tail[1]&0xFF),
                "c4 no-trigger CB1 literal target");
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


    private static void testCandidate5StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NormalContextHotkeyCandidate5.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate5.detectorBytesForTest(rom);
        byte[] trampoline = NormalContextHotkeyCandidate5.trampolineBytesForTest();
        byte[] tail = NormalContextHotkeyCandidate5.callbackTailBytesForTest();
        byte[] installer = NormalContextHotkeyCandidate5.installerBytesForTest(rom);

        assertEquals(32, supervisor.length, "C5 supervisor size");
        assertEquals(16, detector.length, "C5 detector size");
        assertEquals(20, trampoline.length, "C5 trampoline size");
        assertEquals(4, tail.length, "C5 tail size");
        assertEquals(16, installer.length, "C5 installer size");

        // Detector heldKeys LDR remains 03003F94.
        assertEquals(
                0x03003F94L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F70L,
                        detector[0] & 0xFF,
                        detector[1] & 0xFF
                ),
                "C5 detector heldKeys literal"
        );

        // Trigger B at 03003F7C must land exactly at 03003F80.
        assertEquals(
                0x03003F80L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7CL,
                        detector[12] & 0xFF,
                        detector[13] & 0xFF
                ),
                "C5 trigger branch"
        );

        // No-trigger B remains the known CB1 tail at 03003EB4.
        assertEquals(
                0x03003EB4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7EL,
                        detector[14] & 0xFF,
                        detector[15] & 0xFF
                ),
                "C5 no-trigger CB1 branch"
        );

        // Function literal load from 03003F82 -> 03003F9C.
        assertEquals(
                0x03003F9CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F82L,
                        trampoline[2] & 0xFF,
                        trampoline[3] & 0xFF
                ),
                "C5 GetSavedRamScript literal target"
        );

        // ADR at 03003F84 -> continuation 03003F8C.
        assertEquals(
                0x03003F8CL,
                ThumbEncodingChecks.decodeAdrTarget(
                        0x03003F84L,
                        trampoline[4] & 0xFF,
                        trampoline[5] & 0xFF
                ),
                "C5 continuation ADR"
        );

        // ADR at 03003F8C -> result slot 03003FA4.
        assertEquals(
                0x03003FA4L,
                ThumbEncodingChecks.decodeAdrTarget(
                        0x03003F8CL,
                        trampoline[12] & 0xFF,
                        trampoline[13] & 0xFF
                ),
                "C5 result-slot ADR"
        );

        // Exact critical call/return opcodes:
        assertEquals(0xB500L, ((trampoline[1] & 0xFF) << 8) | (trampoline[0] & 0xFF),
                "C5 push {lr}");
        assertEquals(0x4696L, ((trampoline[9] & 0xFF) << 8) | (trampoline[8] & 0xFF),
                "C5 mov lr,r2");
        assertEquals(0x4718L, ((trampoline[11] & 0xFF) << 8) | (trampoline[10] & 0xFF),
                "C5 bx r3");
        assertEquals(0x6008L, ((trampoline[15] & 0xFF) << 8) | (trampoline[14] & 0xFF),
                "C5 str r0,[r1]");
        assertEquals(0xBD00L, ((trampoline[17] & 0xFF) << 8) | (trampoline[16] & 0xFF),
                "C5 pop {pc}");

        assertEquals(0x08069E49L, NormalContextHotkeyCandidate5.getSavedRamScriptThumb(),
                "C5 GetSavedRamScriptIfValid Thumb pointer");
        assertEquals(0x03003FA4L, NormalContextHotkeyCandidate5.resultSlotAddress(),
                "C5 result slot");
        assertEquals(0x03005008L, NormalContextHotkeyCandidate5.gSaveBlock1PtrAddress(),
                "C5 gSaveBlock1Ptr global");

        // Preserve the same VBlank supervisor bytes as Candidate 4.
        byte[] c4Supervisor = NormalContextHotkeyCandidate4.supervisorBytesForTest(rom);
        assertTrue(java.util.Arrays.equals(supervisor, c4Supervisor),
                "C5 must preserve the validated C4 supervisor bytes");
    }


    private static void testCandidate5a2StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NormalContextHotkeyCandidate5a2.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate5a2.detectorBytesForTest(rom);
        byte[] trampoline = NormalContextHotkeyCandidate5a2.trampolineBytesForTest();

        assertEquals(32, supervisor.length, "C5a2 supervisor size");
        assertEquals(16, detector.length, "C5a2 detector size");
        assertEquals(20, trampoline.length, "C5a2 trampoline size");
        assertEquals(0xA202L,
                ((trampoline[7] & 0xFF) << 8) | (trampoline[6] & 0xFF),
                "C5a2 ADR opcode must be 02 A2");


        assertTrue(
                java.util.Arrays.equals(
                        supervisor,
                        NormalContextHotkeyCandidate5.supervisorBytesForTest(rom)
                ),
                "C5a2 must preserve C5 supervisor bytes"
        );

        assertTrue(
                java.util.Arrays.equals(
                        detector,
                        NormalContextHotkeyCandidate5.detectorBytesForTest(rom)
                ),
                "C5a2 must preserve C5 detector bytes"
        );

        assertEquals(
                0x03003F80L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7CL,
                        detector[12] & 0xFF,
                        detector[13] & 0xFF
                ),
                "C5a2 trigger branch"
        );

        assertEquals(
                0x03003F9CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F84L,
                        trampoline[4] & 0xFF,
                        trampoline[5] & 0xFF
                ),
                "C5a2 PlaySE literal target"
        );

        assertEquals(
                0x03003F90L,
                ThumbEncodingChecks.decodeAdrTarget(
                        0x03003F86L,
                        trampoline[6] & 0xFF,
                        trampoline[7] & 0xFF
                ),
                "C5a2 continuation ADR"
        );

        assertEquals(0xB500L, ((trampoline[1] & 0xFF) << 8) | (trampoline[0] & 0xFF),
                "C5a2 push {lr}");
        assertEquals(0x2005L, ((trampoline[3] & 0xFF) << 8) | (trampoline[2] & 0xFF),
                "C5a2 movs r0,#5");
        assertEquals(0x4696L, ((trampoline[11] & 0xFF) << 8) | (trampoline[10] & 0xFF),
                "C5a2 mov lr,r2");
        assertEquals(0x4718L, ((trampoline[13] & 0xFF) << 8) | (trampoline[12] & 0xFF),
                "C5a2 bx r3");
        assertEquals(0xBD00L, ((trampoline[17] & 0xFF) << 8) | (trampoline[16] & 0xFF),
                "C5a2 pop {pc}");

        assertEquals(0x080722CDL, NormalContextHotkeyCandidate5a2.playSeThumb(),
                "C5a2 PlaySE Thumb");
        assertEquals(0x0005L, NormalContextHotkeyCandidate5a2.seSelect(),
                "C5a2 SE_SELECT");
        assertEquals(0x03003F80L, NormalContextHotkeyCandidate5a2.trampolineAddress(),
                "C5a2 deliberately preserves C5 trampoline address");
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
