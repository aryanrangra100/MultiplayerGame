package quiz.net;

import quiz.model.Question;

public interface GameEvent {
    void onConnected(String serverMessage);
    void onLoginSuccess(String username, int playerId);
    void onPlayerJoined(String username, int totalPlayers);
    void onPlayerLeft(String username, int totalPlayers);
    void onGameStarting(int countdownSeconds);
    void onQuestionReceived(Question question, int questionIndex, int totalQuestions);
    void onRoundTimeTick(int secondsRemaining);
    void onAnswerResult(boolean correct, int correctOption, int pointsEarned, int currentScore);
    void onRoundEnded(int correctOption, String roundScores);
    void onLeaderboardUpdated(String leaderboardPayload);
    void onGameOver(String summary);
    void onSystemMessage(String message);
    void onConnectionError(ConnectionException exception);
}

