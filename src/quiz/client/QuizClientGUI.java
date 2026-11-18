package quiz.client;

import quiz.db.DatabaseManager;
import quiz.model.Question;
import quiz.net.ConnectionException;
import quiz.net.GameEvent;
import quiz.net.QuizClientConnection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QuizClientGUI extends JFrame implements GameEvent {

    private QuizClientConnection connection;
    private String currentUsername;
    private int currentPlayerId;
    private int currentScore;

    private Question currentQuestion;
    private long questionStartTime;
    private int selectedOptionIndex;

    private JLabel titleLabel;
    private JLabel questionNumberLabel;
    private JLabel categoryLabel;
    private JLabel timerLabel;
    private JLabel questionTextLabel;
    private JLabel playerInfoLabel;
    private JLabel statusFeedbackLabel;

    private JButton[] optionButtons;
    private JButton readyButton;
    private JButton leaderboardButton;

    private DefaultTableModel leaderboardModel;
    private JTable leaderboardTable;
    private JProgressBar timerBar;

    public QuizClientGUI() {
        super("Multiplayer Quiz Game - Player Client");
        this.selectedOptionIndex = -1;
        this.currentScore = 0;
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 620);
        setMinimumSize(new Dimension(800, 520));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(243, 244, 246));

        JPanel northPanel = new JPanel(new BorderLayout(10, 5));
        northPanel.setBackground(Color.WHITE);
        northPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleLabel = new JLabel("Multiplayer Quiz Network");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(30, 41, 59));

        categoryLabel = new JLabel("Category: Waiting in Lobby...");
        categoryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        categoryLabel.setForeground(new Color(100, 116, 139));
        titleBox.add(titleLabel);
        titleBox.add(categoryLabel);

        JPanel progressBox = new JPanel(new BorderLayout(5, 5));
        progressBox.setOpaque(false);
        questionNumberLabel = new JLabel("Question - / -", SwingConstants.CENTER);
        questionNumberLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        questionNumberLabel.setForeground(new Color(79, 70, 229));

        timerBar = new JProgressBar(0, 15);
        timerBar.setValue(15);
        timerBar.setForeground(new Color(99, 102, 241));
        timerBar.setPreferredSize(new Dimension(180, 12));
        progressBox.add(questionNumberLabel, BorderLayout.NORTH);
        progressBox.add(timerBar, BorderLayout.SOUTH);

        timerLabel = new JLabel("15s", SwingConstants.CENTER);
        timerLabel.setPreferredSize(new Dimension(65, 45));
        timerLabel.setOpaque(true);
        timerLabel.setBackground(new Color(238, 242, 255));
        timerLabel.setForeground(new Color(67, 56, 202));
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        timerLabel.setBorder(BorderFactory.createLineBorder(new Color(199, 210, 254), 2));

        northPanel.add(titleBox, BorderLayout.WEST);
        northPanel.add(progressBox, BorderLayout.CENTER);
        northPanel.add(timerLabel, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));
        centerPanel.setOpaque(false);

        JPanel questionCard = new JPanel(new BorderLayout());
        questionCard.setBackground(Color.WHITE);
        questionCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(25, 20, 25, 20)
        ));

        questionTextLabel = new JLabel("<html><body style='text-align: center; width: 450px;'>Connect and click 'Ready' to begin the match!</body></html>", SwingConstants.CENTER);
        questionTextLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        questionTextLabel.setForeground(new Color(17, 24, 39));
        questionCard.add(questionTextLabel, BorderLayout.CENTER);

        JPanel optionsGrid = new JPanel(new GridLayout(2, 2, 14, 14));
        optionsGrid.setOpaque(false);
        optionsGrid.setPreferredSize(new Dimension(500, 180));

        optionButtons = new JButton[4];
        String[] prefixes = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            final int choiceIndex = i;
            JButton btn = new JButton(prefixes[i] + ": Option");
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(31, 41, 55));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            btn.setEnabled(false);

            btn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleOptionSelected(choiceIndex);
                }
            });

            optionButtons[i] = btn;
            optionsGrid.add(btn);
        }

        centerPanel.add(questionCard, BorderLayout.CENTER);
        centerPanel.add(optionsGrid, BorderLayout.SOUTH);

        JPanel eastPanel = new JPanel(new BorderLayout(5, 5));
        eastPanel.setPreferredSize(new Dimension(240, 0));
        eastPanel.setBackground(Color.WHITE);
        eastPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel scoreboardTitle = new JLabel("Live Leaderboard", SwingConstants.CENTER);
        scoreboardTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scoreboardTitle.setForeground(new Color(31, 41, 55));
        eastPanel.add(scoreboardTitle, BorderLayout.NORTH);

        String[] columnNames = {"Player", "Score", "Correct"};
        leaderboardModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        leaderboardTable = new JTable(leaderboardModel);
        leaderboardTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        leaderboardTable.setRowHeight(24);
        leaderboardTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane scrollTable = new JScrollPane(leaderboardTable);
        scrollTable.setBorder(BorderFactory.createEmptyBorder());
        eastPanel.add(scrollTable, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout(10, 5));
        southPanel.setBackground(Color.WHITE);
        southPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        playerInfoLabel = new JLabel("Player: Not logged in | Score: 0");
        playerInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        playerInfoLabel.setForeground(new Color(55, 65, 81));

        statusFeedbackLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusFeedbackLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        statusFeedbackLabel.setForeground(new Color(75, 85, 99));

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtonPanel.setOpaque(false);

        readyButton = new JButton("Ready for Match");
        readyButton.setBackground(new Color(16, 185, 129));
        readyButton.setForeground(Color.WHITE);
        readyButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        readyButton.setEnabled(false);
        readyButton.addActionListener(e -> {
            if (connection != null && connection.isConnected()) {
                connection.sendReady();
                readyButton.setEnabled(false);
                readyButton.setText("Ready!");
                statusFeedbackLabel.setText("You are ready! Waiting for other players or match start...");
            }
        });

        leaderboardButton = new JButton("All-Time High Scores");
        leaderboardButton.setBackground(new Color(107, 114, 128));
        leaderboardButton.setForeground(Color.WHITE);
        leaderboardButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        leaderboardButton.addActionListener(e -> showAllTimeLeaderboard());

        actionButtonPanel.add(readyButton);
        actionButtonPanel.add(leaderboardButton);

        southPanel.add(playerInfoLabel, BorderLayout.WEST);
        southPanel.add(statusFeedbackLabel, BorderLayout.CENTER);
        southPanel.add(actionButtonPanel, BorderLayout.EAST);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(eastPanel, BorderLayout.EAST);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    public void promptLogin(String defaultHost, int defaultPort) {
        JTextField hostField = new JTextField(defaultHost, 15);
        JTextField portField = new JTextField(String.valueOf(defaultPort), 6);
        JTextField nameField = new JTextField("Player" + (int)(Math.random() * 900 + 100), 12);

        JPanel dialogPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        dialogPanel.add(new JLabel("Server Host:"));
        dialogPanel.add(hostField);
        dialogPanel.add(new JLabel("Server Port:"));
        dialogPanel.add(portField);
        dialogPanel.add(new JLabel("Player Username:"));
        dialogPanel.add(nameField);

        int result = JOptionPane.showConfirmDialog(this, dialogPanel, "Join Multiplayer Quiz Game", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String username = nameField.getText().trim();
            connectToServer(host, port, username);
        } else {
            System.exit(0);
        }
    }

    private void connectToServer(String host, int port, String username) {
        statusFeedbackLabel.setText("Connecting to " + host + ":" + port + "...");
        this.currentUsername = username;

        connection = new QuizClientConnection(host, port, this);
        new Thread(() -> {
            try {

                connection.connect();
                connection.sendLogin(username);
            } catch (ConnectionException ex) {
                SwingUtilities.invokeLater(() -> {
                    statusFeedbackLabel.setText("Connection failed: " + ex.getMessage());
                    int retry = JOptionPane.showConfirmDialog(QuizClientGUI.this,
                            "Failed to connect: " + ex.getMessage() + "\nWould you like to try again?",
                            "Connection Error", JOptionPane.YES_NO_OPTION);
                    if (retry == JOptionPane.YES_OPTION) {
                        promptLogin(host, port);
                    }
                });
            }
        }).start();
    }

    private void handleOptionSelected(int choiceIndex) {
        if (currentQuestion == null || selectedOptionIndex != -1) return;

        selectedOptionIndex = choiceIndex;
        long timeTaken = System.currentTimeMillis() - questionStartTime;

        optionButtons[choiceIndex].setBackground(new Color(199, 210, 254));
        optionButtons[choiceIndex].setForeground(new Color(67, 56, 202));
        optionButtons[choiceIndex].setBorder(BorderFactory.createLineBorder(new Color(99, 102, 241), 2));

        for (JButton btn : optionButtons) {
            btn.setEnabled(false);
        }

        statusFeedbackLabel.setText("Answer submitted (" + (char)('A' + choiceIndex) + ") in " + (timeTaken / 1000.0) + "s! Waiting for others...");

        connection.sendAnswer(currentQuestion.getId(), choiceIndex, timeTaken);
    }

    @Override
    public void onConnected(String serverMessage) {
        statusFeedbackLabel.setText(serverMessage);
    }

    @Override
    public void onLoginSuccess(String username, int playerId) {
        this.currentUsername = username;
        this.currentPlayerId = playerId;
        this.currentScore = 0;
        playerInfoLabel.setText("Player: " + username + " (ID: " + playerId + ") | Score: 0");
        statusFeedbackLabel.setText("Logged in as " + username + ". Press 'Ready' to join the match!");
        readyButton.setEnabled(true);
    }

    @Override
    public void onPlayerJoined(String username, int totalPlayers) {
        statusFeedbackLabel.setText("Player " + username + " joined the lobby. (Total: " + totalPlayers + ")");
    }

    @Override
    public void onPlayerLeft(String username, int totalPlayers) {
        statusFeedbackLabel.setText("Player " + username + " left. (Total: " + totalPlayers + ")");
    }

    @Override
    public void onGameStarting(int countdownSeconds) {
        statusFeedbackLabel.setText("Match starting in " + countdownSeconds + " second(s)!");
        timerLabel.setText(countdownSeconds + "s");
        readyButton.setEnabled(false);
    }

    @Override
    public void onQuestionReceived(Question question, int questionIndex, int totalQuestions) {
        this.currentQuestion = question;
        this.selectedOptionIndex = -1;
        this.questionStartTime = System.currentTimeMillis();

        questionNumberLabel.setText("Question " + questionIndex + " / " + totalQuestions);
        categoryLabel.setText("Category: " + question.getCategory());
        questionTextLabel.setText("<html><body style='text-align: center; width: 450px;'>" + question.getQuestionText() + "</body></html>");

        int timeLimit = question.getTimeLimitSeconds();
        timerBar.setMaximum(timeLimit);
        timerBar.setValue(timeLimit);
        timerLabel.setText(timeLimit + "s");
        timerLabel.setBackground(new Color(238, 242, 255));
        timerLabel.setForeground(new Color(67, 56, 202));

        String[] prefixes = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText("<html><b>" + prefixes[i] + ":</b> " + question.getOption(i) + "</html>");
            optionButtons[i].setEnabled(true);
            optionButtons[i].setBackground(Color.WHITE);
            optionButtons[i].setForeground(new Color(31, 41, 55));
            optionButtons[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
        }

        statusFeedbackLabel.setText("Choose your answer quickly for maximum points!");
    }

    @Override
    public void onRoundTimeTick(int secondsRemaining) {
        timerLabel.setText(secondsRemaining + "s");
        timerBar.setValue(secondsRemaining);
        if (secondsRemaining <= 3) {
            timerLabel.setBackground(new Color(254, 226, 226));
            timerLabel.setForeground(new Color(185, 28, 28));
        }
    }

    @Override
    public void onAnswerResult(boolean correct, int correctOption, int pointsEarned, int totalScore) {
        this.currentScore = totalScore;
        playerInfoLabel.setText("Player: " + currentUsername + " | Score: " + currentScore);

        if (correct) {
            statusFeedbackLabel.setText("Correct! You earned +" + pointsEarned + " points!");
        } else {
            statusFeedbackLabel.setText("Incorrect! Correct answer was Option " + (char)('A' + correctOption));
        }
    }

    @Override
    public void onRoundEnded(int correctOption, String roundScores) {
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setEnabled(false);
            if (i == correctOption) {

                optionButtons[i].setBackground(new Color(209, 250, 229));
                optionButtons[i].setForeground(new Color(6, 95, 70));
                optionButtons[i].setBorder(BorderFactory.createLineBorder(new Color(16, 185, 129), 2));
            } else if (i == selectedOptionIndex) {

                optionButtons[i].setBackground(new Color(254, 226, 226));
                optionButtons[i].setForeground(new Color(153, 27, 27));
                optionButtons[i].setBorder(BorderFactory.createLineBorder(new Color(239, 68, 68), 2));
            }
        }
        statusFeedbackLabel.setText("Round over! Next question loading shortly...");
    }

    @Override
    public void onLeaderboardUpdated(String leaderboardPayload) {

        leaderboardModel.setRowCount(0);
        if (leaderboardPayload == null || leaderboardPayload.trim().isEmpty()) return;

        String[] entries = leaderboardPayload.split(";");
        for (String entry : entries) {
            String[] tokens = entry.split(":");
            if (tokens.length >= 4) {
                String name = tokens[0];
                String score = tokens[1];
                String correct = tokens[2] + "/" + tokens[3];
                leaderboardModel.addRow(new Object[]{name, score, correct});
            }
        }
    }

    @Override
    public void onGameOver(String summary) {
        statusFeedbackLabel.setText("Game Over! Check final scores on the leaderboard.");
        questionTextLabel.setText("<html><body style='text-align: center; font-size: 14pt;'>Match Complete!<br>Final Score: <b>" + currentScore + "</b> points.<br>Click 'Ready' to queue for the next game!</body></html>");
        readyButton.setEnabled(true);
        readyButton.setText("Ready for Match");

        for (JButton btn : optionButtons) {
            btn.setEnabled(false);
            btn.setText("Option");
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(31, 41, 55));
        }

        JOptionPane.showMessageDialog(this,
                "Game Over!\nYour Final Score: " + currentScore + " points.\nScores have been saved to the database.",
                "Match Results", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onSystemMessage(String message) {
        statusFeedbackLabel.setText(message);
    }

    @Override
    public void onConnectionError(ConnectionException exception) {
        statusFeedbackLabel.setText("Error: " + exception.getMessage());
        JOptionPane.showMessageDialog(this, exception.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showAllTimeLeaderboard() {
        String data = DatabaseManager.getInstance().getGlobalLeaderboardFormatted(10);
        JTextArea area = new JTextArea(data);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(550, 300));
        JOptionPane.showMessageDialog(this, scroll, "All-Time Database Leaderboard (JDBC)", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            QuizClientGUI client = new QuizClientGUI();
            client.setVisible(true);
            client.promptLogin("localhost", 5555);
        });
    }
}

