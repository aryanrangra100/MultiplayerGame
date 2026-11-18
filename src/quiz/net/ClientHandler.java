package quiz.net;

import quiz.model.Player;
import quiz.server.QuizServer;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler extends Connection implements Runnable {
    private QuizServer server;
    private Player player;
    private Thread workerThread;

    public ClientHandler(Socket socket, QuizServer server) throws ConnectionException {
        super(socket);
        this.server = server;
        this.player = null;
    }

    public void start() {
        workerThread = new Thread(this, "ClientHandler-" + getRemoteAddress());
        workerThread.start();
    }

    @Override
    public void run() {
        try {
            sendMessage("WELCOME:Connected to Quiz Server v1.0. Send LOGIN:<username> to begin.");
            String line;
            while (connected && (line = in.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    handleMessage(line);
                }
            }
        } catch (IOException e) {

        } catch (ConnectionException e) {
            System.err.println("Connection error for " + (player != null ? player.getUsername() : getRemoteAddress()) + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    @Override
    public void handleMessage(String message) throws ConnectionException {
        try {
            int colonIdx = message.indexOf(':');
            String command = (colonIdx != -1) ? message.substring(0, colonIdx).toUpperCase() : message.toUpperCase();
            String payload = (colonIdx != -1) ? message.substring(colonIdx + 1) : "";

            switch (command) {
                case "LOGIN":
                    handleLogin(payload);
                    break;
                case "READY":
                    handleReady();
                    break;
                case "ANSWER":
                    handleAnswer(payload);
                    break;
                case "REQUEST_LEADERBOARD":
                    handleRequestLeaderboard();
                    break;
                case "QUIT":
                    disconnect();
                    break;
                default:
                    sendMessage("SYSTEM:Unknown command received: " + command);
                    break;
            }
        } catch (Exception e) {
            throw new ConnectionException("Failed to process message: " + message, e);
        }
    }

    private void handleLogin(String username) {
        if (player != null) {
            sendMessage("SYSTEM:Already logged in as " + player.getUsername());
            return;
        }
        server.onPlayerLogin(this, username);
    }

    private void handleReady() {
        if (player != null) {
            player.setReady(true);
            server.onPlayerReady(this);
        }
    }

    private void handleAnswer(String payload) {
        if (player == null) {
            sendMessage("SYSTEM:You must login first.");
            return;
        }

        String[] parts = payload.split(":");
        if (parts.length >= 3) {
            int qId = Integer.parseInt(parts[0]);
            int choice = Integer.parseInt(parts[1]);
            long timeTaken = Long.parseLong(parts[2]);
            server.onPlayerAnswer(player, qId, choice, timeTaken);
        }
    }

    private void handleRequestLeaderboard() {
        server.sendLeaderboard(this);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public synchronized void disconnect() {
        if (connected) {
            super.disconnect();
            if (server != null) {
                server.onPlayerDisconnected(this);
            }
        }
    }
}

