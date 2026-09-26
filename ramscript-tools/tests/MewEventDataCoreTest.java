import java.util.Arrays;
final class MewEventDataCoreTest {
 static void run(){
  var rom=RomProfile.FIRE_RED_EN_10;
  var image=MewEventDataCore.build(rom);
  byte[] data=image.bytes(), old=MewEventDataRewardNative.buildAt(rom,0).codeCopy();
  require(data.length==396 && data.length<=MewEventDataCore.CAPACITY,"Core B must fit 400 bytes");
  require(image.rewardOffset()==0x20 && image.ensureMewOffset()==0x94 && image.transformRebindOffset()==0xDC,"Core A dispatcher entry offsets remain valid");
  require(Arrays.equals(Arrays.copyOfRange(data,0x22,0x20+old.length),Arrays.copyOfRange(old,2,old.length)),"validated PC writer and MYSTRY blob are unchanged");
  int tail=0x20+4+sign11(u16(data,0x20))*2;
  require(tail==332 && u16(data,tail)==0xB500,"reward enters appended helper");
  int pcBranch=tail+14;
  require(pcBranch+4+sign11(u16(data,pcBranch))*2==0x22,"PC result returns to validated writer");
  int movPc=0x16A, sourcePc=movPc+4;
  require(u16(data,movPc)==0x467A && u16(data,movPc+2)==0x3AFF,"party helper reads its PC");
  int blobAddress=sourcePc-255-(u16(data,movPc+4)&0xFF);
  require(blobAddress==0x38,"party source is the same MYSTRY data as PC source");
  require(u32(data,392)==0x0803E775L,"FR1.0 BoxMonToMon Thumb entry");
  byte[] blob=Arrays.copyOfRange(old,24,104);
  require(count(data,blob)==1,"only one 80-byte MYSTRY record");
 }
 private static int u16(byte[]b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
 private static long u32(byte[]b,int p){return Integer.toUnsignedLong(u16(b,p)|(u16(b,p+2)<<16));}
 private static int sign11(int h){int n=h&0x7FF;return (n&0x400)!=0?n-0x800:n;}
 private static int count(byte[]a,byte[]b){int n=0;outer:for(int i=0;i<=a.length-b.length;i++){for(int j=0;j<b.length;j++)if(a[i+j]!=b[j])continue outer;n++;}return n;}
 private static void require(boolean x,String m){if(!x)throw new AssertionError(m);}
}
