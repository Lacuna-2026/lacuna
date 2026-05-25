package lacuna.network;

public final class PeerJoinResult {
    public enum Status {
        JOINED,
        CONNECTION_CLOSED
    }

    private final Status status;
    private final String peerName;

    private PeerJoinResult(Status status, String peerName) {
        this.status = status;
        this.peerName = peerName;
    }

    public static PeerJoinResult joined(String peerName) {
        return new PeerJoinResult(Status.JOINED, peerName);
    }

    public static PeerJoinResult connectionClosed() {
        return new PeerJoinResult(Status.CONNECTION_CLOSED, "");
    }

    public boolean isJoined() {
        return status == Status.JOINED;
    }

    public Status getStatus() {
        return status;
    }

    public String getPeerName() {
        return peerName;
    }
}
