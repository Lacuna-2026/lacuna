package lacuna.view;

import lacuna.model.*;
import lacuna.view.board.BoardGeometry;
import lacuna.view.board.FlowerPair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class TutorialOverlay extends JPanel {
    private final MainFrame mainFrame;
    private final BoardPanel boardPanel;
    private int stepIndex = 0;
    private final Timer repaintTimer;

    private final JPanel cardPanel;
    private final JLabel titleLabel;
    private final JTextPane textPane;
    private final JLabel stepIndicator;
    private final JButton btnQuit;
    private final JButton btnNext;

    private static final String[] TITLES = {
        "Étape 1/7 : Le But du Jeu",
        "Étape 2/7 : Placer un Pion",
        "Étape 3/7 : Intersection & Molette",
        "Étape 4/7 : Capturer des Fleurs",
        "Étape 5/7 : Obstacles & Contraintes",
        "Étape 6/7 : Le Hold-up (Fin de partie)",
        "Étape 7/7 : Majorité & Victoire"
    };

    private static final String[] DESCRIPTIONS = {
        "Le but de Lacuna est de remporter la majorité des fleurs pour au moins 4 des 7 couleurs disponibles. Chaque couleur compte 7 fleurs dispersées sur le plateau.\n\nLes fleurs capturées s'accumulent dans vos compteurs sur les côtés.",
        "À votre tour, vous devez placer un pion sur le plateau. Pour ce faire, vous devez placer le pion en ligne droite imaginaire reliant deux fleurs de la même couleur qui sont encore libres sur le plateau.",
        "Si plusieurs lignes possibles se croisent sous votre curseur, une molette virtuelle s'affiche. Faites tourner la molette de votre souris pour alterner entre les paires de fleurs et choisir la ligne souhaitée.",
        "En plaçant votre pion entre deux fleurs de même couleur, vous capturez instantanément ces deux fleurs ! Elles sont retirées du plateau et s'ajoutent à votre score de couleur.",
        "Attention aux obstacles ! La ligne entre les deux fleurs choisies ne doit traverser aucune autre fleur ni aucun pion déjà placé. De plus, votre pion ne peut pas être placé trop près d'une fleur.",
        "Une fois que les 12 pions (6 par joueur) sont placés, c'est l'étape du Hold-up ! Toutes les fleurs restantes sur le plateau sont automatiquement capturées par le joueur possédant le pion le plus proche.",
        "Pour chaque couleur, le joueur ayant collecté le plus grand nombre de fleurs gagne la majorité pour cette couleur. Le premier à remporter au moins 4 majorités gagne la partie !"
    };

    public TutorialOverlay(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.boardPanel = mainFrame.getBoardPanel();

        setOpaque(false);
        setLayout(null);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { e.consume(); }
            @Override
            public void mousePressed(MouseEvent e) { e.consume(); }
            @Override
            public void mouseReleased(MouseEvent e) { e.consume(); }
            @Override
            public void mouseMoved(MouseEvent e) { e.consume(); }
            @Override
            public void mouseDragged(MouseEvent e) { e.consume(); }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

        cardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(25, 23, 29, 248));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 34));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cardPanel.setOpaque(false);
        cardPanel.setLayout(new BorderLayout(12, 12));
        cardPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        titleLabel = new JLabel(TITLES[0]);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        cardPanel.add(titleLabel, BorderLayout.NORTH);
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setForeground(new Color(200, 200, 210));
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setText(DESCRIPTIONS[0]);
        textPane.setMargin(new Insets(4, 0, 4, 0));
        cardPanel.add(textPane, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        stepIndicator = new JLabel("1 / 7", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(88, 87, 94, 120));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        stepIndicator.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stepIndicator.setForeground(new Color(180, 180, 190));
        stepIndicator.setPreferredSize(new Dimension(50, 24));
        footerPanel.add(stepIndicator, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setOpaque(false);

        btnQuit = new TutorialButton("Quitter", new Color(88, 87, 94, 200), new Color(239, 80, 88, 220));
        btnQuit.setPreferredSize(new Dimension(100, 32));
        btnQuit.addActionListener(e -> mainFrame.fermerTutoriel());

        btnNext = new TutorialButton("Continuer", new Color(66, 135, 245), new Color(95, 155, 255));
        btnNext.setPreferredSize(new Dimension(110, 32));
        btnNext.addActionListener(e -> nextStep());

        buttonsPanel.add(btnQuit);
        buttonsPanel.add(btnNext);
        footerPanel.add(buttonsPanel, BorderLayout.EAST);

        cardPanel.add(footerPanel, BorderLayout.SOUTH);
        add(cardPanel);

        repaintTimer = new Timer(50, e -> repaint());
        repaintTimer.start();
    }

    private void nextStep() {
        stepIndex++;
        if (stepIndex >= TITLES.length) {
            mainFrame.fermerTutoriel();
            return;
        }

        titleLabel.setText(TITLES[stepIndex]);
        textPane.setText(DESCRIPTIONS[stepIndex]);
        stepIndicator.setText((stepIndex + 1) + " / " + TITLES.length);

        if (stepIndex == TITLES.length - 1) {
            btnNext.setText("Terminer");
        } else {
            btnNext.setText("Continuer");
        }

        repaint();
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int cardW = 540;
        int cardH = 200;
        cardPanel.setBounds((w - cardW) / 2, h - cardH - 50, cardW, cardH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(15, 16, 22, 185));
        g2.fillRect(0, 0, getWidth(), getHeight());

        GameAssets.prepare(g2);

        BoardGeometry geometry = boardPanel.getGeometry();
        GameModel model = boardPanel.getModel();

        if (geometry != null && model != null) {
            switch (stepIndex) {
                case 0:
                    paintStepGoal(g2, geometry, model);
                    break;
                case 1:
                    paintStepPlacement(g2, geometry, model);
                    break;
                case 2:
                    paintStepMolette(g2, geometry, model);
                    break;
                case 3:
                    paintStepCapture(g2, geometry, model);
                    break;
                case 4:
                    paintStepConstraints(g2, geometry, model);
                    break;
                case 5:
                    paintStepHoldup(g2, geometry, model);
                    break;
                case 6:
                    paintStepVictory(g2, geometry, model);
                    break;
            }
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private void paintStepGoal(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        List<Flower> flowers = model.getFleurs();
        int size = geometry.flowerSize();
        int highlightedCount = 0;
        for (Flower flower : flowers) {
            if (flower.isOnBoard()) {
                int fx = geometry.toScreenX(flower.getX());
                int fy = geometry.toScreenY(flower.getY());
                
                float pulse = (float) ((Math.sin(System.currentTimeMillis() / 200.0) + 1.0) / 2.0);
                g2.setColor(new Color(255, 255, 255, (int) (40 + pulse * 60)));
                g2.fillOval(fx - size, fy - size, size * 2, size * 2);
                
                highlightedCount++;
                if (highlightedCount >= 6) break;
            }
        }

        Rectangle boundsTop = mainFrame.getPanelTop().getBounds();
        Rectangle boundsBottom = mainFrame.getPanelBottom().getBounds();

        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        g2.setColor(new Color(132, 220, 255, 220));
        g2.drawRoundRect(boundsTop.x, boundsTop.y, boundsTop.width, boundsTop.height, 18, 18);
        g2.drawString("Compteurs " + model.getJoueurs()[0].getName(), boundsTop.x + 5, boundsTop.y - 8);

        g2.setColor(new Color(235, 195, 72, 220));
        g2.drawRoundRect(boundsBottom.x, boundsBottom.y, boundsBottom.width, boundsBottom.height, 18, 18);
        g2.drawString("Compteurs " + model.getJoueurs()[1].getName(), boundsBottom.x + 5, boundsBottom.y - 8);
    }

    private void paintStepPlacement(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        FlowerPair pair = findValidPair(model);
        if (pair == null) return;

        int x1 = geometry.toScreenX(pair.f1().getX());
        int y1 = geometry.toScreenY(pair.f1().getY());
        int x2 = geometry.toScreenX(pair.f2().getX());
        int y2 = geometry.toScreenY(pair.f2().getY());
        int size = geometry.flowerSize();

        g2.setColor(new Color(255, 255, 255, 120));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval(x1 - size / 2 - 4, y1 - size / 2 - 4, size + 8, size + 8);
        g2.drawOval(x2 - size / 2 - 4, y2 - size / 2 - 4, size + 8, size + 8);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6f, 6f}, 0));
        g2.drawLine(x1, y1, x2, y2);

        int px = (x1 + x2) / 2;
        int py = (y1 + y2) / 2;
        int pawnSize = geometry.pawnSize();
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        GameAssets.drawFit(g2, GameAssets.pawn(0), px, py - pawnSize / 8, pawnSize);
        g2.setComposite(oldComp);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString("Alignement de pions", px - 60, py - pawnSize / 2 - 8);
    }

    private void paintStepCapture(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        FlowerPair pair = findValidPair(model);
        if (pair == null) return;

        int x1 = geometry.toScreenX(pair.f1().getX());
        int y1 = geometry.toScreenY(pair.f1().getY());
        int x2 = geometry.toScreenX(pair.f2().getX());
        int y2 = geometry.toScreenY(pair.f2().getY());
        int size = geometry.flowerSize();

        g2.setColor(new Color(132, 220, 255, 150));
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawOval(x1 - size / 2 - 4, y1 - size / 2 - 4, size + 8, size + 8);
        g2.drawOval(x2 - size / 2 - 4, y2 - size / 2 - 4, size + 8, size + 8);
        g2.setColor(new Color(132, 220, 255, 200));
        g2.drawLine(x1, y1, x2, y2);
        int px = (x1 + x2) / 2;
        int py = (y1 + y2) / 2;
        int pawnSize = geometry.pawnSize();
        GameAssets.drawFit(g2, GameAssets.pawn(0), px, py - pawnSize / 8, pawnSize);
        Rectangle boundsTop = mainFrame.getPanelTop().getBounds();
        int destX = boundsTop.x + boundsTop.width;
        int destY1 = boundsTop.y + boundsTop.height / 2 - 50;
        int destY2 = boundsTop.y + boundsTop.height / 2 + 50;

        drawArrow(g2, x1, y1, destX, destY1, new Color(132, 220, 255, 180));
        drawArrow(g2, x2, y2, destX, destY2, new Color(132, 220, 255, 180));

        g2.setColor(new Color(132, 220, 255));
        g2.drawString("Capture !", destX + 15, boundsTop.y + boundsTop.height / 2 - 10);
    }

    private void paintStepConstraints(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        Object[] invalidData = findInvalidPairAndObstacle(model);
        if (invalidData == null) return;

        Flower f1 = (Flower) invalidData[0];
        Flower f2 = (Flower) invalidData[1];
        Object obs = invalidData[2];

        int x1 = geometry.toScreenX(f1.getX());
        int y1 = geometry.toScreenY(f1.getY());
        int x2 = geometry.toScreenX(f2.getX());
        int y2 = geometry.toScreenY(f2.getY());

        g2.setColor(new Color(239, 80, 88, 220));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(x1, y1, x2, y2);

        int obsX = 0, obsY = 0;
        if (obs instanceof Flower flower) {
            obsX = geometry.toScreenX(flower.getX());
            obsY = geometry.toScreenY(flower.getY());
        } else if (obs instanceof Pawn pawn) {
            obsX = geometry.toScreenX(pawn.getX());
            obsY = geometry.toScreenY(pawn.getY()) - geometry.pawnSize() / 8;
        }

        int highlightSize = geometry.flowerSize() + 10;
        g2.setColor(new Color(239, 80, 88, 100));
        g2.fillOval(obsX - highlightSize / 2, obsY - highlightSize / 2, highlightSize, highlightSize);
        g2.setColor(new Color(239, 80, 88, 220));
        g2.drawOval(obsX - highlightSize / 2, obsY - highlightSize / 2, highlightSize, highlightSize);

        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2;
        g2.setStroke(new BasicStroke(3.0f));
        g2.drawLine(mx - 10, my - 10, mx + 10, my + 10);
        g2.drawLine(mx + 10, my - 10, mx - 10, my + 10);

        g2.setColor(new Color(239, 80, 88));
        g2.drawString("Ligne bloquée !", mx - 45, my - 16);
    }

    private void paintStepHoldup(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        List<Pawn> pawnsToUse = new ArrayList<>();
        for (Player p : model.getJoueurs()) {
            for (Pawn pw : p.getPawns()) {
                if (pw.isPlaced()) {
                    pawnsToUse.add(pw);
                }
            }
        }

        Pawn dummyP1 = null;
        Pawn dummyP2 = null;
        boolean usingDummies = pawnsToUse.isEmpty();
        if (usingDummies) {
            dummyP1 = new Pawn(model.getJoueurs()[0]);
            dummyP1.place(0.2, 0.2);
            pawnsToUse.add(dummyP1);

            dummyP2 = new Pawn(model.getJoueurs()[1]);
            dummyP2.place(-0.2, -0.2);
            pawnsToUse.add(dummyP2);
        }
        if (usingDummies) {
            int p1x = geometry.toScreenX(dummyP1.getX());
            int p1y = geometry.toScreenY(dummyP1.getY()) - geometry.pawnSize() / 8;
            int p2x = geometry.toScreenX(dummyP2.getX());
            int p2y = geometry.toScreenY(dummyP2.getY()) - geometry.pawnSize() / 8;
            int pawnSize = geometry.pawnSize();

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            GameAssets.drawFit(g2, GameAssets.pawn(0), p1x, p1y, pawnSize);
            GameAssets.drawFit(g2, GameAssets.pawn(1), p2x, p2y, pawnSize);
            g2.setComposite(old);
        }

        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4f, 4f}, 0));
        int count = 0;
        for (Flower flower : model.getFleurs()) {
            if (flower.isOnBoard()) {
                Pawn closest = null;
                double minDist = Double.MAX_VALUE;
                for (Pawn p : pawnsToUse) {
                    double dist = p.distanceTo(flower);
                    if (dist < minDist) {
                        minDist = dist;
                        closest = p;
                    }
                }

                if (closest != null) {
                    int fx = geometry.toScreenX(flower.getX());
                    int fy = geometry.toScreenY(flower.getY());
                    int px = geometry.toScreenX(closest.getX());
                    int py = geometry.toScreenY(closest.getY()) - geometry.pawnSize() / 8;

                    Color color = closest.getOwner().getIndex() == 0 ? new Color(132, 220, 255, 160) : new Color(235, 195, 72, 160);
                    g2.setColor(color);
                    g2.drawLine(fx, fy, px, py);
                }

                count++;
                if (count >= 10) break;
            }
        }
    }

    private void paintStepVictory(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        Rectangle boundsTop = mainFrame.getPanelTop().getBounds();
        Rectangle boundsBottom = mainFrame.getPanelBottom().getBounds();

        g2.setStroke(new BasicStroke(3.0f));
        g2.setColor(new Color(132, 220, 255, 230));
        g2.drawRoundRect(boundsTop.x, boundsTop.y, boundsTop.width, boundsTop.height, 18, 18);
        g2.setColor(new Color(235, 195, 72, 230));
        g2.drawRoundRect(boundsBottom.x, boundsBottom.y, boundsBottom.width, boundsBottom.height, 18, 18);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(Color.WHITE);
        g2.drawString("Comparaison des majorités", getWidth() / 2 - 90, 80);
    }

    private FlowerPair findValidPair(GameModel model) {
        List<Flower> flowers = model.getFleurs();
        for (int i = 0; i < flowers.size(); i++) {
            Flower f1 = flowers.get(i);
            if (!f1.isOnBoard()) continue;
            for (int j = i + 1; j < flowers.size(); j++) {
                Flower f2 = flowers.get(j);
                if (!f2.isOnBoard() || f1.getColor() != f2.getColor()) continue;
                if (model.estLigneValide(f1, f2)) {
                    return new FlowerPair(f1, f2);
                }
            }
        }
        return null;
    }

    private Object[] findInvalidPairAndObstacle(GameModel model) {
        List<Flower> flowers = model.getFleurs();
        for (int i = 0; i < flowers.size(); i++) {
            Flower f1 = flowers.get(i);
            if (!f1.isOnBoard()) continue;
            for (int j = i + 1; j < flowers.size(); j++) {
                Flower f2 = flowers.get(j);
                if (!f2.isOnBoard() || f1.getColor() != f2.getColor()) continue;

                if (!model.estLigneValide(f1, f2)) {
                    Line2D.Double line = new Line2D.Double(f1.getX(), f1.getY(), f2.getX(), f2.getY());
                    for (Flower obs : flowers) {
                        if (obs != f1 && obs != f2 && obs.isOnBoard() && line.ptSegDist(obs.getX(), obs.getY()) < GameModel.HITBOX_RADIUS) {
                            return new Object[] { f1, f2, obs };
                        }
                    }
                    for (Player p : model.getJoueurs()) {
                        for (Pawn pawn : p.getPawns()) {
                            if (pawn.isPlaced() && line.ptSegDist(pawn.getX(), pawn.getY()) < GameModel.HITBOX_RADIUS) {
                                return new Object[] { f1, f2, pawn };
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6f, 6f}, 0));
        g2.drawLine(x1, y1, x2, y2);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int len = 10;
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(x2, y2, (int) (x2 - len * Math.cos(angle - Math.PI / 6)), (int) (y2 - len * Math.sin(angle - Math.PI / 6)));
        g2.drawLine(x2, y2, (int) (x2 - len * Math.cos(angle + Math.PI / 6)), (int) (y2 - len * Math.sin(angle + Math.PI / 6)));
    }

    private static class TutorialButton extends JButton {
        private final Color normalColor;
        private final Color hoverColor;

        public TutorialButton(String text, Color normalColor, Color hoverColor) {
            super(text);
            this.normalColor = normalColor;
            this.hoverColor = hoverColor;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(normalColor);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void paintStepMolette(Graphics2D g2, BoardGeometry geometry, GameModel model) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2 - 40;
        int fSize = geometry.flowerSize();
        int pawnSize = geometry.pawnSize();

        java.awt.image.BufferedImage blueFlower = GameAssets.flower(FlowerColor.BLUE);

        int a1x = cx - 110, a1y = cy - 60;
        int a2x = cx + 110, a2y = cy + 60;
        int b1x = cx - 100, b1y = cy + 70;
        int b2x = cx + 100, b2y = cy - 70;

        boolean selectedA = (System.currentTimeMillis() / 1500) % 2 == 0;

        if (selectedA) {
            g2.setColor(new Color(132, 220, 255, 230));
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else {
            g2.setColor(new Color(132, 220, 255, 90));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5f, 5f}, 0));
        }
        g2.drawLine(a1x, a1y, a2x, a2y);

        if (!selectedA) {
            g2.setColor(new Color(132, 220, 255, 230));
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        } else {
            g2.setColor(new Color(132, 220, 255, 90));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5f, 5f}, 0));
        }
        g2.drawLine(b1x, b1y, b2x, b2y);

        GameAssets.drawFit(g2, blueFlower, a1x, a1y, fSize);
        GameAssets.drawFit(g2, blueFlower, a2x, a2y, fSize);
        GameAssets.drawFit(g2, blueFlower, b1x, b1y, fSize);
        GameAssets.drawFit(g2, blueFlower, b2x, b2y, fSize);

        GameAssets.drawFit(g2, GameAssets.pawn(0), cx, cy - pawnSize / 8, pawnSize);

        drawMouseAnimation(g2, cx + 180, cy, selectedA);
    }

    private void drawMouseAnimation(Graphics2D g2, int mx, int my, boolean scrollDown) {
        int w = 36;
        int h = 60;
        int rx = mx - w / 2;
        int ry = my - h / 2;

        g2.setColor(new Color(25, 23, 29, 230));
        g2.fillRoundRect(rx, ry, w, h, 18, 18);
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRoundRect(rx, ry, w - 1, h - 1, 18, 18);

        g2.drawLine(rx + w / 2, ry, rx + w / 2, ry + 22);

        int wheelW = 6;
        int wheelH = 14;
        int wx = rx + w / 2 - wheelW / 2;
        int wy = ry + 8;
        g2.setColor(new Color(132, 220, 255));
        g2.fillRoundRect(wx, wy, wheelW, wheelH, 3, 3);

        g2.setColor(new Color(132, 220, 255, 200));
        g2.setStroke(new BasicStroke(1.5f));
        int arrowOffset = (int) ((System.currentTimeMillis() / 250) % 4);
        if (scrollDown) {
            g2.drawLine(mx - 3, wy + wheelH + 4 + arrowOffset, mx, wy + wheelH + 7 + arrowOffset);
            g2.drawLine(mx + 3, wy + wheelH + 4 + arrowOffset, mx, wy + wheelH + 7 + arrowOffset);
        } else {
            g2.drawLine(mx - 3, wy - 4 - arrowOffset, mx, wy - 7 - arrowOffset);
            g2.drawLine(mx + 3, wy - 4 - arrowOffset, mx, wy - 7 - arrowOffset);
        }
    }

    @Override
    public void removeNotify() {
        if (repaintTimer != null) {
            repaintTimer.stop();
        }
        super.removeNotify();
    }
}
