/* Describes one RAM/code region used by a native effect.

   "validated" means the region was manually smoke-tested for the current
   FireRed EN 1.0 seed-hotkey implementation. It does NOT imply the address is
   safe for unrelated payloads or for another ROM build. */
record RuntimeRegion(
        String name,
        long start,
        int size,
        String purpose,
        boolean validated
) {
    long endInclusive() {
        return start + size - 1L;
    }
}
