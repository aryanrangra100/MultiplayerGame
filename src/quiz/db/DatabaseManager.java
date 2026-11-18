package quiz.db;

import quiz.model.Question;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:quizgame.db";
    private static DatabaseManager instance;

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private DatabaseManager() {
        initDatabase();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        try {

            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Notice: SQLite JDBC Driver not found in classpath. Standard fallback enabled: " + e.getMessage());
        }

        String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createQuestionsTable = "CREATE TABLE IF NOT EXISTS questions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "question_text TEXT NOT NULL, " +
                "option_a TEXT NOT NULL, " +
                "option_b TEXT NOT NULL, " +
                "option_c TEXT NOT NULL, " +
                "option_d TEXT NOT NULL, " +
                "correct_option INTEGER NOT NULL, " +
                "category TEXT" +
                ");";

        String createScoresTable = "CREATE TABLE IF NOT EXISTS scores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_id INTEGER NOT NULL, " +
                "score INTEGER NOT NULL, " +
                "questions_answered INTEGER NOT NULL, " +
                "correct_answers INTEGER NOT NULL, " +
                "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (player_id) REFERENCES players(id)" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createQuestionsTable);
            stmt.execute(createScoresTable);

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM questions;");
            if (rs.next() && rs.getInt(1) == 0) {
                seedInitialQuestions(conn);
            }
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    private void seedInitialQuestions(Connection conn) {
        String sql = "INSERT INTO questions (question_text, option_a, option_b, option_c, option_d, correct_option, category) VALUES (?, ?, ?, ?, ?, ?, ?);";
        Object[][] initialData = {
                {"Which Java keyword is used to inherit a class?", "implement", "extends", "inherits", "instanceof", 1, "Java Programming"},
                {"Which of the following creates a thread-safe string in Java?", "String", "StringBuilder", "StringBuffer", "CharBuffer", 2, "Java Programming"},
                {"Which Swing layout manager places components in North, South, East, West, and Center?", "GridLayout", "FlowLayout", "BorderLayout", "BoxLayout", 2, "Java Swing"},
                {"In networking, which protocol provides reliable, connection-oriented byte stream service?", "UDP", "TCP", "IP", "ICMP", 1, "Networking"},
                {"Which interface is implemented for executing a task on a separate thread?", "Callable", "Process", "Runnable", "ThreadRunner", 2, "Java Threads"},
                {"What does JDBC stand for?", "Java Data Broadcast Connector", "Java Database Connectivity", "Joint Database Connection", "Java Distributed Channel", 1, "Databases"},
                {"Which SQL clause is used to filter records resulting from a GROUP BY statement?", "WHERE", "ORDER BY", "HAVING", "LIMIT", 2, "SQL"},
                {"Which class in java.net is used to wait for incoming client connections?", "Socket", "ServerSocket", "DatagramSocket", "InetAddress", 1, "Networking"},
                {"Which method of JButton is used to attach an action listener in Swing?", "addListener()", "addActionListener()", "setAction()", "bindAction()", 1, "Java Swing"},
                {"What is the time complexity of searching an element in a balanced binary search tree?", "O(1)", "O(n)", "O(log n)", "O(n log n)", 2, "Computer Science"}
        };

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Object[] q : initialData) {
                pstmt.setString(1, (String) q[0]);
                pstmt.setString(2, (String) q[1]);
                pstmt.setString(3, (String) q[2]);
                pstmt.setString(4, (String) q[3]);
                pstmt.setString(5, (String) q[4]);
                pstmt.setInt(6, (Integer) q[5]);
                pstmt.setString(7, (String) q[6]);
                pstmt.executeUpdate();
            }
            System.out.println("Default quiz questions seeded into database successfully.");
        } catch (SQLException e) {
            System.err.println("Error seeding questions: " + e.getMessage());
        }
    }

    public synchronized int getOrCreatePlayer(String username) {
        String cleanUser = (username == null || username.trim().isEmpty()) ? "Player_" + System.currentTimeMillis() % 1000 : username.trim();
        String selectSql = "SELECT id FROM players WHERE username = ?;";
        String insertSql = "INSERT INTO players (username) VALUES (?);";

        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, cleanUser);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, cleanUser);
                pstmt.executeUpdate();
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in getOrCreatePlayer: " + e.getMessage());
        }
        return -1;
    }

    public synchronized List<Question> loadQuestions(int limit) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT id, question_text, option_a, option_b, option_c, option_d, correct_option, category " +
                     "FROM questions ORDER BY RANDOM() LIMIT ?;";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("question_text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d"),
                        rs.getInt("correct_option"),
                        rs.getString("category")
                );
                list.add(q);
            }
        } catch (SQLException e) {
            System.err.println("Error loading questions from DB: " + e.getMessage());
        }

        if (list.isEmpty()) {
            list.add(new Question(1, "Which keyword inherits a class in Java?", "implement", "extends", "inherits", "instanceof", 1, "Java"));
            list.add(new Question(2, "Which layout manager arranges components in a grid of cells?", "BorderLayout", "CardLayout", "GridLayout", "FlowLayout", 2, "Swing"));
            list.add(new Question(3, "Which class handles server socket connections?", "Socket", "ServerSocket", "DatagramSocket", "URL", 1, "Networking"));
        }
        return list;
    }

    public synchronized void saveScore(int playerId, int score, int totalAnswered, int correctAnswers) {
        if (playerId <= 0) return;
        String sql = "INSERT INTO scores (player_id, score, questions_answered, correct_answers) VALUES (?, ?, ?, ?);";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.setInt(2, score);
            pstmt.setInt(3, totalAnswered);
            pstmt.setInt(4, correctAnswers);
            pstmt.executeUpdate();
            System.out.println("Saved score for player ID " + playerId + ": " + score + " points.");
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
        }
    }

    public synchronized String getGlobalLeaderboardFormatted(int topN) {
        StringBuffer sb = new StringBuffer();
        sb.append("RANK | PLAYER           | HIGH SCORE | ACCURACY   | DATE\n");
        sb.append("-----+------------------+------------+------------+---------------------\n");

        String sql = "SELECT p.username, s.score, s.correct_answers, s.questions_answered, s.played_at " +
                     "FROM scores s " +
                     "JOIN players p ON s.player_id = p.id " +
                     "ORDER BY s.score DESC, s.played_at DESC LIMIT ?;";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topN);
            ResultSet rs = pstmt.executeQuery();
            int rank = 1;
            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                String user = rs.getString("username");
                int score = rs.getInt("score");
                int correct = rs.getInt("correct_answers");
                int total = rs.getInt("questions_answered");
                String date = rs.getString("played_at");
                String acc = correct + "/" + total;

                sb.append(String.format("#%-3d | %-16s | %-10d | %-10s | %s\n",
                        rank++, user, score, acc, (date != null ? date : "Just now")));
            }
            if (!hasRows) {
                sb.append("No recorded scores yet. Play a game to make history!\n");
            }
        } catch (SQLException e) {
            sb.append("Error retrieving leaderboard: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }
}

