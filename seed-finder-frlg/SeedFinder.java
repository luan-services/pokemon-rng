import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class SeedFinder {
    // Reverse step for the Gen 3 32-bit LCRNG.
    // Equivalent to stepping one state backwards from:
    // next = 0x41C64E6D * current + 0x6073 (mod 2^32)
    private static final long REVERSE_MULTIPLIER = 0xEEB9EB65L;
    private static final long REVERSE_INCREMENT  = 0x0A3561A1L;
    private static final long MASK_32            = 0xFFFF_FFFFL;

    private record Result(int initialSeed, long advances) {}

    private static long previousSeed(long seed) {
        return (REVERSE_MULTIPLIER * seed + REVERSE_INCREMENT) & MASK_32;
    }

    private static List<Result> findInitialSeeds(long targetSeed, int maxResults) {
        List<Result> results = new ArrayList<>(maxResults);

        long seed = targetSeed & MASK_32;
        long advances = 0;

        // The LCRNG has a full 2^32-state period. We walk backwards from
        // the target until enough states in the 0x0000..0xFFFF range appear.
        while (advances < 0xFFFF_FFFFL && results.size() < maxResults) {
            seed = previousSeed(seed);
            advances++;

            if (seed <= 0xFFFFL) {
                results.add(new Result((int) seed, advances));
            }
        }

        return results;
    }

    private static long parseHexSeed(String value) {
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceFirst("^0X", "");

        if (normalized.isEmpty() || normalized.length() > 8) {
            throw new IllegalArgumentException("Seed must contain 1 to 8 hexadecimal digits.");
        }

        try {
            return Long.parseUnsignedLong(normalized, 16) & MASK_32;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hexadecimal seed: " + value);
        }
    }

    private static void writeResults(Path output, long targetSeed, List<Result> results)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write(String.format("Target seed: %08X%n", targetSeed));
            writer.write(String.format("Results: %d%n%n", results.size()));

            for (int i = 0; i < results.size(); i++) {
                Result result = results.get(i);
                writer.write(String.format(
                        "%3d. Initial Seed: %04X | Advances: %d%n",
                        i + 1,
                        result.initialSeed(),
                        result.advances()
                ));
            }

            writer.write(System.lineSeparator());
            writer.write("Seeds only:");
            writer.write(System.lineSeparator());

            for (int i = 0; i < results.size(); i++) {
                if (i > 0) {
                    writer.write(", ");
                }
                writer.write(String.format("0x%04X", results.get(i).initialSeed()));

                if ((i + 1) % 10 == 0) {
                    writer.write(System.lineSeparator());
                }
            }

            writer.write(System.lineSeparator());
        }
    }

    public static void main(String[] args) {
        try {
            String seedInput;
            int maxResults;

            if (args.length >= 2) {
                seedInput = args[0];
                maxResults = Integer.parseInt(args[1]);
            } else {
                Scanner scanner = new Scanner(System.in);

                System.out.print("Target seed (hex, e.g. 41A9CFE6): ");
                seedInput = scanner.nextLine();

                System.out.print("Max results: ");
                maxResults = Integer.parseInt(scanner.nextLine().trim());
            }

            if (maxResults <= 0) {
                throw new IllegalArgumentException("Max results must be greater than zero.");
            }

            long targetSeed = parseHexSeed(seedInput);

            System.out.printf("%nTarget: %08X%n", targetSeed);
            System.out.printf("Searching for %d initial seed(s)...%n%n", maxResults);

            long startedAt = System.nanoTime();
            List<Result> results = findInitialSeeds(targetSeed, maxResults);
            double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;

            for (int i = 0; i < results.size(); i++) {
                Result result = results.get(i);
                System.out.printf(
                        "%3d. Seed: %04X | Advances: %d%n",
                        i + 1,
                        result.initialSeed(),
                        result.advances()
                );
            }

            Path output = Path.of("seeds.txt");
            writeResults(output, targetSeed, results);

            System.out.printf(
                    "%nDone in %.3f s. Results saved to %s%n",
                    elapsedSeconds,
                    output.toAbsolutePath()
            );

            if (results.size() < maxResults) {
                System.out.println("The complete 32-bit cycle was exhausted before reaching the requested result count.");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
