package quiz.net;

public class ConnectionException extends Exception {
    private int errorCode;

    public ConnectionException(String message) {
        super(message);
        this.errorCode = 0;
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 0;
    }

    public ConnectionException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        if (errorCode != 0) {
            return "ConnectionException [Error " + errorCode + "]: " + getMessage();
        }
        return "ConnectionException: " + getMessage();
    }
}

