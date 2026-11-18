@echo off
echo Starting Multiplayer Quiz Server GUI...
java --enable-native-access=ALL-UNNAMED -cp "bin;lib\*" quiz.server.ServerGUI
pause

