package lacuna.network;

public final class PublicSession {
    private final String ownerName;
    private final String sessionId;

    public PublicSession(String ownerName, String sessionId) {
        this.ownerName = ownerName;
        this.sessionId = sessionId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return ownerName + "  -  " + sessionId;
    }
}
