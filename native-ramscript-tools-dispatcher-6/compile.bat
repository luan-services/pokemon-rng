@echo off
if not exist out mkdir out
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 exit /b 1
echo Compiled successfully.
