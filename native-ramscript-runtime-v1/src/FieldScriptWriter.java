import java.io.ByteArrayOutputStream;

final class FieldScriptWriter {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    FieldScriptWriter setPtr(int value, long address) {
        u8(0x11);
        u8(value);
        u32(address);
        return this;
    }

    FieldScriptWriter writeBytes(long address, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            setPtr(Byte.toUnsignedInt(bytes[i]), address + i);
        }

        return this;
    }

    FieldScriptWriter callNative(long address) {
        u8(0x23);
        u32(address);
        return this;
    }

    FieldScriptWriter returnRam() {
        u8(0x0C);
        return this;
    }

    byte[] build() {
        return output.toByteArray();
    }

    private void u8(int value) {
        output.write(value & 0xFF);
    }

    private void u32(long value) {
        output.write((int) value & 0xFF);
        output.write((int) (value >>> 8) & 0xFF);
        output.write((int) (value >>> 16) & 0xFF);
        output.write((int) (value >>> 24) & 0xFF);
    }
}
