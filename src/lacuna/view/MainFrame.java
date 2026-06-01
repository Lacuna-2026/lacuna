package lacuna.view;

import lacuna.model.GameModel;
import lacuna.controller.GameController;
import lacuna.network.OnlineBoardSnapshot;
import lacuna.network.OnlineGameChannel;
import lacuna.network.OnlineGameMessage;
import lacuna.network.OnlineSessionConnection;
import lacuna.network.PeerJoinResult;
import lacuna.view.menu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

public class MainFrame extends JFrame {
    private static final Integer TURN_GLOW_LAYER = JLayeredPane.DEFAULT_LAYER + 50;

    private BoardPanel plateau;
    private PlayerPanel panelTop;
    private PlayerPanel panelBottom;
    private GameController controleur;
    private TutorialOverlay tutorialOverlay;
    private int demoMode;

    private JButton undoButton;
    private JButton redoButton;
    private JButton saveButton;
    private JButton loadButton;
    private JButton rejouerButton;
    private JButton helpButton;
    private JButton quitButton;
    private String lastNom1;
    private String lastNom2;
    private boolean lastP1IsAi;
    private int lastP1AiDepth;
    private boolean lastP2IsAi;
    private int lastP2AiDepth;
    private OnlineSessionConnection onlineSessionConnection;
    private SwingWorker<OnlineStartupResult, Void> onlineStartupWorker;
    private SwingWorker<PeerJoinResult, Void> onlinePeerWaitWorker;
    private OnlineWaitForPlayerDialog onlineWaitDialog;
    private boolean isOnlineSession;

    public BoardPanel getBoardPanel() {
        return plateau;
    }

    public PlayerPanel getPanelTop() {
        return panelTop;
    }

    public PlayerPanel getPanelBottom() {
        return panelBottom;
    }

    public MainFrame() {
        super("Lacuna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(920, 620));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                fermerSessionEnLigneActuelle();
            }
        });

        afficherMenu();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void afficherMenu() {
        MainMenuPanel menu = new MainMenuPanel(
                this::demarrerPartieLocale,
                this::demarrerPartieAvecIA,
                this::demarrerPartieAiVsAi,
                this::chargerPartie,
                this::afficherSessionRejointe,
                this::afficherSessionCreee,
                this::lancerDemo);

        setMinimumSize(new Dimension(550, 620));
        setContentPane(menu);
        revalidate();
        repaint();
    }

    private void demarrerPartieLocale(String nom1, String nom2) {
        fermerSessionEnLigneActuelle();
        isOnlineSession = false;
        demoMode = 0;
        construireInterface(nom1, nom2, false, 0, false, 0, null, -1);
    }

    private void demarrerPartieAvecIA(String nomJoueur, String niveau) {
        fermerSessionEnLigneActuelle();
        isOnlineSession = false;
        demoMode = 0;
        int depth = 1;
        String iaName = "IA (Facile)";
        if ("Moyen".equalsIgnoreCase(niveau)) {
            depth = 2;
            iaName = "IA (Moyen)";
        } else if ("Difficile".equalsIgnoreCase(niveau)) {
            depth = 5;
            iaName = "IA (Difficile)";
        }
        construireInterface(iaName, nomJoueur, true, depth, false, 0, null, -1);
    }

    private void demarrerPartieAiVsAi(String level1, String level2) {
        fermerSessionEnLigneActuelle();
        isOnlineSession = false;
        demoMode = 0;
        int depth1 = 1;
        String ia1Name = "IA 1 (Facile)";
        if ("Moyen".equalsIgnoreCase(level1)) {
            depth1 = 2;
            ia1Name = "IA 1 (Moyen)";
        } else if ("Difficile".equalsIgnoreCase(level1)) {
            depth1 = 5;
            ia1Name = "IA 1 (Difficile)";
        }

        int depth2 = 1;
        String ia2Name = "IA 2 (Facile)";
        if ("Moyen".equalsIgnoreCase(level2)) {
            depth2 = 2;
            ia2Name = "IA 2 (Moyen)";
        } else if ("Difficile".equalsIgnoreCase(level2)) {
            depth2 = 5;
            ia2Name = "IA 2 (Difficile)";
        }
        construireInterface(ia1Name, ia2Name, true, depth1, true, depth2, null, -1);
    }

    private void afficherSessionRejointe(OnlineSessionConnection sessionConnection) {
        demarrerPartieEnLigne(sessionConnection, false);
    }

    private void afficherSessionCreee(OnlineSessionConnection sessionConnection) {
        demarrerPartieEnLigne(sessionConnection, true);
    }

    private void demarrerPartieEnLigne(OnlineSessionConnection sessionConnection, boolean isHost) {
        annulerAttenteNouveauJoueur();
        if (onlineSessionConnection != sessionConnection) {
            fermerSessionEnLigneActuelle();
            onlineSessionConnection = sessionConnection;
        }
        isOnlineSession = true;

        String myName = sessionConnection.getPlayerName();
        int localPlayerIndex = isHost ? 0 : 1;

        lastNom1 = isHost ? myName : "Adversaire";
        lastNom2 = isHost ? "Adversaire" : myName;
        lastP1IsAi = false;
        lastP1AiDepth = 0;
        lastP2IsAi = false;
        lastP2AiDepth = 0;

        afficherPreparationEnLigne("Chargement de la partie...");

        onlineStartupWorker = new SwingWorker<>() {
            @Override
            protected OnlineStartupResult doInBackground() {
                OnlineGameChannel channel = new OnlineGameChannel(sessionConnection);
                try {
                    if (isHost) {
                        String peerName = cleanDisplayName(sessionConnection.getPeerName(), "Adversaire");
                        GameModel modele = new GameModel(myName, peerName, 0, new java.util.Random().nextLong(), true);
                        if (!channel.sendPlayerName(myName)) {
                            return OnlineStartupResult.failure("Impossible d'envoyer le nom du joueur.");
                        }
                        if (!channel.sendBoard(modele.toOnlineBoardSnapshot())) {
                            return OnlineStartupResult.failure("Impossible d'envoyer le plateau.");
                        }

                        OnlineGameMessage response = channel.receiveMessage();
                        if (response.getType() == OnlineGameMessage.Type.BOARD_ACCEPTED) {
                            return OnlineStartupResult.success(modele, channel, localPlayerIndex);
                        }
                        if (response.getType() == OnlineGameMessage.Type.BOARD_REJECTED) {
                            String reason = response.getReason();
                            if (reason == null || reason.isBlank()) {
                                reason = "plateau refuse par l'adversaire.";
                            }
                            return OnlineStartupResult.failure("Connexion annulee : " + reason);
                        }
                        return OnlineStartupResult.failure("Reponse inattendue de l'adversaire.");
                    }

                    String ownerName = cleanDisplayName(channel.receivePlayerName(), "Adversaire");
                    sessionConnection.rememberPeerName(ownerName);
                    OnlineBoardSnapshot snapshot = channel.receiveBoard();
                    String validationError = GameModel.validateOnlineBoardSnapshot(snapshot);
                    if (validationError != null) {
                        channel.sendBoardRejected(validationError);
                        return OnlineStartupResult.failure("Plateau refuse : " + validationError);
                    }

                    GameModel modele;
                    try {
                        modele = GameModel.fromOnlineBoardSnapshot(ownerName, myName, snapshot);
                    } catch (IllegalArgumentException e) {
                        String reason = e.getMessage() == null ? "Plateau invalide." : e.getMessage();
                        channel.sendBoardRejected(reason);
                        return OnlineStartupResult.failure("Plateau refuse : " + reason);
                    }

                    if (!channel.sendBoardAccepted()) {
                        return OnlineStartupResult.failure("Impossible de confirmer le plateau.");
                    }
                    return OnlineStartupResult.success(modele, channel, localPlayerIndex);
                } catch (Exception e) {
                    return OnlineStartupResult.failure("Connexion avec l'adversaire interrompue.");
                }
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                OnlineStartupResult result;
                try {
                    result = get();
                } catch (Exception e) {
                    result = OnlineStartupResult.failure("Impossible de demarrer la partie en ligne.");
                }

                onlineStartupWorker = null;
                if (onlineSessionConnection != sessionConnection) {
                    return;
                }

                if (result.isSuccess()) {
                    afficherInterfaceJeu(result.model, false, 0, false, 0, result.channel, result.localPlayerIndex);
                    return;
                }

                fermerSessionEnLigneActuelle();
                isOnlineSession = false;
                JOptionPane.showMessageDialog(MainFrame.this, result.error, "Partie en ligne", JOptionPane.WARNING_MESSAGE);
                afficherMenu();
            }
        };
        onlineStartupWorker.execute();
    }

    private void fermerSessionEnLigneActuelle() {
        if (onlineStartupWorker != null) {
            onlineStartupWorker.cancel(true);
            onlineStartupWorker = null;
        }
        annulerAttenteNouveauJoueur();
        arreterControleurActuel();
        if (onlineSessionConnection != null) {
            onlineSessionConnection.close();
            onlineSessionConnection = null;
        }
    }

    private void arreterControleurActuel() {
        if (controleur != null) {
            controleur.arreter();
            controleur = null;
        }
    }

    private void annulerAttenteNouveauJoueur() {
        if (onlinePeerWaitWorker != null) {
            onlinePeerWaitWorker.cancel(true);
            onlinePeerWaitWorker = null;
        }
        fermerDialogueAttenteNouveauJoueur(OnlineWaitForPlayerDialog.Choice.CANCEL);
    }

    private void fermerDialogueAttenteNouveauJoueur(OnlineWaitForPlayerDialog.Choice choice) {
        if (onlineWaitDialog == null) {
            return;
        }
        if (choice == OnlineWaitForPlayerDialog.Choice.JOINED) {
            onlineWaitDialog.closeAsJoined();
        } else {
            onlineWaitDialog.closeAsCancelled();
        }
        onlineWaitDialog = null;
    }

    private JPanel createCenteredPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(24, 23, 29));
        return panel;
    }

    private JLabel createOnlineLabel(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, size));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private static String cleanDisplayName(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return name.trim();
    }

    private void afficherPreparationEnLigne(String message) {
        JPanel placeholder = createCenteredPanel();
        placeholder.add(createOnlineLabel(message, 28, new Color(245, 244, 248)));
        setContentPane(placeholder);
        revalidate();
        repaint();
    }

    private void construireInterface(String nom1, String nom2, boolean p1IsAi, int p1AiDepth, boolean p2IsAi, int p2AiDepth,
            OnlineGameChannel onlineChannel, int localPlayerIndex) {
        this.lastNom1 = nom1;
        this.lastNom2 = nom2;
        this.lastP1IsAi = p1IsAi;
        this.lastP1AiDepth = p1AiDepth;
        this.lastP2IsAi = p2IsAi;
        this.lastP2AiDepth = p2AiDepth;

        GameModel gameModel;
        if (onlineChannel == null) {
            gameModel = new GameModel(nom1, nom2, 0, new java.util.Random().nextLong(), true);
        } else {
            gameModel = new GameModel(nom1, nom2);
        }

        afficherInterfaceJeu(gameModel, p1IsAi, p1AiDepth, p2IsAi, p2AiDepth, onlineChannel, localPlayerIndex);
    }

    private void afficherInterfaceJeu(GameModel modele, boolean p1IsAi, int p1AiDepth, boolean p2IsAi, int p2AiDepth,
            OnlineGameChannel onlineChannel, int localPlayerIndex) {
        setMinimumSize(new Dimension(920, 620));
        panelTop = new PlayerPanel(modele, modele.getJoueurs()[0], true);
        panelBottom = new PlayerPanel(modele, modele.getJoueurs()[1], false);
        plateau = new BoardPanel(modele);
        plateau.setDemoMode(demoMode);
        TurnGlowPanel turnGlow = new TurnGlowPanel(modele);

        OnlineSessionConnection networkSession = this.onlineSessionConnection;
        controleur = new GameController(modele, this, plateau, p1IsAi, p1AiDepth, p2IsAi, p2AiDepth, networkSession,
                localPlayerIndex);
        plateau.setController(controleur);

        this.quitButton = createQuitButton();
        this.helpButton = createHelpButton();

        JPanel topLeftBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topLeftBar.setOpaque(false);
        this.undoButton = (networkSession == null) ? createUndoButton(modele) : null;
        this.redoButton = (networkSession == null) ? createRedoButton(modele) : null;
        if (undoButton != null) {
            topLeftBar.add(undoButton);
        }
        if (redoButton != null) {
            topLeftBar.add(redoButton);
        }

        modele.addModelListener(() -> {
            updateButtonsState();
        });

        JPanel topRightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        topRightBar.setOpaque(false);
        this.rejouerButton = (networkSession == null) ? createRejouerButton() : null;
        if (rejouerButton != null) {
            topRightBar.add(rejouerButton);
        }
        topRightBar.add(quitButton);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        headerBar.add(topLeftBar, BorderLayout.WEST);
        headerBar.add(topRightBar, BorderLayout.EAST);

        JPanel bottomLeftBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        bottomLeftBar.setOpaque(false);
        if (networkSession == null) {
            this.saveButton = createSaveButton(modele, p1IsAi, p1AiDepth, p2IsAi, p2AiDepth);
            this.loadButton = createLoadButton();
            bottomLeftBar.add(saveButton);
            bottomLeftBar.add(loadButton);
        } else {
            this.saveButton = null;
            this.loadButton = null;
        }

        JPanel bottomRightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        bottomRightBar.setOpaque(false);
        bottomRightBar.add(helpButton);

        JPanel footerBar = new JPanel(new BorderLayout());
        footerBar.setOpaque(false);
        footerBar.add(bottomLeftBar, BorderLayout.WEST);
        footerBar.add(bottomRightBar, BorderLayout.EAST);
        if (demoMode > 0) {
            JPanel bottomCenterBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
            bottomCenterBar.setOpaque(false);
            bottomCenterBar.add(createDemoNextButton());
            footerBar.add(bottomCenterBar, BorderLayout.CENTER);
        }

        JPanel hud = new JPanel(new BorderLayout());
        hud.setOpaque(false);
        hud.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        hud.add(headerBar, BorderLayout.NORTH);
        hud.add(panelTop, BorderLayout.WEST);
        hud.add(panelBottom, BorderLayout.EAST);
        hud.add(footerBar, BorderLayout.SOUTH);

        JLayeredPane gameRoot = new JLayeredPane() {
            @Override
            public void doLayout() {
                Dimension size = getSize();
                plateau.setBounds(0, 0, size.width, size.height);
                turnGlow.setBounds(0, 0, size.width, size.height);
                hud.setBounds(0, 0, size.width, size.height);
                if (tutorialOverlay != null) {
                    tutorialOverlay.setBounds(0, 0, size.width, size.height);
                }
            }
        };
        gameRoot.setBackground(new Color(15, 16, 22));
        gameRoot.setOpaque(true);
        gameRoot.add(plateau, JLayeredPane.DEFAULT_LAYER);
        gameRoot.add(turnGlow, TURN_GLOW_LAYER);
        gameRoot.add(hud, JLayeredPane.PALETTE_LAYER);

        setContentPane(gameRoot);
        updateButtonsState();
        revalidate();
        repaint();
    }

    private JButton createSaveButton(GameModel modele, boolean p1IsAi, int p1AiDepth, boolean p2IsAi, int p2AiDepth) {
        JButton btn = new JButton("SAUVEGARDER") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(66, 135, 245, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 32));
        btn.addActionListener(e -> sauvegarderPartie(modele, p1IsAi, p1AiDepth, p2IsAi, p2AiDepth));
        return btn;
    }

    private JButton createLoadButton() {
        JButton btn = new JButton("CHARGER") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(104, 214, 132, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 32));
        btn.addActionListener(e -> chargerPartie());
        return btn;
    }

    public void updateButtonsState() {
        if (controleur == null) {
            return;
        }
        GameModel model = controleur.getModel();
        if (model == null) {
            return;
        }

        boolean blocked = controleur.isLocalInputBlocked();
        boolean notStarted = model.estNouvellePartieNonCommencee();
        boolean isPlacing = model.getPhase() == GameModel.GamePhase.PLACING;
        boolean isOnline = (onlineSessionConnection != null);

        if (undoButton != null) {
            undoButton.setEnabled(!isOnline && isPlacing && !blocked && !notStarted && model.peutAnnuler());
        }
        if (redoButton != null) {
            redoButton.setEnabled(!isOnline && isPlacing && !blocked && !notStarted && model.peutRefaire());
        }
        if (saveButton != null) {
            saveButton.setEnabled(!isOnline && isPlacing && !blocked && !notStarted);
        }
        if (loadButton != null) {
            loadButton.setEnabled(!isOnline && isPlacing && !blocked);
        }
        if (rejouerButton != null) {
            rejouerButton.setEnabled(isPlacing || model.getPhase() == GameModel.GamePhase.FINISHED);
        }
    }

    private void sauvegarderPartie(GameModel modele, boolean p1IsAi, int p1AiDepth, boolean p2IsAi, int p2AiDepth) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Sauvegarder la partie");
        fileChooser.setSelectedFile(new java.io.File("sauvegarde_lacuna.dat"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                lacuna.model.SaveState state = new lacuna.model.SaveState(
                    modele, p1IsAi, p1AiDepth, p2IsAi, p2AiDepth,
                    modele.getJoueurs()[0].getName(), modele.getJoueurs()[1].getName()
                );
                oos.writeObject(state);
                JOptionPane.showMessageDialog(this, "Partie sauvegardée avec succès !", "Sauvegarde", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur lors de la sauvegarde : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void chargerPartie() {
        demoMode = 0;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Charger une partie");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                lacuna.model.SaveState state = (lacuna.model.SaveState) ois.readObject();
                afficherInterfaceJeu(state.model, state.p1IsAi, state.p1AiDepth, state.p2IsAi, state.p2AiDepth, null, -1);
                JOptionPane.showMessageDialog(this, "Partie chargée avec succès !", "Chargement", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Erreur lors du chargement : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void afficherTutoriel() {
        if (tutorialOverlay != null) {
            return;
        }
        tutorialOverlay = new TutorialOverlay(this);
        JLayeredPane gameRoot = (JLayeredPane) getContentPane();
        gameRoot.add(tutorialOverlay, JLayeredPane.DRAG_LAYER);
        tutorialOverlay.setBounds(0, 0, gameRoot.getWidth(), gameRoot.getHeight());
        gameRoot.revalidate();
        gameRoot.repaint();
    }

    public void fermerTutoriel() {
        if (tutorialOverlay != null) {
            JLayeredPane gameRoot = (JLayeredPane) getContentPane();
            gameRoot.remove(tutorialOverlay);
            tutorialOverlay = null;
            gameRoot.revalidate();
            gameRoot.repaint();
        }
    }

    public void afficherAttenteRematch() {
        JPanel placeholder = createCenteredPanel();
        placeholder.add(createOnlineLabel("En attente de l'adversaire...", 28, new Color(245, 244, 248)));
        setContentPane(placeholder);
        revalidate();
        repaint();
    }

    public void relancerPartieEnLigneMemeSession(boolean isHost) {
        if (onlineSessionConnection == null) {
            isOnlineSession = false;
            afficherMenu();
            return;
        }

        arreterControleurActuel();
        demarrerPartieEnLigne(onlineSessionConnection, isHost);
    }

    public void attendreNouveauJoueurDansSession() {
        if (onlineSessionConnection == null) {
            isOnlineSession = false;
            afficherMenu();
            return;
        }

        OnlineSessionConnection connection = onlineSessionConnection;
        arreterControleurActuel();
        isOnlineSession = true;
        OnlineWaitForPlayerDialog waitDialog = new OnlineWaitForPlayerDialog(this, connection.getSessionId());
        onlineWaitDialog = waitDialog;

        onlinePeerWaitWorker = new SwingWorker<>() {
            @Override
            protected PeerJoinResult doInBackground() {
                return connection.waitForPeerJoin();
            }

            @Override
            protected void done() {
                if (isCancelled() || onlineSessionConnection != connection) {
                    return;
                }

                onlinePeerWaitWorker = null;
                try {
                    PeerJoinResult result = get();
                    if (result.isJoined()) {
                        fermerDialogueAttenteNouveauJoueur(OnlineWaitForPlayerDialog.Choice.JOINED);
                        demarrerPartieEnLigne(connection, true);
                    } else {
                        fermerDialogueAttenteNouveauJoueur(OnlineWaitForPlayerDialog.Choice.CANCEL);
                        fermerSessionEnLigneActuelle();
                        isOnlineSession = false;
                        afficherMenu();
                    }
                } catch (Exception e) {
                    fermerDialogueAttenteNouveauJoueur(OnlineWaitForPlayerDialog.Choice.CANCEL);
                    fermerSessionEnLigneActuelle();
                    isOnlineSession = false;
                    afficherMenu();
                }
            }
        };
        onlinePeerWaitWorker.execute();

        OnlineWaitForPlayerDialog.Choice choice = waitDialog.showDialog();
        if (onlineWaitDialog == waitDialog) {
            onlineWaitDialog = null;
        }
        if (choice == OnlineWaitForPlayerDialog.Choice.CANCEL
                && onlinePeerWaitWorker != null
                && !onlinePeerWaitWorker.isDone()) {
            fermerSessionEnLigneActuelle();
            isOnlineSession = false;
            afficherMenu();
        }
    }

    public void afficherAdversaireDeconnecte() {
        fermerSessionEnLigneActuelle();
        isOnlineSession = false;
        OnlineDisconnectDialog.show(this);
        afficherMenu();
    }

    public void relancerPartie() {
        if (isOnlineSession) {
            retourMenuPrincipal();
        } else if (lastNom1 != null) {
            construireInterface(lastNom1, lastNom2, lastP1IsAi, lastP1AiDepth, lastP2IsAi, lastP2AiDepth, null, -1);
        } else {
            afficherMenu();
        }
    }

    public void jouerAnimationResolution(Runnable apresAnimation) {
        plateau.jouerAnimationResolution(apresAnimation);
    }

    public void retourMenuPrincipal() {
        fermerSessionEnLigneActuelle();
        isOnlineSession = false;
        demoMode = 0;
        afficherMenu();
    }

    private void lancerDemo() {
        fermerSessionEnLigneActuelle();
        isOnlineSession = false;
        demoMode = 1;
        GameModel model = creerModelDemoVoronoi();
        afficherInterfaceJeu(model, false, 0, false, 0, null, -1);
    }

    private void afficherDemoJeu() {
        demoMode = 2;
        GameModel model = creerModelDemoJeu();
        afficherInterfaceJeu(model, false, 0, false, 0, null, -1);
    }

    private void setGamePhase(GameModel model, GameModel.GamePhase phase) {
        try {
            java.lang.reflect.Field field = GameModel.class.getDeclaredField("phase");
            field.setAccessible(true);
            field.set(model, phase);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GameModel creerModelDemoVoronoi() {
        GameModel model = new GameModel("Orange", "Bleu", 0, 42L, false);
        double[][] p0Coords = {
            {-0.4, -0.4},
            {0.3, 0.4},
            {-0.2, 0.5},
            {0.5, -0.3},
            {-0.1, -0.6},
            {0.2, 0.1}
        };
        double[][] p1Coords = {
            {0.1, -0.5},
            {-0.5, 0.1},
            {0.3, 0.6},
            {-0.3, -0.2},
            {0.6, 0.2},
            {-0.6, 0.5}
        };
        
        lacuna.model.Player p0 = model.getJoueurs()[0];
        lacuna.model.Player p1 = model.getJoueurs()[1];
        
        for (int i = 0; i < 6; i++) {
            lacuna.model.Pawn pawn0 = p0.getPawns().get(i);
            pawn0.place(p0Coords[i][0], p0Coords[i][1]);
            p0.incrementPawnsPlaced();
            
            lacuna.model.Pawn pawn1 = p1.getPawns().get(i);
            pawn1.place(p1Coords[i][0], p1Coords[i][1]);
            p1.incrementPawnsPlaced();
        }
        
        setGamePhase(model, GameModel.GamePhase.RESOLVING);
        return model;
    }

    private GameModel creerModelDemoJeu() {
        GameModel model = new GameModel("Orange", "Bleu", 0, 42L, false);
        java.util.List<lacuna.model.Flower> list = model.getFleurs();
        int placedCount = 0;
        for (int i = 0; i < list.size() && placedCount < 6; i++) {
            lacuna.model.Flower f1 = list.get(i);
            if (!f1.isOnBoard()) continue;
            for (int j = i + 1; j < list.size() && placedCount < 6; j++) {
                lacuna.model.Flower f2 = list.get(j);
                if (!f2.isOnBoard()) continue;
                if (f1.getColor() == f2.getColor()) {
                    double px = (f1.getX() + f2.getX()) / 2.0;
                    double py = (f1.getY() + f2.getY()) / 2.0;
                    if (model.estLigneValide(f1, f2)) {
                        model.placerPionEtCapturer(f1, f2, px, py);
                        placedCount++;
                        break;
                    }
                }
            }
        }
        return model;
    }

    private JButton createDemoNextButton() {
        JButton btn = new JButton("SUIVANT →") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(104, 214, 132, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 32));
        btn.addActionListener(e -> {
            if (demoMode == 1) {
                afficherDemoJeu();
            } else {
                demoMode = 0;
                retourMenuPrincipal();
            }
        });
        return btn;
    }

    public void annulerCoup() {
        if (controleur != null) {
            controleur.annulerCoup();
        }
    }

    public void refaireCoup() {
        if (controleur != null) {
            controleur.refaireCoup();
        }
    }

    private JButton createUndoButton(GameModel modele) {
        JButton btn = new JButton("UNDO") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(120, 118, 130, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        Icon undoIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
                path.moveTo(x + 13, y + 11);
                path.curveTo(x + 13, y + 2, x + 6, y + 2, x + 2, y + 6);
                path.moveTo(x + 2, y + 6);
                path.lineTo(x + 7, y + 6);
                path.moveTo(x + 2, y + 6);
                path.lineTo(x + 2, y + 1);

                g2.draw(path);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };

        btn.setIcon(undoIcon);
        btn.setIconTextGap(6);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(100, 32));
        btn.addActionListener(e -> annulerCoup());
        return btn;
    }

    private JButton createRedoButton(GameModel modele) {
        JButton btn = new JButton("REDO") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(120, 118, 130, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        Icon redoIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
                path.moveTo(x + 1, y + 11);
                path.curveTo(x + 1, y + 2, x + 8, y + 2, x + 12, y + 6);
                path.moveTo(x + 12, y + 6);
                path.lineTo(x + 7, y + 6);
                path.moveTo(x + 12, y + 6);
                path.lineTo(x + 12, y + 1);

                g2.draw(path);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };

        btn.setIcon(redoIcon);
        btn.setIconTextGap(6);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(100, 32));
        btn.addActionListener(e -> refaireCoup());
        return btn;
    }

    private JButton createRejouerButton() {
        JButton btn = new JButton("REJOUER") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(66, 135, 245, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        Icon replayIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                g2.drawArc(x + 2, y + 2, 10, 10, 45, 270);
                g2.drawLine(x + 7, y + 2, x + 7, y + 5);
                g2.drawLine(x + 7, y + 2, x + 10, y + 2);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 14;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };

        btn.setIcon(replayIcon);
        btn.setIconTextGap(6);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 32));
        btn.addActionListener(e -> {
            if (ReplayGameDialog.confirm(this)) {
                relancerPartie();
            }
        });
        return btn;
    }

    private JButton createHelpButton() {
        JButton btn = new JButton("TUTORIEL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(66, 135, 245, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        Icon helpIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                g2.drawArc(x + 2, y + 2, 8, 8, -40, 220);
                g2.drawLine(x + 10, y + 6, x + 6, y + 9);
                g2.drawLine(x + 6, y + 9, x + 6, y + 10);
                g2.fillRect(x + 5, y + 12, 2, 2);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 14;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };

        btn.setIcon(helpIcon);
        btn.setIconTextGap(6);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(135, 32));
        btn.addActionListener(e -> afficherTutoriel());
        return btn;
    }

    private JButton createQuitButton() {
        JButton btn = new JButton("QUIT") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                if (!isEnabled()) {
                    g2.setColor(new Color(40, 39, 46, 120));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(239, 80, 88, 210));
                } else {
                    g2.setColor(new Color(88, 87, 94, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isEnabled() ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 15));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public Color getForeground() {
                return isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118);
            }

            @Override
            public Cursor getCursor() {
                return isEnabled() ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
            }
        };

        Icon quitIcon = new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.isEnabled() ? new Color(220, 220, 230) : new Color(110, 108, 118));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                g2.drawLine(x + 2, y + 2, x + 10, y + 10);
                g2.drawLine(x + 10, y + 2, x + 2, y + 10);

                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }
        };

        btn.setIcon(quitIcon);
        btn.setIconTextGap(6);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(100, 32));
        btn.addActionListener(e -> {
            if (QuitGameDialog.confirm(this)) {
                retourMenuPrincipal();
            }
        });
        return btn;
    }

    private static final class OnlineStartupResult {
        private final GameModel model;
        private final OnlineGameChannel channel;
        private final int localPlayerIndex;
        private final String error;

        private OnlineStartupResult(GameModel model, OnlineGameChannel channel, int localPlayerIndex, String error) {
            this.model = model;
            this.channel = channel;
            this.localPlayerIndex = localPlayerIndex;
            this.error = error;
        }

        private static OnlineStartupResult success(GameModel model, OnlineGameChannel channel, int localPlayerIndex) {
            return new OnlineStartupResult(model, channel, localPlayerIndex, null);
        }

        private static OnlineStartupResult failure(String error) {
            return new OnlineStartupResult(null, null, -1, error);
        }

        private boolean isSuccess() {
            return model != null && channel != null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
            }
            new MainFrame();
        });
    }
}
