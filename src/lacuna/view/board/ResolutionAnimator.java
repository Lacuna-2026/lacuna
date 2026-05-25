package lacuna.view.board;

import lacuna.model.Flower;
import lacuna.model.GameModel;
import lacuna.model.Pawn;
import lacuna.model.Player;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ResolutionAnimator {
    private static final int PHASE_MS = 3000;
    private static final int CAPTURE_START_MS = 600;
    private static final int TICK_MS = 25;

    private final GameModel model;
    private final BoardGeometry geometry;
    private final JComponent component;
    private List<ResolutionPhase> phases = List.of();
    private Timer timer;
    private int phaseIndex;
    private long phaseStartedAt;
    private Runnable onFinished;

    public ResolutionAnimator(GameModel model, BoardGeometry geometry, JComponent component) {
        this.model = model;
        this.geometry = geometry;
        this.component = component;
    }

    public void prepare() {
        stop();
        phaseIndex = 0;
        phases = buildPhases();
    }

    public boolean hasSteps() {
        return !phases.isEmpty();
    }

    public void start(Runnable onFinished) {
        stopTimer();
        this.onFinished = onFinished;
        phaseStartedAt = System.currentTimeMillis();
        timer = new Timer(TICK_MS, e -> tick());
        timer.setCoalesce(true);
        timer.start();
        component.repaint();
    }

    public void clear() {
        stop();
        phases = List.of();
        phaseIndex = 0;
    }

    public void stop() {
        stopTimer();
        onFinished = null;
    }

    public ResolutionPhase currentPhase() {
        if (phaseIndex < 0 || phaseIndex >= phases.size()) {
            return null;
        }
        return phases.get(phaseIndex);
    }

    public float progress() {
        long elapsed = System.currentTimeMillis() - phaseStartedAt;
        return Math.max(0f, Math.min(1f, elapsed / (float) PHASE_MS));
    }

    public float vanishProgress() {
        long elapsed = System.currentTimeMillis() - phaseStartedAt;
        return Math.max(0f, Math.min(1f, (elapsed - CAPTURE_START_MS) / (float) (PHASE_MS - CAPTURE_START_MS)));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        ResolutionPhase phase = currentPhase();
        if (phase == null) {
            finish();
            return;
        }

        long elapsed = now - phaseStartedAt;
        if (!phase.isCaptured() && elapsed >= CAPTURE_START_MS) {
            phase.setCaptured(true);
            for (ResolutionStep step : phase.steps()) {
                model.capturerFleurResolution(step.flower(), step.pawn());
            }
        }

        if (elapsed >= PHASE_MS) {
            phaseIndex++;
            if (phaseIndex >= phases.size()) {
                finish();
                return;
            }
            phaseStartedAt = now;
        }

        component.repaint();
    }

    private void finish() {
        Runnable finished = onFinished;
        stop();
        component.repaint();
        if (finished != null) {
            finished.run();
        }
    }

    private List<ResolutionPhase> buildPhases() {
        List<ResolutionPhase> result = new ArrayList<>();
        Player[] players = model.getJoueurs();

        for (Player player : players) {
            List<ResolutionStep> steps = new ArrayList<>();
            List<Pawn> activePawns = new ArrayList<>();

            for (Pawn pawn : player.getPawns()) {
                if (pawn.isPlaced()) {
                    activePawns.add(pawn);
                }
            }

            if (activePawns.isEmpty()) continue;

            for (Flower flower : model.getFleurs()) {
                if (!flower.isOnBoard()) continue;

                Pawn closest = model.trouverPionPlusProche(flower);
                if (closest != null && closest.getOwner() == player) {
                    steps.add(new ResolutionStep(
                        flower, closest,
                        geometry.toScreenX(flower.getX()),
                        geometry.toScreenY(flower.getY()),
                        geometry.toScreenX(closest.getX()),
                        geometry.toScreenY(closest.getY()) - geometry.pawnSize() / 8
                    ));
                }
            }

            if (!steps.isEmpty()) {
                Color color = BoardColors.playerGlow(activePawns.get(0));
                result.add(new ResolutionPhase(player, activePawns, steps, color));
            }
        }

        return result;
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }


    public static final class ResolutionStep {
        private final Flower flower;
        private final Pawn pawn;
        private final int flowerX, flowerY;
        private final int pawnX, pawnY;

        public ResolutionStep(Flower flower, Pawn pawn, int fx, int fy, int px, int py) {
            this.flower = flower;
            this.pawn = pawn;
            this.flowerX = fx; this.flowerY = fy;
            this.pawnX = px; this.pawnY = py;
        }

        public Flower flower() { return flower; }
        public Pawn pawn() { return pawn; }
        public int flowerX() { return flowerX; }
        public int flowerY() { return flowerY; }
        public int pawnX() { return pawnX; }
        public int pawnY() { return pawnY; }
    }

    public static final class ResolutionPhase {
        private final Player player;
        private final List<Pawn> pawns;
        private final List<ResolutionStep> steps;
        private final Color color;
        private boolean captured;

        public ResolutionPhase(Player player, List<Pawn> pawns, List<ResolutionStep> steps, Color color) {
            this.player = player;
            this.pawns = pawns;
            this.steps = steps;
            this.color = color;
        }

        public Player player() { return player; }
        public List<Pawn> pawns() { return pawns; }
        public List<ResolutionStep> steps() { return steps; }
        public Color color() { return color; }
        public boolean isCaptured() { return captured; }
        public void setCaptured(boolean v) { captured = v; }
    }
}
