WC3 Injector - FireRed / LeafGreen
Java 21, sem Maven e sem packages.

ESTRUTURA
rng-tools/
  wc3-injector/
    src/
      Main.java
      Binary.java
      FireRedLeafGreenSave.java
      SectorChecksum.java
      Wc3File.java

COMO COMPILAR NO CMD
1. Abra o CMD dentro da pasta wc3-injector\src.
2. Execute:

   javac *.java

COMO RODAR
Ainda dentro da pasta src, execute:

   java Main "caminho\firered.sav" "caminho\evento.wc3" "caminho\firered_injetado.sav"

Exemplo, se os três arquivos estiverem dentro de src:

   java Main firered.sav example-wc.wc3 firered_injetado.sav

Os arquivos .class criados pelo javac podem ser apagados quando quiser.
Sempre mantenha uma copia do save original.
