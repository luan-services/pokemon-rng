import java.util.List;
import java.util.Set;

/* Planner-facing preset metadata.

   This class deliberately contains no placement policy. It says what a preset
   IS and which already-known deployment mechanisms it supports. The planner
   will later choose among those mechanisms based on the selected preset set,
   available memory, hotkeys and shared infrastructure costs.
*/
record PresetDefinition(
        String id,
        String displayName,
        PresetPayloadType payloadType,
        boolean hotkeyCapable,
        boolean persistentPreferred,
        Hotkey defaultHotkey,
        Set<RomProfile> supportedRoms,
        List<PresetDeploymentDefinition> deployments,
        String notes
) {
    PresetDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("preset id must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("preset name must not be blank");
        if (payloadType == null || supportedRoms == null || deployments == null) {
            throw new IllegalArgumentException("preset definition fields must not be null");
        }
        supportedRoms = Set.copyOf(supportedRoms);
        deployments = List.copyOf(deployments);
        if (supportedRoms.isEmpty()) throw new IllegalArgumentException("preset must support at least one ROM profile");
        if (deployments.isEmpty()) throw new IllegalArgumentException("preset must expose at least one deployment mode");
        if (hotkeyCapable && defaultHotkey == null) throw new IllegalArgumentException("hotkey-capable preset needs a default hotkey");
        if (!hotkeyCapable && defaultHotkey != null) throw new IllegalArgumentException("non-hotkey preset must not declare a default hotkey");
        notes = notes == null ? "" : notes;
    }

    boolean supports(RomProfile rom) {
        return supportedRoms.contains(rom);
    }

    PresetDeploymentDefinition deployment(PresetDeploymentKind kind) {
        for (PresetDeploymentDefinition deployment : deployments) {
            if (deployment.kind() == kind) return deployment;
        }
        throw new IllegalArgumentException(id + " does not support deployment " + kind);
    }

    boolean supportsDeployment(PresetDeploymentKind kind) {
        for (PresetDeploymentDefinition deployment : deployments) {
            if (deployment.kind() == kind) return true;
        }
        return false;
    }
}
