@echo off
echo ========================================================
echo  Compiling Library Management System (Core Java)
echo ========================================================

if not exist bin mkdir bin

REM Generate quoted list of all java source files to handle paths with spaces
powershell -NoProfile -Command "Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Out-File -Encoding ASCII sources.txt"

REM Compile using sources file
javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt

set EXIT_CODE=%ERRORLEVEL%
if exist sources.txt del sources.txt

if %EXIT_CODE% EQU 0 (
    echo [SUCCESS] Build completed! All classes compiled to bin/ directory.
) else (
    echo [ERROR] Compilation failed with error code %EXIT_CODE%.
)
