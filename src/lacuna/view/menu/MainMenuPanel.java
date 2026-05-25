package lacuna.view.menu;

import lacuna.network.CreateSessionResult;
import lacuna.network.JoinSessionResult;
import lacuna.network.LacunaServerClient;
import lacuna.network.ListPublicSessionsResult;
import lacuna.network.OnlineSessionConnection;
import lacuna.network.PeerJoinResult;
import lacuna.network.PublicSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.CancellationException;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MainMenuPanel extends JPanel {
    private static final String MODE_NORMAL = "Normal";
    private static final String MODE_AI = "Contre IA";
    private static final String MODE_ONLINE = "En ligne";
    private static final String ONLINE_FRIEND = "Avec un ami";
    private static final String ONLINE_CREATE = "Creer";
    private static final String ONLINE_PUBLIC = "Partie publique";
    private static final int CONTROLS_WIDTH = 420;
    private static final Random RANDOM = new Random();

    private final BufferedImage orangeFlower = MenuAssets.load("main_men_top_orange.png");
    private final BufferedImage blueFlower = MenuAssets.load("main_men_blue.png");
    private final BufferedImage pinkFlower = MenuAssets.load("main_men_pink.png");

    private final BiConsumer<String, String> onLocalPlay;
    private final BiConsumer<String, String> onAiPlay;
    private final Consumer<OnlineSessionConnection> onFriendSessionJoined;
    private final Consumer<OnlineSessionConnection> onCreatedSessionJoined;
    private final SegmentedChoice modeSelect;
    private final JPanel modeFields;
    private final PrimaryButton playButton;
    private final LacunaServerClient serverClient;

    private Timer serverStatusTimer;
    private boolean serverStatusCheckRunning;
    private StatusDot serverStatusDot;
    private JLabel serverStatusLabel;
    private JLabel offlineNoticeLabel;
    private JLabel joinFeedbackLabel;
    private JLabel createFeedbackLabel;
    private JLabel createdSessionStatusLabel;
    private JLabel publicSessionsFeedbackLabel;
    private SegmentedChoice onlineActionSelect;
    private JTextField onlinePlayerNameField;
    private JTextField friendSessionCodeField;
    private ToggleSwitch privateSessionSwitch;
    private JButton joinSessionButton;
    private JButton createSessionButton;
    private JButton cancelCreatedSessionButton;
    private JButton joinOpenSessionButton;
    private PublicSessionsList publicSessionsList;
    private JPanel onlineOptionsPanel;
    private Timer publicSessionsTimer;
    private boolean joiningFriendSession;
    private boolean joiningPublicSession;
    private boolean creatingSession;
    private boolean waitingForCreatedPeer;
    private boolean publicSessionsRefreshRunning;
    private PublicSession selectedPublicSession;
    private OnlineSessionConnection createdSessionConnection;
    private SwingWorker<PeerJoinResult, Void> peerWaitWorker;

    public MainMenuPanel(
            BiConsumer<String, String> onLocalPlay,
            BiConsumer<String, String> onAiPlay,
            Consumer<OnlineSessionConnection> onFriendSessionJoined,
            Consumer<OnlineSessionConnection> onCreatedSessionJoined) {
        this.onLocalPlay = onLocalPlay;
        this.onAiPlay = onAiPlay;
        this.onFriendSessionJoined = onFriendSessionJoined;
        this.onCreatedSessionJoined = onCreatedSessionJoined;
        this.modeSelect = new SegmentedChoice(MODE_NORMAL, MODE_AI, MODE_ONLINE);
        this.modeFields = MenuTheme.verticalPanel();
        this.playButton = new PrimaryButton("Jouer Lacuna");
        this.serverClient = new LacunaServerClient();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1100, 680));
        setMinimumSize(new Dimension(920, 620));
        setBackground(MenuTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(40, 58, 22, 58));

        modeSelect.addActionListener((ActionEvent e) -> updateModeFields());

        add(createContent(), BorderLayout.CENTER);

        showNormalFields();
    }

    public void showOnlineMode() {
        modeSelect.selectValue(MODE_ONLINE);
    }

    private JComponent createContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints art = new GridBagConstraints();
        art.gridx = 0;
        art.gridy = 0;
        art.weightx = 1;
        art.weighty = 1;
        art.fill = GridBagConstraints.BOTH;
        content.add(new ProductArtPanel(), art);

        GridBagConstraints controls = new GridBagConstraints();
        controls.gridx = 1;
        controls.gridy = 0;
        controls.weightx = 0;
        controls.weighty = 1;
        controls.fill = GridBagConstraints.VERTICAL;
        controls.anchor = GridBagConstraints.CENTER;
        controls.insets = new Insets(36, 48, 30, 44);
        content.add(createControls(), controls);

        return content;
    }

    private JComponent createControls() {
        JPanel controls = MenuTheme.verticalPanel();
        controls.setPreferredSize(new Dimension(CONTROLS_WIDTH, 560));
        controls.setMinimumSize(new Dimension(CONTROLS_WIDTH, 0));

        controls.add(Box.createVerticalGlue());
        controls.add(MenuTheme.label("Mode de jeu", 16, MenuTheme.MUTED_TEXT));
        controls.add(Box.createVerticalStrut(10));
        controls.add(modeSelect);
        controls.add(Box.createVerticalStrut(26));
        controls.add(modeFields);
        controls.add(Box.createVerticalStrut(28));
        controls.add(playButton);
        controls.add(Box.createVerticalGlue());

        return controls;
    }

    private void updateModeFields() {
        if (waitingForCreatedPeer) {
            return;
        }

        String mode = modeSelect.selectedValue();
        if (MODE_AI.equals(mode)) {
            showAiFields();
        } else if (MODE_ONLINE.equals(mode)) {
            showOnlineFields();
        } else {
            showNormalFields();
        }
    }

    private void showNormalFields() {
        cancelCreatedSessionWait(false);
        stopPublicSessionRefreshes();
        stopServerStatusChecks();
        modeFields.removeAll();

        JTextField playerOne = MenuTheme.textField("Joueur 1");
        JTextField playerTwo = MenuTheme.textField("Joueur 2");
        addLabeledField("Nom du joueur 1", playerOne);
        addLabeledField("Nom du joueur 2", playerTwo);

        playButton.prepare("Jouer Lacuna", true, true);
        playButton.addActionListener((ActionEvent e) -> {
            String nom1 = valueOrDefault(playerOne.getText(), "Joueur 1");
            String nom2 = valueOrDefault(playerTwo.getText(), "Joueur 2");
            onPlay.start(nom1, nom2, 0);
        });

        refreshFields();
    }

    private void showAiFields() {
        cancelCreatedSessionWait(false);
        stopPublicSessionRefreshes();
        stopServerStatusChecks();
        modeFields.removeAll();

        JTextField playerName = MenuTheme.textField("Votre nom");
        SegmentedChoice levels = new SegmentedChoice("Facile", "Moyen", "Difficile");

        addLabeledField("Votre nom", playerName);
        addLabeledField("Niveau de l'IA", levels);
        modeFields.add(Box.createVerticalStrut(8));

        playButton.prepare("Jouer contre l'IA", true, true);
        playButton.addActionListener((ActionEvent e) -> {
            String nom = valueOrDefault(playerName.getText(), "Joueur");
            String niveau = levels.selectedValue();
            onAiPlay.accept(nom, niveau);
        });

        refreshFields();
    }

    private void showOnlineFields() {
        cancelCreatedSessionWait(false);
        stopPublicSessionRefreshes();
        modeFields.removeAll();
        JTextField playerName = MenuTheme.textField(randomOnlinePlayerName());
        onlinePlayerNameField = playerName;
        serverStatusDot = new StatusDot();
        serverStatusLabel = MenuTheme.label("", 14, MenuTheme.MUTED_TEXT);
        onlineActionSelect = new SegmentedChoice(ONLINE_FRIEND, ONLINE_CREATE, ONLINE_PUBLIC);
        onlineOptionsPanel = MenuTheme.verticalPanel();
        offlineNoticeLabel = MenuTheme.label("Impossible de jouer tant que le serveur est hors ligne.", 12, MenuTheme.MUTED_TEXT);

        onlineActionSelect.addActionListener((ActionEvent e) -> updateOnlineOptions());

        modeFields.add(createServerStatusRow());
        modeFields.add(Box.createVerticalStrut(10));
        addLabeledField("Votre nom", playerName);
        modeFields.add(Box.createVerticalStrut(18));
        modeFields.add(MenuTheme.label("Option", 14, MenuTheme.MUTED_TEXT));
        modeFields.add(Box.createVerticalStrut(8));
        modeFields.add(onlineActionSelect);
        modeFields.add(Box.createVerticalStrut(10));
        modeFields.add(onlineOptionsPanel);
        modeFields.add(Box.createVerticalStrut(8));
        modeFields.add(offlineNoticeLabel);

        playButton.prepare("", true, false);
        updateServerStatus(false);
        updateOnlineOptions();
        startServerStatusChecks();
        refreshFields();
    }

    private JPanel createServerStatusRow() {
        JPanel statusRow = new JPanel();
        statusRow.setOpaque(false);
        statusRow.setLayout(new BoxLayout(statusRow, BoxLayout.X_AXIS));
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusRow.setMaximumSize(new Dimension(420, 18));
        statusRow.add(serverStatusDot);
        statusRow.add(Box.createHorizontalStrut(10));
        statusRow.add(serverStatusLabel);
        statusRow.add(Box.createHorizontalGlue());
        return statusRow;
    }

    private void showJoinFriendOptions() {
        stopPublicSessionRefreshes();
        onlineOptionsPanel.removeAll();
        JTextField sessionCode = MenuTheme.textField("");
        friendSessionCodeField = sessionCode;
        joinSessionButton = new PrimaryButton("Rejoindre");
        joinSessionButton.addActionListener((ActionEvent e) -> joinFriendSession());
        joinFeedbackLabel = MenuTheme.label("", 12, new Color(235, 86, 99));
        joinFeedbackLabel.setVisible(false);

        onlineOptionsPanel.add(MenuTheme.label("ID de session", 14, MenuTheme.MUTED_TEXT));
        onlineOptionsPanel.add(Box.createVerticalStrut(8));
        onlineOptionsPanel.add(sessionCode);
        onlineOptionsPanel.add(Box.createVerticalStrut(12));
        onlineOptionsPanel.add(joinSessionButton);
        onlineOptionsPanel.add(Box.createVerticalStrut(8));
        onlineOptionsPanel.add(joinFeedbackLabel);
        updateOnlineActionButtons();
        refreshFields();
    }

    private void showPlayOnlineOptions() {
        stopPublicSessionRefreshes();
        onlineOptionsPanel.removeAll();
        selectedPublicSession = null;
        publicSessionsList = new PublicSessionsList();
        publicSessionsList.setEmptyText("Chargement des sessions...");
        publicSessionsList.addSelectionListener(() -> {
            selectedPublicSession = publicSessionsList.getSelectedSession();
            setPublicSessionsFeedback("", MenuTheme.MUTED_TEXT);
            updateOnlineActionButtons();
        });
        joinOpenSessionButton = new PrimaryButton("Rejoindre la partie");
        joinOpenSessionButton.addActionListener((ActionEvent e) -> joinSelectedPublicSession());
        publicSessionsFeedbackLabel = MenuTheme.label("", 12, MenuTheme.MUTED_TEXT);
        publicSessionsFeedbackLabel.setVisible(false);

        onlineOptionsPanel.add(MenuTheme.label("Sessions ouvertes", 14, MenuTheme.MUTED_TEXT));
        onlineOptionsPanel.add(Box.createVerticalStrut(8));
        onlineOptionsPanel.add(publicSessionsList);
        onlineOptionsPanel.add(Box.createVerticalStrut(12));
        onlineOptionsPanel.add(joinOpenSessionButton);
        onlineOptionsPanel.add(Box.createVerticalStrut(8));
        onlineOptionsPanel.add(publicSessionsFeedbackLabel);
        updateOnlineActionButtons();
        startPublicSessionRefreshes();
        refreshFields();
    }

    private void showCreateSessionOptions() {
        stopPublicSessionRefreshes();
        onlineOptionsPanel.removeAll();
        ToggleSwitch privateSession = new ToggleSwitch();
        privateSessionSwitch = privateSession;

        createSessionButton = new OrangeButton("Creer");
        createSessionButton.addActionListener((ActionEvent e) -> createOnlineSession());
        createFeedbackLabel = MenuTheme.label("", 12, new Color(235, 86, 99));
        createFeedbackLabel.setVisible(false);

        onlineOptionsPanel.add(createPrivateSessionRow(privateSession));
        onlineOptionsPanel.add(Box.createVerticalStrut(12));
        onlineOptionsPanel.add(createSessionButton);
        onlineOptionsPanel.add(Box.createVerticalStrut(8));
        onlineOptionsPanel.add(createFeedbackLabel);
        updateOnlineActionButtons();
        refreshFields();
    }

    private JComponent createPrivateSessionRow(ToggleSwitch privateSession) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        row.setMaximumSize(new Dimension(420, 48));
        row.add(privateSession);
        row.add(Box.createHorizontalStrut(16));
        row.add(MenuTheme.label("Session privee", 14, MenuTheme.MUTED_TEXT));
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private void updateOnlineOptions() {
        if (onlineActionSelect == null) {
            return;
        }
        if (waitingForCreatedPeer) {
            return;
        }

        String option = onlineActionSelect.selectedValue();

        if (ONLINE_CREATE.equals(option)) {
            showCreateSessionOptions();
        } else if (ONLINE_PUBLIC.equals(option)) {
            showPlayOnlineOptions();
        } else {
            showJoinFriendOptions();
        }
    }

    private void startServerStatusChecks() {
        stopServerStatusChecks();
        checkServerStatus();
        serverStatusTimer = new Timer(LacunaServerClient.STATUS_CHECK_MS, (ActionEvent e) -> checkServerStatus());
        serverStatusTimer.start();
    }

    private void stopServerStatusChecks() {
        if (serverStatusTimer != null) {
            serverStatusTimer.stop();
            serverStatusTimer = null;
        }
        serverStatusCheckRunning = false;
    }

    private void startPublicSessionRefreshes() {
        stopPublicSessionsTimer();
        refreshPublicSessions();
        publicSessionsTimer = new Timer(LacunaServerClient.STATUS_CHECK_MS, (ActionEvent e) -> refreshPublicSessions());
        publicSessionsTimer.start();
    }

    private void stopPublicSessionRefreshes() {
        stopPublicSessionsTimer();
        selectedPublicSession = null;
        publicSessionsList = null;
        publicSessionsFeedbackLabel = null;
    }

    private void stopPublicSessionsTimer() {
        if (publicSessionsTimer != null) {
            publicSessionsTimer.stop();
            publicSessionsTimer = null;
        }
        publicSessionsRefreshRunning = false;
    }

    private void refreshPublicSessions() {
        if (publicSessionsRefreshRunning || publicSessionsList == null || joiningPublicSession) {
            return;
        }

        if (serverStatusDot == null || !serverStatusDot.isOnline()) {
            publicSessionsList.setEmptyText("Serveur hors ligne.");
            publicSessionsList.clearSessions();
            selectedPublicSession = null;
            setPublicSessionsFeedback("", MenuTheme.MUTED_TEXT);
            updateOnlineActionButtons();
            return;
        }

        publicSessionsRefreshRunning = true;
        SwingWorker<ListPublicSessionsResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ListPublicSessionsResult doInBackground() {
                return serverClient.listPublicSessions();
            }

            @Override
            protected void done() {
                publicSessionsRefreshRunning = false;
                if (publicSessionsList == null || joiningPublicSession) {
                    return;
                }

                try {
                    updatePublicSessions(get());
                } catch (Exception e) {
                    setPublicSessionsFeedback("Impossible de charger les sessions.", new Color(235, 86, 99));
                }
            }
        };
        worker.execute();
    }

    private void updatePublicSessions(ListPublicSessionsResult result) {
        if (!result.isOk()) {
            publicSessionsList.setEmptyText("Impossible de charger les sessions.");
            publicSessionsList.clearSessions();
            selectedPublicSession = null;
            setPublicSessionsFeedback("", MenuTheme.MUTED_TEXT);
            updateOnlineActionButtons();
            return;
        }

        String selectedId = selectedPublicSession == null ? "" : selectedPublicSession.getSessionId();
        if (result.getSessions().isEmpty()) {
            publicSessionsList.setEmptyText("Aucune session ouverte.");
        }
        publicSessionsList.setSessions(result.getSessions(), selectedId);
        selectedPublicSession = publicSessionsList.getSelectedSession();
        setPublicSessionsFeedback("", MenuTheme.MUTED_TEXT);
        updateOnlineActionButtons();
    }

    private void checkServerStatus() {
        if (serverStatusCheckRunning) {
            return;
        }

        serverStatusCheckRunning = true;
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return serverClient.ping();
            }

            @Override
            protected void done() {
                serverStatusCheckRunning = false;
                try {
                    updateServerStatus(get());
                } catch (Exception e) {
                    updateServerStatus(false);
                }
            }
        };
        worker.execute();
    }

    private void updateServerStatus(boolean online) {
        if (serverStatusDot != null) {
            serverStatusDot.setOnline(online);
        }

        if (serverStatusLabel != null) {
            serverStatusLabel.setText(online ? "Serveur : en ligne" : "Serveur : hors ligne");
            serverStatusLabel.setForeground(online ? new Color(104, 214, 132) : new Color(235, 86, 99));
        }

        if (offlineNoticeLabel != null) {
            offlineNoticeLabel.setVisible(!online);
        }

        if (!online && publicSessionsList != null) {
            publicSessionsList.setEmptyText("Serveur hors ligne.");
            publicSessionsList.clearSessions();
            selectedPublicSession = null;
            setPublicSessionsFeedback("", MenuTheme.MUTED_TEXT);
        }

        updateOnlineActionButtons();
        refreshFields();
    }

    private void updateOnlineActionButtons() {
        boolean online = serverStatusDot != null && serverStatusDot.isOnline();
        boolean busy = joiningFriendSession || joiningPublicSession || creatingSession || waitingForCreatedPeer;
        modeSelect.setEnabled(!busy);
        if (onlinePlayerNameField != null) {
            onlinePlayerNameField.setEnabled(!busy);
        }
        if (onlineActionSelect != null) {
            onlineActionSelect.setEnabled(online && !busy);
        }
        if (friendSessionCodeField != null) {
            friendSessionCodeField.setEnabled(online && !busy);
        }
        if (privateSessionSwitch != null) {
            privateSessionSwitch.setEnabled(online && !busy);
        }
        if (joinSessionButton != null) {
            joinSessionButton.setEnabled(online && !busy);
        }
        if (createSessionButton != null) {
            createSessionButton.setEnabled(online && !busy);
        }
        if (joinOpenSessionButton != null) {
            joinOpenSessionButton.setEnabled(online && !busy && selectedPublicSession != null);
        }
        if (cancelCreatedSessionButton != null) {
            cancelCreatedSessionButton.setEnabled(waitingForCreatedPeer);
        }
    }

    private void joinFriendSession() {
        if (joiningFriendSession) {
            return;
        }

        joiningFriendSession = true;
        setJoinFeedback("Connexion a la session...", MenuTheme.MUTED_TEXT);
        updateOnlineActionButtons();

        String playerName = onlinePlayerNameField == null ? "" : onlinePlayerNameField.getText();
        String sessionId = friendSessionCodeField == null ? "" : friendSessionCodeField.getText();

        SwingWorker<JoinSessionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected JoinSessionResult doInBackground() {
                return serverClient.joinFriendSession(playerName, sessionId);
            }

            @Override
            protected void done() {
                joiningFriendSession = false;
                try {
                    handleJoinFriendResult(get());
                } catch (Exception e) {
                    setJoinFeedback("Impossible de rejoindre la session.", new Color(235, 86, 99));
                }
                updateOnlineActionButtons();
            }
        };
        worker.execute();
    }

    private void joinSelectedPublicSession() {
        if (joiningPublicSession || selectedPublicSession == null) {
            return;
        }

        joiningPublicSession = true;
        setPublicSessionsFeedback("Connexion a la session...", MenuTheme.MUTED_TEXT);
        updateOnlineActionButtons();

        String playerName = onlinePlayerNameField == null ? "" : onlinePlayerNameField.getText();
        String sessionId = selectedPublicSession.getSessionId();

        SwingWorker<JoinSessionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected JoinSessionResult doInBackground() {
                return serverClient.joinFriendSession(playerName, sessionId);
            }

            @Override
            protected void done() {
                joiningPublicSession = false;
                try {
                    handlePublicSessionJoinResult(get());
                } catch (Exception e) {
                    setPublicSessionsFeedback("Impossible de rejoindre la session.", new Color(235, 86, 99));
                }
                updateOnlineActionButtons();
            }
        };
        worker.execute();
    }

    private void handlePublicSessionJoinResult(JoinSessionResult result) {
        if (result.isJoined()) {
            onFriendSessionJoined.accept(result.getConnection());
            return;
        }

        if (result.getStatus() == JoinSessionResult.Status.INVALID_PLAYER_NAME) {
            setPublicSessionsFeedback("Entrez un nom avant de rejoindre une session.", new Color(235, 86, 99));
        } else {
            setPublicSessionsFeedback("Impossible de rejoindre la session.", new Color(235, 86, 99));
        }
        refreshPublicSessions();
    }

    private void handleJoinFriendResult(JoinSessionResult result) {
        if (result.isJoined()) {
            onFriendSessionJoined.accept(result.getConnection());
            return;
        }

        if (result.getStatus() == JoinSessionResult.Status.INVALID_PLAYER_NAME) {
            setJoinFeedback("Entrez un nom avant de rejoindre une session.", new Color(235, 86, 99));
        } else if (result.getStatus() == JoinSessionResult.Status.INVALID_SESSION_ID) {
            setJoinFeedback("ID de session invalide.", new Color(235, 86, 99));
        } else {
            setJoinFeedback("Impossible de rejoindre la session.", new Color(235, 86, 99));
        }
    }

    private void setJoinFeedback(String message, Color color) {
        if (joinFeedbackLabel == null) {
            return;
        }

        joinFeedbackLabel.setText(message);
        joinFeedbackLabel.setForeground(color);
        joinFeedbackLabel.setVisible(!message.isEmpty());
        refreshFields();
    }

    private void setPublicSessionsFeedback(String message, Color color) {
        if (publicSessionsFeedbackLabel == null) {
            return;
        }

        publicSessionsFeedbackLabel.setText(message);
        publicSessionsFeedbackLabel.setForeground(color);
        publicSessionsFeedbackLabel.setVisible(!message.isEmpty());
        refreshFields();
    }

    private void createOnlineSession() {
        if (creatingSession) {
            return;
        }

        creatingSession = true;
        setCreateFeedback("Creation de la session...", MenuTheme.MUTED_TEXT);
        updateOnlineActionButtons();

        String playerName = onlinePlayerNameField == null ? "" : onlinePlayerNameField.getText();
        boolean privateSession = privateSessionSwitch != null && privateSessionSwitch.isSelected();

        SwingWorker<CreateSessionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected CreateSessionResult doInBackground() {
                return serverClient.createSession(playerName, privateSession);
            }

            @Override
            protected void done() {
                creatingSession = false;
                try {
                    handleCreateSessionResult(get());
                } catch (Exception e) {
                    setCreateFeedback("Impossible de creer la session.", new Color(235, 86, 99));
                }
                updateOnlineActionButtons();
            }
        };
        worker.execute();
    }

    private void handleCreateSessionResult(CreateSessionResult result) {
        if (result.isCreated()) {
            showCreatedSessionWait(result.getConnection());
            return;
        }

        if (result.getStatus() == CreateSessionResult.Status.INVALID_PLAYER_NAME) {
            setCreateFeedback("Entrez un nom avant de creer une session.", new Color(235, 86, 99));
        } else {
            setCreateFeedback("Impossible de creer la session.", new Color(235, 86, 99));
        }
    }

    private void showCreatedSessionWait(OnlineSessionConnection connection) {
        stopPublicSessionRefreshes();
        waitingForCreatedPeer = true;
        createdSessionConnection = connection;
        createdSessionStatusLabel = MenuTheme.label("En attente d'un autre joueur...", 12, MenuTheme.MUTED_TEXT);
        cancelCreatedSessionButton = new RedButton("Annuler");
        cancelCreatedSessionButton.addActionListener((ActionEvent e) -> cancelCreatedSessionWait(true));

        JTextField sessionId = MenuTheme.textField(connection.getSessionId());
        sessionId.setEditable(false);
        sessionId.setFocusable(false);

        modeFields.removeAll();
        modeFields.add(createServerStatusRow());
        modeFields.add(Box.createVerticalStrut(14));
        modeFields.add(MenuTheme.label("Session creee", 14, MenuTheme.MUTED_TEXT));
        modeFields.add(Box.createVerticalStrut(8));
        modeFields.add(sessionId);
        modeFields.add(Box.createVerticalStrut(10));
        modeFields.add(createdSessionStatusLabel);
        modeFields.add(Box.createVerticalStrut(14));
        modeFields.add(cancelCreatedSessionButton);

        playButton.prepare("", true, false);
        updateOnlineActionButtons();
        refreshFields();
        waitForCreatedSessionPeer(connection);
    }

    private void waitForCreatedSessionPeer(OnlineSessionConnection connection) {
        peerWaitWorker = new SwingWorker<>() {
            @Override
            protected PeerJoinResult doInBackground() {
                return connection.waitForPeerJoin();
            }

            @Override
            protected void done() {
                if (createdSessionConnection != connection) {
                    return;
                }

                try {
                    PeerJoinResult result = get();
                    peerWaitWorker = null;
                    if (result.isJoined()) {
                        OnlineSessionConnection joinedConnection = createdSessionConnection;
                        createdSessionConnection = null;
                        waitingForCreatedPeer = false;
                        onCreatedSessionJoined.accept(joinedConnection);
                    } else {
                        showCreatedSessionInterrupted();
                    }
                } catch (CancellationException ignored) {
                    peerWaitWorker = null;
                } catch (Exception e) {
                    peerWaitWorker = null;
                    showCreatedSessionInterrupted();
                }
            }
        };
        peerWaitWorker.execute();
    }

    private void showCreatedSessionInterrupted() {
        if (createdSessionStatusLabel != null) {
            createdSessionStatusLabel.setText("Connexion interrompue.");
            createdSessionStatusLabel.setForeground(new Color(235, 86, 99));
        }
        if (cancelCreatedSessionButton != null) {
            cancelCreatedSessionButton.setText("Retour");
        }
        refreshFields();
    }

    private void cancelCreatedSessionWait(boolean returnToOnlineFields) {
        if (peerWaitWorker != null) {
            peerWaitWorker.cancel(true);
            peerWaitWorker = null;
        }

        if (createdSessionConnection != null) {
            createdSessionConnection.close();
            createdSessionConnection = null;
        }

        waitingForCreatedPeer = false;
        createdSessionStatusLabel = null;
        cancelCreatedSessionButton = null;
        modeSelect.setEnabled(true);

        if (returnToOnlineFields) {
            showOnlineFields();
        }
    }

    private void setCreateFeedback(String message, Color color) {
        if (createFeedbackLabel == null) {
            return;
        }

        createFeedbackLabel.setText(message);
        createFeedbackLabel.setForeground(color);
        createFeedbackLabel.setVisible(!message.isEmpty());
        refreshFields();
    }

    private void addLabeledField(String label, JComponent field) {
        if (modeFields.getComponentCount() > 0) {
            modeFields.add(Box.createVerticalStrut(14));
        }
        modeFields.add(MenuTheme.label(label, 14, MenuTheme.MUTED_TEXT));
        modeFields.add(Box.createVerticalStrut(8));
        modeFields.add(field);
    }

    private void refreshFields() {
        modeFields.revalidate();
        modeFields.repaint();
        revalidate();
        repaint();
    }

    private static String valueOrDefault(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String randomOnlinePlayerName() {
        return String.format("Joueur%04d", RANDOM.nextInt(10000));
    }

    @Override
    public void removeNotify() {
        cancelCreatedSessionWait(false);
        stopPublicSessionRefreshes();
        stopServerStatusChecks();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        MenuAssets.prepare(g2);

        g2.setColor(MenuTheme.BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());

        MenuAssets.drawFit(g2, orangeFlower, getWidth() / 2 - 135, -86, 270, 245);
        MenuAssets.drawFit(g2, blueFlower, getWidth() - 270, 98, 168, 96);
        MenuAssets.drawFit(g2, pinkFlower, getWidth() - 190, getHeight() - 154, 165, 112);
        g2.dispose();
    }

}
