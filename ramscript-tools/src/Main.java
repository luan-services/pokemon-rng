import java.util.Arrays;

/**
 * Canonical production CLI.
 *
 * Historical/research entrypoints are intentionally kept out of this facade.
 * They remain reproducible through: Main legacy <old-command> [...]
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "help", "--help", "-h" -> printUsage();
            case "commands" -> printCommands();
            case "presets" -> printPresets();
            case "legacy" -> runLegacy(args);

            // Inspection / import-export.
            case "inspect", "inspect-bin", "extract-bin", "inject-bin" -> LegacyMain.main(args);

            // Production preset facade and metadata/planning.
            case "preset-metadata", "preset-validation", "plan-preset",
                 "build-preset-wc3", "build-preset-object-wc3" -> LegacyMain.main(args);

            // Production cleanup utility.
            case "build-toolkit-cleaner-wc3", "toolkit-cleaner-metadata" -> LegacyMain.main(args);

            // Gift/event utilities intentionally kept outside PresetCatalog.
            case "build-gift-bin", "build-gift-wc3",
                 "build-item-gift-bin", "build-item-gift-wc3",
                 "build-repeatable-item-gift-bin", "build-repeatable-item-gift-wc3",
                 "build-clear-flag-bin", "build-clear-flag-wc3" -> LegacyMain.main(args);

            default -> {
                System.err.println("Unknown production command: " + args[0]);
                System.err.println("Use 'Main commands' for the canonical CLI or 'Main legacy <command> ...' for historical/research entrypoints.");
                System.exit(1);
            }
        }
    }

    private static void runLegacy(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -cp out Main legacy <old-command> [args ...]");
            System.exit(1);
        }
        LegacyMain.main(Arrays.copyOfRange(args, 1, args.length));
    }

    private static void printCommands() {
        System.out.println("Canonical commands:");
        System.out.println("  commands                         Show this production command list");
        System.out.println("  presets                          List production PresetCatalog entries");
        System.out.println("  preset-metadata <rom>            Show preset deployment/cost metadata");
        System.out.println("  preset-validation <rom>          Show validation matrix");
        System.out.println("  plan-preset ...                  Plan deliveryman/hotkey composition");
        System.out.println("  build-preset-wc3 ...             Build production deliveryman/hotkey preset(s)");
        System.out.println("  build-preset-object-wc3 ...      Build production preset(s) on a named object host");
        System.out.println("  build-toolkit-cleaner-wc3 ...    Build the production Toolkit Cleaner");
        System.out.println("  toolkit-cleaner-metadata         Show Toolkit Cleaner metadata");
        System.out.println("  build-gift-bin / build-gift-wc3  Build named gift/event references");
        System.out.println("  build-item-gift-*                Build one-time item gift utility");
        System.out.println("  build-repeatable-item-gift-*     Build repeatable item gift utility");
        System.out.println("  build-clear-flag-*               Build clear-flag utility");
        System.out.println("  inspect / inspect-bin            Inspect WC3/RamScript data");
        System.out.println("  extract-bin / inject-bin         Import/export RamScript binary");
        System.out.println();
        System.out.println("Historical/research commands:");
        System.out.println("  legacy <old-command> [args ...]  Run an old entrypoint explicitly");
    }

    private static void printPresets() {
        System.out.println("Production presets:");
        for (PresetDefinition preset : PresetCatalog.all()) {
            String hotkey = preset.defaultHotkey() == null ? "none" : preset.defaultHotkey().displayName();
            System.out.printf("  %-24s %-22s default hotkey: %s%n", preset.id(), preset.displayName(), hotkey);
        }
        System.out.println();
        System.out.println("Use 'preset-metadata <rom>' for costs/deployments and 'preset-validation <rom>' for validation status.");
        System.out.println("Toolkit Cleaner is a production utility, not a selectable preset:");
        System.out.println("  build-toolkit-cleaner-wc3 <rom> <input.wc3> <output.wc3>");
    }

    private static void printUsage() {
        System.out.println("ramscript-tools — production CLI");
        System.out.println();
        System.out.println("Start here:");
        System.out.println("  java -cp out Main commands");
        System.out.println("  java -cp out Main presets");
        System.out.println();
        System.out.println("Hotkey presets:");
        System.out.println("  java -cp out Main build-preset-wc3 <rom> hotkey <input.wc3> <output-or-prefix> <preset-id> [preset-id ...] [--seed <hex>]");
        System.out.println();
        System.out.println("Clear flag:");
        System.out.println("  java -cp out Main build-clear-flag-wc3 <input.wc3> <output.wc3> <flag> [--message \"...\"]");
        System.out.println();
        System.out.println("Toolkit Cleaner:");
        System.out.println("  java -cp out Main build-toolkit-cleaner-wc3 <rom> <input.wc3> <output.wc3>");
        System.out.println();
        System.out.println("Historical/research compatibility:");
        System.out.println("  java -cp out Main legacy <old-command> [args ...]");
    }
}
