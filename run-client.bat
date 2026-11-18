@echo off
echo Starting Multiplayer Quiz Player Client...
java --enable-native-access=ALL-UNNAMED -cp "bin;lib\*" quiz.client.QuizClientGUI
pause

