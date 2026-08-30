import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/* Production metadata catalog introduced after the Build-28 runtime baseline.

   Important: this catalog is descriptive, not a placement planner. It records
   known costs/capabilities and separates "code supports this ROM" from
   "this exact deployment was validated in-game on this ROM".
*/
final class PresetCatalog {
    private static final Set<RomProfile> ALL_PROFILES = Set.copyOf(EnumSet.allOf(RomProfile.class));
    private static final Set<RomProfile> FR10_VALIDATED = Set.of(RomProfile.FIRE_RED_EN_10);

    private PresetCatalog() {}

    private static List<PresetValidationEntry> validationMatrix(
            Set<PresetUsageMode> supportedModes,
            Set<PresetUsageMode> fr10ValidatedModes
    ) {
        java.util.ArrayList<PresetValidationEntry> out = new java.util.ArrayList<>();
        for (RomProfile rom : RomProfile.values()) {
            for (PresetUsageMode mode : PresetUsageMode.values()) {
                PresetValidationStatus status;
                if (!supportedModes.contains(mode)) {
                    status = PresetValidationStatus.UNSUPPORTED;
                } else if (rom == RomProfile.FIRE_RED_EN_10 && fr10ValidatedModes.contains(mode)) {
                    status = PresetValidationStatus.VALIDATED_IN_GAME;
                } else {
                    status = PresetValidationStatus.SUPPORTED_NOT_TESTED;
                }
                out.add(new PresetValidationEntry(mode, rom, status, ""));
            }
        }
        return List.copyOf(out);
    }


    static List<PresetDefinition> all() {
        return List.of(seedModifier(), repel(), partyIvViewer(), leadIvViewer(), partyEvViewer(), leadEvViewer(), showSecretId(), tradeEvolution());
    }

    static PresetDefinition byId(String id) {
        if (id == null) throw new IllegalArgumentException("preset id must not be null");
        for (PresetDefinition definition : all()) {
            if (definition.id().equalsIgnoreCase(id)) return definition;
        }
        throw new IllegalArgumentException("Unknown preset metadata id: " + id);
    }

    static PresetDefinition seedModifier() {
        return new PresetDefinition(
                "seed-modifier",
                "Seed Modifier",
                PresetPayloadType.FIELD_SCRIPT,
                true,
                true,
                new Hotkey(HotkeyButton.R, HotkeyButton.SELECT),
                ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(),
                                Set.of(),
                                rom -> new PresetDeploymentCost(
                                        SeedModifierPreset.buildPayload(rom, 0x1234).length,
                                        0, 0, 0, 1),
                                "Direct Deliveryman execution of the same Field Script payload; supported but exact path not yet recorded in-game."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(
                                        SeedModifierPreset.buildPayload(rom, 0x1234).length,
                                        0, 0, 0, 1),
                                "Legacy/local hotkey path remains available for small compositions."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SB1_GATEWAY),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(
                                        0, PayloadPlacementPlanner.GATEWAY_SIZE,
                                        SeedModifierPreset.buildPayload(rom, 0x1234).length, 0, 1),
                                "Validated as pure Field Script in SB2 through a 10-byte SB1 gateway."
                        )
                ),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.LEGACY_MULTI_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.LEGACY_MULTI_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY)),
                "Seed value is a build parameter; the prompt is fixed-width 8-hex-digit text, so payload size is invariant across the full u32 range."
        );
    }

    static PresetDefinition repel() {
        return new PresetDefinition(
                "repel",
                "Use Repel",
                PresetPayloadType.FIELD_SCRIPT,
                true,
                true,
                new Hotkey(HotkeyButton.R, HotkeyButton.B),
                ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(),
                                Set.of(),
                                rom -> new PresetDeploymentCost(RepelHotkeyPreset.buildPayload().length, 0, 0, 0, 1),
                                "Direct Deliveryman execution of the same Field Script payload; supported but exact path not yet recorded in-game."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(RepelHotkeyPreset.buildPayload().length, 0, 0, 0, 1),
                                "Legacy/local hotkey path."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SB1_GATEWAY),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(
                                        0, PayloadPlacementPlanner.GATEWAY_SIZE,
                                        RepelHotkeyPreset.buildPayload().length, 0, 1),
                                "Validated as pure Field Script in SB2."
                        )
                ),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.LEGACY_MULTI_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.LEGACY_MULTI_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY)),
                "Pure Field Script; does not require the shared native staging service."
        );
    }

    static PresetDefinition partyIvViewer() {
        return new PresetDefinition(
                "party-iv-viewer",
                "Party IV Viewer",
                PresetPayloadType.HYBRID_NATIVE,
                true,
                true,
                new Hotkey(HotkeyButton.R, HotkeyButton.A),
                ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(PartyIvViewerPreset.payloadSize(rom), 0, 0, 0, 1),
                                "Validated deliveryman/native-helper path."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(PartyIvViewerPreset.payloadSize(rom), 0, 0, 0, 4),
                                "Standalone HotkeyRuntimeV1 path; helper is staged by the local payload."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE,
                                        PresetInfrastructure.SB1_GATEWAY,
                                        PresetInfrastructure.SB2_NATIVE_CATALOG),
                                FR10_VALIDATED,
                                rom -> {
                                    SharedPersistentNativeComposition.Layout layout =
                                            SharedPersistentNativeComposition.layout(rom, 0x1234);
                                    return new PresetDeploymentCost(
                                            0, PayloadPlacementPlanner.GATEWAY_SIZE,
                                            layout.partyBridge().length,
                                            PersistentPartyIvViewerModule.payload(rom).length,
                                            1);
                                },
                                "Build-27b/28 baseline: native body in SB2, small bridge in SB2, one shared staging service."
                        )
                ),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY)),
                "Complex native preset used to validate alignment, staging and shared-native composition."
        );
    }



    static PresetDefinition leadIvViewer() {
        return new PresetDefinition(
                "lead-iv-viewer", "Lead IV Viewer", PresetPayloadType.HYBRID_NATIVE,
                true, true, new Hotkey(HotkeyButton.R, HotkeyButton.LEFT), ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(), FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(LeadIvViewerPreset.payloadSize(rom),0,0,0,1),
                                "Compact slot-0-only IV viewer; Build 40 validated in-game on FR1.0."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                Set.of(),
                                rom -> new PresetDeploymentCost(LeadIvViewerPreset.payloadSize(rom),0,0,0,1),
                                "Standalone HotkeyRuntimeV1 path; same Lead IV payload, awaiting in-game validation."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE,
                                        PresetInfrastructure.SB1_GATEWAY,
                                        PresetInfrastructure.SB2_NATIVE_CATALOG),
                                Set.of(),
                                rom -> {
                                    byte[] bridge = PersistentNativeCallBridge.buildViaSharedStagingService(
                                            rom, PersistentLeadIvViewerModule.MODULE_ID, 0x08010000L,
                                            rom.stringVar4 + 0x140L, b -> {},
                                            b -> b.message(LeadIvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end(),
                                            b -> b.vMessage("lead_iv_bad").waitMessage().waitButtonPress().releaseAll().end()
                                                    .text("lead_iv_bad", "Persistent Lead IV module invalid.")
                                    ).fieldScript();
                                    return new PresetDeploymentCost(0, PayloadPlacementPlanner.GATEWAY_SIZE,
                                            bridge.length, PersistentLeadIvViewerModule.payload(rom).length, 1);
                                },
                                "Compact persistent native body; shares existing staging/runtime.")),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.DELIVERYMAN)),
                "Shows only party slot 0 to reduce native storage cost.");
    }

    static PresetDefinition leadEvViewer() {
        return new PresetDefinition(
                "lead-ev-viewer", "Lead EV Viewer", PresetPayloadType.HYBRID_NATIVE,
                true, true, new Hotkey(HotkeyButton.R, HotkeyButton.RIGHT), ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(), FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(LeadEvViewerPreset.payloadSize(rom),0,0,0,1),
                                "Compact slot-0-only EV viewer; Build 40 validated in-game on FR1.0."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                Set.of(),
                                rom -> new PresetDeploymentCost(LeadEvViewerPreset.payloadSize(rom),0,0,0,1),
                                "Standalone HotkeyRuntimeV1 path; same Lead EV payload, awaiting in-game validation."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE,
                                        PresetInfrastructure.SB1_GATEWAY,
                                        PresetInfrastructure.SB2_NATIVE_CATALOG),
                                Set.of(),
                                rom -> {
                                    byte[] bridge = PersistentNativeCallBridge.buildViaSharedStagingService(
                                            rom, PersistentLeadEvViewerModule.MODULE_ID, 0x08010000L,
                                            rom.stringVar4 + 0x140L, b -> {},
                                            b -> b.message(LeadEvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end(),
                                            b -> b.vMessage("lead_ev_bad").waitMessage().waitButtonPress().releaseAll().end()
                                                    .text("lead_ev_bad", "Persistent Lead EV module invalid.")
                                    ).fieldScript();
                                    return new PresetDeploymentCost(0, PayloadPlacementPlanner.GATEWAY_SIZE,
                                            bridge.length, PersistentLeadEvViewerModule.payload(rom).length, 1);
                                },
                                "Compact persistent native body; shares existing staging/runtime.")),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.DELIVERYMAN)),
                "Shows only party slot 0 to reduce native storage cost.");
    }

    static PresetDefinition partyEvViewer() {
        return new PresetDefinition(
                "party-ev-viewer", "Party EV Viewer", PresetPayloadType.HYBRID_NATIVE,
                true, true, new Hotkey(HotkeyButton.R, HotkeyButton.UP), ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(), FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(PartyEvViewerPreset.payloadSize(rom),0,0,0,1),
                                "Build-38 validated deliveryman path."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                Set.of(),
                                rom -> new PresetDeploymentCost(PartyEvViewerPreset.payloadSize(rom),0,0,0,1),
                                "Standalone HotkeyRuntimeV1 path; same validated EV payload, awaiting in-game validation."),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE,
                                        PresetInfrastructure.SB1_GATEWAY,
                                        PresetInfrastructure.SB2_NATIVE_CATALOG),
                                FR10_VALIDATED,
                                rom -> {
                                    byte[] bridge = PersistentNativeCallBridge.buildViaSharedStagingService(
                                            rom, PersistentPartyEvViewerModule.MODULE_ID, 0x08010000L,
                                            rom.stringVar4 + 0x140L, b -> {},
                                            b -> b.message(PartyEvNativeHelper.dynamicMessageAddress(rom))
                                                    .waitMessage().releaseAll().end(),
                                            b -> b.vMessage("ev_bad").waitMessage().waitButtonPress().releaseAll().end()
                                                    .text("ev_bad", "Persistent Party EV module invalid.")
                                    ).fieldScript();
                                    return new PresetDeploymentCost(
                                            0, PayloadPlacementPlanner.GATEWAY_SIZE, bridge.length,
                                            PersistentPartyEvViewerModule.payload(rom).length, 1);
                                },
                                "Uses existing shared native staging service; no new resident IWRAM.")
                ),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SHARED_N_HOTKEY)),
                "Build-38 validated EV reader. Shared deployment defaults to R+UP; Build 39 IV+EV shared composition validated in-game on FR1.0.");
    }

    static PresetDefinition showSecretId() {
        return new PresetDefinition(
                "show-secret-id",
                "Show Secret ID",
                PresetPayloadType.HYBRID_NATIVE,
                true,
                false,
                new Hotkey(HotkeyButton.R, HotkeyButton.START),
                ALL_PROFILES,
                List.of(
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.DELIVERYMAN_LOCAL,
                                PresetDeploymentDefinition.infra(),
                                FR10_VALIDATED,
                                rom -> new PresetDeploymentCost(ShowSecretIdPreset.payloadSize(rom), 0, 0, 0, 1),
                                "Simple validated deliveryman path is intentionally preserved."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.HOTKEY_LOCAL,
                                PresetDeploymentDefinition.infra(PresetInfrastructure.HOTKEY_RUNTIME),
                                Set.of(),
                                rom -> new PresetDeploymentCost(ShowSecretIdPreset.payloadSize(rom), 0, 0, 0, 1),
                                "Standalone HotkeyRuntimeV1 wrapper around the same SID payload; supported, exact path not yet recorded in-game."
                        ),
                        new PresetDeploymentDefinition(
                                PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                                PresetDeploymentDefinition.infra(
                                        PresetInfrastructure.SHARED_HOTKEY_RUNTIME,
                                        PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE,
                                        PresetInfrastructure.SB1_GATEWAY,
                                        PresetInfrastructure.SB2_NATIVE_CATALOG),
                                FR10_VALIDATED,
                                rom -> {
                                    SharedPersistentNativeComposition.Layout layout =
                                            SharedPersistentNativeComposition.layout(rom, 0x1234);
                                    return new PresetDeploymentCost(
                                            0, PayloadPlacementPlanner.GATEWAY_SIZE,
                                            layout.sidBridge().length,
                                            PersistentSecretIdModule.payload(rom).length,
                                            1);
                                },
                                "Validated with R+START in the dual-native shared-loader baseline."
                        )
                ),
                validationMatrix(
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SINGLE_HOTKEY, PresetUsageMode.SHARED_N_HOTKEY),
                        Set.of(PresetUsageMode.DELIVERYMAN, PresetUsageMode.SHARED_N_HOTKEY)),
                "Deliveryman/local remains cheaper for a standalone SID request; persistent-native becomes useful in shared compositions."
        );
    }


    static PresetDefinition tradeEvolution() {
        java.util.ArrayList<PresetValidationEntry> validation = new java.util.ArrayList<>();
        for (RomProfile rom : RomProfile.values()) {
            for (PresetUsageMode mode : PresetUsageMode.values()) {
                PresetValidationStatus status;
                String note = "";
                if (mode != PresetUsageMode.DELIVERYMAN && mode != PresetUsageMode.OBJECT_EVENT) {
                    status = PresetValidationStatus.UNSUPPORTED;
                } else if (rom == RomProfile.FIRE_RED_EN_10) {
                    status = PresetValidationStatus.VALIDATED_IN_GAME;
                    note = mode == PresetUsageMode.OBJECT_EVENT
                            ? "Object-bound production flow validated in-game on FR1.0 with Lavender Town Worker M, including cancel/no-target/National-Dex-block/success paths."
                            : "Deliveryman production flow validated in-game on FR1.0: decline, Party cancel, no-target, National Dex block and successful Kadabra trade evolution.";
                } else {
                    status = PresetValidationStatus.SUPPORTED_NOT_TESTED;
                }
                validation.add(new PresetValidationEntry(mode, rom, status, note));
            }
        }

        return new PresetDefinition(
                "trade-evolution",
                "Trade Evolution",
                PresetPayloadType.HYBRID_NATIVE,
                false,
                false,
                null,
                ALL_PROFILES,
                List.of(new PresetDeploymentDefinition(
                        PresetDeploymentKind.DEDICATED_LOCAL,
                        PresetDeploymentDefinition.infra(PresetInfrastructure.DEDICATED_IWRAM_CONTINUATION),
                        FR10_VALIDATED,
                        rom -> new PresetDeploymentCost(TradeEvolutionPreset.payloadSize(rom), 0, 0, 0, 1),
                        "Exclusive dedicated deployment for Deliveryman or existing-object binding. Temporarily owns the validated 32-byte continuation slot; no VBlank runtime."
                )),
                List.copyOf(validation),
                "UI selection policy: EXCLUSIVE. Deliveryman and existing-object usage are validated on FR1.0; FR1.1/LG1.0/LG1.1 are symbol-profiled and require in-game validation."
        );
    }

    static String report(RomProfile rom) {
        if (rom == null) throw new IllegalArgumentException("ROM profile must not be null");
        StringBuilder out = new StringBuilder();
        out.append("Preset catalog for ").append(rom.displayName()).append("\n");
        for (PresetDefinition preset : all()) {
            out.append("\n").append(preset.id()).append(" — ").append(preset.displayName()).append("\n")
                    .append("  type: ").append(preset.payloadType()).append("\n")
                    .append("  hotkeyCapable: ").append(preset.hotkeyCapable()).append("\n")
                    .append("  persistentPreferred: ").append(preset.persistentPreferred()).append("\n")
                    .append("  defaultHotkey: ").append(preset.defaultHotkey() == null ? "none" : preset.defaultHotkey().displayName()).append("\n")
                    .append("  selectionPolicy: ").append(preset.selectionPolicy()).append("\n")
                    .append("  exclusiveSelection: ").append(preset.exclusiveSelection()).append("\n");
            for (PresetDeploymentDefinition deployment : preset.deployments()) {
                PresetDeploymentCost cost = deployment.cost(rom);
                out.append("  ").append(deployment.kind())
                        .append(deployment.isValidatedOn(rom) ? " [validated]" : " [not runtime-validated]")
                        .append(" -> RAM ").append(cost.ramScriptPayloadBytes()).append(" B")
                        .append(", SB1 ").append(cost.sb1GatewayBytes()).append(" B")
                        .append(", SB2 field ").append(cost.sb2FieldScriptBytes()).append(" B")
                        .append(", SB2 native ").append(cost.sb2NativeModuleBytes()).append(" B")
                        .append(", align ").append(cost.requiredBaseAlignment())
                        .append("\n");
            }
        }
        return out.toString();
    }

    static String validationReport(RomProfile rom) {
        if (rom == null) throw new IllegalArgumentException("ROM profile must not be null");
        StringBuilder out = new StringBuilder();
        out.append("Preset validation matrix for ").append(rom.displayName()).append("\n");
        for (PresetDefinition preset : all()) {
            out.append("\n").append(preset.id()).append(" — ").append(preset.displayName()).append("\n");
            for (PresetUsageMode mode : PresetUsageMode.values()) {
                out.append("  ").append(mode).append(": ")
                        .append(preset.validationStatus(mode, rom)).append("\n");
            }
        }
        return out.toString();
    }

}
