import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

/* One supported way to deploy a preset. */
record PresetDeploymentDefinition(
        PresetDeploymentKind kind,
        Set<PresetInfrastructure> infrastructure,
        Set<RomProfile> validatedOn,
        Function<RomProfile, PresetDeploymentCost> costFactory,
        String notes
) {
    PresetDeploymentDefinition {
        if (kind == null || infrastructure == null || validatedOn == null || costFactory == null) {
            throw new IllegalArgumentException("deployment definition fields must not be null");
        }
        infrastructure = Set.copyOf(infrastructure);
        validatedOn = Set.copyOf(validatedOn);
        notes = notes == null ? "" : notes;
    }

    PresetDeploymentCost cost(RomProfile rom) {
        if (rom == null) throw new IllegalArgumentException("ROM profile must not be null");
        return costFactory.apply(rom);
    }

    boolean isValidatedOn(RomProfile rom) {
        return validatedOn.contains(rom);
    }

    static Set<PresetInfrastructure> infra(PresetInfrastructure... values) {
        if (values.length == 0) return Set.of();
        EnumSet<PresetInfrastructure> out = EnumSet.noneOf(PresetInfrastructure.class);
        for (PresetInfrastructure value : values) out.add(value);
        return out;
    }
}
