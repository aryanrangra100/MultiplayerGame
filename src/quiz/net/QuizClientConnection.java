package quiz.net;

import quiz.model.Question;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.Socket;

public class QuizClientConnection extends Connection {
    private String host;
    private int port;
    private GameEvent eventListener;
    private Thread listenerThread;

    public QuizClientConnection(String host, int port, GameEvent eventListener) {
        super();
        this.host = host;
        this.port = port;
        this.eventListener = eventListener;
    }

    public void connect() throws ConnectionException {
        try {
            this.socket = new Socket(host, port);
            initializeStreams();
            startListening();
        } catch (IOException e) {
            throw new ConnectionException("Could not connect to Quiz Server at " + host + ":" + port + " (" + e.getMessage() + ")", e);
        }
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    final String msg = line.trim();
                    if (!msg.isEmpty()) {
                        handleMessage(msg);
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    dispatchError(new ConnectionException("Lost connection to server: " + e.getMessage(), e));
                }
            } catch (ConnectionException e) {
                dispatchError(e);
            } finally {
                disconnect();
            }
        }, "Client-Listener-" + host + ":" + port);
        listenerThread.start();
    }

    @Override
    public void handleMessage(String message) throws ConnectionException {
        int colonIdx = message.indexOf(':');
        String cmd = (colonIdx != -1) ? message.substring(0, colonIdx) : message;
        String payload = (colonIdx != -1) ? message.substring(colonIdx + 1) : "";

        switch (cmd) {
            case "WELCOME":
                dispatch(() -> eventListener.onConnected(payload));
                break;
            case "LOGIN_OK": {

                String[] p = payload.split(":", 2);
                int id = Integer.parseInt(p[0]);
                String user = p.length > 1 ? p[1] : "";
                dispatch(() -> eventListener.onLoginSuccess(user, id));
                break;
            }
            case "PLAYER_JOINED": {

                String[] p = payload.split(":", 2);
                String user = p[0];
                int count = p.length > 1 ? Integer.parseInt(p[1]) : 1;
                dispatch(() -> eventListener.onPlayerJoined(user, count));
                break;
            }
            case "PLAYER_LEFT": {
                String[] p = payload.split(":", 2);
                String user = p[0];
                int count = p.length > 1 ? Integer.parseInt(p[1]) : 0;
                dispatch(() -> eventListener.onPlayerLeft(user, count));
                break;
            }
            case "STARTING": {
                int sec = Integer.parseInt(payload);
                dispatch(() -> eventListener.onGameStarting(sec));
                break;
            }
            case "QUESTION": {

                int firstColon = payload.indexOf(':');
                int secondColon = payload.indexOf(':', firstColon + 1);
                int qIndex = Integer.parseInt(payload.substring(0, firstColon));
                int qTotal = Integer.parseInt(payload.substring(firstColon + 1, secondColon));
                String qData = payload.substring(secondColon + 1);

                Question question = Question.fromBroadcastPayload(qData);
                dispatch(() -> eventListener.onQuestionReceived(question, qIndex, qTotal));
                break;
            }
            case "TICK": {
                int sec = Integer.parseInt(payload);
                dispatch(() -> eventListener.onRoundTimeTick(sec));
                break;
            }
            case "ANSWER_RESULT": {

                String[] p = payload.split(":");
                boolean correct = Boolean.parseBoolean(p[0]);
                int correctOpt = Integer.parseInt(p[1]);
                int earned = Integer.parseInt(p[2]);
                int totalScore = Integer.parseInt(p[3]);
                dispatch(() -> eventListener.onAnswerResult(correct, correctOpt, earned, totalScore));
                break;
            }
            case "ROUND_END": {

                int cOpt = Integer.parseInt(payload.substring(0, payload.indexOf(':')));
                String summary = payload.substring(payload.indexOf(':') + 1);
                dispatch(() -> eventListener.onRoundEnded(cOpt, summary));
                break;
            }
            case "LEADERBOARD": {
                dispatch(() -> eventListener.onLeaderboardUpdated(payload));
                break;
            }
            case "GAME_OVER": {
                dispatch(() -> eventListener.onGameOver(payload));
                break;
            }
            case "SYSTEM": {
                dispatch(() -> eventListener.onSystemMessage(payload));
                break;
            }
            default:
                break;
        }
    }

    public void sendLogin(String username) {
        sendMessage("LOGIN:" + username);
    }

    public void sendReady() {
        sendMessage("READY");
    }

    public void sendAnswer(int questionId, int choiceIndex, long timeTakenMs) {
        sendMessage("ANSWER:" + questionId + ":" + choiceIndex + ":" + timeTakenMs);
    }

    public void requestLeaderboard() {
        sendMessage("REQUEST_LEADERBOARD");
    }

    private void dispatch(Runnable runnable) {
        if (eventListener != null) {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private void dispatchError(ConnectionException e) {
        if (eventListener != null) {
            SwingUtilities.invokeLater(() -> eventListener.onConnectionError(e));
        }
    }
}

