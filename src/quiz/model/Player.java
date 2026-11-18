package quiz.model;

public class Player {
    private int id;
    private String username;
    private int score;
    private int selectedOption;
    private long answerTimeMs;
    private int correctCount;
    private int totalAnswered;
    private int streak;
    private boolean ready;

    public Player(String username) {
        this(-1, username);
    }

    public Player(int id, String username) {
        this.id = id;
        this.username = username;
        this.score = 0;
        this.selectedOption = -1;
        this.answerTimeMs = 0;
        this.correctCount = 0;
        this.totalAnswered = 0;
        this.streak = 0;
        this.ready = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(int selectedOption) {
        this.selectedOption = selectedOption;
    }

    public long getAnswerTimeMs() {
        return answerTimeMs;
    }

    public void setAnswerTimeMs(long answerTimeMs) {
        this.answerTimeMs = answerTimeMs;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void incrementCorrectCount() {
        this.correctCount++;
        this.streak++;
    }

    public void resetStreak() {
        this.streak = 0;
    }

    public int getStreak() {
        return streak;
    }

    public int getTotalAnswered() {
        return totalAnswered;
    }

    public void incrementTotalAnswered() {
        this.totalAnswered++;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void resetRoundState() {
        this.selectedOption = -1;
        this.answerTimeMs = 0;
    }

    public String getFormattedStats() {
        StringBuffer sb = new StringBuffer();
        sb.append("Player: ").append(username)
          .append(" | Score: ").append(score)
          .append(" | Correct: ").append(correctCount).append("/").append(totalAnswered)
          .append(" | Streak: ").append(streak);
        return sb.toString();
    }

    public String getRoundSummary(int pointsEarned, boolean correct) {
        StringBuffer sb = new StringBuffer();
        sb.append(username).append(" was ");
        if (correct) {
            sb.append("CORRECT (+").append(pointsEarned).append(" pts)");
            if (streak > 1) {
                sb.append(" [Streak x").append(streak).append("!]");
            }
        } else {
            sb.append("INCORRECT (+0 pts)");
        }
        sb.append(" (Answered in ").append(answerTimeMs).append(" ms)");
        return sb.toString();
    }

    @Override
    public String toString() {
        return username + " (" + score + " pts)";
    }
}

