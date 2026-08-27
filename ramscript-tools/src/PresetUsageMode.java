/* User-facing execution families tracked independently from payload placement.

   A preset can support the same usage mode through different deployment/storage
   mechanisms. Validation is deliberately recorded per ROM/version so the UI
   never turns symbol/profile support into an in-game validation claim.
*/
enum PresetUsageMode {
    DELIVERYMAN,
    OBJECT_EVENT,
    SINGLE_HOTKEY,
    LEGACY_MULTI_HOTKEY,
    SHARED_N_HOTKEY
}
