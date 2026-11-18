# Multiplayer Quiz Game 🎮

A network quiz game where multiple players connect concurrently to a central server and compete on the same questions in real time.

---

## 📌 Project Architecture & Syllabus Concepts Applied

This project strictly adheres to the course syllabus requirements across all 5 units:

| Unit | Syllabus Concept Required | Implementation in Project |
| :--- | :--- | :--- |
| **Unit 1** | Classes `Player`, `Question`, `Game`; `String` and `StringBuffer` | - [`Player.java`](file:///c:/project/MultiplayerGame/src/quiz/model/Player.java): Represents player state, uses `StringBuffer` for stats and round summaries.<br>- [`Question.java`](file:///c:/project/MultiplayerGame/src/quiz/model/Question.java): Multiple-choice questions, uses `String` & `StringBuffer` for network serialization.<br>- [`Game.java`](file:///c:/project/MultiplayerGame/src/quiz/model/Game.java): Session state, score logic, formatted text leaderboard via `StringBuffer`. |
| **Unit 2** | Interface `GameEvent`; abstract class `Connection`; package `quiz.net` | - Package: `quiz.net`<br>- [`GameEvent.java`](file:///c:/project/MultiplayerGame/src/quiz/net/GameEvent.java): Interface with network callbacks for player join, question broadcast, answer results, and leaderboard updates.<br>- [`Connection.java`](file:///c:/project/MultiplayerGame/src/quiz/net/Connection.java): Abstract base class managing `Socket`, `BufferedReader`, `PrintWriter`, and message routing. |
| **Unit 3** | `ServerSocket` and `Socket` communication; thread per player; `ConnectionException` | - [`QuizServer.java`](file:///c:/project/MultiplayerGame/src/quiz/server/QuizServer.java): Uses `ServerSocket(port)` to listen for client connections.<br>- [`ClientHandler.java`](file:///c:/project/MultiplayerGame/src/quiz/net/ClientHandler.java): Implements `Runnable` ("thread per player") to handle each contestant independently.<br>- [`QuizClientConnection.java`](file:///c:/project/MultiplayerGame/src/quiz/net/QuizClientConnection.java): Uses `Socket` for client-side bidirectional communication.<br>- [`ConnectionException.java`](file:///c:/project/MultiplayerGame/src/quiz/net/ConnectionException.java): Custom exception class for socket I/O errors. |
| **Unit 4** | JDBC to save scores and leaderboard (Tables: `players`, `questions`, `scores`) | - [`DatabaseManager.java`](file:///c:/project/MultiplayerGame/src/quiz/db/DatabaseManager.java): Uses SQLite JDBC with `PreparedStatement`, `Statement`, and `ResultSet`.<br>- Automatically initializes schema and seeds default questions.<br>- Persists final match results and queries all-time high score rankings. |
| **Unit 5** | Swing `JLabel`, `JButton` options; `BorderLayout` and `GridLayout`; action events | - [`QuizClientGUI.java`](file:///c:/project/MultiplayerGame/src/quiz/client/QuizClientGUI.java):<br>  * `BorderLayout`: North (Timer & Question Index), Center (Question & Choices), East (Live Scoreboard), South (Player Info & Actions).<br>  * `GridLayout`: (2x2) organizing the four option buttons (`JButton` A, B, C, D).<br>  * `JLabel`: Question display, countdown timer badge, live status banners.<br>  * Action Events: Attached to option buttons, "Ready" button, and Leaderboard triggers with visual color feedback. |

---

## 🚀 Modules Breakdown

### 1. Quiz Server (`quiz.server.QuizServer`, `ServerGUI`)
- Starts a `ServerSocket` on configurable port (default `5555`).
- Listens for incoming connections, delegating each to a dedicated `ClientHandler` thread.
- Offers both a **Swing Control Panel GUI** (`ServerGUI`) and headless execution mode.

### 2. Player Client & Login (`quiz.client.QuizClientGUI`, `quiz.net.QuizClientConnection`)
- Modern Swing GUI with connection dialog.
- Authenticates player credentials against the `players` table in the database.
- Displays live lobby state and readiness indicators.

### 3. Question Broadcast (`quiz.server.QuizServer`)
- Questions retrieved from the `questions` database table.
- Simultaneously broadcasts the exact same question to all connected clients.
- Synchronized countdown timer ticks pushed to clients every second.

### 4. Score Calculation (`quiz.model.Game`)
- **Base Score**: 100 points for correct answer.
- **Speed Bonus**: Up to 50 additional points calculated dynamically:
  $$\text{SpeedBonus} = \left(\frac{\text{RemainingTime}}{\text{TotalTime}}\right) \times 50$$
- **Streak Bonus**: Up to 50 points based on consecutive correct answers.
- **Wrong Answer**: 0 points with streak reset.

### 5. Leaderboard (`quiz.db.DatabaseManager`, `Game.toLeaderboardPayload()`)
- **Live Match Leaderboard**: Dynamic ranking table updated after every question.
- **Persistent Database Leaderboard**: All-time high scores saved into the `scores` table via JDBC and viewable at any time.

---

## 🗄️ Database Tables Schema

```sql
-- 1. Players Table
CREATE TABLE IF NOT EXISTS players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Questions Table
CREATE TABLE IF NOT EXISTS questions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    question_text TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option INTEGER NOT NULL, -- 0 for A, 1 for B, 2 for C, 3 for D
    category TEXT
);

-- 3. Scores Table
CREATE TABLE IF NOT EXISTS scores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    questions_answered INTEGER NOT NULL,
    correct_answers INTEGER NOT NULL,
    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(id)
);
```

---

## 💻 How to Run

### Step 1: Compile the Project
Double click `build.bat` or run:
```cmd
javac -cp ".;lib\*" -d bin src\quiz\model\*.java src\quiz\net\*.java src\quiz\db\*.java src\quiz\server\*.java src\quiz\client\*.java src\quiz\test\*.java
```

### Step 2: Start the Quiz Server
Double click `run-server.bat` or run:
```cmd
java --enable-native-access=ALL-UNNAMED -cp "bin;lib\*" quiz.server.ServerGUI
```
Click **"Start Server"** (default port `5555`).

### Step 3: Launch Player Clients
Double click `run-client.bat` to launch contestant clients:
```cmd
java --enable-native-access=ALL-UNNAMED -cp "bin;lib\*" quiz.client.QuizClientGUI
```
- Enter your username and connect to `localhost:5555`.
- Open a second instance of `run-client.bat` with a different username to test multiplayer competition!
- Click **"Ready for Match"** on both clients; the countdown starts automatically.

### Step 4: Run Automated Simulation Test
To test the complete networking, database, scoring, and leaderboard flow headlessly:
```cmd
run-test.bat
```

