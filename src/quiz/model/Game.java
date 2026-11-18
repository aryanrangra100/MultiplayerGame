package quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Game {
    public enum State {
        WAITING,
        COUNTDOWN,
        IN_ROUND,
        ROUND_REVIEW,
        GAME_OVER
    }

    private String gameId;
    private List<Player> players;
    private List<Question> questions;
    private int currentQuestionIndex;
    private State currentState;
    private long roundStartTime;

    public Game(String gameId) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.currentQuestionIndex = -1;
        this.currentState = State.WAITING;
        this.roundStartTime = 0;
    }

    public synchronized void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
        }
    }

    public synchronized void removePlayer(Player player) {
        players.remove(player);
    }

    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public synchronized void setQuestions(List<Question> questions) {
        this.questions = new ArrayList<>(questions);
        this.currentQuestionIndex = -1;
    }

    public synchronized List<Question> getQuestions() {
        return questions;
    }

    public synchronized int getTotalQuestions() {
        return questions.size();
    }

    public synchronized int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public synchronized Question getCurrentQuestion() {
        if (currentQuestionIndex >= 0 && currentQuestionIndex < questions.size()) {
            return questions.get(currentQuestionIndex);
        }
        return null;
    }

    public synchronized Question nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            currentState = State.IN_ROUND;
            roundStartTime = System.currentTimeMillis();
            for (Player p : players) {
                p.resetRoundState();
            }
            return getCurrentQuestion();
        } else {
            currentState = State.GAME_OVER;
            return null;
        }
    }

    public synchronized boolean hasMoreQuestions() {
        return (currentQuestionIndex + 1) < questions.size();
    }

    public synchronized State getCurrentState() {
        return currentState;
    }

    public synchronized void setCurrentState(State state) {
        this.currentState = state;
    }

    public synchronized long getRoundStartTime() {
        return roundStartTime;
    }

    public synchronized int calculateScore(Player player, int chosenOption, long answerTimeMs, Question question) {
        player.setSelectedOption(chosenOption);
        player.setAnswerTimeMs(answerTimeMs);
        player.incrementTotalAnswered();

        int pointsEarned = 0;
        if (question.isCorrect(chosenOption)) {
            player.incrementCorrectCount();
            int basePoints = 100;

            long maxAllowedMs = question.getTimeLimitSeconds() * 1000L;
            long remainingMs = Math.max(0, maxAllowedMs - answerTimeMs);
            double speedFactor = (double) remainingMs / (double) maxAllowedMs;
            int speedBonus = (int) (speedFactor * 50);

            int streakBonus = Math.min(player.getStreak() * 10, 50);

            pointsEarned = basePoints + speedBonus + streakBonus;
            player.addScore(pointsEarned);
        } else {
            player.resetStreak();
        }

        return pointsEarned;
    }

    public synchronized boolean haveAllAnswered() {
        if (players.isEmpty()) return false;
        for (Player p : players) {
            if (p.getSelectedOption() == -1) {
                return false;
            }
        }
        return true;
    }

    public synchronized List<Player> getRankedPlayers() {
        List<Player> ranked = new ArrayList<>(players);
        ranked.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
        return ranked;
    }

    public synchronized String generateLeaderboardString() {
        StringBuffer sb = new StringBuffer();
        sb.append("=================== LIVE LEADERBOARD ===================\n");
        sb.append(String.format("%-6s %-18s %-10s %-12s\n", "Rank", "Player", "Score", "Accuracy"));
        sb.append("--------------------------------------------------------\n");

        List<Player> ranked = getRankedPlayers();
        int rank = 1;
        for (Player p : ranked) {
            int total = Math.max(1, p.getTotalAnswered());
            int pct = (p.getCorrectCount() * 100) / total;
            String acc = p.getCorrectCount() + "/" + p.getTotalAnswered() + " (" + pct + "%)";
            sb.append(String.format("#%-5d %-18s %-10d %-12s\n", rank++, p.getUsername(), p.getScore(), acc));
        }
        sb.append("========================================================");
        return sb.toString();
    }

    public synchronized String toLeaderboardPayload() {
        StringBuffer sb = new StringBuffer();
        List<Player> ranked = getRankedPlayers();
        for (int i = 0; i < ranked.size(); i++) {
            Player p = ranked.get(i);
            sb.append(p.getUsername()).append(":")
              .append(p.getScore()).append(":")
              .append(p.getCorrectCount()).append(":")
              .append(p.getTotalAnswered());
            if (i < ranked.size() - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public synchronized String generateGameOverSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append("=== FINAL QUIZ RESULTS ===\n");
        List<Player> ranked = getRankedPlayers();
        if (!ranked.isEmpty()) {
            sb.append("WINNER: ").append(ranked.get(0).getUsername())
              .append(" with ").append(ranked.get(0).getScore()).append(" points!\n\n");
        }
        sb.append(generateLeaderboardString());
        return sb.toString();
    }
}

