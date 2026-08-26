/* Deployment mechanisms that are independently selectable by the planner.

   These describe HOW a preset is deployed, not which trigger the user chose.
   Keeping them explicit preserves the project's old/simple paths instead of
   forcing every preset through the newest persistent architecture.
*/
enum PresetDeploymentKind {
    DELIVERYMAN_LOCAL,
    DEDICATED_LOCAL,
    HOTKEY_LOCAL,
    SHARED_PERSISTENT_FIELD_SCRIPT,
    SHARED_PERSISTENT_NATIVE
}
