package lacuna.network;

public final class JoinSessionResult {
    public enum Status {
        JOINED,
        INVALID_PLAYER_NAME,
        INVALID_SESSION_ID,
        REJECTED_BY_SERVER,
        CONNECTION_FAILED
    }

    private final Status status;
    private final OnlineSessionConnection connection;

    private JoinSessionResult(Status status, OnlineSessionConnection connection) {
        this.status = status;
        this.connection = connection;
    }

    public static JoinSessionResult joined(OnlineSessionConnection connection) {
        return new JoinSessionResult(Status.JOINED, connection);
    }

    public static JoinSessionResult failed(Status status) {
        return new JoinSessionResult(status, null);
    }

    public boolean isJoined() {
        return status == Status.JOINED;
    }

    public Status getStatus() {
        return status;
    }

    public OnlineSessionConnection getConnection() {
        return connection;
    }
}
