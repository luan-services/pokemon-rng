/* UI-facing selection/composition policy.

   COMPOSABLE presets may participate in normal multi-preset planning.
   EXCLUSIVE presets currently require a single-preset build. This describes
   the supported production deployment today, not a permanent limitation of
   the underlying mechanic.
*/
enum PresetSelectionPolicy {
    COMPOSABLE,
    EXCLUSIVE
}
