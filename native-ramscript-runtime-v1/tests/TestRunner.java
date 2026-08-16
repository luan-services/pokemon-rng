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

        // runtime release-candidate audits must actually execute.
        // older project versions accidentally declared these methods without
        // invoking them from main(), which allowed RC4 encoding bugs through.
        testRuntimeV1Rc1StaticAudit();
        testRuntimeV1Rc2StaticAudit();
        testRuntimeV1Rc3StaticAudit();
        testRuntimeV1Rc4aStaticAudit();
        testRuntimeV1MultiProfileAudit();

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


    private static void testCandidate5bStaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NormalContextHotkeyCandidate5b.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate5b.detectorBytesForTest(rom);
        byte[] thunk = NormalContextHotkeyCandidate5b.thunkBytesForTest();
        byte[] continuation = NormalContextHotkeyCandidate5b.continuationBytesForTest();
        byte[] wrapper = NormalContextHotkeyCandidate5b.wrapperBytesForTest();

        assertTrue(
                java.util.Arrays.equals(
                        supervisor,
                        NormalContextHotkeyCandidate5a2.supervisorBytesForTest(rom)
                ),
                "C5b must preserve validated supervisor bytes"
        );

        assertEquals(16, detector.length, "C5b detector size");
        assertEquals(4, thunk.length, "C5b thunk size");
        assertEquals(2, continuation.length, "C5b continuation size");
        assertEquals(12, wrapper.length, "C5b wrapper size");

        // Detector still reads heldKeysRaw pointer at 03003F94.
        assertEquals(
                0x03003F94L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F70L,
                        detector[0] & 0xFF,
                        detector[1] & 0xFF
                ),
                "C5b heldKeys literal"
        );

        // Trigger now skips the entire live-global area and lands at 03003FA4.
        assertEquals(
                0x03003FA4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7CL,
                        detector[12] & 0xFF,
                        detector[13] & 0xFF
                ),
                "C5b trigger branch"
        );

        assertEquals(
                0x03003EB4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7EL,
                        detector[14] & 0xFF,
                        detector[15] & 0xFF
                ),
                "C5b no-trigger branch"
        );

        // Tiny thunk's literal is exactly at 03003F9C.
        assertEquals(
                0x03003F9CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F98L,
                        thunk[0] & 0xFF,
                        thunk[1] & 0xFF
                ),
                "C5b thunk literal"
        );

        // Wrapper critical opcodes and final branch.
        assertEquals(0xB500L,
                ((wrapper[1] & 0xFF) << 8) | (wrapper[0] & 0xFF),
                "C5b push {lr}");
        assertEquals(0x2005L,
                ((wrapper[3] & 0xFF) << 8) | (wrapper[2] & 0xFF),
                "C5b movs r0,#5");
        assertEquals(0x467AL,
                ((wrapper[5] & 0xFF) << 8) | (wrapper[4] & 0xFF),
                "C5b mov r2,pc");
        assertEquals(0x3A09L,
                ((wrapper[7] & 0xFF) << 8) | (wrapper[6] & 0xFF),
                "C5b subs r2,#9");
        assertEquals(0x4696L,
                ((wrapper[9] & 0xFF) << 8) | (wrapper[8] & 0xFF),
                "C5b mov lr,r2");

        assertEquals(
                0x03003F98L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003FAEL,
                        wrapper[10] & 0xFF,
                        wrapper[11] & 0xFF
                ),
                "C5b branch to thunk"
        );

        assertEquals(0xBD00L,
                ((continuation[1] & 0xFF) << 8) | (continuation[0] & 0xFF),
                "C5b continuation pop {pc}");

        assertEquals(0x03003F98L, NormalContextHotkeyCandidate5b.functionThunkAddress(),
                "C5b thunk address");
        assertEquals(0x03003FA2L, NormalContextHotkeyCandidate5b.continuationAddress(),
                "C5b continuation address");
        assertEquals(0x03003FA4L, NormalContextHotkeyCandidate5b.callWrapperAddress(),
                "C5b wrapper address");
        assertEquals(0x080722CDL, NormalContextHotkeyCandidate5b.playSeThumb(),
                "C5b PlaySE Thumb");

        // Explicitly assert that no C5b region overlaps 03003F80..03003F93.
        assertTrue(NormalContextHotkeyCandidate5b.functionThunkAddress() >= 0x03003F94L,
                "C5b thunk must be above live C5 region");
        assertTrue(NormalContextHotkeyCandidate5b.callWrapperAddress() >= 0x03003FA1L,
                "C5b wrapper must be in post-FA0 padding");
    }


    private static void testCandidate5cStaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NormalContextHotkeyCandidate5c.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate5c.detectorBytesForTest(rom);
        byte[] thunk = NormalContextHotkeyCandidate5c.thunkBytesForTest();
        byte[] wrapper = NormalContextHotkeyCandidate5c.wrapperBytesForTest();

        assertTrue(
                java.util.Arrays.equals(
                        supervisor,
                        NormalContextHotkeyCandidate5b.supervisorBytesForTest(rom)
                ),
                "C5c must preserve C5b supervisor bytes"
        );

        assertEquals(16, detector.length, "C5c detector size");
        assertEquals(4, thunk.length, "C5c thunk size");
        assertEquals(14, wrapper.length, "C5c wrapper size");

        assertEquals(
                0x03003F94L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F70L,
                        detector[0] & 0xFF,
                        detector[1] & 0xFF
                ),
                "C5c heldKeys literal"
        );

        assertEquals(
                0x03003FA2L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7CL,
                        detector[12] & 0xFF,
                        detector[13] & 0xFF
                ),
                "C5c trigger branch"
        );

        assertEquals(
                0x03003EB4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7EL,
                        detector[14] & 0xFF,
                        detector[15] & 0xFF
                ),
                "C5c no-trigger branch"
        );

        assertEquals(
                0x03003F9CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F98L,
                        thunk[0] & 0xFF,
                        thunk[1] & 0xFF
                ),
                "C5c function literal"
        );

        assertEquals(0xB510L,
                ((wrapper[1] & 0xFF) << 8) | (wrapper[0] & 0xFF),
                "C5c push {r4,lr}");
        assertEquals(0x467CL,
                ((wrapper[3] & 0xFF) << 8) | (wrapper[2] & 0xFF),
                "C5c mov r4,pc");
        assertEquals(0x3C07L,
                ((wrapper[5] & 0xFF) << 8) | (wrapper[4] & 0xFF),
                "C5c subs r4,#7");

        assertEquals(
                0x03003F98L,
                ThumbEncodingChecks.decodeLongBranchWithLinkTarget(
                        0x03003FA8L,
                        wrapper[6] & 0xFF,
                        wrapper[7] & 0xFF,
                        wrapper[8] & 0xFF,
                        wrapper[9] & 0xFF
                ),
                "C5c BL thunk target"
        );

        assertEquals(0x7020L,
                ((wrapper[11] & 0xFF) << 8) | (wrapper[10] & 0xFF),
                "C5c strb r0,[r4]");
        assertEquals(0xBD10L,
                ((wrapper[13] & 0xFF) << 8) | (wrapper[12] & 0xFF),
                "C5c pop {r4,pc}");

        assertEquals(0x03003FA1L, NormalContextHotkeyCandidate5c.resultLowByteAddress(),
                "C5c result marker address");
        assertEquals(0x03003FA2L, NormalContextHotkeyCandidate5c.callWrapperAddress(),
                "C5c wrapper address");
        assertEquals(0x08069E49L, NormalContextHotkeyCandidate5c.getSavedRamScriptThumb(),
                "C5c GetSavedRamScriptIfValid Thumb");
        assertEquals(0x03005008L, NormalContextHotkeyCandidate5c.gSaveBlock1PtrAddress(),
                "C5c gSaveBlock1Ptr");

        assertTrue(NormalContextHotkeyCandidate5c.callWrapperAddress() > 0x03003FA0L,
                "C5c wrapper must stay after gLastSendQueueCount");
        assertTrue(NormalContextHotkeyCandidate5c.callWrapperAddress() + wrapper.length <= 0x03003FB0L,
                "C5c wrapper must end before gLink");
        assertTrue(NormalContextHotkeyCandidate5c.functionThunkAddress() >= 0x03003F94L,
                "C5c must not touch 03003F80..03003F93");
    }


    private static void testCandidate6StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NormalContextHotkeyCandidate6.supervisorBytesForTest(rom);
        byte[] detector = NormalContextHotkeyCandidate6.detectorBytesForTest(rom);
        byte[] stage1 = NormalContextHotkeyCandidate6.stage1BytesForTest();
        byte[] stage2 = NormalContextHotkeyCandidate6.stage2BytesForTest();
        byte[] thunk = NormalContextHotkeyCandidate6.thunkBytesForTest();
        byte[] payload = NormalContextHotkeyCandidate6.payloadBytesForTest();

        assertTrue(
                java.util.Arrays.equals(
                        supervisor,
                        NormalContextHotkeyCandidate5c.supervisorBytesForTest(rom)
                ),
                "C6 must preserve C5c supervisor bytes"
        );

        assertEquals(16, detector.length, "C6 detector size");
        assertEquals(14, stage1.length, "C6 stage1 size");
        assertEquals(14, stage2.length, "C6 stage2 size");
        assertEquals(4, thunk.length, "C6 thunk size");
        assertEquals(7, payload.length, "C6 field payload size");

        assertEquals(
                0x03003FA2L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F7CL,
                        detector[12] & 0xFF,
                        detector[13] & 0xFF
                ),
                "C6 detector trigger"
        );

        assertEquals(
                0x03003F9CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03003F98L,
                        thunk[0] & 0xFF,
                        thunk[1] & 0xFF
                ),
                "C6 GetSaved literal"
        );

        assertEquals(0x4720L,
                ((thunk[3] & 0xFF) << 8) | (thunk[2] & 0xFF),
                "C6 thunk bx r4");

        assertEquals(
                0x03003F98L,
                ThumbEncodingChecks.decodeLongBranchWithLinkTarget(
                        0x03003FA4L,
                        stage1[2] & 0xFF,
                        stage1[3] & 0xFF,
                        stage1[4] & 0xFF,
                        stage1[5] & 0xFF
                ),
                "C6 stage1 BL GetSaved thunk"
        );

        assertEquals(
                0x03003F42L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003FACL,
                        stage1[10] & 0xFF,
                        stage1[11] & 0xFF
                ),
                "C6 stage1 branch stage2"
        );

        assertEquals(
                0x03003F9AL,
                ThumbEncodingChecks.decodeLongBranchWithLinkTarget(
                        0x03003F4AL,
                        stage2[8] & 0xFF,
                        stage2[9] & 0xFF,
                        stage2[10] & 0xFF,
                        stage2[11] & 0xFF
                ),
                "C6 stage2 BL bx-r4"
        );

        assertEquals(
                0x03003FAEL,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F4EL,
                        stage2[12] & 0xFF,
                        stage2[13] & 0xFF
                ),
                "C6 stage2 return to pop"
        );

        assertEquals(0x08069E49L, NormalContextHotkeyCandidate6.getSavedRamScriptThumb(),
                "C6 GetSaved pointer");
        assertEquals(0x08069AE5L, NormalContextHotkeyCandidate6.scriptContextSetupThumb(),
                "C6 SetupScript pointer");
        assertEquals(0x364L,
                NormalContextHotkeyCandidate6.getSavedRamScriptThumb()
                        - NormalContextHotkeyCandidate6.scriptContextSetupThumb(),
                "C6 ROM function delta");

        assertEquals(0x0AL, NormalContextHotkeyCandidate6.payloadOffset(),
                "C6 payload offset");

        assertEquals(0x11L, payload[0] & 0xFF, "C6 payload setptr opcode");
        assertEquals(0x66L, payload[1] & 0xFF, "C6 payload marker value");
        assertEquals(0x02L, payload[6] & 0xFF, "C6 payload end opcode");

        // Stage2 must end before gSendCmd at 03003F50.
        assertEquals(0x03003F50L,
                NormalContextHotkeyCandidate6.stage2Address() + stage2.length,
                "C6 stage2 exact padding boundary");

        // Stage1 must end exactly at gLink start.
        assertEquals(0x03003FB0L,
                NormalContextHotkeyCandidate6.stage1Address() + stage1.length,
                "C6 stage1 exact padding boundary");
    }


    private static void testRuntimeV1Rc1StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NativeRamScriptRuntimeV1Rc1.supervisorBytesForTest();
        byte[] literals = NativeRamScriptRuntimeV1Rc1.supervisorLiteralBytesForTest();
        byte[] wrapper = NativeRamScriptRuntimeV1Rc1.wrapperBytesForTest(rom);
        byte[] stage1 = NativeRamScriptRuntimeV1Rc1.stage1BytesForTest();
        byte[] stage2 = NativeRamScriptRuntimeV1Rc1.stage2BytesForTest();
        byte[] thunk = NativeRamScriptRuntimeV1Rc1.thunkBytesForTest();
        byte[] installer = NativeRamScriptRuntimeV1Rc1.installerBytesForTest(rom);
        byte[] payload = NativeRamScriptRuntimeV1Rc1.payloadBytesForTest();

        assertEquals(14, supervisor.length, "runtime rc1 supervisor size");
        assertEquals(12, literals.length, "runtime rc1 supervisor literal size");
        assertEquals(32, wrapper.length, "runtime rc1 wrapper size");
        assertEquals(14, stage1.length, "runtime rc1 stage1 size");
        assertEquals(14, stage2.length, "runtime rc1 stage2 size");
        assertEquals(4, thunk.length, "runtime rc1 thunk size");
        assertEquals(16, installer.length, "runtime rc1 installer size");

        // Supervisor ADR -> 03003FA4
        assertEquals(
                0x03003FA4L,
                ThumbEncodingChecks.decodeAdrTarget(
                        0x03003F42L,
                        supervisor[0] & 0xFF,
                        supervisor[1] & 0xFF
                ),
                "runtime rc1 supervisor literals ADR"
        );

        assertEquals(
                0x03003EB4L,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x03003F4AL,
                        supervisor[8] & 0xFF,
                        supervisor[9] & 0xFF
                ),
                "runtime rc1 supervisor BNE tail"
        );

        assertEquals(
                0x03003EB4L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x03003F4EL,
                        supervisor[12] & 0xFF,
                        supervisor[13] & 0xFF
                ),
                "runtime rc1 supervisor B tail"
        );

        assertEquals(0x030030F0L, ThumbEncodingChecks.u32(literals, 0),
                "runtime rc1 callback1 literal");
        assertEquals(0x08056535L, ThumbEncodingChecks.u32(literals, 4),
                "runtime rc1 CB1 literal");
        assertEquals(0x03005311L, ThumbEncodingChecks.u32(literals, 8),
                "runtime rc1 wrapper literal");

        // Final wrapper local data/tails.
        assertEquals(
                0x03005320L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03005310L,
                        wrapper[0] & 0xFF,
                        wrapper[1] & 0xFF
                ),
                "runtime rc1 heldKeys literal"
        );

        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x03005316L,
                        wrapper[6] & 0xFF,
                        wrapper[7] & 0xFF
                ),
                "runtime rc1 first no-trigger branch"
        );

        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x0300531AL,
                        wrapper[10] & 0xFF,
                        wrapper[11] & 0xFF
                ),
                "runtime rc1 second no-trigger branch"
        );

        assertEquals(
                0x03005082L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300531CL,
                        wrapper[12] & 0xFF,
                        wrapper[13] & 0xFF
                ),
                "runtime rc1 trigger branch"
        );

        assertEquals(0x03003118L, ThumbEncodingChecks.u32(wrapper, 0x10),
                "runtime rc1 heldKeysRaw value");

        assertEquals(
                0x03005328L,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x03005324L,
                        wrapper[0x14] & 0xFF,
                        wrapper[0x15] & 0xFF
                ),
                "runtime rc1 local CB1 literal"
        );

        assertEquals(0x08056535L, ThumbEncodingChecks.u32(wrapper, 0x18),
                "runtime rc1 local CB1 value");

        // Stage 1 / stage 2 exact branches.
        assertEquals(
                0x03003F98L,
                ThumbEncodingChecks.decodeLongBranchWithLinkTarget(
                        0x03005084L,
                        stage1[2] & 0xFF,
                        stage1[3] & 0xFF,
                        stage1[4] & 0xFF,
                        stage1[5] & 0xFF
                ),
                "runtime rc1 stage1 BL thunk"
        );

        assertEquals(
                0x0300508EL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x0300508AL,
                        stage1[8] & 0xFF,
                        stage1[9] & 0xFF
                ),
                "runtime rc1 null return"
        );

        assertEquals(
                0x03005032L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300508CL,
                        stage1[10] & 0xFF,
                        stage1[11] & 0xFF
                ),
                "runtime rc1 stage1 -> stage2"
        );

        assertEquals(
                0x03003F9AL,
                ThumbEncodingChecks.decodeLongBranchWithLinkTarget(
                        0x0300503AL,
                        stage2[8] & 0xFF,
                        stage2[9] & 0xFF,
                        stage2[10] & 0xFF,
                        stage2[11] & 0xFF
                ),
                "runtime rc1 stage2 BL bx-r4"
        );

        assertEquals(
                0x0300508EL,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300503EL,
                        stage2[12] & 0xFF,
                        stage2[13] & 0xFF
                ),
                "runtime rc1 stage2 -> pop"
        );

        // Exact safe padding boundaries.
        assertEquals(0x03003F50L,
                NativeRamScriptRuntimeV1Rc1.supervisorAddress() + supervisor.length,
                "runtime rc1 supervisor must end before gSendCmd");
        assertEquals(0x03005040L,
                NativeRamScriptRuntimeV1Rc1.stage2Address() + stage2.length,
                "runtime rc1 stage2 must end before VMap");
        assertEquals(0x03005090L,
                NativeRamScriptRuntimeV1Rc1.stage1Address() + stage1.length,
                "runtime rc1 stage1 must end before gTasks");
        assertEquals(0x03005330L,
                NativeRamScriptRuntimeV1Rc1.wrapperAddress() + wrapper.length,
                "runtime rc1 wrapper must exactly fill UnusedVarNeededToMatch");

        // Installer is deliberately replaced by the final wrapper at the same address.
        assertEquals(NativeRamScriptRuntimeV1Rc1.wrapperAddress(), 0x03005310L,
                "runtime rc1 final wrapper address");
        assertEquals(0x03005311L, NativeRamScriptRuntimeV1Rc1.wrapperThumb(),
                "runtime rc1 final wrapper Thumb pointer");

        // No dependency on old experimental persistent areas.
        assertTrue(NativeRamScriptRuntimeV1Rc1.wrapperAddress() != 0x03003F70L,
                "runtime rc1 must not use gLinkTestBGInfo for wrapper");
        assertTrue(NativeRamScriptRuntimeV1Rc1.wrapperAddress() != rom.installerStaging,
                "runtime rc1 must not use old gStringVar4 staging");

        assertEquals(0x11L, payload[0] & 0xFF, "runtime rc1 test payload setptr");
        assertEquals(0x66L, payload[1] & 0xFF, "runtime rc1 test payload marker");
        assertEquals(0x02L, payload[6] & 0xFF, "runtime rc1 test payload end");
    }


    private static void testRuntimeV1Rc2StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] supervisor = NativeRamScriptRuntimeV1Rc2.supervisorBytesForTest();
        byte[] wrapper = NativeRamScriptRuntimeV1Rc2.wrapperBytesForTest(rom);
        byte[] payload = NativeRamScriptRuntimeV1Rc2.payloadBytesForTest();

        assertTrue(
                java.util.Arrays.equals(
                        supervisor,
                        NativeRamScriptRuntimeV1Rc1.supervisorBytesForTest()
                ),
                "runtime rc2 must preserve rc1 supervisor"
        );
        assertTrue(
                java.util.Arrays.equals(
                        wrapper,
                        NativeRamScriptRuntimeV1Rc1.wrapperBytesForTest(rom)
                ),
                "runtime rc2 must preserve rc1 wrapper"
        );

        assertEquals(0x0AL, NativeRamScriptRuntimeV1Rc2.payloadOffset(),
                "runtime rc2 hotkey payload entry");

        assertEquals(0xB8L, payload[0] & 0xFF, "runtime rc2 setvaddress");
        assertEquals(0x08010000L, ThumbEncodingChecks.u32(payload, 1),
                "runtime rc2 virtual base");
        assertEquals(0x69L, payload[5] & 0xFF, "runtime rc2 lockall");
        assertEquals(0xBDL, payload[6] & 0xFF, "runtime rc2 vmessage");
        assertEquals(0x0801000FL, ThumbEncodingChecks.u32(payload, 7),
                "runtime rc2 virtual text pointer");
        assertEquals(0x66L, payload[11] & 0xFF, "runtime rc2 waitmessage");
        assertEquals(0x6DL, payload[12] & 0xFF, "runtime rc2 waitbuttonpress");
        assertEquals(0x6BL, payload[13] & 0xFF, "runtime rc2 releaseall");
        assertEquals(0x02L, payload[14] & 0xFF, "runtime rc2 end");

        assertEquals(0xC2L, payload[0x0F] & 0xFF, "runtime rc2 text H");
        assertEquals(0xABL, payload[payload.length - 2] & 0xFF, "runtime rc2 text !");
        assertEquals(0xFFL, payload[payload.length - 1] & 0xFF, "runtime rc2 text EOS");
    }


    private static void testRuntimeV1Rc3StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] rc2Supervisor = NativeRamScriptRuntimeV1Rc2.supervisorBytesForTest();
        byte[] rc3Supervisor = NativeRamScriptRuntimeV1Rc3.supervisorBytesForTest();
        byte[] rc2Stage1 = NativeRamScriptRuntimeV1Rc2.stage1BytesForTest();
        byte[] rc3Stage1 = NativeRamScriptRuntimeV1Rc3.stage1BytesForTest();
        byte[] rc2Stage2 = NativeRamScriptRuntimeV1Rc2.stage2BytesForTest();
        byte[] rc3Stage2 = NativeRamScriptRuntimeV1Rc3.stage2BytesForTest();
        byte[] wrapper = NativeRamScriptRuntimeV1Rc3.wrapperBytesForTest(rom);
        byte[] gate = NativeRamScriptRuntimeV1Rc3.safetyGateBytesForTest();
        byte[] payload = NativeRamScriptRuntimeV1Rc3.payloadBytesForTest();

        // RC3 safety work must not modify the validated bridge or visual payload.
        assertTrue(java.util.Arrays.equals(rc2Supervisor, rc3Supervisor),
                "runtime rc3 must preserve rc2 supervisor");
        assertTrue(java.util.Arrays.equals(rc2Stage1, rc3Stage1),
                "runtime rc3 must preserve rc2 stage1");
        assertTrue(java.util.Arrays.equals(rc2Stage2, rc3Stage2),
                "runtime rc3 must preserve rc2 stage2");
        assertTrue(java.util.Arrays.equals(
                        NativeRamScriptRuntimeV1Rc2.payloadBytesForTest(),
                        payload),
                "runtime rc3 must preserve rc2 message payload");

        assertEquals(32, wrapper.length, "runtime rc3 wrapper size");
        assertEquals(10, gate.length, "runtime rc3 safety gate size");

        // Existing no-trigger paths still tail-call CB1_Overworld.
        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x03005316L,
                        wrapper[6] & 0xFF,
                        wrapper[7] & 0xFF
                ),
                "runtime rc3 SELECT fail path"
        );
        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x0300531AL,
                        wrapper[10] & 0xFF,
                        wrapper[11] & 0xFF
                ),
                "runtime rc3 R fail path"
        );

        // The old NOP is now a PC-relative load of the lock-state pointer.
        assertEquals(
                0x0300532CL,
                ThumbEncodingChecks.decodeLdrLiteralTarget(
                        0x0300531CL,
                        wrapper[12] & 0xFF,
                        wrapper[13] & 0xFF
                ),
                "runtime rc3 lock pointer literal"
        );

        // Trigger enters safety gate, not bridge directly.
        assertEquals(
                0x03005434L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300531EL,
                        wrapper[14] & 0xFF,
                        wrapper[15] & 0xFF
                ),
                "runtime rc3 wrapper -> safety gate"
        );

        assertEquals(0x03003118L, ThumbEncodingChecks.u32(wrapper, 0x10),
                "runtime rc3 heldKeysRaw pointer");
        assertEquals(0x08056535L, ThumbEncodingChecks.u32(wrapper, 0x18),
                "runtime rc3 CB1_Overworld pointer");
        assertEquals(0x03000F9CL, ThumbEncodingChecks.u32(wrapper, 0x1C),
                "runtime rc3 sLockFieldControls pointer");

        // Gate logic.
        assertEquals(0x7810L,
                ((gate[1] & 0xFF) << 8) | (gate[0] & 0xFF),
                "runtime rc3 ldrb r0,[r2]");
        assertEquals(0x2800L,
                ((gate[3] & 0xFF) << 8) | (gate[2] & 0xFF),
                "runtime rc3 cmp r0,#0");

        assertEquals(
                0x0300543CL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x03005438L,
                        gate[4] & 0xFF,
                        gate[5] & 0xFF
                ),
                "runtime rc3 unlocked branch"
        );

        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300543AL,
                        gate[6] & 0xFF,
                        gate[7] & 0xFF
                ),
                "runtime rc3 locked -> CB1 tail"
        );

        assertEquals(
                0x03005082L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300543CL,
                        gate[8] & 0xFF,
                        gate[9] & 0xFF
                ),
                "runtime rc3 unlocked -> validated bridge"
        );

        assertEquals(0x03005434L, NativeRamScriptRuntimeV1Rc3.safetyGateAddress(),
                "runtime rc3 gate address");
        assertEquals(0x03000F9CL, NativeRamScriptRuntimeV1Rc3.lockFieldControlsAddress(),
                "runtime rc3 lock flag address");

        // 03005434..3D uses 10 of the known 12-byte padding; do not reach 03005440.
        assertTrue(NativeRamScriptRuntimeV1Rc3.safetyGateAddress() + gate.length <= 0x03005440L,
                "runtime rc3 gate must stay before gHostRfuGameData");
    }


    private static void testRuntimeV1Rc4StaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] rc3Supervisor = NativeRamScriptRuntimeV1Rc3.supervisorBytesForTest();
        byte[] rc4Supervisor = NativeRamScriptRuntimeV1Rc4.supervisorBytesForTest();
        byte[] rc3Wrapper = NativeRamScriptRuntimeV1Rc3.wrapperBytesForTest(rom);
        byte[] rc4Wrapper = NativeRamScriptRuntimeV1Rc4.wrapperBytesForTest(rom);
        byte[] rc3Gate = NativeRamScriptRuntimeV1Rc3.safetyGateBytesForTest();
        byte[] rc4Gate = NativeRamScriptRuntimeV1Rc4.safetyGateBytesForTest();
        byte[] stage1 = NativeRamScriptRuntimeV1Rc4.stage1BytesForTest();
        byte[] stage2 = NativeRamScriptRuntimeV1Rc4.stage2BytesForTest();
        byte[] validator = NativeRamScriptRuntimeV1Rc4.formatValidatorBytesForTest();

        // RC4 preserves all validated resident trigger/safety code.
        assertTrue(java.util.Arrays.equals(rc3Supervisor, rc4Supervisor),
                "runtime rc4 must preserve rc3 supervisor");
        assertTrue(java.util.Arrays.equals(rc3Wrapper, rc4Wrapper),
                "runtime rc4 must preserve rc3 wrapper");
        assertTrue(java.util.Arrays.equals(rc3Gate, rc4Gate),
                "runtime rc4 must preserve rc3 safety gate");

        assertEquals(0x00A7L, NativeRamScriptRuntimeV1Rc4.runtimeFormatSignature(),
                "runtime rc4 format signature");
        assertEquals(0x0AL, NativeRamScriptRuntimeV1Rc4.signatureOffset(),
                "runtime rc4 signature offset");
        assertEquals(0x0CL, NativeRamScriptRuntimeV1Rc4.payloadOffset(),
                "runtime rc4 payload offset");

        // Stage1 now enters validator after NULL check.
        assertEquals(
                0x030053A8L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300508CL,
                        stage1[10] & 0xFF,
                        stage1[11] & 0xFF
                ),
                "runtime rc4 stage1 -> validator"
        );

        assertEquals(8, validator.length, "runtime rc4 validator size");
        assertEquals(0x8881L,
                ((validator[1] & 0xFF) << 8) | (validator[0] & 0xFF),
                "runtime rc4 ldrh r1,[r0,#10]");
        assertEquals(0x29A7L,
                ((validator[3] & 0xFF) << 8) | (validator[2] & 0xFF),
                "runtime rc4 cmp r1,#A7");

        assertEquals(
                0x0300508EL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x030053ACL,
                        validator[4] & 0xFF,
                        validator[5] & 0xFF
                ),
                "runtime rc4 invalid-format -> pop"
        );

        assertEquals(
                0x03005032L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x030053AEL,
                        validator[6] & 0xFF,
                        validator[7] & 0xFF
                ),
                "runtime rc4 valid-format -> stage2"
        );

        assertEquals(0x300CL,
                ((stage2[1] & 0xFF) << 8) | (stage2[0] & 0xFF),
                "runtime rc4 stage2 payload +0x0C");

        assertEquals(0x030053A8L, NativeRamScriptRuntimeV1Rc4.formatValidatorAddress(),
                "runtime rc4 validator address");
        assertEquals(0x030053B0L,
                NativeRamScriptRuntimeV1Rc4.formatValidatorAddress() + validator.length,
                "runtime rc4 validator must exactly end before gRamSaveSectorLocations");
    }


    private static void testRuntimeV1Rc4aStaticAudit() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] rc3Supervisor = NativeRamScriptRuntimeV1Rc3.supervisorBytesForTest();
        byte[] rc4aSupervisor = NativeRamScriptRuntimeV1Rc4a.supervisorBytesForTest();
        byte[] rc3Wrapper = NativeRamScriptRuntimeV1Rc3.wrapperBytesForTest(rom);
        byte[] rc4aWrapper = NativeRamScriptRuntimeV1Rc4a.wrapperBytesForTest(rom);
        byte[] stage1 = NativeRamScriptRuntimeV1Rc4a.stage1BytesForTest();
        byte[] stage2 = NativeRamScriptRuntimeV1Rc4a.stage2BytesForTest();
        byte[] gate = NativeRamScriptRuntimeV1Rc4a.safetyGateBytesForTest();
        byte[] validator = NativeRamScriptRuntimeV1Rc4a.formatValidatorBytesForTest();

        assertTrue(java.util.Arrays.equals(rc3Supervisor, rc4aSupervisor),
                "runtime rc4a must preserve rc3 supervisor");
        assertTrue(java.util.Arrays.equals(rc3Wrapper, rc4aWrapper),
                "runtime rc4a must preserve rc3 wrapper");

        assertEquals(0x00A7L, NativeRamScriptRuntimeV1Rc4a.runtimeFormatSignature(),
                "runtime rc4a format signature");
        assertEquals(0x0AL, NativeRamScriptRuntimeV1Rc4a.signatureOffset(),
                "runtime rc4a signature offset");
        assertEquals(0x0CL, NativeRamScriptRuntimeV1Rc4a.payloadOffset(),
                "runtime rc4a payload offset");

        assertEquals(
                0x030053A8L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300508CL,
                        stage1[10] & 0xFF,
                        stage1[11] & 0xFF
                ),
                "runtime rc4a stage1 -> validator"
        );

        assertEquals(8, validator.length, "runtime rc4a validator size");

        // Correct Thumb opcode: LDRH Rd,[Rb,#imm] uses imm5 scaled by 2.
        // imm=10 => imm5=5, Rb=r0, Rd=r1 => 0x8941.
        assertEquals(0x8941L,
                ((validator[1] & 0xFF) << 8) | (validator[0] & 0xFF),
                "runtime rc4a ldrh r1,[r0,#10]");

        assertEquals(0x29A7L,
                ((validator[3] & 0xFF) << 8) | (validator[2] & 0xFF),
                "runtime rc4a cmp r1,#A7");

        // Invalid format uses a NEAR conditional branch to a local pop tail.
        assertEquals(
                0x0300543EL,
                ThumbEncodingChecks.decodeConditionalBranchTarget(
                        0x030053ACL,
                        validator[4] & 0xFF,
                        validator[5] & 0xFF
                ),
                "runtime rc4a invalid-format -> local reject tail"
        );

        // Valid format uses the wider unconditional B range to stage2.
        assertEquals(
                0x03005032L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x030053AEL,
                        validator[6] & 0xFF,
                        validator[7] & 0xFF
                ),
                "runtime rc4a valid-format -> stage2"
        );

        assertEquals(12, gate.length, "runtime rc4a safety gate + reject tail size");
        assertEquals(0xBD10L,
                ((gate[11] & 0xFF) << 8) | (gate[10] & 0xFF),
                "runtime rc4a local reject pop {r4,pc}");

        // Existing safety gate branches remain unchanged.
        assertEquals(
                0x03005324L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300543AL,
                        gate[6] & 0xFF,
                        gate[7] & 0xFF
                ),
                "runtime rc4a locked safety path"
        );
        assertEquals(
                0x03005082L,
                ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                        0x0300543CL,
                        gate[8] & 0xFF,
                        gate[9] & 0xFF
                ),
                "runtime rc4a unlocked safety path"
        );

        assertEquals(0x300CL,
                ((stage2[1] & 0xFF) << 8) | (stage2[0] & 0xFF),
                "runtime rc4a stage2 payload +0x0C");

        assertEquals(0x030053B0L,
                NativeRamScriptRuntimeV1Rc4a.formatValidatorAddress() + validator.length,
                "runtime rc4a validator exact boundary");
        assertEquals(0x03005440L,
                NativeRamScriptRuntimeV1Rc4a.safetyGateAddress() + gate.length,
                "runtime rc4a gate exact boundary");
    }


    private static void testRuntimeV1MultiProfileAudit() {
        RomProfile[] profiles = {
                RomProfile.FIRE_RED_EN_10,
                RomProfile.LEAF_GREEN_EN_10,
                RomProfile.FIRE_RED_EN_11,
                RomProfile.LEAF_GREEN_EN_11
        };

        for (RomProfile rom : profiles) {
            byte[] supervisor = NativeRamScriptRuntimeV1.supervisorBytesForTest(rom);
            byte[] literals = NativeRamScriptRuntimeV1.supervisorLiteralBytesForTest(rom);
            byte[] wrapper = NativeRamScriptRuntimeV1.wrapperBytesForTest(rom);
            byte[] stage1 = NativeRamScriptRuntimeV1.stage1BytesForTest();
            byte[] stage2 = NativeRamScriptRuntimeV1.stage2BytesForTest(rom);
            byte[] validator = NativeRamScriptRuntimeV1.formatValidatorBytesForTest();

            assertEquals(14, supervisor.length, "runtime v1 supervisor size " + rom.id());
            assertEquals(12, literals.length, "runtime v1 literals size " + rom.id());
            assertEquals(32, wrapper.length, "runtime v1 wrapper size " + rom.id());

            assertEquals(0x030030F0L, ThumbEncodingChecks.u32(literals, 0),
                    "runtime v1 callback slot " + rom.id());
            assertEquals(rom.cb1OverworldThumb, ThumbEncodingChecks.u32(literals, 4),
                    "runtime v1 CB1 literal " + rom.id());
            assertEquals(0x03005311L, ThumbEncodingChecks.u32(literals, 8),
                    "runtime v1 wrapper literal " + rom.id());

            assertEquals(rom.heldKeysRaw, ThumbEncodingChecks.u32(wrapper, 0x10),
                    "runtime v1 heldKeysRaw " + rom.id());
            assertEquals(rom.cb1OverworldThumb, ThumbEncodingChecks.u32(wrapper, 0x18),
                    "runtime v1 wrapper CB1 " + rom.id());
            assertEquals(rom.lockFieldControls, ThumbEncodingChecks.u32(wrapper, 0x1C),
                    "runtime v1 lock flag " + rom.id());

            assertEquals(0x364L,
                    rom.getSavedRamScriptThumb - rom.scriptContextSetupThumb,
                    "runtime v1 function delta " + rom.id());

            assertEquals(0x8941L,
                    ((validator[1] & 0xFF) << 8) | (validator[0] & 0xFF),
                    "runtime v1 validator LDRH " + rom.id());

            assertEquals(
                    0x0300543EL,
                    ThumbEncodingChecks.decodeConditionalBranchTarget(
                            0x030053ACL,
                            validator[4] & 0xFF,
                            validator[5] & 0xFF
                    ),
                    "runtime v1 validator reject " + rom.id()
            );

            assertEquals(
                    0x03005032L,
                    ThumbEncodingChecks.decodeUnconditionalBranchTarget(
                            0x030053AEL,
                            validator[6] & 0xFF,
                            validator[7] & 0xFF
                    ),
                    "runtime v1 validator accept " + rom.id()
            );

            assertEquals(0x300CL,
                    ((stage2[1] & 0xFF) << 8) | (stage2[0] & 0xFF),
                    "runtime v1 payload add " + rom.id());

            assertEquals(
                    0x03003F98L,
                    ThumbEncodingChecks.decodeLongBranchWithLinkTarget(
                            0x03005084L,
                            stage1[2] & 0xFF,
                            stage1[3] & 0xFF,
                            stage1[4] & 0xFF,
                            stage1[5] & 0xFF
                    ),
                    "runtime v1 stage1 thunk " + rom.id()
            );
        }

        RamScript fr10 = NativeRamScriptRuntimeV1.build(RomProfile.FIRE_RED_EN_10);
        RamScript lg10 = NativeRamScriptRuntimeV1.build(RomProfile.LEAF_GREEN_EN_10);
        assertTrue(java.util.Arrays.equals(fr10.bytes(), lg10.bytes()),
                "FR10 and LG10 must build byte-identically");

        RamScript fr11 = NativeRamScriptRuntimeV1.build(RomProfile.FIRE_RED_EN_11);
        RamScript lg11 = NativeRamScriptRuntimeV1.build(RomProfile.LEAF_GREEN_EN_11);
        assertTrue(java.util.Arrays.equals(fr11.bytes(), lg11.bytes()),
                "FR11 and LG11 must build byte-identically");

        assertTrue(!java.util.Arrays.equals(fr10.bytes(), fr11.bytes()),
                "revision 1.0 and 1.1 runtime binaries must differ");
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
