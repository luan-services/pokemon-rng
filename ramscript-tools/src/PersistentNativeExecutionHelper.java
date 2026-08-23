import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
   Production native helpers for persistent modules.

   Two execution contracts are intentionally kept:

   DIRECT_DISPATCH
     Used by the already validated small-module/SID path. The helper resolves
     and validates a catalog entry and tail-jumps to the persistent Thumb body.

   STAGE_ONLY
     Used by complex modules such as Party IV Viewer. The helper resolves and
     validates the SB2 module, copies it to stable EWRAM scratch, writes
     VAR_RESULT=1 and RETURNS. The Field Script then enters the staged module
     with the stock callnative command.

   The stage-only contract is the production path for complex native modules;
   it avoids executing preset code inside the resolver and matches the calling
   convention validated by the standalone Party IV Viewer.
*/
final class PersistentNativeExecutionHelper {
    private PersistentNativeExecutionHelper() {}

    static NativeHelper buildDirectDispatcherAt(RomProfile rom, long address) {
        Thumb b = new Thumb();
        b.emit(0xB430); // push {r4,r5}
        b.ldrLit(5,"sb2ptr");
        b.emit(0x682D);
        b.ldrLit(0,"sb2off");
        b.emit(0x182D);
        b.emit(0x6828);
        b.ldrLit(1,"magic");
        b.emit(0x4288);
        b.bCond(1,"fail");
        b.emit(0x7928);
        b.emit(0x2800 | PersistentNativeCatalogFormat.VERSION);
        b.bCond(1,"fail");
        b.emit(0x796B);
        b.emit(0x2B00);
        b.bCond(0,"fail");
        b.ldrLit(0,"desired");
        b.emit(0x8801);
        b.emit(0x1C2C);
        b.emit(0x3410);
        b.label("scan");
        b.emit(0x8820);
        b.emit(0x4288);
        b.bCond(0,"found");
        b.emit(0x3410);
        b.emit(0x3B01);
        b.bCond(1,"scan");
        b.b("fail");

        b.label("found");
        b.emit(0x78A0);
        b.emit(0x2801);
        b.bCond(1,"fail");
        b.emit(0x78E0);
        b.emit(0x2801);
        b.bCond(0,"area1");
        b.emit(0x2802);
        b.bCond(0,"area2");
        b.b("fail");

        b.label("area1");
        b.ldrLit(2,"sb1ptr");
        b.emit(0x6812);
        b.ldrLit(0,"sb1off");
        b.emit(0x1812);
        b.b("gotbase");
        b.label("area2");
        b.emit(0x1C2A);
        b.label("gotbase");
        b.emit(0x88A0);
        b.emit(0x1812);
        b.emit(0x1C15);
        b.emit(0x88E1);
        b.emit(0x8923);
        b.emit(0x2900);
        b.bCond(0,"fail");
        b.emit(0x2000);
        b.label("sum");
        b.emit(0x7814);
        b.emit(0x1900);
        b.emit(0x3201);
        b.emit(0x3901);
        b.bCond(1,"sum");
        b.emit(0x0400);
        b.emit(0x0C00);
        b.emit(0x4298);
        b.bCond(1,"fail");
        b.emit(0x1C2B);
        b.emit(0xBC30);
        b.emit(0x3301);
        b.emit(0x4718);

        b.label("fail");
        b.emit(0xBC30);
        b.emit(0x2000);
        b.ldrLit(1,"result");
        b.emit(0x8008);
        b.emit(0x4770);

        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentNativeCatalogFormat.MAGIC);
        b.literal("desired",rom.specialVar8005);
        b.literal("sb1ptr",rom.saveBlock1Ptr);
        b.literal("sb1off",PayloadStorageArea.SAVE_BLOCK1.offset());
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }

    static NativeHelper buildStagingLoaderAt(
            RomProfile rom, long address, long stagingAddress, int stagingCapacity) {
        if ((stagingAddress & 3L) != 0) {
            throw new IllegalArgumentException("native staging address must be word-aligned");
        }
        if (stagingCapacity <= 0 || stagingCapacity > 0xFFFF) {
            throw new IllegalArgumentException("invalid native staging capacity");
        }

        Thumb b = new Thumb();
        b.emit(0xB470); // push {r4,r5,r6}
        b.ldrLit(5,"sb2ptr"); b.emit(0x682D);
        b.ldrLit(0,"sb2off"); b.emit(0x182D);
        b.emit(0x6828); b.ldrLit(1,"magic"); b.emit(0x4288); b.bCond(1,"fail");
        b.emit(0x7928); b.emit(0x2800 | PersistentNativeCatalogFormat.VERSION); b.bCond(1,"fail");
        b.emit(0x796B); b.emit(0x2B00); b.bCond(0,"fail");
        b.ldrLit(0,"desired"); b.emit(0x8801);
        b.emit(0x1C2C); b.emit(0x3410);
        b.label("scan");
        b.emit(0x8820); b.emit(0x4288); b.bCond(0,"found");
        b.emit(0x3410); b.emit(0x3B01); b.bCond(1,"scan"); b.b("fail");
        b.label("found");
        b.emit(0x78A0); b.emit(0x2801); b.bCond(1,"fail");
        b.emit(0x78E0); b.emit(0x2802); b.bCond(1,"fail");
        b.emit(0x1C2A); b.emit(0x88A0); b.emit(0x1812);
        b.emit(0x1C15);
        b.emit(0x88E1);
        b.emit(0x8923);
        b.emit(0x2900); b.bCond(0,"fail");
        b.ldrLit(0,"capacity"); b.emit(0x4281); b.bCond(8,"fail");
        b.emit(0x1C0E);
        b.emit(0x2000);
        b.label("sum");
        b.emit(0x7814); b.emit(0x1900); b.emit(0x3201); b.emit(0x3901); b.bCond(1,"sum");
        b.emit(0x0400); b.emit(0x0C00); b.emit(0x4298); b.bCond(1,"fail");
        b.emit(0x1C28); b.ldrLit(2,"stage"); b.emit(0x1C31);
        b.label("copy");
        b.emit(0x7803); b.emit(0x7013); b.emit(0x3001); b.emit(0x3201); b.emit(0x3901); b.bCond(1,"copy");
        b.emit(0x2001); b.ldrLit(1,"result"); b.emit(0x8008);
        b.emit(0xBC70); b.emit(0x4770);
        b.label("fail");
        b.emit(0x2000); b.ldrLit(1,"result"); b.emit(0x8008);
        b.emit(0xBC70); b.emit(0x4770);
        b.literal("sb2ptr",rom.saveBlock2Ptr);
        b.literal("sb2off",PayloadStorageArea.SAVE_BLOCK2.offset());
        b.literal("magic",PersistentNativeCatalogFormat.MAGIC);
        b.literal("desired",rom.specialVar8005);
        b.literal("capacity",stagingCapacity);
        b.literal("stage",stagingAddress);
        b.literal("result",rom.specialVarResult);
        return new NativeHelper(address,b.finish());
    }

    private static void put16(byte[] b,int o,int v){b[o]=(byte)v;b[o+1]=(byte)(v>>>8);}

    private static final class Thumb {
        private final List<Integer> words=new ArrayList<>();
        private final Map<String,Integer> labels=new HashMap<>();
        private final List<BranchFixup> branches=new ArrayList<>();
        private final List<LiteralFixup> loads=new ArrayList<>();
        private final List<Literal> literals=new ArrayList<>();
        int pos(){return words.size()*2;}
        void emit(int w){words.add(w&0xFFFF);}
        void label(String n){if(labels.put(n,pos())!=null)throw new IllegalArgumentException("duplicate label "+n);}
        void bCond(int cond,String label){branches.add(new BranchFixup(words.size(),cond,label,true));emit(0);}
        void b(String label){branches.add(new BranchFixup(words.size(),0,label,false));emit(0);}
        void ldrLit(int rt,String name){loads.add(new LiteralFixup(words.size(),rt,name));emit(0);}
        void literal(String name,long value){literals.add(new Literal(name,value));}
        byte[] finish(){
            if((pos()&3)!=0)emit(0x46C0);
            Map<String,Integer> literalOffsets=new HashMap<>();
            int literalStart=pos();
            for(int i=0;i<literals.size();i++)literalOffsets.put(literals.get(i).name,literalStart+i*4);
            for(BranchFixup f:branches){
                Integer target=labels.get(f.label);if(target==null)throw new IllegalStateException("missing label "+f.label);
                int insn=f.wordIndex*2;int delta=target-(insn+4);
                if((delta&1)!=0)throw new IllegalStateException("unaligned branch");
                int hw=delta/2;
                if(f.conditional){if(hw< -128||hw>127)throw new IllegalStateException("conditional branch range");words.set(f.wordIndex,0xD000|(f.cond<<8)|(hw&0xFF));}
                else {if(hw< -1024||hw>1023)throw new IllegalStateException("branch range");words.set(f.wordIndex,0xE000|(hw&0x7FF));}
            }
            for(LiteralFixup f:loads){
                Integer target=literalOffsets.get(f.name);if(target==null)throw new IllegalStateException("missing literal "+f.name);
                int insn=f.wordIndex*2;int base=(insn+4)&~3;int delta=target-base;
                if(delta<0||(delta&3)!=0||delta/4>255)throw new IllegalStateException("literal load range");
                words.set(f.wordIndex,0x4800|(f.rt<<8)|(delta/4));
            }
            byte[] out=new byte[pos()+literals.size()*4];
            for(int i=0;i<words.size();i++)put16(out,i*2,words.get(i));
            int o=pos();for(Literal l:literals){PersistentNativeCatalogFormat.putU32(out,o,l.value);o+=4;}
            return out;
        }
        private record BranchFixup(int wordIndex,int cond,String label,boolean conditional){}
        private record LiteralFixup(int wordIndex,int rt,String name){}
        private record Literal(String name,long value){}
    }
}
