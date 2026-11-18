@echo off
echo ==================================================
echo Building Multiplayer Quiz Game...
echo ==================================================

if not exist bin mkdir bin

javac -cp ".;lib\*" -d bin src\quiz\model\*.java src\quiz\net\*.java src\quiz\db\*.java src\quiz\server\*.java src\quiz\client\*.java src\quiz\test\*.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation finished successfully. Output in \bin.
) else (
    echo [ERROR] Compilation failed. Please check error messages above.
)
pause

