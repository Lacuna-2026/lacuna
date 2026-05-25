package lacuna.network;

public final class CreateSessionResult {
    public enum Status {
        CREATED,
        INVALID_PLAYER_NAME,
        REJECTED_BY_SERVER,
        CONNECTION_FAILED
    }

    private final Status status;
    private final OnlineSessionConnection connection;

    private CreateSessionResult(Status status, OnlineSessionConnection connection) {
        this.status = status;
        this.connection = connection;
    }

    public static CreateSessionResult created(OnlineSessionConnection connection) {
        return new CreateSessionResult(Status.CREATED, connection);
    }

    public static CreateSessionResult failed(Status status) {
        return new CreateSessionResult(status, null);
    }

    public boolean isCreated() {
        return status == Status.CREATED;
    }

    public Status getStatus() {
        return status;
    }

    public OnlineSessionConnection getConnection() {
        return connection;
    }
}
