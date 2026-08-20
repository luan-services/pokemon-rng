import java.util.Arrays;

/*
   Small piece of Thumb code staged into a known-safe IWRAM scratch region
   immediately before callnative.

   NativeHelper is intentionally tiny: it does not own a trigger and it does
   not imply HotkeyRuntimeV1. A normal deliveryman Field Script can install,
   call, and return from a helper inline.
*/
final class NativeHelper {
    private final long stagingAddress;
    private final byte[] code;

    NativeHelper(long stagingAddress, byte[] code) {
        if ((stagingAddress & 1L) != 0) {
            throw new IllegalArgumentException("stagingAddress must be even");
        }
        if (code == null || code.length == 0) {
            throw new IllegalArgumentException("code must not be empty");
        }
        this.stagingAddress = stagingAddress;
        this.code = Arrays.copyOf(code, code.length);
    }

    long stagingAddress() {
        return stagingAddress;
    }

    long thumbEntryAddress() {
        return stagingAddress | 1L;
    }

    int size() {
        return code.length;
    }

    byte[] codeCopy() {
        return Arrays.copyOf(code, code.length);
    }

    RamScriptBuilder install(RamScriptBuilder builder) {
        return builder.writeBytes(stagingAddress, code);
    }

    RamScriptBuilder installAndCall(RamScriptBuilder builder) {
        return install(builder).callNative(thumbEntryAddress());
    }
}
