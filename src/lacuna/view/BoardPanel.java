package lacuna.view;

import lacuna.controller.GameController;
import lacuna.model.Flower;
import lacuna.model.GameModel;
import lacuna.model.ModelListener;
import lacuna.model.Pawn;
import lacuna.model.Player;
import lacuna.view.board.BoardGeometry;
import lacuna.view.board.BoardRenderer;
import lacuna.view.board.FlowerIntroAnimator;
import lacuna.view.board.CylinderIntroAnimator;
import lacuna.view.board.PlacementAnimator;
import lacuna.view.board.ResolutionAnimator;
import lacuna.view.board.ToastMessage;
import lacuna.view.board.WordSplashAnimator;
import lacuna.view.board.FlowerPair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

public class BoardPanel extends JPanel implements ModelListener {
    private static final Cursor BLANK_CURSOR = createBlankCursor();

    private static Cursor createBlankCursor() {
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension dim = toolkit.getBestCursorSize(1, 1);
            int w = Math.max(1, dim.width);
            int h = Math.max(1, dim.height);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            return toolkit.createCustomCursor(img, new Point(0, 0), "blank cursor");
        } catch (Exception e) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
    }



    private static final int LETS_PLAY_SPLASH_MS = 3200;
    private static final int HOLD_UP_SPLASH_MS = 3200;
    private static final int GAME_OVER_SPLASH_MS = 3200;

    private final GameModel model;
    private final BoardGeometry geometry;
    private final WordSplashAnimator wordSplash;
    private final FlowerIntroAnimator flowerIntro;
    private final CylinderIntroAnimator cylinderIntro;
    private final PlacementAnimator placement;
    private final ResolutionAnimator resolution;
    private final ToastMessage toast;
    private final BoardRenderer renderer;

    private GameController controller;
    private Flower hoveredFlower;
    private Point mousePoint;
    private Runnable onResolutionFinished;

    private List<FlowerPair> candidatePairs = new ArrayList<>();
    private int selectedPairIndex = 0;
    private boolean showingAiIntention = false;
    private Point aiIntentionPoint = null;
    private FlowerPair lastPhantomPair = null;

    public BoardPanel(GameModel model) {
        this.model = model;
        this.geometry = new BoardGeometry(this);
        this.wordSplash = new WordSplashAnimator(this);
        this.flowerIntro = new FlowerIntroAnimator(model, this);
        this.cylinderIntro = new CylinderIntroAnimator(model, this);
        this.placement = new PlacementAnimator(this);
        this.resolution = new ResolutionAnimator(model, geometry, this);
        this.toast = new ToastMessage(this);
        this.renderer = new BoardRenderer(model, geometry, this, wordSplash, flowerIntro, cylinderIntro, placement, resolution);

        this.model.addModelListener(this);
        setBackground(new Color(20, 22, 28));
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                updateHover(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mousePoint = e.getPoint();
                updateHover(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY(), e.getButton());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mousePoint = e.getPoint();
                updateCursor();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mousePoint = null;
                updateCursor();
                repaint();
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                handleMouseWheel(e.getWheelRotation());
            }
        });
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public BoardGeometry getGeometry() {
        return geometry;
    }

    public GameModel getModel() {
        return model;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        cylinderIntro.start(geometry, () -> {
            if (controller != null) {
                controller.declencherCoupIASiNecessaire();
            }
        });
    }

    @Override
    public void removeNotify() {
        flowerIntro.stop();
        cylinderIntro.stop();
        placement.stop();
        resolution.clear();
        wordSplash.stop();
        toast.stop();
        super.removeNotify();
    }

    public void jouerAnimationResolution(Runnable onFinished) {
        clearSelection();
        onResolutionFinished = onFinished;
        resolution.prepare();

        if (!resolution.hasSteps()) {
            finishResolutionAnimation();
            return;
        }

        resolution.start(this::finishResolutionAnimation);
    }

    @Override
    public void onModelUpdated() {
        if (model.getPhase() != GameModel.GamePhase.PLACING) {
            clearSelection();
        }
        updateCursor();
        repaint();
    }

    public void onUndoPerformed() {
        clearSelection();
        lastPhantomPair = null;
        toast.show("Coup annulé !", true);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GameAssets.prepare(g2);
        Point effectiveMousePoint = showingAiIntention ? aiIntentionPoint : ((isInputBlocked() || (controller != null && controller.isAiTurn())) ? null : mousePoint);
        renderer.paint(g2, candidatePairs, selectedPairIndex, hoveredFlower, effectiveMousePoint, lastPhantomPair);
        toast.paint(g2);
        g2.dispose();
    }

    private void updateHover(int x, int y) {
        updateCursor();
        if (isInputBlocked() || model.getPhase() != GameModel.GamePhase.PLACING
                || (controller != null && controller.isAiTurn())) {
            return;
        }

        hoveredFlower = flowerAt(x, y);
        updateCandidatePairs(x, y);
        repaint();
    }

    private void updateCursor() {
        boolean shouldHide = !(isInputBlocked() || model.getPhase() != GameModel.GamePhase.PLACING
                || (controller != null && controller.isAiTurn())
                || mousePoint == null);

        if (shouldHide && mousePoint != null) {
            if (mousePoint.x < 100 || mousePoint.x > getWidth() - 100) {
                shouldHide = false;
            }
        }

        Cursor c = shouldHide ? BLANK_CURSOR : Cursor.getDefaultCursor();
        if (getCursor() != c) {
            setCursor(c);
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame frame) {
                frame.getContentPane().setCursor(c);
            }
        }
    }

    private void updateCandidatePairs(int x, int y) {
        double modelX = geometry.toModelX(x);
        double modelY = geometry.toModelY(y);
        double threshold = (geometry.pawnSize() / 2.2) / geometry.pixelScale();

        List<FlowerPair> newCandidates = new ArrayList<>();
        List<Flower> flowers = model.getFleurs();

        for (int i = 0; i < flowers.size(); i++) {
            Flower f1 = flowers.get(i);
            if (!f1.isOnBoard()) continue;
            for (int j = i + 1; j < flowers.size(); j++) {
                Flower f2 = flowers.get(j);
                if (!f2.isOnBoard() || f1.getColor() != f2.getColor()) continue;


                java.awt.geom.Line2D.Double segment = new java.awt.geom.Line2D.Double(f1.getX(), f1.getY(), f2.getX(), f2.getY());
                if (segment.ptSegDist(modelX, modelY) < threshold) {
                    if (model.estLigneValide(f1, f2)) {
                        newCandidates.add(new FlowerPair(f1, f2));
                    }
                }
            }
        }

        if (!areCandidateListsEqual(candidatePairs, newCandidates)) {
            FlowerPair currentSelected = candidatePairs.isEmpty() ? null : candidatePairs.get(selectedPairIndex);
            candidatePairs = newCandidates;
            selectedPairIndex = 0;
            
            if (currentSelected != null) {
                for (int i = 0; i < candidatePairs.size(); i++) {
                    FlowerPair p = candidatePairs.get(i);
                    if (p.f1() == currentSelected.f1() && p.f2() == currentSelected.f2()) {
                        selectedPairIndex = i;
                        break;
                    }
                }
            }
        }
    }

    private boolean areCandidateListsEqual(List<FlowerPair> list1, List<FlowerPair> list2) {
        if (list1.size() != list2.size()) return false;
        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i).f1() != list2.get(i).f1() || list1.get(i).f2() != list2.get(i).f2()) {
                return false;
            }
        }
        return true;
    }

    private void handleMouseWheel(int rotation) {
        if (candidatePairs.isEmpty()) return;
        selectedPairIndex = (selectedPairIndex + rotation) % candidatePairs.size();
        if (selectedPairIndex < 0) selectedPairIndex += candidatePairs.size();
        repaint();
    }

    private void handleClick(int x, int y, int button) {
        if (isInputBlocked() || model.getPhase() != GameModel.GamePhase.PLACING || controller == null
                || controller.isAiTurn()) {
            return;
        }

        if (candidatePairs.isEmpty()) {
            toast.show("Place le curseur entre deux fleurs de meme couleur.");
            return;
        }

        FlowerPair pair = candidatePairs.get(selectedPairIndex);
        placePawnIfValid(pair.f1(), pair.f2());
        repaint();
    }

    private void placePawnIfValid(Flower f1, Flower f2) {
        double modelX = geometry.toModelX(mousePoint.x);
        double modelY = geometry.toModelY(mousePoint.y);

        double minDistance = (geometry.flowerSize() / 2.0 + geometry.pawnSize() / 2.0) / geometry.pixelScale() * 0.85;
        for (Flower flower : model.getFleurs()) {
            if (flower.isOnBoard() && flower.distanceTo(modelX, modelY) < minDistance) {
                toast.show("Le pion est trop pres d'une fleur.");
                return;
            }
        }

        if (model.estLigneValide(f1, f2)) {
            Pawn placedPawn = nextUnplacedPawn(model.getJoueurCourant());
            boolean success = controller.onPlacementValid(f1, f2, modelX, modelY);
            if (success && placedPawn != null) {
                lastPhantomPair = null;
                placement.start(placedPawn, f1, f2, this::finishPlacementAnimation);
                candidatePairs.clear();
            }
        } else {
            toast.show("La ligne traverse une autre piece.");
        }
    }

    private void finishPlacementAnimation() {
        if (controller != null) {
            controller.onPlacementAnimationFinished();
        }
        if (model.getPhase() == GameModel.GamePhase.PLACING
                && (controller == null || !controller.isAiTurn())) {
            boolean isPlayer1 = model.getJoueurCourant().getIndex() == 0;
            toast.show("C'est ton tour, " + model.getJoueurCourant().getName() + " !", false);
        }
    }

    private void finishResolutionAnimation() {
        resolution.clear();
        wordSplash.stop();
        model.terminerResolution();

        Runnable finished = onResolutionFinished;
        onResolutionFinished = null;
        if (finished != null) {
            finished.run();
        }
    }

    private Flower flowerAt(int x, int y) {
        double modelX = geometry.toModelX(x);
        double modelY = geometry.toModelY(y);
        double threshold = (geometry.flowerSize() / 2.0 + 5) / geometry.pixelScale();

        for (Flower flower : model.getFleurs()) {
            if (flower.isOnBoard() && flower.distanceTo(modelX, modelY) <= threshold) {
                return flower;
            }
        }
        return null;
    }

    private void clearSelection() {
        candidatePairs.clear();
        selectedPairIndex = 0;
        hoveredFlower = null;
        mousePoint = null;
        updateCursor();
    }

    private boolean isInputBlocked() {
        return cylinderIntro.active() || placement.active()
                || (controller != null && (controller.isAiThinking() || controller.isAiTurn()))
                || model.getPhase() != GameModel.GamePhase.PLACING;
    }

    public void montrerIntentionIA(Flower f1, Flower f2, double pawnX, double pawnY, Runnable onFinished) {
        candidatePairs.clear();
        candidatePairs.add(new FlowerPair(f1, f2));
        selectedPairIndex = 0;
        
        aiIntentionPoint = new Point(geometry.toScreenX(pawnX), geometry.toScreenY(pawnY));
        showingAiIntention = true;
        updateCursor();
        repaint();

        Timer timer = new Timer(800, e -> {
            showingAiIntention = false;
            aiIntentionPoint = null;
            onFinished.run();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void jouerAnimationPionIA(Pawn pawn, Flower f1, Flower f2, Runnable onFinished) {
        clearSelection();
        lastPhantomPair = new FlowerPair(f1, f2);
        placement.start(pawn, f1, f2, onFinished);
        repaint();
    }

    private Pawn nextUnplacedPawn(Player player) {
        for (Pawn pawn : player.getPawns()) {
            if (!pawn.isPlaced()) {
                return pawn;
            }
        }
        return null;
    }
}
