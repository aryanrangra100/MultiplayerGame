package quiz.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public abstract class Connection {
    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected volatile boolean connected;

    public Connection() {
        this.socket = null;
        this.in = null;
        this.out = null;
        this.connected = false;
    }

    public Connection(Socket socket) throws ConnectionException {
        this.socket = socket;
        initializeStreams();
    }

    protected void initializeStreams() throws ConnectionException {
        try {
            if (socket == null || socket.isClosed()) {
                throw new ConnectionException("Socket is null or closed.");
            }
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.connected = true;
        } catch (IOException e) {
            this.connected = false;
            throw new ConnectionException("Failed to initialize connection streams: " + e.getMessage(), e);
        }
    }

    public synchronized void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }

    public abstract void handleMessage(String message) throws ConnectionException;

    public synchronized void disconnect() {
        connected = false;
        closeStreams();
    }

    protected synchronized void closeStreams() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (out != null) out.close();
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getRemoteAddress() {
        if (socket != null && socket.getRemoteSocketAddress() != null) {
            return socket.getRemoteSocketAddress().toString();
        }
        return "Unknown";
    }
}

