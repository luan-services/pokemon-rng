/* A user-facing native effect.

   Native effects are deliberately separate from normal Field Scripts.
   Implementations may install Thumb/ARM runtime code into RAM and therefore
   can be ROM-profile dependent.

   The first validated implementation is SeedHotkeyEffect. */
interface NativeEffect {
    String id();
    String displayName();
    String description();
    RamScript build();
}
