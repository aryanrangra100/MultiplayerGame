package quiz.server;

import quiz.db.DatabaseManager;
import quiz.net.ConnectionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerGUI extends JFrame implements QuizServer.ServerLogListener {
    private QuizServer server;
    private JTextField portField;
    private JButton startButton;
    private JButton stopButton;
    private JButton startGameButton;
    private JButton viewLeaderboardButton;
    private JLabel statusLabel;
    private JLabel playerCountLabel;
    private JLabel gameStateLabel;
    private JTextArea logArea;

    public ServerGUI() {
        super("Multiplayer Quiz Server - Control Panel");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 520);
        setMinimumSize(new Dimension(650, 450));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(245, 247, 250));

        JPanel northPanel = new JPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createTitledBorder("Server Configuration & Status"));
        northPanel.setBackground(Color.WHITE);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlsPanel.setOpaque(false);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        portField = new JTextField("5555", 6);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        startButton = new JButton("Start Server");
        startButton.setBackground(new Color(40, 167, 69));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 12));

        stopButton = new JButton("Stop Server");
        stopButton.setBackground(new Color(220, 53, 69));
        stopButton.setForeground(Color.WHITE);
        stopButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stopButton.setEnabled(false);

        startGameButton = new JButton("Force Start Game");
        startGameButton.setBackground(new Color(0, 123, 255));
        startGameButton.setForeground(Color.WHITE);
        startGameButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        startGameButton.setEnabled(false);

        viewLeaderboardButton = new JButton("DB Leaderboard");
        viewLeaderboardButton.setBackground(new Color(108, 117, 125));
        viewLeaderboardButton.setForeground(Color.WHITE);
        viewLeaderboardButton.setFont(new Font("Segoe UI", Font.BOLD, 12));

        controlsPanel.add(portLabel);
        controlsPanel.add(portField);
        controlsPanel.add(startButton);
        controlsPanel.add(stopButton);
        controlsPanel.add(startGameButton);
        controlsPanel.add(viewLeaderboardButton);

        JPanel statusGrid = new JPanel(new GridLayout(1, 3, 10, 5));
        statusGrid.setOpaque(false);
        statusGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel("Status: OFFLINE", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        playerCountLabel = new JLabel("Connected Players: 0", JLabel.CENTER);
        playerCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        gameStateLabel = new JLabel("Game State: IDLE", JLabel.CENTER);
        gameStateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        statusGrid.add(statusLabel);
        statusGrid.add(playerCountLabel);
        statusGrid.add(gameStateLabel);

        northPanel.add(controlsPanel, BorderLayout.NORTH);
        northPanel.add(statusGrid, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Live Activity Log"));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(230, 230, 230));
        JScrollPane scrollPane = new JScrollPane(logArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        add(mainPanel);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStartServer();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStopServer();
            }
        });

        startGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (server != null) {
                    server.startGameManually();
                }
            }
        });

        viewLeaderboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDatabaseLeaderboard();
            }
        });
    }

    private void handleStartServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            server = new QuizServer(port);
            server.setLogListener(this);
            server.startServer();

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            startGameButton.setEnabled(true);
            portField.setEditable(false);
            statusLabel.setText("Status: RUNNING (Port " + port + ")");
            statusLabel.setForeground(new Color(40, 167, 69));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid port number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (ConnectionException ex) {
            JOptionPane.showMessageDialog(this, "Error starting server: " + ex.getMessage(), "Server Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleStopServer() {
        if (server != null) {
            server.stopServer();
            server = null;
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        startGameButton.setEnabled(false);
        portField.setEditable(true);
        statusLabel.setText("Status: OFFLINE");
        statusLabel.setForeground(Color.RED);
        gameStateLabel.setText("Game State: IDLE");
        playerCountLabel.setText("Connected Players: 0");
    }

    private void showDatabaseLeaderboard() {
        String data = DatabaseManager.getInstance().getGlobalLeaderboardFormatted(10);
        JTextArea area = new JTextArea(data);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(550, 300));
        JOptionPane.showMessageDialog(this, scroll, "Database Leaderboard (Unit 4 JDBC)", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void onLog(final String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onPlayerCountChanged(final int count) {
        SwingUtilities.invokeLater(() -> {
            playerCountLabel.setText("Connected Players: " + count);
        });
    }

    @Override
    public void onGameStateChanged(final String stateName) {
        SwingUtilities.invokeLater(() -> {
            gameStateLabel.setText("Game State: " + stateName);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}

