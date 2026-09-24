import java.io.ByteArrayOutputStream;
import java.util.*;

/*
   Mew boss research helper, based on the game-validated Probe 4 path.

   The field script first uses stock setwildbattle/CreateScriptedWildMon, which
   produces a structurally valid normal Pokemon.  This helper then changes only
   the four move slots through the stock SetMonMoveSlot API.

   FR1.0 in-game validation recorded by the project:
     - Lv100 Mew enters battle normally.
     - Psychic / Recover / Thunderbolt / Ice Beam are present.
     - Capturing the Mew produces a valid Pokemon (no Bad Egg).

   Capture blocking is intentionally a separate research problem.
*/
final class MewBossProbeNative {
    private static final long G_ENEMY_PARTY = 0x0202402CL;
    private static final int[] MOVES = {94, 105, 85, 58};

    private MewBossProbeNative() {}

    static NativeHelper buildAt(RomProfile rom, long stagingAddress) {
        MiniThumb t = new MiniThumb(stagingAddress);
        t.u16(0xB500); // push {lr}

        for (int slot = 0; slot < MOVES.length; slot++) {
            t.ldrLit(0, "enemyParty");
            t.movImm(1, MOVES[slot]);
            t.movImm(2, slot);
            t.ldrLit(3, "setMonMoveSlot");
            t.bl("thunk_r3");
        }

        t.u16(0xBD00); // pop {pc}
        t.align4();
        t.label("thunk_r3");
        t.u16(0x4718); // bx r3
        t.align4();
        t.literal("enemyParty", G_ENEMY_PARTY);
        t.literal("setMonMoveSlot", setMonMoveSlotThumb(rom));
        return new NativeHelper(stagingAddress, t.finish());
    }

    static int[] moves() { return MOVES.clone(); }

    private static long setMonMoveSlotThumb(RomProfile rom) {
        return switch (rom) {
            // FR1.0 exact entry is game-validated by Probe 4.
            case FIRE_RED_EN_10 -> 0x0803E965L;
            // Do not silently claim the same address for other revisions.
            default -> throw new IllegalArgumentException(
                    "Mew boss stock/custom-moves probe is currently validated/supported only for FireRed EN 1.0");
        };
    }

    private static final class MiniThumb {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final Map<String,Integer> labels = new HashMap<>();
        private final List<BlFix> bls = new ArrayList<>();
        private final List<LiteralFix> literalLoads = new ArrayList<>();
        private final Map<String,Long> literals = new LinkedHashMap<>();
        MiniThumb(long baseAddress) {}
        int pos(){return out.size();}
        void u16(int v){out.write(v&255);out.write((v>>>8)&255);}
        void label(String s){if(labels.put(s,pos())!=null)throw new IllegalStateException("duplicate label "+s);}
        void align4(){while((pos()&3)!=0)u16(0x46C0);}
        void bl(String label){int p=pos();u16(0);u16(0);bls.add(new BlFix(p,label));}
        void ldrLit(int rt,String name){int p=pos();u16(0x4800|(rt<<8));literalLoads.add(new LiteralFix(p,rt,name));}
        void movImm(int rd,int imm){if(imm<0||imm>255)throw new IllegalArgumentException("Thumb mov immediate out of range");u16(0x2000|(rd<<8)|imm);}
        void literal(String name,long value){literals.put(name,value);}
        byte[] finish(){
            align4(); Map<String,Integer> lp=new HashMap<>();
            for(var e:literals.entrySet()){lp.put(e.getKey(),pos());long v=e.getValue();out.write((int)v&255);out.write((int)(v>>>8)&255);out.write((int)(v>>>16)&255);out.write((int)(v>>>24)&255);}
            byte[] b=out.toByteArray();
            for(var f:bls){int target=req(f.label),d=target-(f.pos+4);put16(b,f.pos,0xF000|((d>>12)&0x7FF));put16(b,f.pos+2,0xF800|((d>>1)&0x7FF));}
            for(var f:literalLoads){int p=lp.get(f.name),pc=(f.pos+4)&~3,d=p-pc;put16(b,f.pos,0x4800|(f.rt<<8)|(d/4));}
            return b;
        }
        private int req(String n){Integer v=labels.get(n);if(v==null)throw new IllegalStateException("missing label "+n);return v;}
        private static void put16(byte[]b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>>8);}
        record BlFix(int pos,String label){} record LiteralFix(int pos,int rt,String name){}
    }
}
