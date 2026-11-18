package quiz.model;

public class Question {
    private int id;
    private String questionText;
    private String[] options;
    private int correctOption;
    private String category;
    private int timeLimitSeconds;

    public Question(int id, String questionText, String optionA, String optionB, String optionC, String optionD, int correctOption, String category) {
        this.id = id;
        this.questionText = questionText;
        this.options = new String[] { optionA, optionB, optionC, optionD };
        this.correctOption = correctOption;
        this.category = (category == null || category.trim().isEmpty()) ? "General Knowledge" : category;
        this.timeLimitSeconds = 15;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String[] getOptions() {
        return options;
    }

    public String getOption(int index) {
        if (index >= 0 && index < options.length) {
            return options[index];
        }
        return "";
    }

    public int getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(int correctOption) {
        this.correctOption = correctOption;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public void setTimeLimitSeconds(int timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public boolean isCorrect(int chosenOption) {
        return chosenOption == this.correctOption;
    }

    public String getFormattedQuestion() {
        StringBuffer sb = new StringBuffer();
        sb.append("[").append(category).append("] ").append(questionText).append("\n");
        char label = 'A';
        for (int i = 0; i < options.length; i++) {
            sb.append("  ").append((char)(label + i)).append(") ").append(options[i]).append("\n");
        }
        return sb.toString();
    }

    public String toBroadcastPayload() {
        StringBuffer sb = new StringBuffer();
        sb.append(id).append("|||")
          .append(category).append("|||")
          .append(questionText).append("|||")
          .append(options[0]).append("|||")
          .append(options[1]).append("|||")
          .append(options[2]).append("|||")
          .append(options[3]).append("|||")
          .append(timeLimitSeconds);
        return sb.toString();
    }

    public static Question fromBroadcastPayload(String payload) {
        String[] parts = payload.split("\\|\\|\\|", -1);
        if (parts.length < 8) {
            return null;
        }
        int id = Integer.parseInt(parts[0]);
        String category = parts[1];
        String text = parts[2];
        String a = parts[3];
        String b = parts[4];
        String c = parts[5];
        String d = parts[6];
        int timeLimit = Integer.parseInt(parts[7]);

        Question q = new Question(id, text, a, b, c, d, -1, category);
        q.setTimeLimitSeconds(timeLimit);
        return q;
    }

    @Override
    public String toString() {
        return "Q" + id + ": " + questionText;
    }
}

