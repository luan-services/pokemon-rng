/* High-level deployment choice for a preset.

   SIMPLE_RAMSCRIPT preserves the original, validated one-Wonder-Card path.
   PERSISTENT_MODULE opts into SaveBlock-backed installation.
   AUTO is reserved for callers that explicitly want the planner to choose.

   Existing preset commands do not use AUTO implicitly. This is deliberate:
   discovering persistent storage must not make old simple presets more complex.
*/
enum PresetDeploymentMode {
    SIMPLE_RAMSCRIPT,
    PERSISTENT_MODULE,
    AUTO
}
