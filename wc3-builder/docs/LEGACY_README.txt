WC3 Builder / Inspector

Compile from the project root:

    javac -encoding UTF-8 -d out src\*.java

Inspect:

    java -cp out Main inspect ..\dummy-saves\aurora-ticket-fr-eng.wc3

Edit example:

    java -cp out Main edit ^
    ..\dummy-saves\aurora-ticket-fr-eng.wc3 ^
    ..\dummy-saves\pikachu-test.wc3 ^
    --title "PIKACHU TEST" ^
    --subtitle "JAVA WC3 BUILDER" ^
    --body1 "This card should show" ^
    --body2 "a Pikachu icon in the" ^
    --body3 "upper-right corner." ^
    --body4 "Injection test complete!" ^
    --footer1 "Custom Wonder Card" ^
    --footer2 "Created with Java" ^
    --icon 25 ^
    --id 0 ^
    --type 0 ^
    --bg 5 ^
    --send 0 ^
    --stamps 0

Important:
--icon is handled by Wc3File.setIconSpecies(), which updates both
WonderCard.iconSpecies and WonderCardMetadata.iconSpecies.
