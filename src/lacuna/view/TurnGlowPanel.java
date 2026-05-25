package lacuna.view;

import lacuna.model.GameModel;
import lacuna.model.ModelListener;

import javax.swing.*;
import java.awt.*;

final class TurnGlowPanel extends JPanel implements ModelListener {
    private static final int TIMER_DELAY_MS = 40;
    private static final int SIDE_GLOW_WIDTH = 220;
    private static final int TRANSITION_MS = 650;
    private static final float BASE_ALPHA = 0.35f;
    private static final float BREATH_ALPHA = 0.22f;
    private static final Color TOP_GLOW = new Color(118, 102, 214);
    private static final Color BOTTOM_GLOW = new Color(184, 168, 32);

    private final GameModel model;
    private final Timer timer;
    private double pulse;
    private Side visibleSide;
    private Side fromSide;
    private Side targetSide;
    private long transitionStartedAt;

    TurnGlowPanel(GameModel model) {
        this.model = model;
        this.model.addModelListener(this);
        this.visibleSide = activeSide();
        this.fromSide = visibleSide;
        this.targetSide = visibleSide;
        setOpaque(false);

        timer = new Timer(TIMER_DELAY_MS, e -> {
            pulse += 0.055;
            updateTransition();
            repaint();
        });
        timer.start();
    }

    @Override
    public void onModelUpdated() {
        Side nextSide = activeSide();
        if (nextSide != targetSide) {
            startTransition(nextSide);
        }
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        GameAssets.prepare(g2);

        float alpha = breathingAlpha();
        if (isTransitioning()) {
            float progress = easedTransitionProgress();
            paintSide(g2, fromSide, alpha * (1f - progress));
            paintSide(g2, targetSide, alpha * progress);
        } else {
            paintSide(g2, visibleSide, alpha);
        }

        g2.dispose();
    }

    private void startTransition(Side nextSide) {
        fromSide = visibleSide;
        targetSide = nextSide;
        transitionStartedAt = System.currentTimeMillis();
    }

    private void updateTransition() {
        if (!isTransitioning()) {
            return;
        }

        if (rawTransitionProgress() >= 1f) {
            visibleSide = targetSide;
            fromSide = targetSide;
        }
    }

    private boolean isTransitioning() {
        return visibleSide != targetSide;
    }

    private Side activeSide() {
        if (model.getPhase() != GameModel.GamePhase.PLACING) {
            return Side.NONE;
        }
        return model.getJoueurCourant().getIndex() == 0 ? Side.LEFT : Side.RIGHT;
    }

    private float breathingAlpha() {
        double wave = (Math.sin(pulse) + 1.0) / 2.0;
        return (float) (BASE_ALPHA + BREATH_ALPHA * wave);
    }

    private float easedTransitionProgress() {
        float progress = rawTransitionProgress();
        return progress * progress * (3f - 2f * progress);
    }

    private float rawTransitionProgress() {
        long elapsed = System.currentTimeMillis() - transitionStartedAt;
        return Math.max(0f, Math.min(1f, elapsed / (float) TRANSITION_MS));
    }

    private void paintSide(Graphics2D g2, Side side, float alpha) {
        if (side == Side.LEFT) {
            paintLeftGlow(g2, alpha);
        } else if (side == Side.RIGHT) {
            paintRightGlow(g2, alpha);
        }
    }

    private void paintLeftGlow(Graphics2D g2, float alpha) {
        int width = Math.min(SIDE_GLOW_WIDTH, getWidth() / 3);

        g2.setPaint(new LinearGradientPaint(
            0, 0,
            width, 0,
            new float[]{0f, 0.45f, 1f},
            new Color[]{
                withAlpha(TOP_GLOW, alpha),
                withAlpha(TOP_GLOW, alpha * 0.55f),
                withAlpha(TOP_GLOW, 0f)
            }
        ));
        g2.fillRect(0, 0, width, getHeight());
    }

    private void paintRightGlow(Graphics2D g2, float alpha) {
        int width = Math.min(SIDE_GLOW_WIDTH, getWidth() / 3);
        int left = getWidth() - width;

        g2.setPaint(new LinearGradientPaint(
            left, 0,
            getWidth(), 0,
            new float[]{0f, 0.55f, 1f},
            new Color[]{
                withAlpha(BOTTOM_GLOW, 0f),
                withAlpha(BOTTOM_GLOW, alpha * 0.55f),
                withAlpha(BOTTOM_GLOW, alpha)
            }
        ));
        g2.fillRect(left, 0, width, getHeight());
    }

    private Color withAlpha(Color color, float alpha) {
        int value = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), value);
    }

    private enum Side {
        LEFT,
        RIGHT,
        NONE
    }
}
