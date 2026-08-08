import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* Small control-flow-aware disassembler for FireRed/LeafGreen field-script bytecode.
   The first version implements the operand layouts needed by the official Aurora/Mystic Ticket RamScripts,
   plus some commands that will be useful for custom scripts later.
*/
final class ScriptDisassembler {
    private static final String[] CONDITIONS = {"LT", "EQ", "GT", "LE", "GE", "NE"};

    private final byte[] script;
    private Long virtualBase;

    ScriptDisassembler(byte[] script) {
        this.script = script;
    }

    List<ScriptInstruction> disassemble() {
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        List<ScriptInstruction> instructions = new ArrayList<>();
        pending.add(0);

        while (!pending.isEmpty()) {
            int pc = pending.removeFirst();

            while (pc >= 0 && pc < script.length && !visited.contains(pc)) {
                visited.add(pc);

                int opcode = Binary.u8(script, pc);
                String name = ScriptOpcode.name(opcode);
                ScriptOpcode.OperandFormat format = ScriptOpcode.format(opcode);

                if (format == null) {
                    instructions.add(new ScriptInstruction(
                            pc, opcode, name,
                            new byte[]{script[pc]},
                            "",
                            "operand layout not implemented; stopped this path"
                    ));
                    break;
                }

                int length = 1 + format.size();
                if (pc + length > script.length) {
                    instructions.add(new ScriptInstruction(
                            pc, opcode, name,
                            slice(pc, script.length),
                            "",
                            "truncated instruction"
                    ));
                    break;
                }

                Decoded decoded = decodeOperands(pc, opcode, format);
                instructions.add(new ScriptInstruction(
                        pc, opcode, name,
                        slice(pc, pc + length),
                        decoded.operands(),
                        decoded.annotation()
                ));

                if (opcode == 0xB8) { // setvaddress
                    virtualBase = Binary.u32(script, pc + 1);
                }

                if (opcode == 0xB9) { // vgoto
                    addVirtualTarget(pending, Binary.u32(script, pc + 1));
                    break;
                }

                if (opcode == 0xBA) { // vcall
                    addVirtualTarget(pending, Binary.u32(script, pc + 1));
                }

                if (opcode == 0xBB || opcode == 0xBC) { // vgoto_if / vcall_if
                    addVirtualTarget(pending, Binary.u32(script, pc + 2));
                }

                if (opcode == 0x02 || opcode == 0x03 || opcode == 0x0C || opcode == 0x0D) {
                    break;
                }

                pc += length;
            }
        }

        instructions.sort(Comparator.comparingInt(ScriptInstruction::offset));
        return instructions;
    }

    Long virtualBase() {
        return virtualBase;
    }

    private Decoded decodeOperands(int pc, int opcode, ScriptOpcode.OperandFormat format) {
        int p = pc + 1;

        // Semantic formatting for commands commonly used by Wonder Card RamScripts.
        switch (opcode) {
            case 0x09: { // callstd
                int id = Binary.u8(script, p);
                return new Decoded(StdScripts.format(id), "");
            }
            case 0x16: { // setvar
                int variable = Binary.u16(script, p);
                int value = Binary.u16(script, p + 2);
                return new Decoded(Vars.format(variable) + ", " + formatValue(value), "");
            }
            case 0x1A: { // setorcopyvar
                int destination = Binary.u16(script, p);
                int source = Binary.u16(script, p + 2);
                String sourceText;
                if (destination == 0x8000) { // giveitem-style macros store the item id here
                    sourceText = Items.format(source);
                } else if (destination == 0x8001) { // quantity argument used by giveitem-style macros
                    sourceText = formatValue(source);
                } else {
                    sourceText = formatVarOrValue(source);
                }
                return new Decoded(Vars.format(destination) + ", " + sourceText, "");
            }
            case 0x21: { // compare_var_to_value
                int variable = Binary.u16(script, p);
                int value = Binary.u16(script, p + 2);
                return new Decoded(Vars.format(variable) + ", " + formatValue(value), "");
            }
            case 0x29: // setflag
            case 0x2A: // clearflag
            case 0x2B: { // checkflag
                int flag = Binary.u16(script, p);
                return new Decoded(Flags.format(flag), "");
            }
            case 0x44: // additem
            case 0x45: // removeitem
            case 0x46: // checkitemspace
            case 0x47: { // checkitem
                int item = Binary.u16(script, p);
                int quantity = Binary.u16(script, p + 2);
                return new Decoded(Items.format(item) + ", " + formatValue(quantity), "");
            }
            case 0x25: { // special
                int id = Binary.u16(script, p);
                String annotation = specialAnnotation(id);
                return new Decoded(hex16(id), annotation);
            }
            case 0x26: { // specialvar
                int variable = Binary.u16(script, p);
                int id = Binary.u16(script, p + 2);
                String annotation = specialAnnotation(id);
                return new Decoded(Vars.format(variable) + ", " + hex16(id), annotation);
            }
            case 0xBD: { // vmessage
                long ptr = Binary.u32(script, p);
                return new Decoded(hex32(ptr), messageAnnotation(ptr));
            }
        }

        return switch (format) {
            case NONE -> new Decoded("", "");
            case U8 -> new Decoded(hex8(Binary.u8(script, p)), "");
            case U16 -> new Decoded(hex16(Binary.u16(script, p)), "");
            case PTR -> {
                long ptr = Binary.u32(script, p);
                yield new Decoded(hex32(ptr), pointerAnnotation(opcode, ptr));
            }
            case U8_U8 -> new Decoded(
                    hex8(Binary.u8(script, p)) + ", " + hex8(Binary.u8(script, p + 1)), ""
            );
            case U8_U16 -> new Decoded(
                    hex8(Binary.u8(script, p)) + ", " + hex16(Binary.u16(script, p + 1)), ""
            );
            case U8_U32 -> new Decoded(
                    hex8(Binary.u8(script, p)) + ", " + hex32(Binary.u32(script, p + 1)), ""
            );
            case U16_U16 -> new Decoded(
                    hex16(Binary.u16(script, p)) + ", " + hex16(Binary.u16(script, p + 2)), ""
            );
            case COND_U8 -> new Decoded(
                    condition(Binary.u8(script, p)) + ", " + hex8(Binary.u8(script, p + 1)), ""
            );
            case COND_PTR -> {
                int condition = Binary.u8(script, p);
                long ptr = Binary.u32(script, p + 1);
                yield new Decoded(condition(condition) + ", " + hex32(ptr), virtualPointerAnnotation(ptr));
            }
            case U8_PTR -> {
                int first = Binary.u8(script, p);
                long ptr = Binary.u32(script, p + 1);
                yield new Decoded(hex8(first) + ", " + hex32(ptr), virtualPointerAnnotationIfNeeded(opcode, ptr));
            }
            case PTR_U8 -> new Decoded(
                    hex32(Binary.u32(script, p)) + ", " + hex8(Binary.u8(script, p + 4)), ""
            );
            case PTR_PTR -> new Decoded(
                    hex32(Binary.u32(script, p)) + ", " + hex32(Binary.u32(script, p + 4)), ""
            );
        };
    }

    private String messageAnnotation(long ptr) {
        Integer offset = virtualToOffset(ptr);
        if (offset == null) {
            return "message target; virtual base not known yet";
        }
        if (offset < 0 || offset >= script.length) {
            return "message target outside script: " + signedOffset(offset);
        }

        String text = Gen3TextCodec.decodeString(script, offset);
        if (text == null) {
            return "script+" + hexOffset(offset);
        }
        return "script+" + hexOffset(offset) + " text=\"" + escapeAnnotation(text) + "\"";
    }

    private static String formatValue(int value) {
        if (value == 0) return "0";
        if (value == 1) return "1";
        return String.format("0x%04X", value & 0xFFFF);
    }

    private static String formatVarOrValue(int value) {
        String variable = value >= 0x4000 ? Vars.name(value) : null;
        if (variable != null) return variable;
        return formatValue(value);
    }

    private static String escapeAnnotation(String text) {
        return text.replace("\"", "\\\"");
    }

    private String pointerAnnotation(int opcode, long ptr) {
        if (opcode == 0xB8) {
            return "virtual base used for relocatable v* pointers";
        }
        if (opcode == 0xB9 || opcode == 0xBA || opcode == 0xBD || opcode == 0xBE) {
            return virtualPointerAnnotation(ptr);
        }
        if (opcode == 0x23 || opcode == 0x24) {
            return "absolute native function address; ROM-build dependent";
        }
        return "";
    }

    private String virtualPointerAnnotationIfNeeded(int opcode, long ptr) {
        if (opcode == 0xBF) {
            return virtualPointerAnnotation(ptr);
        }
        return "";
    }

    private String virtualPointerAnnotation(long ptr) {
        Integer offset = virtualToOffset(ptr);
        if (offset == null) {
            return "virtual target (base not known yet)";
        }
        if (offset < 0 || offset >= script.length) {
            return "virtual target outside script: " + signedOffset(offset);
        }
        return "script+" + hexOffset(offset);
    }

    private void addVirtualTarget(ArrayDeque<Integer> pending, long ptr) {
        Integer offset = virtualToOffset(ptr);
        if (offset != null && offset >= 0 && offset < script.length) {
            pending.add(offset);
        }
    }

    private Integer virtualToOffset(long ptr) {
        if (virtualBase == null) {
            return null;
        }
        long diff = ptr - virtualBase;
        if (diff < Integer.MIN_VALUE || diff > Integer.MAX_VALUE) {
            return null;
        }
        return (int) diff;
    }

    private String specialAnnotation(int id) {
        String name = Specials.name(id);
        return name == null ? "unknown special id" : "special " + name;
    }

    private static String condition(int value) {
        if (value >= 0 && value < CONDITIONS.length) {
            return CONDITIONS[value] + "(" + hex8(value) + ")";
        }
        return hex8(value);
    }

    private byte[] slice(int from, int to) {
        byte[] result = new byte[to - from];
        System.arraycopy(script, from, result, 0, result.length);
        return result;
    }

    private static String hex8(int value) {
        return String.format("0x%02X", value & 0xFF);
    }

    private static String hex16(int value) {
        return String.format("0x%04X", value & 0xFFFF);
    }

    private static String hex32(long value) {
        return String.format("0x%08X", value & 0xFFFF_FFFFL);
    }

    private static String hexOffset(int value) {
        return String.format("0x%04X", value);
    }

    private static String signedOffset(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    private record Decoded(String operands, String annotation) {}
}
