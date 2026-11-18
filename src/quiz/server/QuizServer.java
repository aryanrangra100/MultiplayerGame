package quiz.server;

import quiz.db.DatabaseManager;
import quiz.model.Game;
import quiz.model.Player;
import quiz.model.Question;
import quiz.net.ClientHandler;
import quiz.net.ConnectionException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class QuizServer implements Runnable {
    public interface ServerLogListener {
        void onLog(String message);
        void onPlayerCountChanged(int count);
        void onGameStateChanged(String stateName);
    }

    private int port;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private final List<ClientHandler> clients;
    private Game currentGame;
    private DatabaseManager dbManager;
    private Thread serverAcceptThread;
    private ServerLogListener logListener;

    private Timer gameTimer;
    private int currentRoundRemainingSeconds;

    public QuizServer(int port) {
        this.port = port;
        this.running = false;
        this.clients = Collections.synchronizedList(new ArrayList<>());
        this.currentGame = new Game("Session-" + System.currentTimeMillis());
        this.dbManager = DatabaseManager.getInstance();
    }

    public void setLogListener(ServerLogListener listener) {
        this.logListener = listener;
    }

    private void log(String message) {
        System.out.println("[SERVER] " + message);
        if (logListener != null) {
            logListener.onLog(message);
        }
    }

    public synchronized void startServer() throws ConnectionException {
        if (running) return;
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            serverAcceptThread = new Thread(this, "QuizServer-Acceptor");
            serverAcceptThread.start();
            log("Quiz Server started successfully on port " + port);
            if (logListener != null) {
                logListener.onGameStateChanged("WAITING FOR PLAYERS");
            }
        } catch (IOException e) {
            throw new ConnectionException("Failed to bind ServerSocket on port " + port + ": " + e.getMessage(), e);
        }
    }

    public synchronized void stopServer() {
        if (!running) return;
        running = false;
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        synchronized (clients) {
            for (ClientHandler client : new ArrayList<>(clients)) {
                client.sendMessage("SYSTEM:Server shutting down.");
                client.disconnect();
            }
            clients.clear();
        }
        log("Quiz Server stopped.");
    }

    @Override
    public void run() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {

                Socket clientSocket = serverSocket.accept();
                log("New player connection accepted from: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this);
                synchronized (clients) {
                    clients.add(handler);
                }
                handler.start();
                updatePlayerCount();
            } catch (IOException e) {
                if (running) {
                    log("Socket accept error: " + e.getMessage());
                }
            } catch (ConnectionException e) {
                log("Error initializing client handler: " + e.getMessage());
            }
        }
    }

    public synchronized void onPlayerLogin(ClientHandler handler, String username) {
        int playerId = dbManager.getOrCreatePlayer(username);
        Player player = new Player(playerId, username);
        handler.setPlayer(player);
        currentGame.addPlayer(player);

        log("Player '" + username + "' (ID: " + playerId + ") logged in successfully.");
        handler.sendMessage("LOGIN_OK:" + playerId + ":" + username);

        broadcast("PLAYER_JOINED:" + username + ":" + currentGame.getPlayers().size());
        broadcastLeaderboard();
        updatePlayerCount();

        handler.sendMessage("SYSTEM:Welcome, " + username + "! Click 'Ready' when you are prepared to start.");
    }

    public synchronized void onPlayerReady(ClientHandler handler) {
        Player p = handler.getPlayer();
        if (p == null) return;
        log("Player '" + p.getUsername() + "' is READY.");
        broadcast("SYSTEM:Player " + p.getUsername() + " is ready!");

        checkAndStartGame();
    }

    public synchronized void startGameManually() {
        if (currentGame.getCurrentState() == Game.State.WAITING || currentGame.getCurrentState() == Game.State.GAME_OVER) {
            if (currentGame.getPlayers().isEmpty()) {
                log("Cannot start game: No players connected.");
                return;
            }
            initiateGameCountdown();
        }
    }

    private synchronized void checkAndStartGame() {
        if (currentGame.getCurrentState() != Game.State.WAITING) return;
        List<Player> players = currentGame.getPlayers();
        if (players.isEmpty()) return;

        boolean allReady = true;
        for (Player p : players) {
            if (!p.isReady()) {
                allReady = false;
                break;
            }
        }

        if (allReady) {
            initiateGameCountdown();
        }
    }

    private synchronized void initiateGameCountdown() {
        currentGame.setCurrentState(Game.State.COUNTDOWN);
        if (logListener != null) logListener.onGameStateChanged("STARTING MATCH");
        log("Initiating game countdown for " + currentGame.getPlayers().size() + " player(s)...");

        List<Question> questions = dbManager.loadQuestions(5);
        currentGame.setQuestions(questions);

        if (gameTimer != null) gameTimer.cancel();
        gameTimer = new Timer("QuizGameTimer", true);

        gameTimer.scheduleAtFixedRate(new TimerTask() {
            int countdown = 3;
            @Override
            public void run() {
                if (countdown > 0) {
                    broadcast("STARTING:" + countdown);
                    broadcast("SYSTEM:Match starts in " + countdown + "...");
                    countdown--;
                } else {
                    cancel();
                    startNextRound();
                }
            }
        }, 0, 1000);
    }

    private synchronized void startNextRound() {
        if (!currentGame.hasMoreQuestions()) {
            endGame();
            return;
        }

        Question question = currentGame.nextQuestion();
        int qIdx = currentGame.getCurrentQuestionIndex() + 1;
        int total = currentGame.getTotalQuestions();

        log("Broadcasting Question " + qIdx + "/" + total + ": " + question.getQuestionText());
        if (logListener != null) logListener.onGameStateChanged("ROUND " + qIdx + "/" + total);

        broadcast("QUESTION:" + qIdx + ":" + total + ":" + question.toBroadcastPayload());

        currentRoundRemainingSeconds = question.getTimeLimitSeconds();
        if (gameTimer != null) gameTimer.cancel();
        gameTimer = new Timer("RoundTimer", true);

        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (QuizServer.this) {
                    if (currentGame.getCurrentState() != Game.State.IN_ROUND) {
                        cancel();
                        return;
                    }

                    broadcast("TICK:" + currentRoundRemainingSeconds);

                    if (currentRoundRemainingSeconds <= 0 || currentGame.haveAllAnswered()) {
                        cancel();
                        finishRound();
                    } else {
                        currentRoundRemainingSeconds--;
                    }
                }
            }
        }, 0, 1000);
    }

    public synchronized void onPlayerAnswer(Player player, int questionId, int choiceIndex, long timeTakenMs) {
        if (currentGame.getCurrentState() != Game.State.IN_ROUND) {
            return;
        }
        Question currentQ = currentGame.getCurrentQuestion();
        if (currentQ == null || currentQ.getId() != questionId) {
            return;
        }

        if (player.getSelectedOption() != -1) {

            return;
        }

        int pointsEarned = currentGame.calculateScore(player, choiceIndex, timeTakenMs, currentQ);
        boolean correct = currentQ.isCorrect(choiceIndex);

        log("Player " + player.getUsername() + " answered Option " + (char)('A' + choiceIndex) +
            " (" + (correct ? "CORRECT" : "INCORRECT") + ", +" + pointsEarned + " pts).");

        ClientHandler handler = findHandlerByPlayer(player);
        if (handler != null) {
            handler.sendMessage("ANSWER_RESULT:" + correct + ":" + currentQ.getCorrectOption() + ":" + pointsEarned + ":" + player.getScore());
        }

        if (currentGame.haveAllAnswered()) {
            if (gameTimer != null) gameTimer.cancel();
            finishRound();
        }
    }

    private synchronized void finishRound() {
        currentGame.setCurrentState(Game.State.ROUND_REVIEW);
        Question q = currentGame.getCurrentQuestion();
        int correctOpt = (q != null) ? q.getCorrectOption() : 0;

        StringBuffer summary = new StringBuffer();
        for (Player p : currentGame.getPlayers()) {
            boolean correct = (p.getSelectedOption() == correctOpt);
            summary.append(p.getUsername()).append(": ").append(correct ? "Correct" : "Wrong")
                   .append(" (Total: ").append(p.getScore()).append(" pts) | ");
        }

        log("Round ended. Correct answer: Option " + (char)('A' + correctOpt));
        broadcast("ROUND_END:" + correctOpt + ":" + summary.toString());
        broadcastLeaderboard();

        if (gameTimer != null) gameTimer.cancel();
        gameTimer = new Timer("ReviewTimer", true);
        gameTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                startNextRound();
            }
        }, 4000);
    }

    private synchronized void endGame() {
        currentGame.setCurrentState(Game.State.GAME_OVER);
        if (logListener != null) logListener.onGameStateChanged("GAME OVER");
        String finalSummary = currentGame.generateGameOverSummary();
        log("Match completed!\n" + finalSummary);

        for (Player p : currentGame.getPlayers()) {
            dbManager.saveScore(p.getId(), p.getScore(), p.getTotalAnswered(), p.getCorrectCount());
        }

        broadcast("GAME_OVER:" + currentGame.toLeaderboardPayload());
        broadcastLeaderboard();

        for (Player p : currentGame.getPlayers()) {
            p.setReady(false);
            p.setScore(0);
        }
        currentGame.setCurrentState(Game.State.WAITING);
    }

    public synchronized void broadcastLeaderboard() {
        String payload = currentGame.toLeaderboardPayload();
        broadcast("LEADERBOARD:" + payload);
    }

    public synchronized void sendLeaderboard(ClientHandler handler) {
        handler.sendMessage("LEADERBOARD:" + currentGame.toLeaderboardPayload());
    }

    public void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void onPlayerDisconnected(ClientHandler handler) {
        synchronized (clients) {
            clients.remove(handler);
        }
        Player p = handler.getPlayer();
        if (p != null) {
            currentGame.removePlayer(p);
            log("Player " + p.getUsername() + " disconnected.");
            broadcast("PLAYER_LEFT:" + p.getUsername() + ":" + currentGame.getPlayers().size());
            broadcastLeaderboard();
        }
        updatePlayerCount();
    }

    private void updatePlayerCount() {
        if (logListener != null) {
            logListener.onPlayerCountChanged(clients.size());
        }
    }

    private ClientHandler findHandlerByPlayer(Player p) {
        synchronized (clients) {
            for (ClientHandler h : clients) {
                if (h.getPlayer() == p) {
                    return h;
                }
            }
        }
        return null;
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public static void main(String[] args) {
        int port = 5555;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        QuizServer server = new QuizServer(port);
        try {
            server.startServer();
            System.out.println("Quiz Server is running. Press Enter to stop...");
            System.in.read();
            server.stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

