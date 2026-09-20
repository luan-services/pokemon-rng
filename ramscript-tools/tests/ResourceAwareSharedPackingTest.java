import java.util.List;

/* Production regression for the first resource-aware Shared-local fallback.
   The preferred persistent layout must stay unchanged while it fits; only a
   capacity failure may move an explicitly relocation-safe payload into the
   final Runtime RamScript. */
final class ResourceAwareSharedPackingTest {
    private ResourceAwareSharedPackingTest() {}

    static void runAll() {
        preferredPersistentPathRemainsSelectedWhenItFits();
        showSidFallsBackToRuntimeWhenSb2IsFull();
        relocatedShowSidUsesItsActualRuntimeOffset();
        localShowSidUsesStrictButtonWait();
        localShowSidDoesNotOverwriteSharedWrapper();
        fallbackMaterializesAValidRuntime();
        seedRepelMuteAdvertiseSharedLocalFallbacks();
        plannerCanChooseRepelLocal();
        plannerCanChooseSeedLocal();
        seedLocalUsesRequestedBuildParameter();
        relocatedSeedRepelMuteUseActualRuntimeBase();
        explicitMuteLocalMaterializerBuilds();
    }

    private static void preferredPersistentPathRemainsSelectedWhenItFits() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "show-secret-id", "party-iv-viewer"));
        PresetCompositionPlan.SelectedPresetDeployment sid = selection(plan, "show-secret-id");
        require(sid.deployment().kind() == PresetDeploymentKind.SHARED_PERSISTENT_NATIVE,
                "Show SID preferred persistent deployment changed even though SB2 still fits");
    }

    private static void showSidFallsBackToRuntimeWhenSb2IsFull() {
        PresetCompositionPlan plan = crowdedPlan();
        PresetCompositionPlan.SelectedPresetDeployment sid = selection(plan, "show-secret-id");
        require(sid.deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT,
                "crowded composition did not select Show SID Shared-local fallback");
        require(plan.ramScriptBytes() <= RamScript.SCRIPT_SIZE, "fallback overflowed RamScript");
        require(plan.sb2Bytes() <= PayloadStorageArea.SAVE_BLOCK2.capacity(), "fallback overflowed SB2");
        require(plan.hotkeyBindings() == 3, "expected three Shared hotkeys");
        require(plan.diagnostics().stream().anyMatch(s -> s.contains("show-secret-id")),
                "plan did not explain the Shared-local fallback");
    }

    private static void relocatedShowSidUsesItsActualRuntimeOffset() {
        PresetCompositionPlan plan = crowdedPlan();
        boolean runAnywhere = plan.infrastructure().contains(PresetInfrastructure.RUN_ANYWHERE_EWRAM_SIDECAR);
        boolean runBikeAnywhere = plan.infrastructure().contains(PresetInfrastructure.RUN_BIKE_ANYWHERE_EWRAM_SIDECAR);
        boolean nativeService = plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE);
        List<String> locals = plan.selections().stream()
                .filter(item -> item.deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)
                .map(item -> item.preset().id()).toList();
        SharedRuntimeSupportLayout layout = SharedRuntimeSupportLayout.build(
                plan.rom(), plan.hotkeyBindings(), runAnywhere, runBikeAnywhere, locals, nativeService, 0x140);
        int offset = layout.localPayloadOffset("show-secret-id");
        require(offset > SharedHotkeyRuntime.PAYLOAD_OFFSET, "Show SID local offset was not placed after dispatcher");
        byte[] sid = ShowSecretIdPreset.buildScriptAtOffset(plan.rom(), offset);
        require((sid[0] & 0xFF) == 0xB8, "relocated Show SID must start with setvaddress");
        long embedded = u32(sid, 1);
        long expected = (HotkeyRuntimeV1.VIRTUAL_BASE + Integer.toUnsignedLong(offset)) & 0xFFFF_FFFFL;
        require(embedded == expected,
                "relocated Show SID setvaddress does not match actual Runtime offset");
    }

    private static void localShowSidUsesStrictButtonWait() {
        PresetCompositionPlan plan = crowdedPlan();
        boolean runAnywhere = plan.infrastructure().contains(PresetInfrastructure.RUN_ANYWHERE_EWRAM_SIDECAR);
        boolean runBikeAnywhere = plan.infrastructure().contains(PresetInfrastructure.RUN_BIKE_ANYWHERE_EWRAM_SIDECAR);
        boolean nativeService = plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE);
        List<String> locals = plan.selections().stream()
                .filter(item -> item.deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)
                .map(item -> item.preset().id()).toList();
        SharedRuntimeSupportLayout layout = SharedRuntimeSupportLayout.build(
                plan.rom(), plan.hotkeyBindings(), runAnywhere, runBikeAnywhere, locals, nativeService, 0x140);
        byte[] sid = ShowSecretIdPreset.buildScriptAtOffset(plan.rom(), layout.localPayloadOffset("show-secret-id"));
        require(contains(sid, new byte[] {0x6D, 0x00, 0x6B, 0x02}),
                "Show SID local path must use strict waitbuttonpress before releaseall/end");
    }


    private static void localShowSidDoesNotOverwriteSharedWrapper() {
        PresetCompositionPlan plan = crowdedPlan();
        boolean runAnywhere = plan.infrastructure().contains(PresetInfrastructure.RUN_ANYWHERE_EWRAM_SIDECAR);
        boolean runBikeAnywhere = plan.infrastructure().contains(PresetInfrastructure.RUN_BIKE_ANYWHERE_EWRAM_SIDECAR);
        boolean nativeService = plan.infrastructure().contains(PresetInfrastructure.SHARED_NATIVE_STAGING_SERVICE);
        List<String> locals = plan.selections().stream()
                .filter(item -> item.deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT)
                .map(item -> item.preset().id()).toList();
        SharedRuntimeSupportLayout layout = SharedRuntimeSupportLayout.build(
                plan.rom(), plan.hotkeyBindings(), runAnywhere, runBikeAnywhere, locals, nativeService, 0x140);
        byte[] sid = ShowSecretIdPreset.buildScriptAtOffset(plan.rom(), layout.localPayloadOffset("show-secret-id"));
        byte[] wrapperAddress = new byte[] {0x10, 0x53, 0x00, 0x03};
        require(!contains(sid, wrapperAddress),
                "Show SID Shared-local path must not stage its native helper over the resident Shared wrapper at 03005310");
        long safeScratch = plan.rom().stringVar4 + 0x100L;
        byte[] expected = new byte[] {(byte)safeScratch, (byte)(safeScratch >>> 8), (byte)(safeScratch >>> 16), (byte)(safeScratch >>> 24)};
        require(contains(sid, expected),
                "Show SID Shared-local path must stage its helper in temporary EWRAM scratch");
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer: for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static void fallbackMaterializesAValidRuntime() {
        PresetCompositionPlan plan = crowdedPlan();
        CompositionArtifactBuilder.Build build = CompositionArtifactBuilder.build(plan, 0x0001234);
        require(build.runtime() != null, "fallback composition did not materialize final Shared runtime");
        require(build.runtime().totalScriptBytes() == plan.ramScriptBytes(),
                "materialized runtime size differs from planner accounting");
        require(build.runtime().ramScript().isChecksumValid(), "materialized fallback RamScript checksum invalid");
    }

    private static PresetCompositionPlan crowdedPlan() {
        return PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("party-iv-viewer", "party-ev-viewer", "show-secret-id"));
    }


    private static void seedRepelMuteAdvertiseSharedLocalFallbacks() {
        for (String id : List.of("seed-modifier", "repel", "mute-music")) {
            require(PresetCatalog.byId(id).supportsDeployment(PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT),
                    id + " must advertise an explicit Shared-local fallback");
        }
    }

    private static void plannerCanChooseRepelLocal() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("repel", "party-iv-viewer", "party-ev-viewer"));
        require(selection(plan, "repel").deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT,
                "planner did not choose Repel local fallback when persistent SB2 was over capacity");
        CompositionArtifactBuilder.Build build = CompositionArtifactBuilder.build(plan, 0);
        require(build.runtime() != null && build.runtime().ramScript().isChecksumValid(),
                "Repel local fallback did not materialize a valid runtime");
    }

    private static void plannerCanChooseSeedLocal() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "lead-iv-viewer", "party-ev-viewer", "run-bike-anywhere"));
        require(selection(plan, "seed-modifier").deployment().kind() == PresetDeploymentKind.SHARED_LOCAL_FIELD_SCRIPT,
                "planner did not choose Seed local fallback when it was the best feasible advertised placement");
        CompositionArtifactBuilder.Build build = CompositionArtifactBuilder.build(plan, 0xB5B1E7AD);
        require(build.runtime() != null && build.runtime().ramScript().isChecksumValid(),
                "Seed local fallback did not materialize a valid runtime");
    }

    private static void seedLocalUsesRequestedBuildParameter() {
        PresetCompositionPlan plan = PresetCompositionPlanner.planHotkeys(
                RomProfile.FIRE_RED_EN_10,
                List.of("seed-modifier", "lead-iv-viewer", "party-ev-viewer", "run-bike-anywhere"));
        byte[] a = CompositionArtifactBuilder.build(plan, 0x00001234).runtime().ramScript().scriptCopy();
        byte[] b = CompositionArtifactBuilder.build(plan, 0xB5B1E7AD).runtime().ramScript().scriptCopy();
        require(!java.util.Arrays.equals(a, b),
                "Shared-local Seed runtime ignored the requested seed build parameter");
    }

    private static void relocatedSeedRepelMuteUseActualRuntimeBase() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        int offset = 0x180;
        long expected = (HotkeyRuntimeV1.VIRTUAL_BASE + Integer.toUnsignedLong(offset)) & 0xFFFF_FFFFL;

        byte[] seed = SeedModifierPreset.buildPayloadAtOffset(rom, 0x12345678, offset);
        byte[] repel = RepelHotkeyPreset.buildPayloadAtOffset(offset);
        byte[] mute = MuteMusicPreset.buildPayloadAtOffset(rom, offset);
        for (byte[] payload : List.of(seed, repel, mute)) {
            require((payload[0] & 0xFF) == 0xB8, "relocated Shared-local payload must begin with setvaddress");
            require(u32(payload, 1) == expected, "relocated Shared-local payload uses the wrong virtual base");
        }

        // Mute's CPU_SET_BLOCK copier embeds a virtual source pointer derived
        // from the relocated base. Building at a different offset must alter
        // more than the leading setvaddress literal.
        byte[] mute0 = MuteMusicPreset.buildPayloadAtOffset(rom, 0);
        require(!java.util.Arrays.equals(mute0, mute),
                "Mute Shared-local payload did not relocate its embedded native-copy source");
    }


    private static void explicitMuteLocalMaterializerBuilds() {
        RomProfile rom = RomProfile.FIRE_RED_EN_10;
        SharedRuntimeSupportLayout layout = SharedRuntimeSupportLayout.build(
                rom, 2, false, false, List.of("mute-music"), false, 0x140, 0);
        int offset = layout.localPayloadOffset("mute-music");
        require(offset > SharedHotkeyRuntime.PAYLOAD_OFFSET, "Mute local payload offset missing");
        byte[] mute = MuteMusicPreset.buildPayloadAtOffset(rom, offset);
        require(layout.support().length >= mute.length, "Mute local support bytes were not packed");
        var entries = List.of(
                new SharedHotkeyDispatcher.Entry(HotkeyButton.DOWN, offset),
                new SharedHotkeyDispatcher.Entry(HotkeyButton.B, offset));
        TriggerBuildResult runtime = SharedHotkeyRuntime.compose(
                rom, HotkeyButton.R, entries, layout.support(), layout.alignment());
        require(runtime.ramScript().isChecksumValid(), "explicit Mute Shared-local runtime checksum invalid");
    }

    private static PresetCompositionPlan.SelectedPresetDeployment selection(PresetCompositionPlan plan, String id) {
        return plan.selections().stream().filter(item -> item.preset().id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("missing selection " + id));
    }

    private static long u32(byte[] data, int offset) {
        return Integer.toUnsignedLong((data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
