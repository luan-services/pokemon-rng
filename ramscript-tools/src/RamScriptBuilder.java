import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Builds relocatable FireRed/LeafGreen field scripts for the RamScript slot.

   Saved Wonder Card scripts use the normal field-script engine, but their
   pointers cannot depend on the physical RAM address where the save is loaded.
   setvaddress + the v* commands solve this by using a virtual address space.

   This builder writes labels first and patches their virtual addresses after
   the complete code/data layout is known.
*/
final class RamScriptBuilder {
    static final int COND_LT = 0;
    static final int COND_EQ = 1;
    static final int COND_GT = 2;
    static final int COND_LE = 3;
    static final int COND_GE = 4;
    static final int COND_NE = 5;

    private final long virtualBase;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Map<String, Integer> labels = new LinkedHashMap<>();
    private final List<PointerFixup> fixups = new ArrayList<>();

    RamScriptBuilder(long virtualBase) {
        if (virtualBase < 0 || virtualBase > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException("virtualBase must fit in u32");
        }
        this.virtualBase = virtualBase;
    }

    int position() {
        return output.size();
    }

    RamScriptBuilder label(String name) {
        if (labels.putIfAbsent(name, position()) != null) {
            throw new IllegalArgumentException("Duplicate label: " + name);
        }
        return this;
    }

    /* --- Relocatable control flow --- */

    RamScriptBuilder setVAddress() {
        opcode(0xB8);
        u32(virtualBase);
        return this;
    }

    /* Re-establish the same relocation offset from a non-zero position in
       the script. ScrCmd_setvaddress computes addr2 - currentOpcodeAddress,
       so a reset emitted at +N must embed virtualBase + N rather than the
       script-start virtualBase. */
    RamScriptBuilder setVAddressHere() {
        int opcodeOffset = position();
        opcode(0xB8);
        u32((virtualBase + Integer.toUnsignedLong(opcodeOffset)) & 0xFFFF_FFFFL);
        return this;
    }

    RamScriptBuilder vGoto(String label) {
        opcode(0xB9);
        labelPointer(label);
        return this;
    }

    RamScriptBuilder vCall(String label) {
        opcode(0xBA);
        labelPointer(label);
        return this;
    }

    RamScriptBuilder vCallAddress(long virtualAddress) {
        opcode(0xBA);
        u32(virtualAddress);
        return this;
    }

    RamScriptBuilder vGotoIf(int condition, String label) {
        requireCondition(condition);
        opcode(0xBB);
        u8(condition);
        labelPointer(label);
        return this;
    }

    RamScriptBuilder vCallIf(int condition, String label) {
        requireCondition(condition);
        opcode(0xBC);
        u8(condition);
        labelPointer(label);
        return this;
    }

    RamScriptBuilder vGotoIfEqual(String label) {
        return vGotoIf(COND_EQ, label);
    }

    RamScriptBuilder vGotoIfNotEqual(String label) {
        return vGotoIf(COND_NE, label);
    }

    RamScriptBuilder returnScript() {
        opcode(0x03);
        return this;
    }

    RamScriptBuilder end() {
        opcode(0x02);
        return this;
    }

    RamScriptBuilder returnRam() {
        opcode(0x0C);
        return this;
    }

    RamScriptBuilder endRam() {
        opcode(0x0D);
        return this;
    }

    RamScriptBuilder nop() {
        opcode(0x00);
        return this;
    }

    /* --- Variables / comparisons / flags --- */

    RamScriptBuilder setVar(int variable, int value) {
        opcode(0x16);
        u16(variable);
        u16(value);
        return this;
    }

    RamScriptBuilder addVar(int variable, int value) {
        opcode(0x17);
        u16(variable);
        u16(value);
        return this;
    }

    RamScriptBuilder subVar(int variable, int valueOrVariable) {
        opcode(0x18);
        u16(variable);
        u16(valueOrVariable);
        return this;
    }

    RamScriptBuilder copyVar(int destination, int source) {
        opcode(0x19);
        u16(destination);
        u16(source);
        return this;
    }

    RamScriptBuilder setOrCopyVar(int variable, int valueOrVariable) {
        opcode(0x1A);
        u16(variable);
        u16(valueOrVariable);
        return this;
    }

    RamScriptBuilder compareVarToValue(int variable, int value) {
        opcode(0x21);
        u16(variable);
        u16(value);
        return this;
    }

    RamScriptBuilder compareVarToVar(int firstVariable, int secondVariable) {
        opcode(0x22);
        u16(firstVariable);
        u16(secondVariable);
        return this;
    }

    RamScriptBuilder setFlag(int flag) {
        opcode(0x29);
        u16(flag);
        return this;
    }

    RamScriptBuilder clearFlag(int flag) {
        opcode(0x2A);
        u16(flag);
        return this;
    }

    RamScriptBuilder checkFlag(int flag) {
        opcode(0x2B);
        u16(flag);
        return this;
    }

    RamScriptBuilder random(int maxOrVariable) {
        opcode(0x8F);
        u16(maxOrVariable);
        return this;
    }

    /* --- Arbitrary byte writes for native-helper staging --- */

    RamScriptBuilder setPtr(int value, long address) {
        opcode(0x11);
        u8(value);
        u32(address);
        return this;
    }

    RamScriptBuilder writeBytes(long address, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            setPtr(Byte.toUnsignedInt(bytes[i]), address + i);
        }
        return this;
    }

    /* --- Items --- */

    RamScriptBuilder addItem(int itemOrVariable, int amountOrVariable) {
        opcode(0x44);
        u16(itemOrVariable);
        u16(amountOrVariable);
        return this;
    }

    RamScriptBuilder removeItem(int itemOrVariable, int amountOrVariable) {
        opcode(0x45);
        u16(itemOrVariable);
        u16(amountOrVariable);
        return this;
    }

    RamScriptBuilder checkItemSpace(int itemOrVariable, int amountOrVariable) {
        opcode(0x46);
        u16(itemOrVariable);
        u16(amountOrVariable);
        return this;
    }

    RamScriptBuilder checkItem(int itemOrVariable, int amountOrVariable) {
        opcode(0x47);
        u16(itemOrVariable);
        u16(amountOrVariable);
        return this;
    }

    RamScriptBuilder callStd(int stdScript) {
        opcode(0x09);
        u8(stdScript);
        return this;
    }

    /* Convenience macro matching the event.inc giveitem macro used by the game. */
    RamScriptBuilder giveItem(int item, int amount) {
        return setOrCopyVar(0x8000, item)
                .setOrCopyVar(0x8001, amount)
                .callStd(0x00); // STD_OBTAIN_ITEM
    }

    /* --- Pokémon --- */

    RamScriptBuilder giveEgg(int speciesOrVariable) {
        opcode(0x7A);
        u16(speciesOrVariable);
        return this;
    }

    /* Direct wrapper for ScrCmd_givemon.
       unkParam1/unkParam2/unkParam3 are intentionally left named as in the
       decomp until we establish their exact semantics from ScriptGiveMon(). */
    RamScriptBuilder giveMon(
            int speciesOrVariable,
            int level,
            int heldItemOrVariable,
            long unkParam1,
            long unkParam2,
            int unkParam3
    ) {
        opcode(0x79);
        u16(speciesOrVariable);
        u8(level);
        u16(heldItemOrVariable);
        u32(unkParam1);
        u32(unkParam2);
        u8(unkParam3);
        return this;
    }

    RamScriptBuilder bufferSpeciesName(int stringVarIndex, int speciesOrVariable) {
        opcode(0x7D);
        u8(stringVarIndex);
        u16(speciesOrVariable);
        return this;
    }

    RamScriptBuilder bufferItemName(int stringVarIndex, int itemOrVariable) {
        opcode(0x80);
        u8(stringVarIndex);
        u16(itemOrVariable);
        return this;
    }

    RamScriptBuilder bufferNumberString(int stringVarIndex, int valueOrVariable) {
        opcode(0x83);
        u8(stringVarIndex);
        u16(valueOrVariable);
        return this;
    }

    /* --- Money --- */

    RamScriptBuilder addMoney(long amount) {
        opcode(0x90);
        u32(amount);
        u8(0);
        return this;
    }

    RamScriptBuilder removeMoney(long amount) {
        opcode(0x91);
        u32(amount);
        u8(0);
        return this;
    }

    RamScriptBuilder checkMoney(long amount) {
        opcode(0x92);
        u32(amount);
        u8(0);
        return this;
    }

    /* --- Native functions already exposed by the script engine --- */

    RamScriptBuilder special(int specialId) {
        opcode(0x25);
        u16(specialId);
        return this;
    }

    RamScriptBuilder specialVar(int destinationVariable, int specialId) {
        opcode(0x26);
        u16(destinationVariable);
        u16(specialId);
        return this;
    }

    RamScriptBuilder waitState() {
        opcode(0x27);
        return this;
    }

    /* These use absolute function addresses and are therefore ROM-build dependent.
       They are primitives for later native-code research; normal custom events
       should prefer regular script commands or special/specialvar. */
    RamScriptBuilder callNative(long functionAddress) {
        opcode(0x23);
        u32(functionAddress);
        return this;
    }

    RamScriptBuilder gotoNative(long functionAddress) {
        opcode(0x24);
        u32(functionAddress);
        return this;
    }

    /* --- UI / sound / field state --- */

    RamScriptBuilder lock() {
        opcode(0x6A);
        return this;
    }

    RamScriptBuilder lockAll() {
        opcode(0x69);
        return this;
    }

    RamScriptBuilder facePlayer() {
        opcode(0x5A);
        return this;
    }

    RamScriptBuilder release() {
        opcode(0x6C);
        return this;
    }

    RamScriptBuilder releaseAll() {
        opcode(0x6B);
        return this;
    }

    RamScriptBuilder message(long address) {
        opcode(0x67);
        u32(address);
        return this;
    }

    RamScriptBuilder closeMessage() {
        opcode(0x68);
        return this;
    }

    RamScriptBuilder vMessage(String label) {
        opcode(0xBD);
        labelPointer(label);
        return this;
    }

    RamScriptBuilder waitMessage() {
        opcode(0x66);
        return this;
    }

    RamScriptBuilder waitButtonPress() {
        opcode(0x6D);
        return this;
    }

    /* FR/LG waitbuttonpress has Quest Log handling that may also accept held
       field inputs when release/releaseall is the immediately following
       command. Insert a NOP so callers that explicitly want A/B-only waiting
       are not completed by a still-held hotkey modifier such as R. */
    RamScriptBuilder waitButtonPressStrict() {
        opcode(0x6D); // waitbuttonpress
        opcode(0x00); // nop: prevents NextCommandEndsScript from seeing release/all
        return this;
    }

    RamScriptBuilder delay(int frames) {
        opcode(0x28);
        u16(frames);
        return this;
    }

    RamScriptBuilder playSe(int soundId) {
        opcode(0x2F);
        u16(soundId);
        return this;
    }

    RamScriptBuilder waitSe() {
        opcode(0x30);
        return this;
    }

    RamScriptBuilder playFanfare(int fanfareId) {
        opcode(0x31);
        u16(fanfareId);
        return this;
    }

    RamScriptBuilder waitFanfare() {
        opcode(0x32);
        return this;
    }

    RamScriptBuilder fadeScreen(int mode) {
        opcode(0x97);
        u8(mode);
        return this;
    }

    RamScriptBuilder setWeather(int weatherOrVariable) {
        opcode(0xA4);
        u16(weatherOrVariable);
        return this;
    }

    RamScriptBuilder doWeather() {
        opcode(0xA5);
        return this;
    }

    RamScriptBuilder setWildBattle(int species, int level, int heldItem) {
        opcode(0xB6);
        u16(species);
        u8(level);
        u16(heldItem);
        return this;
    }

    RamScriptBuilder doWildBattle() {
        opcode(0xB7);
        return this;
    }

    /* --- Embedded relocatable text --- */

    RamScriptBuilder text(String label, String text) {
        label(label);
        byte[] encoded = Gen3TextCodec.encodeString(text);
        output.writeBytes(encoded);
        return this;
    }

    /* Embeds inert data inside the relocatable payload. Control flow must skip
       over these bytes; they are not interpreted as Field Script opcodes. */
    RamScriptBuilder raw(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        output.writeBytes(data);
        return this;
    }

    RamScriptBuilder padding(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("padding count must be >= 0");
        }
        for (int i = 0; i < count; i++) output.write(0);
        return this;
    }

    byte[] buildScript() {
        byte[] result = output.toByteArray();
        if (result.length > RamScript.SCRIPT_SIZE) {
            throw new IllegalStateException(
                    "Script/data is " + result.length + " bytes; RamScript limit is " + RamScript.SCRIPT_SIZE
            );
        }

        for (PointerFixup fixup : fixups) {
            Integer targetOffset = labels.get(fixup.label());
            if (targetOffset == null) {
                throw new IllegalStateException("Undefined label: " + fixup.label());
            }

            long address = (virtualBase + Integer.toUnsignedLong(targetOffset)) & 0xFFFF_FFFFL;
            putU32(result, fixup.offset(), address);
        }

        return result;
    }

    RamScript buildWonderCardRamScript() {
        return RamScript.createWonderCard(buildScript());
    }

    private void labelPointer(String label) {
        int offset = position();
        u32(0);
        fixups.add(new PointerFixup(offset, label));
    }

    private void opcode(int value) {
        u8(value);
    }

    private static void requireCondition(int condition) {
        if (condition < COND_LT || condition > COND_NE) {
            throw new IllegalArgumentException("condition must be between 0 and 5");
        }
    }

    private void u8(int value) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(String.format("Value 0x%X does not fit in u8", value));
        }
        output.write(value);
    }

    private void u16(int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException(String.format("Value 0x%X does not fit in u16", value));
        }
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
    }

    private void u32(long value) {
        if (value < 0 || value > 0xFFFF_FFFFL) {
            throw new IllegalArgumentException(String.format("Value 0x%X does not fit in u32", value));
        }
        output.write((int) value & 0xFF);
        output.write((int) (value >>> 8) & 0xFF);
        output.write((int) (value >>> 16) & 0xFF);
        output.write((int) (value >>> 24) & 0xFF);
    }

    private static void putU32(byte[] data, int offset, long value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }

    private record PointerFixup(int offset, String label) {}
}
