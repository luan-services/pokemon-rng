import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Materializes the bytes described by a PresetCompositionPlan.
   Placement policy stays in the planners; this class only turns an already
   chosen concrete layout into payload/gateway/catalog/runtime bytes. */
final class CompositionArtifactBuilder {
    private static final long VIRTUAL_BASE = HotkeyRuntimeV1.VIRTUAL_BASE;
    private static final long STATIC_SB1 = 0x0202552CL;
    private static final long STATIC_SB2 = 0x02024588L;
    private static final int RAMSCRIPT_OFFSET_IN_SB1 = 0x3624;
    private static final int VAR_RESULT = 0x800D;
    private static final int NATIVE_STAGING_CAPACITY = 0x140;

    private CompositionArtifactBuilder() {}

    static Build build(PresetCompositionPlan plan, int seed) {
        if (plan == null) throw new IllegalArgumentException("plan must not be null");
        ConcreteCompositionLayout layout = plan.concreteLayout();
        Map<String, byte[]> components = new HashMap<>();

        boolean persistentInstall = plan.selections().stream().anyMatch(item ->
                item.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT
                        || item.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE);
        if (persistentInstall) components.put("installation-manifest", InstallationManifest.build(plan));

        if (layout.hasNativeCatalog()) {
            components.put("native-catalog", buildNativeCatalog(plan));
        }

        int serviceOffset = -1;
        if (plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)) {
            serviceOffset = SharedPersistentNativeStagingService.offsetForBindings(plan.hotkeyBindings(), 4);
        }

        for (ConcretePresetAllocation allocation : layout.allocations()) {
            if (allocation.hasGateway()) {
                components.put(allocation.presetId() + ":gateway",
                        gatewayFor(allocation.sb2FieldScriptOffset(), allocation.sb1GatewayOffset()));
            }
            if (allocation.hasSb2FieldScript()) {
                components.put(allocation.presetId() + ":field",
                        buildPersistentField(plan.rom(), allocation, serviceOffset, seed));
            }
        }

        TriggerBuildResult runtime = null;
        if (plan.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            runtime = buildSharedRuntime(plan, serviceOffset);
        }

        return new Build(Map.copyOf(components), runtime);
    }

    static TriggerBuildResult buildObjectBoundSharedRuntime(
            PresetCompositionPlan plan,
            ObjectEventTarget target
    ) {
        if (plan == null || target == null) throw new IllegalArgumentException("plan/target must not be null");
        if (!plan.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME)) {
            throw new IllegalArgumentException("composition does not use SharedHotkeyRuntime");
        }
        int serviceOffset = plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)
                ? SharedPersistentNativeStagingService.offsetForBindings(plan.hotkeyBindings(), 4)
                : -1;
        return buildSharedRuntime(plan, serviceOffset, target);
    }

    static RamScript buildLocal(PresetCompositionPlan plan, int seed) {
        if (plan.selections().size() != 1) throw new IllegalArgumentException("local emission currently supports one preset");
        var item = plan.selections().get(0);
        return switch (item.deployment().kind()) {
            case DELIVERYMAN_LOCAL -> switch (item.preset().id()) {
                case "party-iv-viewer" -> PartyIvViewerPreset.buildDeliveryman(plan.rom()).ramScript();
                case "show-secret-id" -> RamScript.createWonderCard(ShowSecretIdPreset.buildScript(plan.rom()));
                case "party-ev-viewer" -> PartyEvViewerPreset.buildDeliveryman(plan.rom()).ramScript();
                case "lead-iv-viewer" -> LeadIvViewerPreset.buildDeliveryman(plan.rom()).ramScript();
                case "lead-ev-viewer" -> LeadEvViewerPreset.buildDeliveryman(plan.rom()).ramScript();
                default -> throw unsupported(item.preset().id(), item.deployment().kind());
            };
            case HOTKEY_LOCAL -> switch (item.preset().id()) {
                case "seed-modifier" -> HotkeyRuntimeV1.build(plan.rom(), SeedModifierPreset.buildPayload(plan.rom(), seed));
                case "seed-modifier-box14" -> HotkeyRuntimeV1.build(plan.rom(), Box14SeedModifierPreset.buildPayload(plan.rom()));
                case "repel" -> HotkeyRuntimeV1.build(plan.rom(), RepelHotkeyPreset.buildPayload());
                case "party-iv-viewer" -> HotkeyRuntimeV1.build(plan.rom(), PartyIvViewerPreset.buildPayload(plan.rom()));
                case "party-ev-viewer" -> HotkeyRuntimeV1.build(plan.rom(), PartyEvViewerPreset.buildPayload(plan.rom()));
                case "lead-iv-viewer" -> HotkeyRuntimeV1.build(plan.rom(), LeadIvViewerPreset.buildPayload(plan.rom()));
                case "lead-ev-viewer" -> HotkeyRuntimeV1.build(plan.rom(), LeadEvViewerPreset.buildPayload(plan.rom()));
                default -> throw unsupported(item.preset().id(), item.deployment().kind());
            };
            default -> throw new IllegalArgumentException("composition is not local-only");
        };
    }

    private static byte[] buildNativeCatalog(PresetCompositionPlan plan) {
        List<PersistentNativeModuleSpec> modules = new ArrayList<>();
        for (var item : plan.selections()) {
            if (item.deployment().kind() != PresetDeploymentKind.SHARED_PERSISTENT_NATIVE) continue;
            modules.add(switch (item.preset().id()) {
                case "party-iv-viewer" -> new PersistentNativeModuleSpec(
                        PersistentPartyIvViewerModule.MODULE_ID, PersistentPartyIvViewerModule.payload(plan.rom()));
                case "show-secret-id" -> new PersistentNativeModuleSpec(
                        PersistentSecretIdModule.MODULE_ID, PersistentSecretIdModule.payload(plan.rom()));
                case "party-ev-viewer" -> new PersistentNativeModuleSpec(
                        PersistentPartyEvViewerModule.MODULE_ID, PersistentPartyEvViewerModule.payload(plan.rom()));
                case "lead-iv-viewer" -> new PersistentNativeModuleSpec(
                        PersistentLeadIvViewerModule.MODULE_ID, PersistentLeadIvViewerModule.payload(plan.rom()));
                case "lead-ev-viewer" -> new PersistentNativeModuleSpec(
                        PersistentLeadEvViewerModule.MODULE_ID, PersistentLeadEvViewerModule.payload(plan.rom()));
                case "seed-modifier-box14" -> new PersistentNativeModuleSpec(
                        PersistentBox14SeedModule.MODULE_ID, PersistentBox14SeedModule.payload(plan.rom()));
                default -> throw unsupported(item.preset().id(), item.deployment().kind());
            });
        }
        return PersistentNativeModuleCatalog.build(plan.concreteLayout().nativeCatalogOffset(), modules).bytes();
    }

    private static byte[] buildPersistentField(RomProfile rom, ConcretePresetAllocation allocation, int serviceOffset, int seed) {
        return switch (allocation.presetId()) {
            case "seed-modifier" -> SeedModifierPreset.buildPayload(rom, seed);
            case "seed-modifier-box14" -> {
                requireService(serviceOffset, allocation.presetId());
                long target = virtualTargetFromSb2ToRamScript(allocation.sb2FieldScriptOffset(), serviceOffset);
                yield PersistentBox14SeedBridge.build(rom, target);
            }
            case "repel" -> RepelHotkeyPreset.buildPayload();
            case "party-iv-viewer" -> {
                requireService(serviceOffset, allocation.presetId());
                long target = virtualTargetFromSb2ToRamScript(allocation.sb2FieldScriptOffset(), serviceOffset);
                yield PersistentNativeCallBridge.buildViaSharedStagingService(
                        rom, PersistentPartyIvViewerModule.MODULE_ID, target,
                        rom.stringVar4 + 0x140L, b -> {},
                        b -> b.message(PartyMonDataNativeHelper.dynamicMessageAddress(rom))
                                .waitMessage().waitButtonPressStrict().releaseAll().end(),
                        b -> b.vMessage("party_bad").waitMessage().waitButtonPress().releaseAll().end()
                                .text("party_bad", "Persistent Party IV module invalid.")
                ).fieldScript();
            }

            case "lead-iv-viewer" -> {
                requireService(serviceOffset, allocation.presetId());
                long target = virtualTargetFromSb2ToRamScript(allocation.sb2FieldScriptOffset(), serviceOffset);
                yield PersistentNativeCallBridge.buildViaSharedStagingService(
                        rom, PersistentLeadIvViewerModule.MODULE_ID, target, rom.stringVar4 + 0x140L, b -> {},
                        b -> b.message(LeadIvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end(),
                        b -> b.vMessage("lead_iv_bad").waitMessage().waitButtonPress().releaseAll().end()
                                .text("lead_iv_bad", "Persistent Lead IV module invalid.")
                ).fieldScript();
            }
            case "lead-ev-viewer" -> {
                requireService(serviceOffset, allocation.presetId());
                long target = virtualTargetFromSb2ToRamScript(allocation.sb2FieldScriptOffset(), serviceOffset);
                yield PersistentNativeCallBridge.buildViaSharedStagingService(
                        rom, PersistentLeadEvViewerModule.MODULE_ID, target, rom.stringVar4 + 0x140L, b -> {},
                        b -> b.message(LeadEvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end(),
                        b -> b.vMessage("lead_ev_bad").waitMessage().waitButtonPress().releaseAll().end()
                                .text("lead_ev_bad", "Persistent Lead EV module invalid.")
                ).fieldScript();
            }
            case "party-ev-viewer" -> {
                requireService(serviceOffset, allocation.presetId());
                long target=virtualTargetFromSb2ToRamScript(allocation.sb2FieldScriptOffset(),serviceOffset);
                yield PersistentNativeCallBridge.buildViaSharedStagingService(
                        rom,PersistentPartyEvViewerModule.MODULE_ID,target,rom.stringVar4+0x140L,b->{},
                        b->b.message(PartyEvNativeHelper.dynamicMessageAddress(rom)).waitMessage().releaseAll().end(),
                        b->b.vMessage("ev_bad").waitMessage().waitButtonPress().releaseAll().end()
                                .text("ev_bad","Persistent Party EV module invalid.")
                ).fieldScript();
            }
            case "show-secret-id" -> {
                requireService(serviceOffset, allocation.presetId());
                long target = virtualTargetFromSb2ToRamScript(allocation.sb2FieldScriptOffset(), serviceOffset);
                yield PersistentNativeCallBridge.buildViaSharedStagingService(
                        rom, PersistentSecretIdModule.MODULE_ID, target,
                        rom.stringVar4 + 0x140L, b -> {},
                        b -> b.bufferNumberString(0, VAR_RESULT)
                                .vMessage("sid_msg").waitMessage().waitButtonPressStrict().releaseAll().end()
                                .text("sid_msg", "Your Secret ID is {STR_VAR_1}."),
                        b -> b.vMessage("sid_bad").waitMessage().waitButtonPressStrict().releaseAll().end()
                                .text("sid_bad", "Persistent SID module invalid.")
                ).fieldScript();
            }
            default -> throw new IllegalArgumentException("no persistent field materializer for " + allocation.presetId());
        };
    }

    private static TriggerBuildResult buildSharedRuntime(PresetCompositionPlan plan, int serviceOffset) {
        return buildSharedRuntime(plan, serviceOffset, null);
    }

    private static TriggerBuildResult buildSharedRuntime(
            PresetCompositionPlan plan,
            int serviceOffset,
            ObjectEventTarget objectTarget
    ) {
        List<SharedHotkeyDispatcher.Entry> entries = new ArrayList<>();
        HotkeyButton modifier = null;
        Map<String, ConcretePresetAllocation> byId = new HashMap<>();
        for (ConcretePresetAllocation allocation : plan.concreteLayout().allocations()) byId.put(allocation.presetId(), allocation);
        for (HotkeyBinding binding : plan.concreteLayout().bindingPlan().bindings()) {
            ConcretePresetAllocation allocation = byId.get(binding.presetId());
            if (allocation == null || !allocation.hasGateway()) throw new IllegalArgumentException("shared binding has no gateway: " + binding.presetId());
            if (modifier == null) modifier = binding.hotkey().heldButton();
            entries.add(new SharedHotkeyDispatcher.Entry(binding.hotkey().pressedButton(), gatewayDelta(allocation.sb1GatewayOffset())));
        }
        if (modifier == null) throw new IllegalArgumentException("shared runtime has no bindings");

        byte[] service = new byte[0];
        int alignment = 1;
        if (plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE)) {
            SharedPersistentNativeStagingService.Build build = SharedPersistentNativeStagingService.build(
                    plan.rom(), serviceOffset, plan.rom().stringVar4 + 0x140L, NATIVE_STAGING_CAPACITY);
            service = build.fieldScript();
            alignment = build.requiredBaseAlignment();
        }
        if (objectTarget == null) {
            return SharedHotkeyRuntime.compose(plan.rom(), modifier, entries, service, alignment);
        }
        return EarlyObjectBoundSharedHotkeyInstaller.compose(
                plan.rom(), modifier, entries, service, alignment, objectTarget
        );
    }

    private static byte[] gatewayFor(int sb2TargetOffset, int gatewayOffset) {
        if (sb2TargetOffset < 0) throw new IllegalArgumentException("gateway target missing");
        long entryStatic = STATIC_SB1 + gatewayOffset;
        long targetStatic = STATIC_SB2 + sb2TargetOffset;
        long delta = targetStatic - entryStatic;
        long virtualTarget = (VIRTUAL_BASE + delta) & 0xFFFF_FFFFL;
        byte[] out = new byte[10];
        out[0] = (byte) 0xB8;
        putU32(out, 1, VIRTUAL_BASE);
        out[5] = (byte) 0xB9;
        putU32(out, 6, virtualTarget);
        return out;
    }

    private static long virtualTargetFromSb2ToRamScript(int sb2CallerOffset, int ramScriptTargetOffset) {
        long callerStatic = STATIC_SB2 + sb2CallerOffset;
        long targetStatic = STATIC_SB1 + RAMSCRIPT_OFFSET_IN_SB1 + ramScriptTargetOffset;
        return (VIRTUAL_BASE + (targetStatic - callerStatic)) & 0xFFFF_FFFFL;
    }

    private static int gatewayDelta(int gatewayOffset) { return gatewayOffset - RAMSCRIPT_OFFSET_IN_SB1; }
    private static void requireService(int serviceOffset, String id) { if (serviceOffset < 0) throw new IllegalArgumentException(id + " requires shared native staging service"); }
    private static IllegalArgumentException unsupported(String id, PresetDeploymentKind kind) { return new IllegalArgumentException("no artifact materializer for " + id + " / " + kind); }
    private static void putU32(byte[] b, int o, long v) { b[o]=(byte)v; b[o+1]=(byte)(v>>>8); b[o+2]=(byte)(v>>>16); b[o+3]=(byte)(v>>>24); }

    record Build(Map<String, byte[]> components, TriggerBuildResult runtime) {
        Build {
            Map<String, byte[]> copy = new HashMap<>();
            for (var e : components.entrySet()) copy.put(e.getKey(), e.getValue().clone());
            components = Map.copyOf(copy);
        }
        byte[] component(String id) {
            byte[] data = components.get(id);
            if (data == null) throw new IllegalArgumentException("missing component bytes: " + id);
            return data.clone();
        }
    }
}
