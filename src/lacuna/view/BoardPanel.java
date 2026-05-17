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
import lacuna.view.board.PlacementAnimator;
import lacuna.view.board.ResolutionAnimator;
import lacuna.view.board.ToastMessage;
import lacuna.view.board.WordSplashAnimator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import lacuna.view.board.FlowerPair;

public class BoardPanel extends JPanel implements ModelListener {


    private final GameModel model;
    private final BoardGeometry geometry;
    private final WordSplashAnimator wordSplash;
    private final FlowerIntroAnimator flowerIntro;
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

    public BoardPanel(GameModel model) {
        this.model = model;
        this.geometry = new BoardGeometry(this);
        this.wordSplash = new WordSplashAnimator(this);
        this.flowerIntro = new FlowerIntroAnimator(model, this);
        this.placement = new PlacementAnimator(this);
        this.resolution = new ResolutionAnimator(model, geometry, this);
        this.toast = new ToastMessage(this);
        this.renderer = new BoardRenderer(model, geometry, this, wordSplash, flowerIntro, placement, resolution);

        this.model.addModelListener(this);
        setBackground(new Color(20, 22, 28));
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                updateHover(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY(), e.getButton());
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

    @Override
    public void addNotify() {
        super.addNotify();
        flowerIntro.skipToEnd();
    }

    @Override
    public void removeNotify() {
        flowerIntro.stop();
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
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        GameAssets.prepare(g2);
        renderer.paint(g2, candidatePairs, selectedPairIndex, hoveredFlower, mousePoint);
        toast.paint(g2);
        g2.dispose();
    }

    private void updateHover(int x, int y) {
        if (isInputBlocked() || model.getPhase() != GameModel.GamePhase.PLACING) {
            return;
        }

        hoveredFlower = flowerAt(x, y);
        updateCandidatePairs(x, y);
        repaint();
    }

    private void updateCandidatePairs(int x, int y) {
        double modelX = geometry.toModelX(x);
        double modelY = geometry.toModelY(y);
        double threshold = 25.0 / geometry.pixelScale();

        List<FlowerPair> newCandidates = new ArrayList<>();
        List<Flower> flowers = model.getFleurs();

        for (int i = 0; i < flowers.size(); i++) {
            Flower f1 = flowers.get(i);
            if (!f1.isOnBoard()) continue;
            for (int j = i + 1; j < flowers.size(); j++) {
                Flower f2 = flowers.get(j);
                if (!f2.isOnBoard() || f1.getColor() != f2.getColor()) continue;

                double midX = (f1.getX() + f2.getX()) / 2.0;
                double midY = (f1.getY() + f2.getY()) / 2.0;

                double dx = midX - modelX;
                double dy = midY - modelY;
                if (Math.sqrt(dx * dx + dy * dy) < threshold) {
                    if (model.estLigneValide(f1, f2)) {
                        newCandidates.add(new FlowerPair(f1, f2));
                    }
                }
            }
        }

        if (!areCandidateListsEqual(candidatePairs, newCandidates)) {
            candidatePairs = newCandidates;
            selectedPairIndex = 0;
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
        if (isInputBlocked() || model.getPhase() != GameModel.GamePhase.PLACING || controller == null) {
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
        double modelX = (f1.getX() + f2.getX()) / 2.0;
        double modelY = (f1.getY() + f2.getY()) / 2.0;

        if (model.estLigneValide(f1, f2)) {
            Pawn placedPawn = nextUnplacedPawn(model.getJoueurCourant());
            boolean success = controller.onPlacementValid(f1, f2, modelX, modelY);
            if (success && placedPawn != null) {
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
        if (model.getPhase() == GameModel.GamePhase.PLACING) {
            boolean isPlayer1 = model.getJoueurCourant().getIndex() == 0;
            toast.show("C'est ton tour, " + model.getJoueurCourant().getName() + " !", isPlayer1);
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
    }

    private boolean isInputBlocked() {
        return wordSplash.active() || flowerIntro.blocksInput() || placement.active()
            || (controller != null && controller.isAiTurn());
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
