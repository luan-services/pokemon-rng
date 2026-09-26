/* Persistent Mew battle module A. Only the battle native is installed by WC #1.
   Scene/dispatcher extension is installed by WC #2 so each WC stays below 995 B. */
final class MewBattleCore {
    static final int STORAGE_OFFSET=0x0B20, HEADER_SIZE=0x20;
    static final int SCRATCH_RELATIVE=MewBossBattleTransactionNative.BACKUP_OFFSET-STORAGE_OFFSET;
    static final long MAGIC=0x4157454DL; static final int VERSION=2;
    record Image(byte[] bytes,int prepareOffset,int restoreOffset,int launchOffset,int sceneStanOffset,int sceneShaneOffset,int dispatcherOffset,int nativeSize){}
    private MewBattleCore(){}
    static Image build(RomProfile rom){
        MewBattleCoreNative.Layout tx=MewBattleCoreNative.buildAt(rom,0);byte[] code=tx.helper().codeCopy();int total=align4(HEADER_SIZE+code.length);byte[]out=new byte[total];
        put32(out,0,MAGIC);out[4]=VERSION;out[5]=1;put16(out,8,total);put16(out,0x0A,HEADER_SIZE);put16(out,0x0C,HEADER_SIZE+tx.restoreOffset());put16(out,0x0E,HEADER_SIZE+tx.launchOffset());put16(out,0x10,code.length);put16(out,0x12,checksum16(code));
        System.arraycopy(code,0,out,HEADER_SIZE,code.length);
        var x=MewBattleCoreExtension.layout(rom);
        return new Image(out,HEADER_SIZE,HEADER_SIZE+tx.restoreOffset(),HEADER_SIZE+tx.launchOffset(),x.sceneStanOffset(),x.sceneShaneOffset(),x.dispatcherOffset(),code.length);
    }
    static int align4(int n){return(n+3)&~3;}static int checksum16(byte[]d){int s=0;for(byte b:d)s=(s+Byte.toUnsignedInt(b))&0xffff;return s;}static void put16(byte[]d,int o,int v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);}static void put32(byte[]d,int o,long v){d[o]=(byte)v;d[o+1]=(byte)(v>>>8);d[o+2]=(byte)(v>>>16);d[o+3]=(byte)(v>>>24);}
}
