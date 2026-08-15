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
        testDispatcherCandidate1();
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

    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder result = new StringBuilder();

        for (byte value : hash) {
            result.append(String.format("%02x", Byte.toUnsignedInt(value)));
        }

        return result.toString();
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
