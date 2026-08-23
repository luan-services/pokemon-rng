/* Historical deployment policy above RawPayloadPlacementPlanner.

   Important compatibility rule:
     SIMPLE_RAMSCRIPT is not shorthand for AUTO. It means "keep the old path".

   Kept only for rejected/research native-module code. Production Build-16
   Field Script composition uses PayloadPlacementPlanner instead.
*/
final class PresetDeploymentPlanner {
    private PresetDeploymentPlanner() {}

    static PlacedPayload place(PresetPayload payload, PresetDeploymentMode mode, int availableRamScriptBytes) {
        if (payload == null) throw new IllegalArgumentException("payload must not be null");
        if (mode == null) throw new IllegalArgumentException("deployment mode must not be null");

        return switch (mode) {
            case SIMPLE_RAMSCRIPT -> RawPayloadPlacementPlanner.place(payload.bytes(), PayloadPlacement.RAMSCRIPT, availableRamScriptBytes);
            case PERSISTENT_MODULE -> {
                if (payload.kind() != PresetPayloadKind.THUMB) {
                    throw new IllegalArgumentException("persistent deployment currently requires a THUMB payload");
                }
                // Prefer the smaller validated persistent region first.
                yield RawPayloadPlacementPlanner.place(payload.bytes(), PayloadPlacement.AUTO, 0);
            }
            case AUTO -> {
                // AUTO keeps field scripts in the legacy execution model.
                if (payload.kind() == PresetPayloadKind.FIELD_SCRIPT) {
                    yield RawPayloadPlacementPlanner.place(payload.bytes(), PayloadPlacement.RAMSCRIPT, availableRamScriptBytes);
                }
                yield RawPayloadPlacementPlanner.place(payload.bytes(), PayloadPlacement.AUTO, availableRamScriptBytes);
            }
        };
    }
}
