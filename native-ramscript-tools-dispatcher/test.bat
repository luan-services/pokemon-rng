@echo off
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java tests\TestRunner.java
if errorlevel 1 exit /b 1
java -cp out TestRunner
