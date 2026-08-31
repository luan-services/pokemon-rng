# FRLG / RSE Initial Seed Finder --- Java CLI

Ferramenta simples em Java para encontrar **Initial Seeds** a partir de
uma **target seed de 32 bits** obtida no PokéFinder.

Ela é útil para RNG tradicional de Pokémon Gen 3, especialmente quando
você encontra um spread no **Searcher** e quer descobrir uma Initial
Seed que chegue até ele em uma quantidade conhecida de advances.

## Requisitos

-   Java 17 ou superior
-   PokéFinder

Para conferir sua versão do Java:

``` bash
java -version
```

## Compilando

Abra um terminal na pasta onde está `SeedFinder.java` e execute:

``` bash
javac SeedFinder.java
```

Isso criará:

``` text
SeedFinder.class
```

A compilação só precisa ser refeita se você modificar o código-fonte.

## Encontrando o spread no PokéFinder

No PokéFinder, abra:

``` text
Gen 3 → Static → Searcher
```

Configure o jogo, Pokémon, Method e os filtros desejados.

Por exemplo, para um Bulbasaur shiny, o Searcher pode retornar:

``` text
Seed:    41A9CFE6
PID:     DAC1E489
Shiny:   Star
Nature:  Mild
IVs:     28 / 27 / 29 / 27 / 31 / 30
```

A informação que deve ser passada ao Seed Finder é a coluna **Seed**.

Neste exemplo:

``` text
41A9CFE6
```

Essa é a **target seed**, não a Initial Seed.

## Executando o Seed Finder

Há duas formas.

### Modo interativo

Execute:

``` bash
java SeedFinder
```

O programa perguntará:

``` text
Target seed (hex, e.g. 41A9CFE6):
Max results:
```

Exemplo:

``` text
Target seed (hex, e.g. 41A9CFE6): 41A9CFE6
Max results: 100
```

### Passando os argumentos diretamente

Também é possível executar:

``` bash
java SeedFinder 41A9CFE6 100
```

Onde:

-   `41A9CFE6` é a target seed encontrada no PokéFinder;
-   `100` é a quantidade máxima de Initial Seeds que você quer
    encontrar.

## Entendendo o resultado

Para a target seed `41A9CFE6`, os primeiros resultados começam assim:

``` text
Target: 41A9CFE6

1. Seed: D96A | Advances: 2748
2. Seed: 2A37 | Advances: 44617
3. Seed: B9D1 | Advances: 54095
```

Isso significa que:

``` text
Initial Seed D96A
       ↓
   2748 advances
       ↓
Target Seed 41A9CFE6
       ↓
spread desejado
```

Normalmente é conveniente escolher uma Initial Seed com uma quantidade
pequena de advances.

Nesse exemplo, `D96A` é uma ótima escolha porque chega à target em
apenas `2748` advances.

O programa também salva os resultados em:

``` text
seeds.txt
```

## Confirmando no PokéFinder

Depois de escolher uma Initial Seed, volte ao PokéFinder e abra:

``` text
Gen 3 → Static → Generator
```

Configure o mesmo profile, Pokémon e Method usados anteriormente.

No exemplo:

``` text
Method:            Method 1
Seed:              D96A
Initial Advances:  0
Max Advances:      10000
Pokémon:           Bulbasaur
```

Clique em **Generate**.

O spread original deverá aparecer no advance encontrado pelo Seed
Finder:

``` text
Advances: 2748
PID:      DAC1E489
Shiny:    Star
Nature:   Mild
IVs:      28 / 27 / 29 / 27 / 31 / 30
```

Isso confirma:

``` text
D96A + 2748 advances → 41A9CFE6 → DAC1E489
```

## Fluxo completo

``` text
PokéFinder
Static → Searcher
        │
        │ encontra o spread desejado
        ▼
Target Seed
ex.: 41A9CFE6
        │
        ▼
SeedFinder
        │
        │ procura Initial Seeds
        ▼
Initial Seed + Advances
ex.: D96A + 2748
        │
        ▼
PokéFinder
Static → Generator
        │
        │ confirmar o resultado
        ▼
Spread desejado
```

## Como funciona

A Gen 3 utiliza um LCRNG de 32 bits. O avanço normal é:

``` text
next = (0x41C64E6D × current + 0x6073) mod 2^32
```

O Seed Finder faz o processo inverso: começa na target seed e percorre o
LCRNG para trás.

Quando encontra um estado entre:

``` text
0000–FFFF
```

ele registra esse estado como uma possível Initial Seed e informa
quantos advances existem entre ela e a target.

Por isso a ferramenta consegue transformar uma target seed como:

``` text
41A9CFE6
```

em algo utilizável no RNG tradicional, como:

``` text
Initial Seed: D96A
Advances:     2748
```
