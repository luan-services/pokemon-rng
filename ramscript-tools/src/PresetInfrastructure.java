/* Shared infrastructure that a deployment may require.
   A future plan should union these requirements across all selected presets,
   so shared costs are paid once rather than once per preset.
*/
enum PresetInfrastructure {
    HOTKEY_RUNTIME,
    SHARED_HOTKEY_RUNTIME,
    SHARED_NATIVE_STAGING_SERVICE,
    SB1_GATEWAY,
    SB2_NATIVE_CATALOG,
    DEDICATED_IWRAM_CONTINUATION
}
