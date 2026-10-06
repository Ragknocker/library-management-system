@echo off
echo ========================================================
echo  Launching Athena Library Management System Desktop GUI
echo ========================================================

REM Run main desktop application with clean Java 26+ runtime arguments
java --enable-native-access=ALL-UNNAMED -cp "bin;lib/*;resources" com.library.Main
