import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Versioned machine-facing integration boundary for desktop/other clients.
 *
 * IMPORTANT: this deliberately lives in the toolkit's current unnamed package
 * so it can call the frozen production core without a repository-wide package
 * migration. External clients consume the JSON protocol through `Main api ...`.
 * Human CLI output is NOT part of this contract.
 */
final class ToolkitIntegrationApi {
    static final int API_VERSION = 1;

    private ToolkitIntegrationApi() {}

    static int run(String[] args) {
        try {
            if (args.length < 2) return fail("INVALID_REQUEST", "api command is required", List.of(), null);
            return switch (args[1].toLowerCase()) {
                case "version" -> version();
                case "list-content" -> listContent(args);
                case "plan-composition" -> planComposition(args);
                case "build-composition" -> buildComposition(args);
                default -> fail("UNKNOWN_API_COMMAND", "unknown api command: " + args[1], List.of(), null);
            };
        } catch (IllegalArgumentException ex) {
            return fail("INVALID_REQUEST", safeMessage(ex), List.of(), ex.getClass().getSimpleName());
        } catch (Exception ex) {
            return fail(ex instanceof java.io.IOException ? "IO_ERROR" : "INTERNAL_ERROR",
                    safeMessage(ex), List.of(), ex.getClass().getSimpleName());
        }
    }

    private static int version() {
        System.out.println("{\"apiVersion\":" + API_VERSION + ",\"protocol\":\"ramscript-tools-json\"}");
        return 0;
    }

    private static int listContent(String[] args) {
        Parsed parsed = Parsed.parse(args, 2, false, false);
        RomProfile rom = parsed.rom == null ? null : RomProfile.fromId(parsed.rom);
        String context = parsed.context == null ? "all" : parsed.context.toLowerCase();
        if (!context.equals("all") && !context.equals("hotkey-composition")) {
            return fail("INVALID_CONTEXT", "context must be all or hotkey-composition", List.of(), context);
        }

        StringBuilder out = new StringBuilder();
        out.append('{').append("\"apiVersion\":").append(API_VERSION)
                .append(",\"content\":[");
        boolean first = true;
        for (PresetDefinition preset : PresetCatalog.all()) {
            boolean composableHotkey = preset.hotkeyCapable() && !preset.exclusiveSelection();
            if (context.equals("hotkey-composition") && !composableHotkey) continue;
            if (rom != null && !preset.supports(rom)) continue;
            if (!first) out.append(',');
            first = false;
            appendContent(out, preset, rom, composableHotkey);
        }
        out.append("],\"diagnostics\":[]}");
        System.out.println(out);
        return 0;
    }

    private static void appendContent(StringBuilder out, PresetDefinition preset, RomProfile rom, boolean composableHotkey) {
        out.append('{');
        field(out, "id", preset.id()).append(',');
        field(out, "name", preset.displayName()).append(',');
        field(out, "description", preset.notes()).append(',');
        field(out, "kind", preset.exclusiveSelection() ? "DEDICATED_UTILITY" : "HOTKEY_PRESET").append(',');
        out.append("\"composable\":").append(composableHotkey).append(',');
        out.append("\"hotkeyCapable\":").append(preset.hotkeyCapable()).append(',');
        if (preset.defaultHotkey() == null) out.append("\"defaultHotkey\":null,");
        else {
            out.append("\"defaultHotkey\":{");
            field(out, "id", preset.defaultHotkey().id()).append(',');
            field(out, "displayName", preset.defaultHotkey().displayName());
            out.append("},");
        }
        out.append("\"supportedRoms\":[");
        boolean firstRom = true;
        for (RomProfile supported : RomProfile.values()) {
            if (!preset.supports(supported)) continue;
            if (!firstRom) out.append(',');
            firstRom = false;
            out.append('{'); field(out, "id", supported.id()).append(','); field(out, "name", supported.displayName()); out.append('}');
        }
        out.append("],");
        out.append("\"parameters\":[");
        boolean firstParameter = true;
        for (PresetParameterDefinition parameter : preset.parameters()) {
            if (!firstParameter) out.append(',');
            firstParameter = false;
            out.append('{');
            field(out, "id", parameter.id()).append(',');
            field(out, "type", parameter.type().name()).append(',');
            out.append("\"required\":").append(parameter.required()).append(',');
            field(out, "label", parameter.label()).append(',');
            field(out, "description", parameter.description()).append(',');
            field(out, "example", parameter.example());
            out.append('}');
        }
        out.append("],");
        out.append("\"validation\":{");
        appendValidationField(out, "deliveryman", preset, PresetUsageMode.DELIVERYMAN, rom); out.append(',');
        appendValidationField(out, "singleHotkey", preset, PresetUsageMode.SINGLE_HOTKEY, rom); out.append(',');
        appendValidationField(out, "sharedHotkey", preset, PresetUsageMode.SHARED_N_HOTKEY, rom);
        out.append("}");
        out.append('}');
    }

    private static void appendValidationField(StringBuilder out, String name, PresetDefinition preset, PresetUsageMode mode, RomProfile rom) {
        quote(out, name).append(':');
        if (rom == null) {
            out.append('[');
            boolean first = true;
            for (RomProfile candidate : RomProfile.values()) {
                if (!preset.supports(candidate)) continue;
                if (!first) out.append(',');
                first = false;
                appendValidation(out, preset, mode, candidate);
            }
            out.append(']');
        } else appendValidation(out, preset, mode, rom);
    }

    private static void appendValidation(StringBuilder out, PresetDefinition preset, PresetUsageMode mode, RomProfile rom) {
        PresetValidationStatus status = preset.validationStatus(mode, rom);
        String note = "";
        for (PresetValidationEntry entry : preset.validationMatrix()) {
            if (entry.usageMode() == mode && entry.rom() == rom) { note = entry.notes(); break; }
        }
        out.append('{'); field(out, "rom", rom.id()).append(','); field(out, "status", status.name()).append(','); field(out, "note", note); out.append('}');
    }

    private static int planComposition(String[] args) {
        Parsed parsed = Parsed.parse(args, 2, true, false);
        if (parsed.presets.isEmpty()) {
            return invalidPlan(parsed, "EMPTY_SELECTION", "select at least one preset", List.of());
        }
        try {
            validateParameters(parsed);
            RomProfile rom = RomProfile.fromId(parsed.rom);
            PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(rom, parsed.presets);
            InstallationPlan install = CompositionInstallationPlanner.plan(plan);
            printPlanJson(plan, install, parsed.presets);
            return 0;
        } catch (CompositionPlanningException ex) {
            return invalidPlan(parsed, ex.code(), safeMessage(ex), ex.affectedPresetIds(), ex.field());
        } catch (IllegalArgumentException ex) {
            return invalidPlan(parsed, "COMPOSITION_INVALID", safeMessage(ex), parsed.presets);
        }
    }

    private static int buildComposition(String[] args) {
        Parsed parsed = Parsed.parse(args, 2, true, true);
        if (parsed.presets.isEmpty()) return fail("EMPTY_SELECTION", "select at least one preset", List.of(), null);
        try {
            validateParameters(parsed);
            RomProfile rom = RomProfile.fromId(parsed.rom);
            Path input = Path.of(parsed.input);
            if (!Files.isRegularFile(input)) return fail("INPUT_FILE_NOT_FOUND", "input WC3 does not exist", List.of(), input.toString());
            if (Files.size(input) != RamScript.WC3_FILE_SIZE) {
                return fail("INVALID_INPUT_FILE", "input must be a 0x58C-byte WC3", List.of(), input.toString());
            }

            PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(rom, parsed.presets);
            InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
            InstallationEmitter.EmittedInstallation emitted = InstallationEmitter.emit(plan, parsed.hexU32OrDefault("seed", 0));
            List<Artifact> artifacts = new ArrayList<>();
            if (plan.localOnly()) {
                Path target = Path.of(parsed.output);
                emitted.persistentStages().get(0).ramScript().replaceInWc3(input, target);
                artifacts.add(new Artifact(1, "LOCAL_RUNTIME", target));
            } else {
                int order = 1;
                for (InstallationEmitter.EmittedStage stage : emitted.persistentStages()) {
                    Path target = Path.of(parsed.output + "-" + stage.name() + ".wc3");
                    stage.ramScript().replaceInWc3(input, target);
                    artifacts.add(new Artifact(order++, "INSTALL_STAGE", target));
                }
                if (emitted.runtime() != null) {
                    Path target = Path.of(parsed.output + "-runtime.wc3");
                    emitted.runtime().replaceInWc3(input, target);
                    artifacts.add(new Artifact(order, "RUNTIME", target));
                }
            }
            printBuildJson(composition, plan, artifacts);
            return 0;
        } catch (CompositionPlanningException ex) {
            return fail(ex.code(), safeMessage(ex), ex.affectedPresetIds(), ex.field());
        } catch (IllegalArgumentException ex) {
            return fail("BUILD_REJECTED", safeMessage(ex), parsed.presets, null);
        } catch (java.io.IOException ex) {
            return fail("IO_ERROR", safeMessage(ex), parsed.presets, ex.getClass().getSimpleName());
        }
    }

    private static void validateParameters(Parsed parsed) {
        Map<String, PresetParameterDefinition> allowed = new LinkedHashMap<>();
        Map<String, String> ownerByParameter = new LinkedHashMap<>();
        for (String presetId : parsed.presets) {
            PresetDefinition preset = PresetCatalog.byId(presetId);
            for (PresetParameterDefinition parameter : preset.parameters()) {
                PresetParameterDefinition previous = allowed.putIfAbsent(parameter.id(), parameter);
                if (previous != null && previous.type() != parameter.type()) {
                    throw new IllegalArgumentException("parameter metadata collision: " + parameter.id());
                }
                ownerByParameter.putIfAbsent(parameter.id(), preset.id());
                if (parameter.required() && !parsed.parameters.containsKey(parameter.id())) {
                    throw new CompositionPlanningException(
                            "MISSING_PARAMETER",
                            preset.id() + " requires parameter " + parameter.id(),
                            List.of(preset.id()),
                            parameter.id()
                    );
                }
            }
        }

        for (var entry : parsed.parameters.entrySet()) {
            PresetParameterDefinition parameter = allowed.get(entry.getKey());
            if (parameter == null) {
                throw new CompositionPlanningException(
                        "UNKNOWN_PARAMETER",
                        "parameter is not used by the selected presets: " + entry.getKey(),
                        parsed.presets,
                        entry.getKey()
                );
            }
            try {
                if (parameter.type() == PresetParameterType.HEX_U32) parameter.parseHexU32(entry.getValue());
            } catch (IllegalArgumentException ex) {
                throw new CompositionPlanningException(
                        "INVALID_PARAMETER",
                        safeMessage(ex),
                        List.of(ownerByParameter.get(entry.getKey())),
                        entry.getKey()
                );
            }
        }
    }

    private static void printPlanJson(PresetCompositionPlan plan, InstallationPlan install, List<String> requested) {
        StringBuilder out = basePlan(plan, requested);
        out.append(",\"installation\":{")
                .append("\"localOnly\":").append(install.localOnly()).append(',')
                .append("\"persistentStageCount\":").append(install.persistentStages().size()).append(',')
                .append("\"runtimeStageRequired\":").append(install.runtimeStageRequired())
                .append("},\"diagnostics\":[");
        appendStringArray(out, plan.diagnostics());
        out.append("]}");
        System.out.println(out);
    }

    private static StringBuilder basePlan(PresetCompositionPlan plan, List<String> requested) {
        StringBuilder out = new StringBuilder();
        out.append('{').append("\"apiVersion\":").append(API_VERSION).append(",\"valid\":true,");
        out.append("\"rom\":{"); field(out, "id", plan.rom().id()).append(','); field(out, "name", plan.rom().displayName()); out.append("},");
        out.append("\"requestedPresetIds\":["); appendStringArray(out, requested); out.append("],");
        out.append("\"normalizedPresetIds\":[");
        List<String> normalized = plan.selections().stream().map(s -> s.preset().id()).toList(); appendStringArray(out, normalized); out.append("],");
        out.append("\"capacity\":{");
        capacity(out, "ramscript", plan.ramScriptBytes(), RamScript.SCRIPT_SIZE, plan.ramScriptFree()).append(',');
        capacity(out, "sb1", plan.sb1Bytes(), PayloadStorageArea.SAVE_BLOCK1.capacity(), plan.sb1Free()).append(',');
        capacity(out, "sb2", plan.sb2Bytes(), PayloadStorageArea.SAVE_BLOCK2.capacity(), plan.sb2Free());
        out.append("},\"hotkeyBindings\":").append(plan.hotkeyBindings()).append(',');
        out.append("\"runtimeFamily\":"); quote(out, plan.concreteLayout().bindingPlan().runtime().toString()); out.append(',');
        out.append("\"resources\":[");
        boolean firstResource = true;
        for (PresetResourceAllocation allocation : plan.resources()) {
            if (!firstResource) out.append(',');
            firstResource = false;
            PresetOwnedResource resource = allocation.resource();
            out.append('{');
            field(out, "id", resource.id()).append(',');
            field(out, "memoryRegion", resource.memoryRegion()).append(',');
            field(out, "address", String.format("0x%08X", resource.address())).append(',');
            out.append("\"sizeBytes\":").append(resource.sizeBytes()).append(',');
            field(out, "sharing", resource.sharing().name()).append(',');
            out.append("\"ownerPresetIds\":["); appendStringArray(out, allocation.ownerPresetIds()); out.append("]}");
        }
        out.append("],");
        out.append("\"selections\":[");
        boolean first = true;
        for (PresetCompositionPlan.SelectedPresetDeployment selection : plan.selections()) {
            if (!first) out.append(','); first = false;
            out.append('{'); field(out, "id", selection.preset().id()).append(',');
            field(out, "name", selection.preset().displayName()).append(',');
            field(out, "deployment", selection.deployment().kind().name()).append(',');
            out.append("\"deploymentValidatedOnRom\":").append(selection.deployment().isValidatedOn(plan.rom()));
            ConcretePresetAllocation allocation = plan.concreteLayout().allocations().stream()
                    .filter(a -> a.presetId().equals(selection.preset().id())).findFirst().orElse(null);
            if (allocation != null && allocation.hotkeyBinding() != null) {
                out.append(',').append("\"hotkey\":{"); field(out, "id", allocation.hotkeyBinding().hotkey().id()).append(','); field(out, "displayName", allocation.hotkeyBinding().hotkey().displayName()); out.append('}');
            }
            out.append('}');
        }
        out.append(']');
        return out;
    }

    private static void printBuildJson(PresetCompositionPlan composition, InstallationPlan plan, List<Artifact> artifacts) throws java.io.IOException {
        StringBuilder out = basePlan(composition, composition.selections().stream().map(s -> s.preset().id()).toList());
        out.append(",\"installation\":{")
                .append("\"localOnly\":").append(plan.localOnly()).append(',')
                .append("\"persistentStageCount\":").append(plan.persistentStages().size()).append(',')
                .append("\"runtimeStageRequired\":").append(plan.runtimeStageRequired()).append("},");
        out.append("\"artifacts\":[");
        boolean first = true;
        for (Artifact artifact : artifacts) {
            if (!first) out.append(','); first = false;
            out.append('{').append("\"order\":").append(artifact.order).append(',');
            field(out, "role", artifact.role).append(','); field(out, "name", artifact.path.getFileName().toString()).append(',');
            field(out, "path", artifact.path.toAbsolutePath().normalize().toString()).append(',');
            out.append("\"sizeBytes\":").append(Files.size(artifact.path)).append('}');
        }
        out.append("],\"instructions\":[");
        List<String> instructions = plan.localOnly()
                ? List.of("Activate the generated WC3 in game.")
                : List.of("Activate installation stages in order and save after each stage.", "Activate the runtime WC3 after all persistent stages are installed.");
        appendStringArray(out, instructions);
        out.append("],\"diagnostics\":[]}");
        System.out.println(out);
    }

    private static int invalidPlan(Parsed parsed, String code, String message, List<String> affected) {
        return invalidPlan(parsed, code, message, affected, null);
    }

    private static int invalidPlan(Parsed parsed, String code, String message, List<String> affected, String detail) {
        StringBuilder out = new StringBuilder();
        out.append('{').append("\"apiVersion\":").append(API_VERSION).append(",\"valid\":false,");
        if (parsed.rom != null) { field(out, "romId", parsed.rom).append(','); }
        out.append("\"requestedPresetIds\":["); appendStringArray(out, parsed.presets); out.append("],\"diagnostics\":[");
        appendDiagnostic(out, code, "ERROR", message, affected, detail); out.append("]}");
        System.out.println(out);
        return 2;
    }

    private static int fail(String code, String message, List<String> affected, String detail) {
        StringBuilder out = new StringBuilder();
        out.append('{').append("\"apiVersion\":").append(API_VERSION).append(",\"ok\":false,\"diagnostics\":[");
        appendDiagnostic(out, code, "ERROR", message, affected, detail); out.append("]}");
        System.out.println(out);
        return 2;
    }

    private static void appendDiagnostic(StringBuilder out, String code, String severity, String message, List<String> affected, String detail) {
        out.append('{'); field(out, "code", code).append(','); field(out, "severity", severity).append(','); field(out, "message", message).append(',');
        out.append("\"affectedPresetIds\":["); appendStringArray(out, affected); out.append(']');
        if (detail != null) { out.append(','); field(out, "detail", detail); }
        out.append('}');
    }

    private static StringBuilder capacity(StringBuilder out, String id, int used, int cap, int free) {
        quote(out, id).append(":{\"used\":").append(used).append(",\"capacity\":").append(cap).append(",\"free\":").append(free).append('}');
        return out;
    }

    private static void appendStringArray(StringBuilder out, List<String> values) {
        boolean first = true;
        for (String value : values) { if (!first) out.append(','); first = false; quote(out, value); }
    }

    private static StringBuilder field(StringBuilder out, String name, String value) { quote(out, name).append(':'); quote(out, value == null ? "" : value); return out; }
    private static StringBuilder quote(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"');
    }

    private static String safeMessage(Throwable ex) { return ex.getMessage() == null ? ex.toString() : ex.getMessage(); }

    private record Artifact(int order, String role, Path path) {}

    private static final class Parsed {
        String rom, context, input, output;
        final List<String> presets = new ArrayList<>();
        final Map<String, String> parameters = new LinkedHashMap<>();

        static Parsed parse(String[] args, int start, boolean requireRom, boolean requireBuildPaths) {
            Parsed p = new Parsed();
            for (int i = start; i < args.length; i++) {
                switch (args[i]) {
                    case "--rom" -> p.rom = value(args, ++i, "--rom");
                    case "--context" -> p.context = value(args, ++i, "--context");
                    case "--preset" -> p.presets.add(value(args, ++i, "--preset"));
                    case "--seed" -> p.putParameter("seed", value(args, ++i, "--seed"), "--seed");
                    case "--param" -> p.putParameterEntry(value(args, ++i, "--param"));
                    case "--input" -> p.input = value(args, ++i, "--input");
                    case "--output" -> p.output = value(args, ++i, "--output");
                    default -> throw new IllegalArgumentException("unknown api option: " + args[i]);
                }
            }
            if (requireRom && (p.rom == null || p.rom.isBlank())) throw new IllegalArgumentException("--rom is required");
            if (requireBuildPaths && (p.input == null || p.output == null)) throw new IllegalArgumentException("--input and --output are required");
            return p;
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length) throw new IllegalArgumentException(option + " requires a value");
            return args[index];
        }

        private void putParameterEntry(String entry) {
            int equals = entry.indexOf('=');
            if (equals <= 0) throw new IllegalArgumentException("--param must use key=value");
            putParameter(entry.substring(0, equals), entry.substring(equals + 1), "--param");
        }

        private void putParameter(String key, String value, String source) {
            if (key == null || key.isBlank()) throw new IllegalArgumentException(source + " parameter key must not be blank");
            String previous = parameters.putIfAbsent(key, value);
            if (previous != null && !previous.equals(value)) {
                throw new IllegalArgumentException("parameter supplied more than once with different values: " + key);
            }
        }

        int hexU32OrDefault(String id, int defaultValue) {
            String raw = parameters.get(id);
            if (raw == null) return defaultValue;
            PresetParameterDefinition definition = parameterDefinition(id);
            return definition.parseHexU32(raw);
        }

        private PresetParameterDefinition parameterDefinition(String id) {
            for (String presetId : presets) {
                PresetDefinition preset = PresetCatalog.byId(presetId);
                for (PresetParameterDefinition parameter : preset.parameters()) {
                    if (parameter.id().equals(id)) return parameter;
                }
            }
            throw new IllegalArgumentException("unknown parameter: " + id);
        }
    }
}
