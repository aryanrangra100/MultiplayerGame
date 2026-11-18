@echo off
echo Running Multiplayer Quiz Game Automated Simulation Test...
java --enable-native-access=ALL-UNNAMED -cp "bin;lib\*" quiz.test.GameSimulationTest
pause

