/* User-facing activation intent. Runtime family is an implementation detail:
   one HOTKEY selection uses HotkeyRuntimeV1; 2..8 use SharedHotkeyRuntime. */
enum PresetActivation {
    DELIVERYMAN,
    HOTKEY
}
