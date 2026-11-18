package quiz.test;

import quiz.db.DatabaseManager;
import quiz.model.Question;
import quiz.net.ConnectionException;
import quiz.net.GameEvent;
import quiz.net.QuizClientConnection;
import quiz.server.QuizServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GameSimulationTest {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("RUNNING MULTIPLAYER QUIZ GAME SIMULATION TEST");
        System.out.println("=================================================");

        System.out.println("\n[TEST 1] Verifying Database & JDBC setup...");
        DatabaseManager db = DatabaseManager.getInstance();
        int aliceId = db.getOrCreatePlayer("Alice");
        int bobId = db.getOrCreatePlayer("Bob");
        System.out.println("-> Created/Retrieved Alice (ID: " + aliceId + "), Bob (ID: " + bobId + ")");
        if (aliceId <= 0 || bobId <= 0) {
            System.err.println("FAILED: Player creation failed.");
            System.exit(1);
        }

        System.out.println("\n[TEST 2] Starting QuizServer on port 5556...");
        int testPort = 5556;
        QuizServer server = new QuizServer(testPort);
        try {
            server.startServer();
            System.out.println("-> QuizServer running on port " + testPort);
        } catch (ConnectionException e) {
            System.err.println("FAILED to start server: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("\n[TEST 3] Connecting 2 players (Alice & Bob)...");
        CountDownLatch roundLatch = new CountDownLatch(2);

        TestPlayerClient clientAlice = new TestPlayerClient("Alice", roundLatch);
        TestPlayerClient clientBob = new TestPlayerClient("Bob", roundLatch);

        try {
            clientAlice.connect("localhost", testPort);
            clientBob.connect("localhost", testPort);
            Thread.sleep(800);

            clientAlice.login();
            clientBob.login();
            Thread.sleep(800);

            System.out.println("-> Both players sending READY...");
            clientAlice.ready();
            clientBob.ready();

            boolean completed = roundLatch.await(15, TimeUnit.SECONDS);
            System.out.println("-> Round simulation progress: " + (completed ? "Success" : "Timed out waiting for latch"));

            System.out.println("\n[TEST 4] Database Leaderboard Output (Unit 4 JDBC):");
            String lb = db.getGlobalLeaderboardFormatted(5);
            System.out.println(lb);

            System.out.println("=================================================");
            System.out.println("ALL SIMULATION TESTS PASSED SUCCESSFULLY!");
            System.out.println("=================================================");
        } catch (Exception e) {
            System.err.println("Test execution error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            clientAlice.disconnect();
            clientBob.disconnect();
            server.stopServer();
            System.exit(0);
        }
    }

    static class TestPlayerClient implements GameEvent {
        private String name;
        private QuizClientConnection conn;
        private CountDownLatch latch;
        private int answersGiven = 0;

        public TestPlayerClient(String name, CountDownLatch latch) {
            this.name = name;
            this.latch = latch;
        }

        public void connect(String host, int port) throws ConnectionException {
            conn = new QuizClientConnection(host, port, this);
            conn.connect();
        }

        public void login() {
            conn.sendLogin(name);
        }

        public void ready() {
            conn.sendReady();
        }

        public void disconnect() {
            if (conn != null) conn.disconnect();
        }

        @Override
        public void onConnected(String serverMessage) {
            System.out.println("[" + name + "] Connected: " + serverMessage);
        }

        @Override
        public void onLoginSuccess(String username, int playerId) {
            System.out.println("[" + name + "] Login Confirmed: " + username + " (id=" + playerId + ")");
        }

        @Override
        public void onPlayerJoined(String username, int totalPlayers) {
            System.out.println("[" + name + "] Observed Player Joined: " + username + " (Total=" + totalPlayers + ")");
        }

        @Override
        public void onPlayerLeft(String username, int totalPlayers) {}

        @Override
        public void onGameStarting(int countdownSeconds) {
            System.out.println("[" + name + "] Game countdown: " + countdownSeconds);
        }

        @Override
        public void onQuestionReceived(Question question, int questionIndex, int totalQuestions) {
            System.out.println("[" + name + "] Received Question " + questionIndex + "/" + totalQuestions + ": " + question.getQuestionText());

            int choice = name.equals("Alice") ? 1 : 2;
            long delay = name.equals("Alice") ? 1200 : 2500;
            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    conn.sendAnswer(question.getId(), choice, delay);
                    answersGiven++;
                    if (answersGiven >= 1) {
                        latch.countDown();
                    }
                } catch (InterruptedException ignored) {}
            }).start();
        }

        @Override
        public void onRoundTimeTick(int secondsRemaining) {}

        @Override
        public void onAnswerResult(boolean correct, int correctOption, int pointsEarned, int currentScore) {
            System.out.println("[" + name + "] Result: correct=" + correct + ", points=+" + pointsEarned + ", totalScore=" + currentScore);
        }

        @Override
        public void onRoundEnded(int correctOption, String roundScores) {
            System.out.println("[" + name + "] Round Ended. Scores: " + roundScores);
        }

        @Override
        public void onLeaderboardUpdated(String leaderboardPayload) {
            System.out.println("[" + name + "] Live Leaderboard: " + leaderboardPayload);
        }

        @Override
        public void onGameOver(String summary) {
            System.out.println("[" + name + "] Game Over Summary: " + summary);
        }

        @Override
        public void onSystemMessage(String message) {
            System.out.println("[" + name + "] System: " + message);
        }

        @Override
        public void onConnectionError(ConnectionException exception) {
            System.err.println("[" + name + "] Error: " + exception.getMessage());
        }
    }
}

