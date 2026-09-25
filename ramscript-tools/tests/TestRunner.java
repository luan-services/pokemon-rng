import java.util.List;

public final class TestRunner {
    private static int tests;

    public static void main(String[] args) {
        testValidatedBaselineRuntimeSize();
        testRealPresetPlacementPlan();
        testHotkeyDoesNotForcePersistence();
        testAutoFallsBackToPersistentPair();
        testExplicitLocalDoesNotSilentlyFallback();
        testPersistentRequiresBothResources();
        testGatewayReachabilityLimit();
        testNativePersistentIsNotSilentlyPlanned();
        testSingleBindingUsesValidatedSingleRuntime();
        testCompatiblePairUsesValidatedMultiRuntime();
        testDuplicateBindingIsRejected();
        testIncompatiblePairFallsBackToSharedRuntime();
        testThreeBindingsUseSharedRuntime();
        testBindingComposerKeepsPersistentGatewayTiny();
        testSharedDispatcherSizesAndReset();
        testSharedWrapperFitsExact32Bytes();
        testSharedCandidateBuildsEightBindings();
        testSharedCandidateRejectsBadModifiersAndDuplicates();
        testRealFourPresetSharedPlan();
        testSharedNativeSmokeLayout();
        testSharedNativeSmokeInstallersBuild();
        testSharedNativeSmokeRuntimeUsesSharedCore();
        testPersistentNativeModuleCatalogGeneric();
        testPersistentNativeCallBridgeGeneric();
        testPersistentPartyIvModule();
        testProductionNativeHelpersMatchValidatedLegacyBytes();
        testCpuSetPersistentBridgeReportsWordAlignment();
        testSharedPartyIvSmokeLayout();
        testSharedPartyIvSmokeBuilds();
        testStagedDispatcherPreservesEntryUntilChecksumLoaded();
        testPartyIvDirectCallDiagnosticBuilds();
        testPartyIvProductionBridgeUsesStockCallnative();
        testSharedPersistentNativeStagingService();
        testPartyIvBridgeTargetsSharedStagingService();
        testPersistentSecretIdModule();
        testDualNativeSharedLayout();
        testDualNativeSharedBuilds();
        testDualNativeBridgesShareOneService();
        testDualNativeSidUsesStrictWait();
        testDualNativeSidRestoresRelocationFromCurrentPosition();
        testPresetCatalogDefinesProductionPresets();
        testPresetCatalogSeparatesSharedInfrastructureFromPerPresetCosts();
        testPresetCatalogPreservesSimpleSidDeliverymanPath();
        testPresetCatalogDoesNotOverclaimRomValidation();
        testValidationMatrixSeparatesUsageFromDeployment();
        testValidationMatrixTracksRomVersionsIndependently();
        testValidationMatrixRecordsSharedBaseline();
        testValidationMatrixRecordsLegacyPair();
        testValidationMatrixPreservesSimplePartyIvPaths();
        testValidationMatrixSupportsGeneralizedPresetTriggers();
        testGeneralizedPresetTriggersReuseFunctionalPayloads();
        testSeedModifierAcceptsFullU32State();
        testSeedModifierClosesLinkBeforePrompt();
        testSeedModifierU32SharedLayoutDoesNotOverlap();
        testBox14SeedSharedIntegration();
        testMuteMusicProductionLayout();
        testMuteMusicCatalogAndComposition();
        testRunAnywhereHelperLayout();
        testRunAnywhereProfilesUseSameMapHeader();
        testRunAnywhereProbeAvoidsIwramStaging();
        testRunAnywhereSharedMaintenanceRoutine();
        testRunAnywhereSharedRuntimeFitsBaseline();
        testRunAnywhereSharedRuntimeUsesNoAdditionalIwramBlock();
        testCompositionPlannerChoosesSimpleSidPath();
        testCompositionPlannerChoosesLocalPartyIvWhenAlone();
        testCompositionPlannerSharesPureFieldRuntime();
        testCompositionPlannerMatchesValidatedFourPresetBaseline();
        testCompositionPlannerPaysNativeServiceOnce();
        testConcretePlannerMatchesValidatedBindingsAndOffsets();
        testConcretePlannerOmitsSharedLoaderForPureFieldScripts();
        testInstallationPlannerKeepsLocalSidSingleWc();
        testInstallationPlannerSplitsValidatedBaselineAutomatically();
        testInstallationPlannerChunksLargeCatalogWrites();
        testInstallationPlannerOmitsRuntimeServiceForPureLocalPlan();
        testInstallationEmitterMatchesValidatedBaselineArtifacts();
        testInstallationEmitterBuildsPlannedTwoStageSequence();
        testPlannedPersistentImageMatchesValidatedBaseline();
        testInstallationManifestIsTaggedAndChecksummed();
        testPersistentPlannerReservesManifestTail();
        testCleanerBuildsAndFits();
        testPartyEvSharedIntegration();
        testLeadViewerCompactSizes();
        ObjectEventBindingTest.run();
        TradeEvolutionPresetProductionTest.run();
        MewBossProbeTest.run();
        PersistentEventPackageProbeTest.run();
        PersistentEventRebindingProbeTest.run();
        PersistentEventMewHostProbeTest.run();
        PersistentEventMewMoveProbeTest.run();
        PersistentEventMewRecoveryProbeTest.run();

        System.out.println("All tests passed: " + tests + " (+ TradeEvolutionPresetProductionTest + MewBossProbeTest + PersistentEventPackageProbeTest + PersistentEventRebindingProbeTest)");
    }

    private static void testValidatedBaselineRuntimeSize() {
        TriggerBuildResult runtime = PersistentFieldScriptGatewayRuntime.buildRuntime(RomProfile.fromId("fr10"));
        check(runtime.totalScriptBytes() == 395, "Build 15a runtime must remain 395 bytes");
    }

    private static void testRealPresetPlacementPlan() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] showSid = ShowSecretIdPreset.buildScript(rom);
        byte[] seed = SeedModifierPreset.buildPayload(rom, 0x1234);
        byte[] repel = RepelHotkeyPreset.buildPayload();

        PlacementPlan plan = PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("show-sid", showSid, PresetPlacementPreference.LOCAL, false),
                PresetRequest.fieldScript("seed-modifier", seed, PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("repel", repel, PresetPlacementPreference.PERSISTENT, true)
        ), 600);

        check(plan.ramScriptAllocations().size() == 1, "one local allocation expected");
        check(plan.persistentAllocations().size() == 2, "two persistent allocations expected");

        RamScriptAllocation local = plan.ramScriptAllocations().get(0);
        check(local.offset() == 395, "local content must start after the 395-byte runtime package");
        check(local.size() == showSid.length, "Show SID size mismatch");

        PersistentFieldScriptAllocation seedAllocation = plan.persistentAllocations().get(0);
        PersistentFieldScriptAllocation repelAllocation = plan.persistentAllocations().get(1);
        check(seedAllocation.sb1GatewayOffset() == 0x3612, "first gateway must reuse validated 0x3612 slot");
        check(repelAllocation.sb1GatewayOffset() == 0x3608, "second gateway must reuse validated 0x3608 slot");
        check(seedAllocation.sb2PayloadOffset() == 0x0B20, "first SB2 payload must start at validated storage base");
        check(repelAllocation.sb2PayloadOffset() == 0x0B20 + seed.length, "SB2 payloads should be compact");

        check(plan.diagnostics().ramScriptFree() == 600 - showSid.length, "RamScript diagnostic mismatch");
        check(plan.diagnostics().sb2PayloadBytesFree() == 1024 - seed.length - repel.length, "SB2 diagnostic mismatch");
        check(PayloadPlacementPlanner.buildGateway(seedAllocation).length == 10, "gateway must remain 10 bytes");
    }

    private static void testHotkeyDoesNotForcePersistence() {
        PlacementPlan plan = PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("small-hotkey", bytes(32), PresetPlacementPreference.AUTO, true)
        ), 600);
        check(plan.ramScriptAllocations().size() == 1, "hotkey metadata must not force persistence");
        check(plan.persistentAllocations().isEmpty(), "hotkey metadata must not force an SB1/SB2 allocation");
    }

    private static void testAutoFallsBackToPersistentPair() {
        PlacementPlan plan = PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("large", bytes(601), PresetPlacementPreference.AUTO, true)
        ), 600);
        check(plan.ramScriptAllocations().isEmpty(), "601-byte AUTO payload cannot be local with 600 bytes free");
        check(plan.persistentAllocations().size() == 1, "AUTO must fall back to persistent pair");
        check(plan.persistentAllocations().get(0).sb2PayloadOffset() == 0x0B20, "persistent body must start in SB2");
    }

    private static void testExplicitLocalDoesNotSilentlyFallback() {
        expectFailure(() -> PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("too-large-local", bytes(601), PresetPlacementPreference.LOCAL, false)
        ), 600), "explicit local request must fail instead of silently becoming persistent");
    }

    private static void testPersistentRequiresBothResources() {
        expectFailure(() -> PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("too-large-persistent", bytes(1025), PresetPlacementPreference.PERSISTENT, true)
        ), 600), "persistent placement must fail when SB2 body does not fit");
    }

    private static void testGatewayReachabilityLimit() {
        check(PayloadPlacementPlanner.reachableGatewaySlots() == 24,
                "current u8-distance runtime should expose 24 reachable 10-byte gateway slots");

        var requests = new java.util.ArrayList<PresetRequest>();
        for (int i = 0; i < 25; i++) {
            requests.add(PresetRequest.fieldScript("p" + i, bytes(1), PresetPlacementPreference.PERSISTENT, true));
        }
        expectFailure(() -> PayloadPlacementPlanner.plan(requests, 600),
                "planner must not allocate an unreachable 25th gateway");
    }

    private static void testNativePersistentIsNotSilentlyPlanned() {
        PresetRequest nativeRequest = new PresetRequest(
                "native", PresetPayloadKind.THUMB, bytes(20), PresetPlacementPreference.PERSISTENT, true
        );
        expectFailure(() -> PayloadPlacementPlanner.plan(List.of(nativeRequest), 600),
                "native persistent placement must remain a separate unresolved deployment class");
    }

    private static void testSingleBindingUsesValidatedSingleRuntime() {
        HotkeyBindingPlan plan = HotkeyBindingAllocator.plan(List.of(
                new HotkeyBinding("seed", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT))
        ));
        check(plan.runtime() == HotkeyBindingRuntime.SINGLE_HOTKEY_V1,
                "one binding must preserve validated HotkeyRuntimeV1");
    }

    private static void testCompatiblePairUsesValidatedMultiRuntime() {
        HotkeyBindingPlan plan = HotkeyBindingAllocator.plan(List.of(
                new HotkeyBinding("seed", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT)),
                new HotkeyBinding("repel", new Hotkey(HotkeyButton.R, HotkeyButton.B))
        ));
        check(plan.runtime() == HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "two compatible bindings must now select SharedHotkeyRuntime; MultiHotkeyRuntimeV1 is archived");
    }

    private static void testDuplicateBindingIsRejected() {
        expectFailure(() -> HotkeyBindingAllocator.plan(List.of(
                new HotkeyBinding("seed", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT)),
                new HotkeyBinding("repel", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT))
        )), "duplicate chord must be rejected");
    }

    private static void testIncompatiblePairFallsBackToSharedRuntime() {
        HotkeyBindingPlan plan = HotkeyBindingAllocator.plan(List.of(
                new HotkeyBinding("seed", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT)),
                new HotkeyBinding("sid", new Hotkey(HotkeyButton.R, HotkeyButton.A))
        ));
        check(plan.runtime() == HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "non-adjacent pair should use validated SharedHotkeyRuntime");
    }

    private static void testThreeBindingsUseSharedRuntime() {
        HotkeyBindingPlan plan = HotkeyBindingAllocator.plan(List.of(
                new HotkeyBinding("repel", new Hotkey(HotkeyButton.R, HotkeyButton.B)),
                new HotkeyBinding("seed", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT)),
                new HotkeyBinding("sid", new Hotkey(HotkeyButton.R, HotkeyButton.A))
        ));
        check(plan.runtime() == HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "three bindings should use validated SharedHotkeyRuntime");
    }

    private static void testBindingComposerKeepsPersistentGatewayTiny() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] seed = SeedModifierPreset.buildPayload(rom, 0x1234);
        byte[] repel = RepelHotkeyPreset.buildPayload();
        PlacementPlan placement = PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("seed", seed, PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("repel", repel, PresetPlacementPreference.PERSISTENT, true)
        ), 600);

        PersistentFieldScriptAllocation seedAllocation = placement.persistentAllocations().get(0);
        PersistentFieldScriptAllocation repelAllocation = placement.persistentAllocations().get(1);
        byte[] seedGateway = PayloadPlacementPlanner.buildGateway(seedAllocation);
        byte[] repelGateway = PayloadPlacementPlanner.buildGateway(repelAllocation);

        HotkeyBindingPlan runtimePlan = HotkeyBindingAllocator.plan(List.of(
                new HotkeyBinding("seed", new Hotkey(HotkeyButton.R, HotkeyButton.SELECT)),
                new HotkeyBinding("repel", new Hotkey(HotkeyButton.R, HotkeyButton.B))
        ));

        check(seedGateway.length == 10 && repelGateway.length == 10,
                "persistent bindings should route through 10-byte gateways");
        check(runtimePlan.runtime() == HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "new two-binding compositions should route through SharedHotkeyRuntime");
        check(seedGateway.length + repelGateway.length < 500,
                "gateway-bound runtime should stay compact in the RamScript");
    }

    private static void testSharedDispatcherSizesAndReset() {
        check(SharedHotkeyDispatcher.sizeForBindings(1) == 21, "1-binding dispatcher size mismatch");
        check(SharedHotkeyDispatcher.sizeForBindings(8) == 84, "8-binding dispatcher size mismatch");

        byte[] dispatcher = SharedHotkeyDispatcher.build(List.of(
                new SharedHotkeyDispatcher.Entry(HotkeyButton.A, -0x12),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.B, -0x1C),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.SELECT, 0x80)
        ));
        check(Byte.toUnsignedInt(dispatcher[0]) == 0xB8, "dispatcher must reset relocation with setvaddress");
        check(Byte.toUnsignedInt(dispatcher[5]) == 0x12, "dispatcher must load selected key byte from RAM");
        check(Byte.toUnsignedInt(dispatcher[dispatcher.length - 1]) == 0x02, "dispatcher fallback must end safely");
    }

    private static void testSharedWrapperFitsExact32Bytes() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] r = SharedHotkeyRuntime.wrapperBytesForTest(rom, HotkeyButton.R);
        byte[] l = SharedHotkeyRuntime.wrapperBytesForTest(rom, HotkeyButton.L);
        check(r.length == 32 && l.length == 32, "shared wrapper must stay inside the validated 32-byte block");
        check(SharedHotkeyRuntime.safetyGateBytesForTest().length == 12,
                "shared safety gate must stay inside the validated 12-byte gap");
        check(Byte.toUnsignedInt(SharedHotkeyRuntime.safetyGateBytesForTest()[6]) == 0x75,
                "locked-input branch must target the new wrapper return thunk at 03005328");
    }

    private static void testSharedCandidateBuildsEightBindings() {
        RomProfile rom = RomProfile.fromId("fr10");
        List<SharedHotkeyDispatcher.Entry> entries = List.of(
                new SharedHotkeyDispatcher.Entry(HotkeyButton.A, -0x12),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.B, -0x1C),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.SELECT, -0x26),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.START, -0x30),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.RIGHT, -0x3A),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.LEFT, -0x44),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.UP, -0x4E),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.DOWN, -0x58)
        );
        TriggerBuildResult result = SharedHotkeyRuntime.compose(rom, HotkeyButton.R, entries);
        check(result.payloadBytes() == 84, "8-binding shared runtime should carry only the 84-byte dispatcher as RamScript payload");
        check(result.totalScriptBytes() < 500, "8-binding shared candidate should remain below 500 RamScript bytes before local payloads");
        check(SharedHotkeyRuntime.nativeBlobSize(rom, HotkeyButton.R) == 241,
                "shared native blob should be 241 bytes");
    }

    private static void testSharedCandidateRejectsBadModifiersAndDuplicates() {
        expectFailure(() -> SharedHotkeyRuntime.compose(
                RomProfile.fromId("fr10"), HotkeyButton.A,
                List.of(new SharedHotkeyDispatcher.Entry(HotkeyButton.B, -0x12))
        ), "shared runtime modifier must be R or L");

        expectFailure(() -> SharedHotkeyRuntime.compose(
                RomProfile.fromId("fr10"), HotkeyButton.R,
                List.of(
                        new SharedHotkeyDispatcher.Entry(HotkeyButton.A, -0x12),
                        new SharedHotkeyDispatcher.Entry(HotkeyButton.A, -0x1C)
                )
        ), "shared runtime must reject duplicate pressed buttons");
    }

    private static void testRealFourPresetSharedPlan() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] seed = SeedModifierPreset.buildPayload(rom, 0x1234);
        byte[] repel = RepelHotkeyPreset.buildPayload();
        byte[] iv = PartyIvViewerPreset.buildPayload(rom);
        byte[] sid = ShowSecretIdPreset.buildScript(rom);

        PlacementPlan placement = PayloadPlacementPlanner.plan(List.of(
                PresetRequest.fieldScript("seed", seed, PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("repel", repel, PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("party-iv", iv, PresetPlacementPreference.PERSISTENT, true),
                PresetRequest.fieldScript("show-sid", sid, PresetPlacementPreference.PERSISTENT, true)
        ), 600);

        HotkeyButton[] buttons = {HotkeyButton.SELECT, HotkeyButton.B, HotkeyButton.A, HotkeyButton.START};
        java.util.ArrayList<SharedHotkeyDispatcher.Entry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < buttons.length; i++) {
            PersistentFieldScriptAllocation allocation = placement.persistentAllocations().get(i);
            entries.add(new SharedHotkeyDispatcher.Entry(
                    buttons[i], -allocation.gatewayDistanceFromRamScript()
            ));
        }

        TriggerBuildResult runtime = SharedHotkeyRuntime.compose(rom, HotkeyButton.R, entries);
        check(placement.diagnostics().sb2PayloadBytesUsed() == 923,
                "Seed+Repel+PartyIV+SID should use 923 bytes of SB2 with fixed-width u32 Seed prompt");
        check(placement.diagnostics().sb2PayloadBytesFree() == 101,
                "real four-preset plan should leave 101 bytes of SB2 with fixed-width u32 Seed prompt");
        check(runtime.payloadBytes() == 48,
                "four bindings should need a 48-byte shared Field Script dispatcher");
        check(runtime.totalScriptBytes() == 427 && runtime.freeScriptBytes() == 568,
                "four-binding shared runtime should be 427/995 and leave 568 bytes free");
    }

    private static void testSharedNativeSmokeLayout() {
        RomProfile rom = RomProfile.fromId("fr10");
        SharedHotkeyNativeSmokeTestPreset.Layout l = SharedHotkeyNativeSmokeTestPreset.layout(rom, 0x1234);
        check(l.seedOffset() == 0x0B60, "Build 20 seed offset must follow 64-byte native catalog");
        check(l.repelOffset() == 0x0BB4, "Build 20 repel offset mismatch");
        check(l.sidBridgeOffset() == 0x0C68, "Build 20 SID bridge offset mismatch");
        check(l.sb2Used() == 789, "Build 20-derived layout should use 789/1024 SB2 bytes with fixed-width u32 Seed prompt");
        check(l.sidBridge().length == 461, "Build 20 SID bridge size regression");
    }

    private static void testSharedNativeSmokeInstallersBuild() {
        RomProfile rom = RomProfile.fromId("fr10");
        RamScript a = SharedHotkeyNativeSmokeTestPreset.buildInstallerA(rom, 0x1234);
        RamScript b = SharedHotkeyNativeSmokeTestPreset.buildInstallerB(rom, 0x1234);
        check(a.isChecksumValid(), "Build 20 installer A checksum");
        check(b.isChecksumValid(), "Build 20 installer B checksum");
    }

    private static void testSharedNativeSmokeRuntimeUsesSharedCore() {
        TriggerBuildResult r = SharedHotkeyNativeSmokeTestPreset.buildRuntime(RomProfile.fromId("fr10"), 0x1234);
        check(r.totalScriptBytes() == 419 && r.freeScriptBytes() == 576,
                "Build 20 runtime must remain the validated 3-binding shared core size");
        check(r.payloadBytes() == 39, "three bindings should keep the 39-byte shared dispatcher");
    }


    private static void testPersistentNativeModuleCatalogGeneric() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] sid = PersistentShowSecretIdModule.payload(rom).bytes();
        PersistentNativeModuleCatalog.Image image = PersistentNativeModuleCatalog.build(
                PayloadStorageArea.SAVE_BLOCK2.offset(),
                java.util.List.of(new PersistentNativeModuleSpec(PersistentShowSecretIdModule.MODULE_ID, sid))
        );
        check(image.bytes().length == 64, "one 32-byte native module should produce a 64-byte catalog image");
        check(image.placements().size() == 1, "native catalog should expose one placement");
        check(image.placements().get(0).payloadOffset() == 0x20, "first native payload should start at catalog + 0x20");
        check(image.placements().get(0).payloadSize() == 32, "SID native payload size should be reported separately");
    }

    private static void testPersistentNativeCallBridgeGeneric() {
        RomProfile rom = RomProfile.fromId("fr10");
        PersistentNativeCallBridge.Build build = PersistentNativeCallBridge.build(
                rom,
                PersistentShowSecretIdModule.MODULE_ID,
                PersistentShowSecretIdModule.SUCCESS_VALUE,
                b -> b.setVar(0x8004, 0),
                b -> b.end(),
                b -> b.end()
        );
        check(build.nativeHelperBytes() == 164, "validated persistent-native dispatcher size regression");
        check(build.fieldScriptBytes() > build.nativeHelperBytes(), "bridge should include dispatcher staging plus Field Script control");
    }
    private static void testPersistentPartyIvModule() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] payload = PersistentPartyIvViewerModule.payload(rom);
        check(payload.length == PartyMonDataNativeHelper.CODE_SIZE, "persistent Party IV module should reuse 296-byte validated helper");
    }


    private static void testProductionNativeHelpersMatchValidatedLegacyBytes() {
        RomProfile rom = RomProfile.fromId("fr10");
        long directAddress = rom.stringVar4 + 0x40L;
        byte[] productionDirect = PersistentNativeExecutionHelper.buildDirectDispatcherAt(rom, directAddress).codeCopy();
        byte[] legacyDirect = PersistentToolkitStorageV6NativeHelper.buildDispatcherAt(rom, directAddress).codeCopy();
        check(java.util.Arrays.equals(productionDirect, legacyDirect),
                "promoted direct persistent-native dispatcher must remain byte-identical to validated legacy bytes");

        long stagingAddress = rom.stringVar4 + 0x140L;
        byte[] productionLoader = PersistentNativeExecutionHelper.buildStagingLoaderAt(
                rom, directAddress, stagingAddress, PartyMonDataNativeHelper.CODE_SIZE).codeCopy();
        byte[] legacyLoader = PersistentToolkitStorageV6NativeHelper.buildStagingLoaderAt(
                rom, directAddress, stagingAddress, PartyMonDataNativeHelper.CODE_SIZE).codeCopy();
        check(java.util.Arrays.equals(productionLoader, legacyLoader),
                "promoted stage-only persistent-native loader must remain byte-identical to Build-24 bytes");
    }

    private static void testCpuSetPersistentBridgeReportsWordAlignment() {
        RomProfile rom = RomProfile.fromId("fr10");
        PersistentNativeCallBridge.Build bridge = PersistentNativeCallBridge.buildStagedThenCallNative(
                rom, PersistentPartyIvViewerModule.MODULE_ID,
                rom.stringVar4 + 0x140L, PartyMonDataNativeHelper.CODE_SIZE,
                b -> {}, b -> b.end(), b -> b.end());
        check(bridge.requiredBaseAlignment() == 4,
                "CPU_SET_BLOCK persistent bridge must report 4-byte base alignment");
        check((0x0D60 % bridge.requiredBaseAlignment()) == 0,
                "validated Party IV bridge offset must satisfy reported alignment");
    }
    private static void testSharedPartyIvSmokeLayout() {
        RomProfile rom = RomProfile.fromId("fr10");
        SharedHotkeyPartyIvSmokeTestPreset.Layout l = SharedHotkeyPartyIvSmokeTestPreset.layout(rom,0x1234);
        check(l.catalog().length == 328, "296-byte Party IV module should produce 328-byte one-module catalog");
        check(l.sb2Used() <= PayloadStorageArea.SAVE_BLOCK2.capacity(), "Party IV shared smoke must fit SB2");
        check(l.partyBridgeOffset() == 0x0D6C, "shared-loader Party IV bridge offset regression");
        check(l.partyBridge().length == 97, "shared-loader Party IV bridge size regression");
        check(l.sb2Used() == 685, "shared-loader Party IV SB2 size regression");
        check(PayloadStorageArea.SAVE_BLOCK2.capacity() - l.sb2Used() == 339,
                "shared-loader Party IV composition should leave 339 bytes of SB2 headroom");
    }

    private static void testSharedPartyIvSmokeBuilds() {
        RomProfile rom = RomProfile.fromId("fr10");
        RamScript a = SharedHotkeyPartyIvSmokeTestPreset.buildInstallerA(rom,0x1234);
        RamScript b = SharedHotkeyPartyIvSmokeTestPreset.buildInstallerB(rom,0x1234);
        RamScript c = SharedHotkeyPartyIvSmokeTestPreset.buildInstallerC(rom,0x1234);
        TriggerBuildResult r = SharedHotkeyPartyIvSmokeTestPreset.buildRuntime(rom,0x1234);
        check(a.isChecksumValid() && b.isChecksumValid() && c.isChecksumValid(), "Party IV shared smoke installers checksum");
        check(r.totalScriptBytes() == 787 && r.freeScriptBytes() == 208,
                "Party IV shared-loader runtime size regression");
    }


    private static void testStagedDispatcherPreservesEntryUntilChecksumLoaded() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] code = PersistentToolkitStorageV6NativeHelper.buildStagingDispatcherAt(
                rom, rom.stringVar4 + 0x40L, rom.stringVar4 + 0x140L, PartyMonDataNativeHelper.CODE_SIZE
        ).codeCopy();

        int checksumLoad = findHalfword(code, 0x8923); // ldrh r3,[r4,#8]
        int scratchLoad = findHalfword(code, 0x7814);  // ldrb r4,[r2]
        check(checksumLoad >= 0 && scratchLoad >= 0 && checksumLoad < scratchLoad,
                "staged dispatcher must load expected checksum before reusing r4 as checksum-loop scratch");
    }


    private static void testPartyIvDirectCallDiagnosticBuilds() {
        RomProfile rom = RomProfile.fromId("fr10");
        RamScript script = PartyIvDirectCallDiagnostic.build(rom);
        check(script.isChecksumValid(), "Party IV direct-call diagnostic checksum");
    }

    private static void testPartyIvProductionBridgeUsesStockCallnative() {
        RomProfile rom = RomProfile.fromId("fr10");
        PersistentNativeCallBridge.Build bridge = PersistentNativeCallBridge.buildStagedThenCallNative(
                rom, PersistentPartyIvViewerModule.MODULE_ID,
                rom.stringVar4 + 0x140L, PartyMonDataNativeHelper.CODE_SIZE,
                b -> {}, b -> b.end(), b -> b.end());
        check(bridge.nativeHelperBytes() == 176, "production staged loader size regression");
        byte[] script = bridge.fieldScript();
        long target = (rom.stringVar4 + 0x140L) | 1L;
        boolean found = false;
        for (int i = 0; i + 4 < script.length; i++) {
            if ((script[i] & 0xFF) == 0x23) {
                long v = (script[i+1]&0xFFL) | ((script[i+2]&0xFFL)<<8) | ((script[i+3]&0xFFL)<<16) | ((script[i+4]&0xFFL)<<24);
                if (v == target) { found = true; break; }
            }
        }
        check(found, "production Party IV bridge must return to Field Script and use stock callnative on staged module");
    }

    private static void testSharedPersistentNativeStagingService() {
        RomProfile rom = RomProfile.fromId("fr10");
        int offset = SharedPersistentNativeStagingService.offsetForBindings(3, 4);
        check(offset == 0x34, "3-binding shared native service offset regression");
        SharedPersistentNativeStagingService.Build service = SharedPersistentNativeStagingService.build(
                rom, offset, rom.stringVar4 + 0x140L, 0x140);
        check(service.loaderBytes() == 176, "shared service must reuse validated 176-byte stage-only loader");
        check(service.requiredBaseAlignment() == 4, "shared service CPU_SET_BLOCK base alignment");
        check(service.fieldScript().length == 367, "shared native staging service size regression");
    }

    private static void testPartyIvBridgeTargetsSharedStagingService() {
        RomProfile rom = RomProfile.fromId("fr10");
        SharedHotkeyPartyIvSmokeTestPreset.Layout l = SharedHotkeyPartyIvSmokeTestPreset.layout(rom, 0x1234);
        byte[] script = l.partyBridge();
        int vcall = -1;
        for (int i = 0; i + 4 < script.length; i++) {
            if ((script[i] & 0xFF) == 0xBA) { vcall = i; break; }
        }
        check(vcall >= 0, "Party IV shared bridge must vcall shared staging service");
        long encoded = (script[vcall+1]&0xFFL) | ((script[vcall+2]&0xFFL)<<8)
                | ((script[vcall+3]&0xFFL)<<16) | ((script[vcall+4]&0xFFL)<<24);
        long bridgeStatic = 0x02024588L + l.partyBridgeOffset();
        long sAddressOffset = HotkeyRuntimeV1.VIRTUAL_BASE - bridgeStatic;
        long resolved = (encoded - sAddressOffset) & 0xFFFF_FFFFL;
        long expected = 0x0202552CL + 0x3624L
                + SharedPersistentNativeStagingService.offsetForBindings(3, 4);
        check(resolved == expected, "Party IV vcall must resolve to shared RamScript staging service");

        long stagedTarget = (rom.stringVar4 + 0x140L) | 1L;
        boolean foundCallnative = false;
        for (int i = 0; i + 4 < script.length; i++) {
            if ((script[i] & 0xFF) == 0x23) {
                long target = (script[i+1]&0xFFL) | ((script[i+2]&0xFFL)<<8)
                        | ((script[i+3]&0xFFL)<<16) | ((script[i+4]&0xFFL)<<24);
                if (target == stagedTarget) { foundCallnative = true; break; }
            }
        }
        check(foundCallnative, "Party IV bridge must retain stock callnative after shared staging service returns");
    }

    private static void testPersistentSecretIdModule() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] persistent = PersistentSecretIdModule.payload(rom);
        byte[] standalone = SecretIdNativeHelper.build(rom).codeCopy();
        check(persistent.length == 20, "persistent SID helper size regression");
        check(java.util.Arrays.equals(persistent, standalone),
                "persistent SID module must reuse validated standalone helper bytes");
    }

    private static void testDualNativeSharedLayout() {
        RomProfile rom = RomProfile.fromId("fr10");
        SharedPersistentNativeComposition.Layout l = SharedPersistentNativeComposition.layout(rom, 0x1234);
        check(l.catalog().length == 364, "Party IV + SID two-module catalog size regression");
        check(l.partyBridge().length == 97, "dual-native Party IV bridge size regression");
        check(l.sidBridge().length == 119, "dual-native SID bridge size regression");
        check(l.sb2Used() == 840, "dual-native SB2 usage regression");
        check(PayloadStorageArea.SAVE_BLOCK2.capacity() - l.sb2Used() == 184,
                "dual-native composition should leave 184 bytes free in SB2");
        check(l.gatewaySid() == 0x35F4, "fourth shared gateway slot regression");
    }

    private static void testDualNativeSharedBuilds() {
        RomProfile rom = RomProfile.fromId("fr10");
        RamScript a = SharedPersistentNativeComposition.buildInstallerA(rom, 0x1234);
        RamScript b = SharedPersistentNativeComposition.buildInstallerB(rom, 0x1234);
        RamScript c = SharedPersistentNativeComposition.buildInstallerC(rom, 0x1234);
        TriggerBuildResult runtime = SharedPersistentNativeComposition.buildRuntime(rom, 0x1234);
        check(a.isChecksumValid() && b.isChecksumValid() && c.isChecksumValid(),
                "dual-native installers checksum");
        check(runtime.totalScriptBytes() == 795 && runtime.freeScriptBytes() == 200,
                "dual-native shared runtime size regression");
    }


    private static void testDualNativeSidUsesStrictWait() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        byte[] sid = SharedPersistentNativeComposition.layout(rom, 0x1234).sidBridge();
        check(containsSequence(sid, new byte[] {0x6D, 0x00, 0x6B, 0x02}),
                "dual-native SID must use waitbuttonpress+nop before releaseall/end");
    }

    private static void testDualNativeBridgesShareOneService() {
        RomProfile rom = RomProfile.fromId("fr10");
        SharedPersistentNativeComposition.Layout l = SharedPersistentNativeComposition.layout(rom, 0x1234);
        int serviceOffset = SharedPersistentNativeStagingService.offsetForBindings(4, 4);
        long expectedService = 0x0202552CL + 0x3624L + serviceOffset;
        check(resolveFirstVcall(l.partyBridge(), l.partyBridgeOffset()) == expectedService,
                "Party IV bridge must target the one shared native staging service");
        check(resolveFirstVcall(l.sidBridge(), l.sidBridgeOffset()) == expectedService,
                "SID bridge must target the same shared native staging service");
        long stagedTarget = (rom.stringVar4 + 0x140L) | 1L;
        check(containsCallnative(l.partyBridge(), stagedTarget),
                "Party IV bridge must stock-callnative the common staged address");
        check(containsCallnative(l.sidBridge(), stagedTarget),
                "SID bridge must stock-callnative the same common staged address");
    }


    private static void testDualNativeSidRestoresRelocationFromCurrentPosition() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        SharedPersistentNativeComposition.Layout l = SharedPersistentNativeComposition.layout(rom, 0x1234);
        byte[] sid = l.sidBridge();
        // The second setvaddress is emitted immediately after the 5-byte vcall,
        // at bridge offset 0x15. To restore the original relocation delta it
        // must embed VIRTUAL_BASE+0x15, not VIRTUAL_BASE.
        check((sid[0x15] & 0xFF) == 0xB8, "SID bridge second setvaddress opcode regression");
        long encoded = (sid[0x16]&0xFFL) | ((sid[0x17]&0xFFL)<<8)
                | ((sid[0x18]&0xFFL)<<16) | ((sid[0x19]&0xFFL)<<24);
        check(encoded == HotkeyRuntimeV1.VIRTUAL_BASE + 0x15L,
                "second setvaddress must encode current virtual opcode address");
        // Success vmessage is at +0x2E and points to sid_msg at +0x38. With
        // the restored delta it must resolve to the real embedded text.
        check((sid[0x2E] & 0xFF) == 0xBD, "SID success vmessage opcode regression");
        long msgVirtual = (sid[0x2F]&0xFFL) | ((sid[0x30]&0xFFL)<<8)
                | ((sid[0x31]&0xFFL)<<16) | ((sid[0x32]&0xFFL)<<24);
        long bridgeStatic = 0x02024588L + l.sidBridgeOffset();
        long restoredOffset = encoded - (bridgeStatic + 0x15L);
        long resolved = (msgVirtual - restoredOffset) & 0xFFFF_FFFFL;
        check(resolved == bridgeStatic + 0x38L,
                "SID vmessage must resolve to embedded sid_msg rather than its 0xFF terminator");
    }


    private static void testPresetCatalogDefinesProductionPresets() {
        List<PresetDefinition> all = PresetCatalog.all();
        check(all.size() == 12, "production preset metadata catalog should contain twelve real reference presets");
        check(PresetCatalog.byId("seed-modifier").payloadType() == PresetPayloadType.FIELD_SCRIPT,
                "Seed metadata payload type");
        check(PresetCatalog.byId("seed-modifier-box14").payloadType() == PresetPayloadType.HYBRID_NATIVE,
                "BOX 14 Seed metadata payload type");
        check(PresetCatalog.byId("repel").payloadType() == PresetPayloadType.FIELD_SCRIPT,
                "Repel metadata payload type");
        check(PresetCatalog.byId("party-iv-viewer").payloadType() == PresetPayloadType.HYBRID_NATIVE,
                "Party IV metadata payload type");
        check(PresetCatalog.byId("show-secret-id").payloadType() == PresetPayloadType.HYBRID_NATIVE,
                "SID metadata payload type");
        check(PresetCatalog.byId("mute-music").payloadType() == PresetPayloadType.HYBRID_NATIVE,
                "Mute Music metadata payload type");
        check(PresetCatalog.byId("run-anywhere").payloadType() == PresetPayloadType.FIELD_SCRIPT,
                "Run Anywhere metadata payload type");
        check(PresetCatalog.byId("run-bike-anywhere").payloadType() == PresetPayloadType.HYBRID_NATIVE,
                "Run + Bike Anywhere metadata payload type");
    }

    private static void testPresetCatalogSeparatesSharedInfrastructureFromPerPresetCosts() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        PresetDeploymentDefinition party = PresetCatalog.partyIvViewer()
                .deployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE);
        PresetDeploymentDefinition sid = PresetCatalog.showSecretId()
                .deployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE);
        check(party.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                "Party IV persistent metadata must request shared native service");
        check(sid.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                "SID persistent metadata must request the same shared native service");
        check(party.cost(rom).ramScriptPayloadBytes() == 0 && sid.cost(rom).ramScriptPayloadBytes() == 0,
                "shared service bytes must not be double-counted as per-preset RamScript payload");
        check(party.cost(rom).sb2FieldScriptBytes() == 97 && party.cost(rom).sb2NativeModuleBytes() == 296,
                "Party IV persistent component costs regression");
        check(sid.cost(rom).sb2FieldScriptBytes() == 119 && sid.cost(rom).sb2NativeModuleBytes() == 20,
                "SID persistent component costs regression");
    }

    private static void testPresetCatalogPreservesSimpleSidDeliverymanPath() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        PresetDefinition sid = PresetCatalog.showSecretId();
        check(sid.supportsDeployment(PresetDeploymentKind.DELIVERYMAN_LOCAL),
                "SID metadata must preserve validated simple deliveryman mode");
        PresetDeploymentCost local = sid.deployment(PresetDeploymentKind.DELIVERYMAN_LOCAL).cost(rom);
        check(local.ramScriptPayloadBytes() == ShowSecretIdPreset.payloadSize(rom),
                "SID local metadata must report existing standalone payload size");
        check(sid.deployment(PresetDeploymentKind.DELIVERYMAN_LOCAL).infrastructure().isEmpty(),
                "simple SID deliveryman mode must not require shared infrastructure");
    }

    private static void testPresetCatalogDoesNotOverclaimRomValidation() {
        PresetDeploymentDefinition party = PresetCatalog.partyIvViewer()
                .deployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE);
        check(party.isValidatedOn(RomProfile.FIRE_RED_EN_10),
                "dual-native shared Party IV must be marked validated on FR1.0");
        check(!party.isValidatedOn(RomProfile.FIRE_RED_EN_11),
                "metadata must not claim FR1.1 runtime validation from symbol support alone");
        check(PresetCatalog.partyIvViewer().supports(RomProfile.FIRE_RED_EN_11),
                "code/profile support must remain distinct from runtime validation");
    }



    private static void testValidationMatrixSeparatesUsageFromDeployment() {
        PresetDefinition party = PresetCatalog.partyIvViewer();
        check(party.validationMatrix().size() == RomProfile.values().length * PresetUsageMode.values().length,
                "validation matrix must contain one cell per ROM and usage family");
    }

    private static void testValidationMatrixTracksRomVersionsIndependently() {
        PresetDefinition party = PresetCatalog.partyIvViewer();
        check(party.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, RomProfile.FIRE_RED_EN_10)
                        == PresetValidationStatus.VALIDATED_IN_GAME,
                "FR1.0 shared Party IV must remain validated");
        check(party.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, RomProfile.FIRE_RED_EN_11)
                        == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                "FR1.1 must not inherit FR1.0 validation");
    }

    private static void testValidationMatrixRecordsSharedBaseline() {
        for (String id : List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id")) {
            check(PresetCatalog.byId(id).isValidated(PresetUsageMode.SHARED_N_HOTKEY, RomProfile.FIRE_RED_EN_10),
                    id + " must record the validated Build-33/34 shared baseline");
        }
    }

    private static void testValidationMatrixRecordsLegacyPair() {
        check(PresetCatalog.seedModifier().isValidated(PresetUsageMode.LEGACY_MULTI_HOTKEY, RomProfile.FIRE_RED_EN_10),
                "Seed must retain historical two-hotkey validation");
        check(PresetCatalog.repel().isValidated(PresetUsageMode.LEGACY_MULTI_HOTKEY, RomProfile.FIRE_RED_EN_10),
                "Repel must retain historical two-hotkey validation");
    }

    private static void testValidationMatrixPreservesSimplePartyIvPaths() {
        PresetDefinition party = PresetCatalog.partyIvViewer();
        check(party.isValidated(PresetUsageMode.DELIVERYMAN, RomProfile.FIRE_RED_EN_10),
                "Party IV deliveryman path must remain recorded");
        check(party.isValidated(PresetUsageMode.SINGLE_HOTKEY, RomProfile.FIRE_RED_EN_10),
                "Party IV standalone hotkey path must remain recorded");
    }

    private static void testValidationMatrixSupportsGeneralizedPresetTriggers() {
        for (String id : List.of(
                "seed-modifier", "repel", "show-secret-id", "party-iv-viewer",
                "party-ev-viewer", "lead-iv-viewer", "lead-ev-viewer")) {
            PresetDefinition preset = PresetCatalog.byId(id);
            check(preset.supportsDeployment(PresetDeploymentKind.DELIVERYMAN_LOCAL),
                    id + " must expose a direct Deliveryman deployment");
            check(preset.supportsDeployment(PresetDeploymentKind.HOTKEY_LOCAL),
                    id + " must expose HotkeyRuntimeV1");
            check(preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_FIELD_SCRIPT)
                            || preset.supportsDeployment(PresetDeploymentKind.SHARED_PERSISTENT_NATIVE),
                    id + " must expose SharedHotkeyRuntime deployment");
            check(preset.validationStatus(PresetUsageMode.DELIVERYMAN, RomProfile.FIRE_RED_EN_10)
                            != PresetValidationStatus.UNSUPPORTED,
                    id + " Deliveryman usage must be represented as supported");
            check(preset.validationStatus(PresetUsageMode.SINGLE_HOTKEY, RomProfile.FIRE_RED_EN_10)
                            != PresetValidationStatus.UNSUPPORTED,
                    id + " single-hotkey usage must be represented as supported");
            check(preset.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, RomProfile.FIRE_RED_EN_10)
                            != PresetValidationStatus.UNSUPPORTED,
                    id + " Shared/N-hotkey usage must be represented as supported");
        }

        check(PresetCatalog.seedModifier().validationStatus(PresetUsageMode.DELIVERYMAN, RomProfile.FIRE_RED_EN_10)
                        == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                "new Seed Deliveryman path must not be overclaimed as game-validated");
        check(PresetCatalog.repel().validationStatus(PresetUsageMode.DELIVERYMAN, RomProfile.FIRE_RED_EN_10)
                        == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                "new Repel Deliveryman path must not be overclaimed as game-validated");
        check(PresetCatalog.showSecretId().validationStatus(PresetUsageMode.SINGLE_HOTKEY, RomProfile.FIRE_RED_EN_10)
                        == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                "new SID HotkeyRuntimeV1 path must not be overclaimed as game-validated");
    }

    private static void testMuteMusicCatalogAndComposition() {
        PresetDefinition mute = PresetCatalog.byId("mute-music");
        check(mute.defaultHotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.DOWN)),
                "Mute Music default hotkey must remain R+DOWN");
        for (RomProfile rom : RomProfile.values()) {
            check(mute.supports(rom), "Mute Music must support all four English FR/LG profiles: " + rom.id());
        }
        check(mute.validationStatus(PresetUsageMode.SINGLE_HOTKEY, RomProfile.LEAF_GREEN_EN_10)
                        == PresetValidationStatus.VALIDATED_IN_GAME,
                "Mute Music standalone LG1.0 path must record real-cart validation");
        check(mute.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, RomProfile.LEAF_GREEN_EN_10)
                        == PresetValidationStatus.VALIDATED_IN_GAME,
                "Mute Music Shared LG1.0 path must record corrected real-cart validation");
        check(mute.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, RomProfile.FIRE_RED_EN_10)
                        == PresetValidationStatus.VALIDATED_IN_GAME,
                "Mute Music Shared FR1.0 path must record the SID-local four-pack cart validation");
        for (RomProfile rom : List.of(RomProfile.FIRE_RED_EN_11, RomProfile.LEAF_GREEN_EN_11)) {
            check(mute.validationStatus(PresetUsageMode.SHARED_N_HOTKEY, rom)
                            == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                    "Mute Music Shared untested revisions must remain supported/not-tested: " + rom.id());
        }

        PresetCompositionPlan single = PresetCompositionPlanner.planSelections(
                RomProfile.LEAF_GREEN_EN_10,
                List.of(new PresetSelection("mute-music", PresetActivation.HOTKEY)));
        check(single.infrastructure().contains(PresetInfrastructure.HOTKEY_RUNTIME),
                "single Mute Music must use HotkeyRuntimeV1");
        RamScript singleScript = CompositionArtifactBuilder.buildLocal(single, 0);
        check(singleScript != null, "single Mute Music production composition must materialize");

        PresetCompositionPlan shared = PresetCompositionPlanner.planSelections(
                RomProfile.LEAF_GREEN_EN_10,
                List.of(
                        new PresetSelection("seed-modifier", PresetActivation.HOTKEY),
                        new PresetSelection("mute-music", PresetActivation.HOTKEY)));
        check(shared.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME),
                "Seed + Mute Music must plan through SharedHotkeyRuntime");
        check(shared.concreteLayout().bindingPlan().bindings().stream()
                        .anyMatch(b -> b.presetId().equals("mute-music")
                                && b.hotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.DOWN))),
                "Shared Mute Music binding must remain R+DOWN");
        CompositionArtifactBuilder.Build built = CompositionArtifactBuilder.build(shared, 0x12345678);
        check(built.components().containsKey("mute-music:field"),
                "Shared Mute Music must materialize its self-contained SB2 field payload");
        ConcretePresetAllocation muteAllocation = shared.concreteLayout().allocations().stream()
                .filter(a -> a.presetId().equals("mute-music"))
                .findFirst().orElseThrow();
        check((muteAllocation.sb2FieldScriptOffset() & 3) == 0,
                "Shared Mute Music Field Script must be 4-byte aligned for CPU_SET_BLOCK/CpuSet source alignment");

        PresetCompositionPlan fullShared = PresetCompositionPlanner.planSelections(
                RomProfile.LEAF_GREEN_EN_10,
                List.of(
                        new PresetSelection("seed-modifier", PresetActivation.HOTKEY),
                        new PresetSelection("show-secret-id", PresetActivation.HOTKEY),
                        new PresetSelection("party-iv-viewer", PresetActivation.HOTKEY),
                        new PresetSelection("mute-music", PresetActivation.HOTKEY)));
        ConcretePresetAllocation fullMuteAllocation = fullShared.concreteLayout().allocations().stream()
                .filter(a -> a.presetId().equals("mute-music"))
                .findFirst().orElseThrow();
        check((fullMuteAllocation.sb2FieldScriptOffset() & 3) == 0,
                "validated early-RNG + Mute Shared composition must align Mute payload to 4 bytes");
    }

    private static void testSeedModifierAcceptsFullU32State() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        int state = (int) 0xB5B1E7ADL;
        long predecessor = SeedModifierPreset.predecessor(state);

        check(RngMath.nextState(predecessor) == 0xB5B1E7ADL,
                "Seed Modifier predecessor must round-trip a full u32 PRNG state");
        check(SeedModifierPreset.message(state).contains("B5B1E7AD"),
                "Seed Modifier prompt must display the full 32-bit PRNG state");
        check(SeedModifierPreset.buildPayload(rom, state).length == SeedModifierPreset.buildPayload(rom, 0x1234).length,
                "Seed Modifier payload size must remain invariant across the full u32 range");
        check(SeedModifierPreset.message(0x1234).contains("00001234"),
                "short PRNG states must be zero-padded to eight hex digits");

        int maxState = (int) 0xFFFF_FFFFL;
        check(RngMath.nextState(SeedModifierPreset.predecessor(maxState)) == 0xFFFF_FFFFL,
                "Seed Modifier must accept FFFFFFFF as a valid u32 PRNG state");
        check(RngMath.nextState(SeedModifierPreset.predecessor(0)) == 0,
                "Seed Modifier must accept 00000000 as a valid u32 PRNG state");
    }

    private static void testSeedModifierClosesLinkBeforePrompt() {
        byte[] payload = SeedModifierPreset.buildPayload(RomProfile.LEAF_GREEN_EN_10, 0x1234);

        check((payload[0] & 0xFF) == 0xB8, "Seed Modifier must begin with setvaddress");
        check((payload[5] & 0xFF) == 0x69, "Seed Modifier must lock before closing link");
        check((payload[6] & 0xFF) == 0x25, "Seed Modifier must execute a special before opening the prompt");
        check((payload[7] & 0xFF) == 0x1F && (payload[8] & 0xFF) == 0x00,
                "Seed Modifier must call stock special 0x001F (CloseLink)");
        check((payload[9] & 0xFF) == 0x11, "Seed Modifier must clear gWirelessCommType after CloseLink");
        check((payload[10] & 0xFF) == 0x00, "Seed Modifier must write zero to gWirelessCommType");
        check((payload[11] & 0xFF) == 0x3C && (payload[12] & 0xFF) == 0x3F
                        && (payload[13] & 0xFF) == 0x00 && (payload[14] & 0xFF) == 0x03,
                "Seed Modifier must target gWirelessCommType at 0x03003F3C");
        check((payload[15] & 0xFF) == 0xBD,
                "Seed Modifier must open the message only after CloseLink and clearing wireless type");
        check(payload.length == 81,
                "Seed Modifier normalized payload must remain an 81-byte field script");
    }

    private static void testSeedModifierU32SharedLayoutDoesNotOverlap() {
        RomProfile rom = RomProfile.LEAF_GREEN_EN_10;
        int state = (int) 0xB5B1E7ADL;
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                rom, List.of("seed-modifier", "show-secret-id", "party-iv-viewer"));
        CompositionArtifactBuilder.Build artifacts = CompositionArtifactBuilder.build(plan, state);
        InstallationPlan installation = CompositionInstallationPlanner.plan(plan);

        for (InstallationWrite write : installation.writes()) {
            byte[] component = artifacts.component(write.componentId());
            check(component.length == write.size(),
                    "planned write size must match emitted component for " + write.componentId());
        }

        ConcretePresetAllocation seed = plan.concreteLayout().allocations().stream()
                .filter(a -> a.presetId().equals("seed-modifier")).findFirst().orElseThrow();
        ConcretePresetAllocation sid = plan.concreteLayout().allocations().stream()
                .filter(a -> a.presetId().equals("show-secret-id")).findFirst().orElseThrow();
        check(seed.sb2FieldScriptOffset() + artifacts.component("seed-modifier:field").length
                        <= sid.sb2FieldScriptOffset(),
                "full-u32 Seed payload must not overlap the next Shared component");
    }

    private static void testGeneralizedPresetTriggersReuseFunctionalPayloads() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;

        byte[] seedPayload = SeedModifierPreset.buildPayload(rom, 0x1234);
        TriggerBuildResult seedDelivery = SeedModifierPreset.buildDeliveryman(rom, 0x1234);
        TriggerBuildResult seedHotkey = SeedModifierPreset.build(rom, 0x1234, new Hotkey(HotkeyButton.R, HotkeyButton.SELECT));
        check(seedDelivery.payloadBytes() == seedPayload.length,
                "Seed Deliveryman must use the canonical Seed payload");
        check(seedHotkey.payloadBytes() == seedPayload.length,
                "Seed HotkeyRuntimeV1 must use the canonical Seed payload");
        check(seedDelivery.runtimeOverheadBytes() == 0,
                "Seed Deliveryman must add no hotkey runtime overhead");

        byte[] repelPayload = RepelHotkeyPreset.buildPayload();
        TriggerBuildResult repelDelivery = RepelHotkeyPreset.buildDeliveryman(rom);
        TriggerBuildResult repelHotkey = RepelHotkeyPreset.build(rom, new Hotkey(HotkeyButton.R, HotkeyButton.B));
        check(repelDelivery.payloadBytes() == repelPayload.length,
                "Repel Deliveryman must use the canonical Repel payload");
        check(repelHotkey.payloadBytes() == repelPayload.length,
                "Repel HotkeyRuntimeV1 must use the canonical Repel payload");
        check(repelDelivery.runtimeOverheadBytes() == 0,
                "Repel Deliveryman must add no hotkey runtime overhead");

        byte[] sidPayload = ShowSecretIdPreset.buildScript(rom);
        TriggerBuildResult sidDelivery = ShowSecretIdPreset.buildDeliveryman(rom);
        TriggerBuildResult sidHotkey = ShowSecretIdPreset.buildHotkey(rom, new Hotkey(HotkeyButton.R, HotkeyButton.START));
        check(sidDelivery.payloadBytes() == sidPayload.length,
                "SID Deliveryman must use the canonical SID payload");
        check(sidHotkey.payloadBytes() == sidPayload.length,
                "SID HotkeyRuntimeV1 must wrap the canonical SID payload");
        check(sidDelivery.runtimeOverheadBytes() == 0,
                "SID Deliveryman must add no hotkey runtime overhead");
        check(sidHotkey.runtimeOverheadBytes() > 0,
                "SID HotkeyRuntimeV1 must add runtime overhead");
    }


    private static void testRunAnywhereHelperLayout() {
        NativeHelper helper = RunAnywhereNativeHelper.build(RomProfile.FIRE_RED_EN_10);
        byte[] code = helper.codeCopy();
        check(code.length == 16, "Run Anywhere helper must remain 16 bytes");
        long pointer = ((long)code[12] & 0xFF)
                | (((long)code[13] & 0xFF) << 8)
                | (((long)code[14] & 0xFF) << 16)
                | (((long)code[15] & 0xFF) << 24);
        check(pointer == 0x02036E15L, "Run Anywhere helper must target gMapHeader+0x19");
        check((code[4] & 0xFF) == 0x02 && (code[5] & 0xFF) == 0x22,
                "Run Anywhere helper must OR only allowRunning bit 1");
    }

    private static void testRunAnywhereProfilesUseSameMapHeader() {
        for (RomProfile rom : RomProfile.values()) {
            check(rom.mapHeader == 0x02036DFCL,
                    rom.id() + " must use symbol-verified gMapHeader 0x02036DFC");
        }
    }

    private static void testRunAnywhereProbeAvoidsIwramStaging() {
        for (RomProfile rom : RomProfile.values()) {
            NativeHelper helper = RunAnywhereNativeHelper.build(rom);
            check((helper.stagingAddress() & 0xFF000000L) == 0x02000000L,
                    "Run Anywhere probe helper must stage in EWRAM");
            check(helper.stagingAddress() == rom.stringVar4 + 0x140,
                    "Run Anywhere probe must reuse validated EWRAM scratch convention");
            check(RunAnywhereProbePreset.payloadSize(rom) <= RamScript.SCRIPT_SIZE,
                    "Run Anywhere probe must fit RamScript");
        }
    }

    private static void testCompositionPlannerChoosesSimpleSidPath() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planDeliveryman(
                RomProfile.FIRE_RED_EN_10, "show-secret-id");
        check(plan.selections().get(0).deployment().kind() == PresetDeploymentKind.DELIVERYMAN_LOCAL,
                "explicit SID Deliveryman plan must use the local Deliveryman path");
        check(plan.ramScriptBytes() == ShowSecretIdPreset.payloadSize(RomProfile.FIRE_RED_EN_10),
                "SID-only plan RamScript size regression");
        check(plan.sb1Bytes() == 0 && plan.sb2Bytes() == 0,
                "SID-only deliveryman plan must not consume persistent storage");
        check(!plan.usesSharedNativeStaging(),
                "SID-only deliveryman plan must not install shared native staging");
    }

    private static void testCompositionPlannerChoosesLocalPartyIvWhenAlone() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(rom, List.of("party-iv-viewer"));
        check(plan.selections().get(0).deployment().kind() == PresetDeploymentKind.HOTKEY_LOCAL,
                "one Party IV hotkey must use standalone HotkeyRuntimeV1");
        check(plan.ramScriptBytes() == HotkeyRuntimeV1.scriptSize(rom, new byte[PartyIvViewerPreset.payloadSize(rom)]),
                "Party IV local runtime cost regression");
        check(plan.sb1Bytes() == 0 && plan.sb2Bytes() == 0,
                "standalone Party IV must not consume persistent storage");
    }

    private static void testCompositionPlannerSharesPureFieldRuntime() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10, List.of("seed-modifier", "repel"));
        check(plan.infrastructure().contains(PresetInfrastructure.SHARED_HOTKEY_RUNTIME),
                "Seed+Repel should use shared hotkey runtime");
        check(!plan.usesSharedNativeStaging(),
                "pure Field Script composition must not install native staging service");
        check(plan.sb1Bytes() == 20 && plan.sb2Bytes() == 292,
                "Seed+Repel should include 260 B payload + 32 B installation manifest");
    }

    private static void testCompositionPlannerMatchesValidatedFourPresetBaseline() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        check(plan.ramScriptBytes() == 795,
                "planner must reproduce Build-27b/28 shared runtime + native service size");
        check(plan.sb1Bytes() == 40, "four validated gateways should cost 40 B SB1");
        check(plan.sb2Bytes() == 872, "planner must account for current 840 B payload + 32 B manifest");
        check(plan.ramScriptFree() == 200 && plan.sb1Free() == 360 && plan.sb2Free() == 152,
                "four-preset free-space report must reserve the 32 B manifest");
        check(plan.hotkeyBindings() == 4 && plan.usesSharedNativeStaging(),
                "validated four-preset plan should have four bindings and one shared native service");
    }

    private static void testCompositionPlannerPaysNativeServiceOnce() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10, List.of("party-iv-viewer", "show-secret-id"));
        check(plan.usesSharedNativeStaging(), "dual-native plan must require staging service");
        check(plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                "dual-native infrastructure should contain one shared native service requirement");
        check(plan.infrastructure().size() == 4,
                "shared native infrastructure requirements should be unioned rather than duplicated");
        check(plan.ramScriptBytes() < 800,
                "dual-native planner must not pay a second copy of the shared staging service");
    }

    private static void testConcretePlannerMatchesValidatedBindingsAndOffsets() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        ConcreteCompositionLayout layout = plan.concreteLayout();
        check(layout.bindingPlan().runtime() == HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "four-preset concrete plan must select SharedHotkeyRuntime");
        check(layout.nativeCatalogOffset() == 0x0B20 && layout.nativeCatalogSize() == 364,
                "dual-native catalog concrete placement regression");

        ConcretePresetAllocation seed = layout.allocations().get(0);
        ConcretePresetAllocation repel = layout.allocations().get(1);
        ConcretePresetAllocation party = layout.allocations().get(2);
        ConcretePresetAllocation sid = layout.allocations().get(3);

        check(seed.hotkeyBinding().hotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.SELECT)),
                "Seed default hotkey regression");
        check(repel.hotkeyBinding().hotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.B)),
                "Repel default hotkey regression");
        check(party.hotkeyBinding().hotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.A)),
                "Party IV default hotkey regression");
        check(sid.hotkeyBinding().hotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.START)),
                "SID default hotkey regression");

        check(seed.sb1GatewayOffset() == 0x3612 && repel.sb1GatewayOffset() == 0x3608
                        && party.sb1GatewayOffset() == 0x35FE && sid.sb1GatewayOffset() == 0x35F4,
                "concrete gateway offsets must reproduce validated Build-27b/28 layout");
        check(party.sb2NativeModuleOffset() == 0x0B50 && sid.sb2NativeModuleOffset() == 0x0C78,
                "native module offsets must reproduce persistent catalog layout");
        check(seed.sb2FieldScriptOffset() == 0x0C8C
                        && repel.sb2FieldScriptOffset() == 0x0CDD
                        && party.sb2FieldScriptOffset() == 0x0D90
                        && sid.sb2FieldScriptOffset() == 0x0DF1,
                "SB2 bridge/body offsets must reproduce validated baseline");
    }

    private static void testConcretePlannerOmitsSharedLoaderForPureFieldScripts() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10, List.of("seed-modifier", "repel"));
        ConcreteCompositionLayout layout = plan.concreteLayout();
        check(!layout.hasNativeCatalog(), "pure Field Script plan must not allocate native catalog");
        check(layout.bindingPlan().runtime() == HotkeyBindingRuntime.SHARED_HOTKEY_RUNTIME,
                "persistent Seed+Repel composition should use the shared runtime family");
        check(layout.allocations().get(0).sb2FieldScriptOffset() == 0x0B20
                        && layout.allocations().get(1).sb2FieldScriptOffset() == 0x0B71,
                "pure Field Script SB2 placement should remain compact");
    }


    private static void testInstallationPlannerKeepsLocalSidSingleWc() {
        PresetCompositionPlan composition = PresetCompositionPlanner.planDeliveryman(
                RomProfile.FIRE_RED_EN_10, "show-secret-id");
        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        check(plan.localOnly(), "standalone SID must remain a local-only installation");
        check(plan.totalWonderCards() == 1, "standalone SID must need exactly one WC");
        check(plan.localRamScriptBytes() == 166, "standalone SID local size regression");
        check(plan.persistentStages().isEmpty(), "standalone SID must not create persistent stages");
    }

    private static void testInstallationPlannerSplitsValidatedBaselineAutomatically() {
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        check(!plan.localOnly(), "four-preset baseline must be persistent");
        check(plan.runtimeStageRequired(), "four-preset baseline needs shared runtime WC");
        check(plan.runtimeStageBytes() == 795, "runtime stage must reproduce validated Build-27b/28 size");
        check(plan.writes().stream().mapToInt(InstallationWrite::size).sum() == 912,
                "persistent writes must equal 840 payload + 32 manifest + 40 SB1 bytes");
        check(plan.persistentStages().size() >= 2, "large baseline must need multiple temporary installer stages");
        check(plan.totalWonderCards() == plan.persistentStages().size() + 1,
                "runtime WC must be counted after persistent stages");
        for (InstallationStage stage : plan.persistentStages()) {
            check(stage.encodedInstallerBytes() <= RamScript.SCRIPT_SIZE,
                    "automatic installer stage must respect 995-byte RamScript limit");
        }
    }

    private static void testInstallationPlannerChunksLargeCatalogWrites() {
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("party-iv-viewer", "show-secret-id"));
        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        long catalogChunks = plan.persistentStages().stream().flatMap(stage -> stage.chunks().stream())
                .filter(chunk -> chunk.componentId().equals("native-catalog")).count();
        check(catalogChunks == 2, "364-byte native catalog must split into 255 + 109 byte copy chunks");
        boolean has255 = plan.persistentStages().stream().flatMap(stage -> stage.chunks().stream())
                .anyMatch(chunk -> chunk.componentId().equals("native-catalog") && chunk.size() == 255);
        boolean has109 = plan.persistentStages().stream().flatMap(stage -> stage.chunks().stream())
                .anyMatch(chunk -> chunk.componentId().equals("native-catalog") && chunk.size() == 109);
        check(has255 && has109, "native catalog chunk sizes must preserve the validated u8 copy limit");
    }

    private static void testInstallationPlannerOmitsRuntimeServiceForPureLocalPlan() {
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10, List.of("show-secret-id"));
        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        check(!plan.runtimeStageRequired(), "standalone SID hotkey must keep HotkeyRuntimeV1 inside its single WC");
    }

    private static long resolveFirstVcall(byte[] script, int sb2Offset) {
        for (int i = 0; i + 4 < script.length; i++) {
            if ((script[i] & 0xFF) == 0xBA) {
                long encoded = (script[i+1]&0xFFL) | ((script[i+2]&0xFFL)<<8)
                        | ((script[i+3]&0xFFL)<<16) | ((script[i+4]&0xFFL)<<24);
                long callerStatic = 0x02024588L + sb2Offset;
                long addressOffset = HotkeyRuntimeV1.VIRTUAL_BASE - callerStatic;
                return (encoded - addressOffset) & 0xFFFF_FFFFL;
            }
        }
        return -1;
    }

    private static boolean containsSequence(byte[] data, byte[] needle) {
        outer: for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static boolean containsCallnative(byte[] script, long target) {
        for (int i = 0; i + 4 < script.length; i++) {
            if ((script[i] & 0xFF) == 0x23) {
                long actual = (script[i+1]&0xFFL) | ((script[i+2]&0xFFL)<<8)
                        | ((script[i+3]&0xFFL)<<16) | ((script[i+4]&0xFFL)<<24);
                if (actual == target) return true;
            }
        }
        return false;
    }

    private static int findHalfword(byte[] data, int value) {
        for (int i = 0; i + 1 < data.length; i += 2) {
            int actual = (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8);
            if (actual == value) return i;
        }
        return -1;
    }
    private static byte[] bytes(int size) {
        byte[] data = new byte[size];
        data[0] = 1;
        return data;
    }


    private static void testInstallationEmitterMatchesValidatedBaselineArtifacts() {
        RomProfile rom = RomProfile.fromId("fr10");
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(rom, List.of(
                "seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        CompositionArtifactBuilder.Build artifacts = CompositionArtifactBuilder.build(composition, 0x1234);
        SharedPersistentNativeComposition.Layout validated = SharedPersistentNativeComposition.layout(rom, 0x1234);

        check(java.util.Arrays.equals(artifacts.component("native-catalog"), validated.catalog()),
                "planned native catalog must match validated Build-27b/28 bytes");
        check(java.util.Arrays.equals(artifacts.component("seed-modifier:field"), validated.seedBody()),
                "planned Seed body must match validated bytes");
        check(java.util.Arrays.equals(artifacts.component("repel:field"), validated.repelBody()),
                "planned Repel body must match validated bytes");
        check(java.util.Arrays.equals(artifacts.component("party-iv-viewer:field"), validated.partyBridge()),
                "planned Party IV bridge must match validated bytes");
        check(java.util.Arrays.equals(artifacts.component("show-secret-id:field"), validated.sidBridge()),
                "planned SID bridge must match validated bytes");
        check(java.util.Arrays.equals(artifacts.runtime().ramScript().bytesCopy(),
                        SharedPersistentNativeComposition.buildRuntime(rom, 0x1234).ramScript().bytesCopy()),
                "planned runtime must be byte-for-byte identical to validated baseline runtime");
    }

    private static void testInstallationEmitterBuildsPlannedTwoStageSequence() {
        RomProfile rom = RomProfile.fromId("fr10");
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(rom, List.of(
                "seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        InstallationPlan plan = CompositionInstallationPlanner.plan(composition);
        InstallationEmitter.EmittedInstallation emitted = InstallationEmitter.emit(plan, 0x1234);
        check(emitted.persistentStages().size() == 2, "baseline automatic emitter should produce two persistent installers");
        check(emitted.runtime() != null, "baseline automatic emitter should produce runtime WC");
        check(emitted.persistentStages().get(0).ramScript().isChecksumValid(), "emitted installer 1 checksum must be valid");
        check(emitted.persistentStages().get(1).ramScript().isChecksumValid(), "emitted installer 2 checksum must be valid");
        check(emitted.runtime().isChecksumValid(), "emitted runtime checksum must be valid");
    }


    private static void testPlannedPersistentImageMatchesValidatedBaseline() {
        RomProfile rom = RomProfile.fromId("fr10");
        PresetCompositionPlan composition = PresetCompositionPlanner.planHotkeys(rom, List.of(
                "seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        CompositionArtifactBuilder.Build artifacts = CompositionArtifactBuilder.build(composition, 0x1234);
        byte[] sb1 = new byte[PayloadStorageArea.SAVE_BLOCK1.offset() + PayloadStorageArea.SAVE_BLOCK1.capacity() + 1];
        byte[] sb2 = new byte[PayloadStorageArea.SAVE_BLOCK2.offset() + PayloadStorageArea.SAVE_BLOCK2.capacity() + 1];
        for (InstallationWrite write : CompositionInstallationPlanner.plan(composition).writes()) {
            byte[] data = artifacts.component(write.componentId());
            byte[] target = write.target() == InstallationTarget.SAVE_BLOCK1 ? sb1 : sb2;
            System.arraycopy(data, 0, target, write.offset(), data.length);
        }
        SharedPersistentNativeComposition.Layout validated = SharedPersistentNativeComposition.layout(rom, 0x1234);
        check(java.util.Arrays.equals(java.util.Arrays.copyOfRange(sb2, 0x0B20, 0x0B20 + validated.catalog().length), validated.catalog()),
                "automatic writes must reconstruct validated native catalog");
        check(java.util.Arrays.equals(java.util.Arrays.copyOfRange(sb2, validated.seedOffset(), validated.seedOffset() + validated.seedBody().length), validated.seedBody()),
                "automatic writes must reconstruct validated Seed bytes");
        check(java.util.Arrays.equals(java.util.Arrays.copyOfRange(sb2, validated.repelOffset(), validated.repelOffset() + validated.repelBody().length), validated.repelBody()),
                "automatic writes must reconstruct validated Repel bytes");
        check(java.util.Arrays.equals(java.util.Arrays.copyOfRange(sb2, validated.partyBridgeOffset(), validated.partyBridgeOffset() + validated.partyBridge().length), validated.partyBridge()),
                "automatic writes must reconstruct validated Party IV bridge");
        check(java.util.Arrays.equals(java.util.Arrays.copyOfRange(sb2, validated.sidBridgeOffset(), validated.sidBridgeOffset() + validated.sidBridge().length), validated.sidBridge()),
                "automatic writes must reconstruct validated SID bridge");
    }


    private static void testInstallationManifestIsTaggedAndChecksummed() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        byte[] manifest = InstallationManifest.build(plan);
        check(manifest.length == 32, "installation manifest must remain 32 bytes");
        check(InstallationManifest.OFFSET == 0x0F00, "manifest must stay at the fixed SB2 tail slot");
        check(InstallationManifest.hasMagic(manifest), "fresh manifest must validate magic/version/CRC");
        manifest[12] ^= 1;
        check(!InstallationManifest.hasMagic(manifest), "manifest CRC must detect corruption");
    }

    private static void testPersistentPlannerReservesManifestTail() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10, List.of("seed-modifier", "repel"));
        InstallationPlan install = CompositionInstallationPlanner.plan(plan);
        InstallationWrite manifest = install.writes().stream()
                .filter(w -> w.componentId().equals("installation-manifest")).findFirst().orElseThrow();
        check(manifest.offset() == InstallationManifest.OFFSET && manifest.size() == InstallationManifest.SIZE,
                "persistent plans must write the manifest at the fixed reserved tail");
        check(plan.concreteLayout().allocations().stream()
                        .filter(ConcretePresetAllocation::hasSb2FieldScript)
                        .allMatch(a -> a.sb2FieldScriptOffset() + a.sb2FieldScriptSize() <= InstallationManifest.OFFSET),
                "payload placement must never overlap the manifest slot");
    }

    private static void testRunAnywhereSharedMaintenanceRoutine() {
        byte[] code = RunAnywhereSharedRuntime.maintenanceRoutine(RomProfile.FIRE_RED_EN_10);
        check(code.length == 24, "Run Anywhere maintenance routine must remain 24 bytes");
        check((code[0x10] & 0xFF) == 0x15 && (code[0x11] & 0xFF) == 0x6E
                        && (code[0x12] & 0xFF) == 0x03 && (code[0x13] & 0xFF) == 0x02,
                "maintenance routine must target gMapHeader+0x19");
    }

    private static void testRunAnywhereSharedRuntimeFitsBaseline() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        TriggerBuildResult runtime = RunAnywhereSharedRuntime.build(plan);
        check(runtime.totalScriptBytes() <= RamScript.SCRIPT_SIZE,
                "four-preset shared runtime plus Run Anywhere must fit RamScript");
        check(runtime.ramScript().isChecksumValid(),
                "Run Anywhere shared runtime must have a valid RamScript checksum");
    }

    private static void testRunAnywhereSharedRuntimeUsesNoAdditionalIwramBlock() {
        // Maintenance mode removes the old static original-VBlank literal record
        // and patches that exact existing address during the temporary bootstrap.
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "repel", "party-iv-viewer", "show-secret-id"));
        TriggerBuildResult normal = CompositionArtifactBuilder.build(plan, 0x1234).runtime();
        TriggerBuildResult run = RunAnywhereSharedRuntime.build(plan);
        check(run.totalScriptBytes() > normal.totalScriptBytes(),
                "Run Anywhere should cost RamScript/EWRAM bytes, not a new IWRAM claim");
        check(RunAnywhereSharedRuntime.maintenanceSize() == 24,
                "maintenance EWRAM code size regression");
    }

    private static void testCleanerBuildsAndFits() {
        RamScript cleaner = ToolkitCleanerPreset.build(RomProfile.FIRE_RED_EN_10);
        check(cleaner.isChecksumValid(), "Cleaner RamScript must build with a valid checksum");
        check(ToolkitCleanerPreset.helperBytesForTest(RomProfile.FIRE_RED_EN_10).length < 256,
                "Cleaner native helper should remain compact");
    }


    private static void testPartyEvSharedIntegration() {
        RomProfile rom = RomProfile.fromId("fr10");
        byte[] compact = PersistentPartyEvViewerModule.payload(rom);
        check(compact.length == 0x140, "persistent Party EV helper must fit the validated 0x140 staging window");

        PresetDefinition ev = PresetCatalog.byId("party-ev-viewer");
        check(ev.defaultHotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.UP)),
                "Party EV shared default should be R+UP");

        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                rom, List.of("party-iv-viewer", "party-ev-viewer"));
        check(plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                "IV+EV composition should reuse shared native staging");
        check(plan.concreteLayout().bindingPlan().bindings().stream()
                        .anyMatch(b -> b.presetId().equals("party-ev-viewer")
                                && b.hotkey().equals(new Hotkey(HotkeyButton.R, HotkeyButton.UP))),
                "IV+EV composition should bind Party EV to R+UP");

        InstallationPlan install = CompositionInstallationPlanner.plan(plan);
        InstallationEmitter.EmittedInstallation emitted = InstallationEmitter.emit(install, 0x1234);
        check(emitted.runtime() != null, "IV+EV composition should emit a shared runtime");
    }

    private static void testLeadViewerCompactSizes() {
        RomProfile rom = RomProfile.fromId("fr10");
        check(LeadIvNativeHelper.buildAt(rom, 0x02000000L).codeCopy().length == 272,
                "Lead IV helper should be 272 B");
        check(LeadEvNativeHelper.buildAt(rom, 0x02000000L).codeCopy().length == 284,
                "Lead EV helper should be 284 B");
        check(PersistentPartyIvViewerModule.payload(rom).length - PersistentLeadIvViewerModule.payload(rom).length == 24,
                "Lead IV should save 24 persistent native bytes");
        check(PersistentPartyEvViewerModule.payload(rom).length - PersistentLeadEvViewerModule.payload(rom).length == 36,
                "Lead EV should save 36 persistent native bytes");
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(rom, List.of("lead-iv-viewer", "lead-ev-viewer"));
        check(plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                "Lead IV+EV should share native staging");
    }


    private static void testBox14SeedSharedIntegration() {
        RomProfile rom = RomProfile.LEAF_GREEN_EN_10;
        check(PersistentBox14SeedModule.payload(rom).length == Box14SeedNativeHelper.CODE_SIZE,
                "BOX 14 persistent module must preserve the validated 72-byte helper");
        check(Box14SeedModifierPreset.buildPayload(rom).length == 309,
                "BOX 14 standalone payload size regression");

        PresetDefinition preset = PresetCatalog.byId("seed-modifier-box14");
        check(preset.isValidated(PresetUsageMode.SINGLE_HOTKEY, rom),
                "BOX 14 standalone LG1.0 path must record the in-game validation");
        check(preset.isValidated(PresetUsageMode.SHARED_N_HOTKEY, rom),
                "BOX 14 shared LG1.0 path must record the in-game BOX14 + Party IV validation");
        check(preset.supports(RomProfile.FIRE_RED_EN_10),
                "BOX 14 RFU-normalized preset must expose the symbol-verified FR1.0 port");
        check(preset.validationStatus(PresetUsageMode.SINGLE_HOTKEY, RomProfile.FIRE_RED_EN_10)
                        == PresetValidationStatus.SUPPORTED_NOT_TESTED,
                "BOX 14 FR1.0 port must not overclaim in-game validation");

        PresetCompositionPlan local = PresetCompositionPlanner.planHotkeys(rom, List.of("seed-modifier-box14"));
        check(local.ramScriptBytes() == 677 && local.ramScriptFree() == 318,
                "BOX 14 standalone runtime must remain 677/995 B");

        PresetCompositionPlan pair = PresetCompositionPlanner.planHotkeys(
                rom, List.of("seed-modifier-box14", "party-iv-viewer"));
        check(pair.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE),
                "BOX 14 + Party IV must reuse the existing shared native staging service");
        check(pair.concreteLayout().nativeCatalogSize() > 0,
                "BOX 14 + Party IV must emit one shared native catalog");
        check(pair.selections().stream().filter(x -> x.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE).count() == 2,
                "BOX 14 + Party IV must contain exactly two persistent native modules");

        CompositionArtifactBuilder.Build built = CompositionArtifactBuilder.build(pair, 0);
        check(built.component("seed-modifier-box14:field").length == PersistentBox14SeedBridge.fieldScriptSize(rom),
                "BOX 14 shared field bytes must match planner metadata");
        check(built.component("native-catalog").length == pair.concreteLayout().nativeCatalogSize(),
                "BOX 14 shared native catalog size must match the concrete layout");
    }

    private static void testMuteMusicProductionLayout() {
        RomProfile rom = RomProfile.LEAF_GREEN_EN_10;
        NativeHelper helper = MuteMusicPreset.buildHelper(rom, 0x02000000L);
        byte[] code = helper.codeCopy();

        check(helper.size() == 68, "Mute Music VolumeControl helper must remain 68 bytes");
        check(readU32(code, 0x38) == rom.gDisableMusic,
                "Mute Music helper must reference profiled gDisableMusic");
        check(readU32(code, 0x3C) == rom.gMPlayInfoBgm,
                "Mute Music helper must reference profiled gMPlayInfo_BGM");
        check(readU32(code, 0x40) == (rom.m4aMPlayVolumeControlThumb | 1L),
                "Mute Music helper must call profiled Thumb m4aMPlayVolumeControl");
        check((code[0x1A] & 0xFF) == 0x00 && (code[0x1B] & 0xFF) == 0xF0
                        && (code[0x1C] & 0xFF) == 0x0B && (code[0x1D] & 0xFF) == 0xF8,
                "Mute Music OFF path must use BL to the local ARMv4T-safe thunk");
        check((code[0x2E] & 0xFF) == 0x00 && (code[0x2F] & 0xFF) == 0xF0
                        && (code[0x30] & 0xFF) == 0x01 && (code[0x31] & 0xFF) == 0xF8,
                "Mute Music ON path must use BL to the local ARMv4T-safe thunk");
        check((code[0x34] & 0xFF) == 0x18 && (code[0x35] & 0xFF) == 0x47,
                "Mute Music helper must end calls through bx r3 thunk");

        for (RomProfile profile : RomProfile.values()) {
            NativeHelper profiled = MuteMusicPreset.buildHelper(profile, 0x02000000L);
            byte[] profiledCode = profiled.codeCopy();
            check(readU32(profiledCode, 0x38) == profile.gDisableMusic,
                    "Mute Music gDisableMusic literal must follow RomProfile: " + profile.id());
            check(readU32(profiledCode, 0x3C) == profile.gMPlayInfoBgm,
                    "Mute Music gMPlayInfo_BGM literal must follow RomProfile: " + profile.id());
            check(readU32(profiledCode, 0x40) == (profile.m4aMPlayVolumeControlThumb | 1L),
                    "Mute Music VolumeControl literal must follow RomProfile: " + profile.id());
            TriggerBuildResult result = MuteMusicPreset.build(profile);
            check(result.totalScriptBytes() <= RamScript.SCRIPT_SIZE,
                    "Mute Music production payload must fit RamScript: " + profile.id());
        }
    }

    private static long readU32(byte[] data, int offset) {
        return (data[offset] & 0xFFL)
                | ((data[offset + 1] & 0xFFL) << 8)
                | ((data[offset + 2] & 0xFFL) << 16)
                | ((data[offset + 3] & 0xFFL) << 24);
    }

    private static void check(boolean condition, String message) {
        tests++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(Runnable action, String message) {
        tests++;
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
