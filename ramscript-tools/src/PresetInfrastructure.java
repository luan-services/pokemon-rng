import java.util.Set;

/* Shared implementation infrastructure required by a deployment.

   Infrastructure and owned resources are related but not identical. Runtime,
   SB1/SB2 services are shareable infrastructure; some infrastructure variants
   also claim a fixed/session-resident resource that the planner must arbitrate.
*/
enum PresetInfrastructure {
    HOTKEY_RUNTIME,
    SHARED_HOTKEY_RUNTIME,
    SHARED_NATIVE_STAGING_SERVICE,
    SB1_GATEWAY,
    SB2_NATIVE_CATALOG,
    DEDICATED_IWRAM_CONTINUATION,
    RUN_ANYWHERE_EWRAM_SIDECAR(PresetOwnedResource.MOBILITY_EWRAM_SIDECAR),
    RUN_BIKE_ANYWHERE_EWRAM_SIDECAR(PresetOwnedResource.MOBILITY_EWRAM_SIDECAR);

    private final Set<PresetOwnedResource> ownedResources;

    PresetInfrastructure(PresetOwnedResource... resources) {
        this.ownedResources = resources.length == 0 ? Set.of() : Set.of(resources);
    }

    Set<PresetOwnedResource> ownedResources() {
        return ownedResources;
    }
}
