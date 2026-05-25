package lacuna.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LacunaServerClient {
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 38400;
    public static final int STATUS_TIMEOUT_MS = 700;
    public static final int STATUS_CHECK_MS = 2000;
    public static final int REQUEST_TIMEOUT_MS = 2000;
    public static final int SESSION_ID_LENGTH = 5;

    private static final String SESSION_ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public boolean ping() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), STATUS_TIMEOUT_MS);
            socket.setSoTimeout(STATUS_TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("PING");
            return "pong".equals(in.readLine());
        } catch (IOException e) {
            return false;
        }
    }

    public JoinSessionResult joinFriendSession(String playerName, String sessionId) {
        String cleanName = cleanPlayerName(playerName);
        if (cleanName.isEmpty()) {
            return JoinSessionResult.failed(JoinSessionResult.Status.INVALID_PLAYER_NAME);
        }

        String cleanSessionId = cleanSessionId(sessionId);
        if (!isPossibleSessionId(cleanSessionId)) {
            return JoinSessionResult.failed(JoinSessionResult.Status.INVALID_SESSION_ID);
        }

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(HOST, PORT), REQUEST_TIMEOUT_MS);
            socket.setSoTimeout(REQUEST_TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("JOIN " + cleanName + " " + cleanSessionId);

            String response = in.readLine();
            if (!"OK".equals(response)) {
                closeSocket(socket);
                return JoinSessionResult.failed(JoinSessionResult.Status.REJECTED_BY_SERVER);
            }

            socket.setSoTimeout(0);
            return JoinSessionResult.joined(
                    new OnlineSessionConnection(socket, in, out, cleanName, cleanSessionId)
            );
        } catch (IOException e) {
            closeSocket(socket);
            return JoinSessionResult.failed(JoinSessionResult.Status.CONNECTION_FAILED);
        }
    }

    public CreateSessionResult createSession(String playerName, boolean privateSession) {
        String cleanName = cleanPlayerName(playerName);
        if (cleanName.isEmpty()) {
            return CreateSessionResult.failed(CreateSessionResult.Status.INVALID_PLAYER_NAME);
        }

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(HOST, PORT), REQUEST_TIMEOUT_MS);
            socket.setSoTimeout(REQUEST_TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(privateSession ? "CREAT " + cleanName + " 1" : "CREAT " + cleanName);

            String response = in.readLine();
            String sessionId = readCreatedSessionId(response);
            if (sessionId.isEmpty()) {
                closeSocket(socket);
                return CreateSessionResult.failed(CreateSessionResult.Status.REJECTED_BY_SERVER);
            }

            socket.setSoTimeout(0);
            return CreateSessionResult.created(
                    new OnlineSessionConnection(socket, in, out, cleanName, sessionId)
            );
        } catch (IOException e) {
            closeSocket(socket);
            return CreateSessionResult.failed(CreateSessionResult.Status.CONNECTION_FAILED);
        }
    }

    public ListPublicSessionsResult listPublicSessions() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), REQUEST_TIMEOUT_MS);
            socket.setSoTimeout(REQUEST_TIMEOUT_MS);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("LIST");

            String countLine = in.readLine();
            int count = Integer.parseInt(countLine);
            if (count < 0) {
                return ListPublicSessionsResult.failed(ListPublicSessionsResult.Status.BAD_RESPONSE);
            }

            List<PublicSession> sessions = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String line = in.readLine();
                PublicSession session = readPublicSession(line);
                if (session == null) {
                    return ListPublicSessionsResult.failed(ListPublicSessionsResult.Status.BAD_RESPONSE);
                }
                sessions.add(session);
            }

            return ListPublicSessionsResult.ok(sessions);
        } catch (NumberFormatException e) {
            return ListPublicSessionsResult.failed(ListPublicSessionsResult.Status.BAD_RESPONSE);
        } catch (IOException e) {
            return ListPublicSessionsResult.failed(ListPublicSessionsResult.Status.CONNECTION_FAILED);
        }
    }

    private static String cleanPlayerName(String playerName) {
        if (playerName == null) {
            return "";
        }

        return playerName.trim().replaceAll("\\s+", "_");
    }

    private static String cleanSessionId(String sessionId) {
        if (sessionId == null) {
            return "";
        }

        return sessionId.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isPossibleSessionId(String sessionId) {
        if (sessionId.length() != SESSION_ID_LENGTH) {
            return false;
        }

        for (int i = 0; i < sessionId.length(); i++) {
            if (SESSION_ID_CHARACTERS.indexOf(sessionId.charAt(i)) < 0) {
                return false;
            }
        }

        return true;
    }

    private static String readCreatedSessionId(String response) {
        if (response == null) {
            return "";
        }

        String[] parts = response.trim().split("\\s+");
        if (parts.length != 2 || !"OK".equals(parts[0])) {
            return "";
        }

        String sessionId = cleanSessionId(parts[1]);
        return isPossibleSessionId(sessionId) ? sessionId : "";
    }

    private static PublicSession readPublicSession(String line) {
        if (line == null) {
            return null;
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length != 2) {
            return null;
        }

        String sessionId = cleanSessionId(parts[1]);
        if (!isPossibleSessionId(sessionId)) {
            return null;
        }

        return new PublicSession(parts[0], sessionId);
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignore) {
        }
    }
}
