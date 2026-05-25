package lacuna.network;

import java.util.Collections;
import java.util.List;

public final class ListPublicSessionsResult {
    public enum Status {
        OK,
        CONNECTION_FAILED,
        BAD_RESPONSE
    }

    private final Status status;
    private final List<PublicSession> sessions;

    private ListPublicSessionsResult(Status status, List<PublicSession> sessions) {
        this.status = status;
        this.sessions = sessions;
    }

    public static ListPublicSessionsResult ok(List<PublicSession> sessions) {
        return new ListPublicSessionsResult(Status.OK, Collections.unmodifiableList(sessions));
    }

    public static ListPublicSessionsResult failed(Status status) {
        return new ListPublicSessionsResult(status, Collections.emptyList());
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    public List<PublicSession> getSessions() {
        return sessions;
    }
}
