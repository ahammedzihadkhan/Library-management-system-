@echo off
cd /d "%~dp0"
javac -cp lib\sqlite-jdbc.jar -d out src\*.java
if errorlevel 1 (pause & exit /b 1)
java --enable-native-access=ALL-UNNAMED -cp "out;lib\sqlite-jdbc.jar" LibraryApp
