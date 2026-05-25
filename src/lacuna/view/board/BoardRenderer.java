package lacuna.view.board;

import lacuna.model.Flower;
import lacuna.model.GameModel;
import lacuna.model.Pawn;
import lacuna.model.Player;
import lacuna.model.FlowerColor;
import lacuna.view.GameAssets;
import lacuna.view.Theme;
import lacuna.view.PlayerPanel;
import lacuna.view.MainFrame;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.awt.Component;
import java.util.List;
import javax.swing.JPanel;

public final class BoardRenderer {
    private static final BasicStroke RESOLUTION_LINE_STROKE = new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND);

    private final GameModel model;
    private final BoardGeometry geometry;
    private final JComponent component;
    private final WordSplashAnimator wordSplash;
    private final FlowerIntroAnimator flowerIntro;
    private final CylinderIntroAnimator cylinderIntro;
    private final PlacementAnimator placement;
    private final ResolutionAnimator resolution;
    private BufferedImage cachedVoronoi;
    private ResolutionAnimator.ResolutionPhase lastPhase;

    public BoardRenderer(
            GameModel model,
            BoardGeometry geometry,
            JComponent component,
            WordSplashAnimator wordSplash,
            FlowerIntroAnimator flowerIntro,
            CylinderIntroAnimator cylinderIntro,
            PlacementAnimator placement,
            ResolutionAnimator resolution) {
        this.model = model;
        this.geometry = geometry;
        this.component = component;
        this.wordSplash = wordSplash;
        this.flowerIntro = flowerIntro;
        this.cylinderIntro = cylinderIntro;
        this.placement = placement;
        this.resolution = resolution;
    }

    public void paint(Graphics2D g2, List<FlowerPair> candidates, int selectedIndex, Flower hoveredFlower,
            Point mousePoint, FlowerPair lastPhantomPair) {
        drawBoard(g2);
        if (model.getPhase() == GameModel.GamePhase.PLACING) {
            drawAiPhantomPair(g2, lastPhantomPair);
            drawSelectionGuide(g2, candidates, selectedIndex, hoveredFlower, mousePoint);
        }
        
        drawLandingRipples(g2);

        drawElements(g2, null, hoveredFlower);
        drawPlacementFlowerAnimation(g2);
        drawResolutionPhase(g2);
        wordSplash.paint(g2);

        if (cylinderIntro.active()) {
            drawCylinder(g2);
        }
    }

    private void drawAiPhantomPair(Graphics2D g2, FlowerPair pair) {
        if (pair == null) return;
        
        Color pairColor = Theme.getColor(pair.f1().getColor());
        int x1 = geometry.toScreenX(pair.f1().getX());
        int y1 = geometry.toScreenY(pair.f1().getY());
        int x2 = geometry.toScreenX(pair.f2().getX());
        int y2 = geometry.toScreenY(pair.f2().getY());

        g2.setColor(withAlpha(pairColor, 0.5f));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6f, 6f}, 0));
        g2.drawLine(x1, y1, x2, y2);

        int size = geometry.flowerSize();
        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        GameAssets.drawFit(g2, GameAssets.flower(pair.f1().getColor()), x1, y1, size);
        GameAssets.drawFit(g2, GameAssets.flower(pair.f2().getColor()), x2, y2, size);
        g2.setComposite(oldComposite);
    }

    private void drawBoard(Graphics2D g2) {
        BufferedImage board = GameAssets.board();
        Rectangle bounds = geometry.boardBounds();

        if (board == null) {
            g2.setColor(new Color(19, 23, 30));
            g2.fillRect(0, 0, component.getWidth(), component.getHeight());
            return;
        }

        g2.drawImage(board, bounds.x, bounds.y, bounds.width, bounds.height, null);
    }

    private void drawSelectionGuide(Graphics2D g2, List<FlowerPair> candidates, int selectedIndex, Flower hoveredFlower,
            Point mousePoint) {
        if (mousePoint == null)
            return;

        // pion fantôme
        int pawnSize = geometry.pawnSize();
        Point snappedPoint = mousePoint;

        if (!candidates.isEmpty()) {
            FlowerPair selectedPair = candidates.get(selectedIndex);
            int x1 = geometry.toScreenX(selectedPair.f1().getX());
            int y1 = geometry.toScreenY(selectedPair.f1().getY());
            int x2 = geometry.toScreenX(selectedPair.f2().getX());
            int y2 = geometry.toScreenY(selectedPair.f2().getY());

            double dx = x2 - x1;
            double dy = y2 - y1;
            double lenSq = dx * dx + dy * dy;
            double t = (lenSq == 0) ? 0 : ((mousePoint.x - x1) * dx + (mousePoint.y - y1) * dy) / lenSq;
            t = Math.max(0.1, Math.min(0.9, t));

            snappedPoint = new Point((int) (x1 + t * dx), (int) (y1 + t * dy));
        }

        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        GameAssets.drawFit(g2, GameAssets.pawn(model.getJoueurCourant().getIndex()), mousePoint.x,
                mousePoint.y - pawnSize / 8, pawnSize);
        g2.setComposite(oldComp);

        if (candidates.isEmpty())
            return;

        for (int i = 0; i < candidates.size(); i++) {
            FlowerPair pair = candidates.get(i);
            boolean isSelected = (i == selectedIndex);
            Color pairColor = Theme.getColor(pair.f1().getColor());

            int x1 = geometry.toScreenX(pair.f1().getX());
            int y1 = geometry.toScreenY(pair.f1().getY());
            int x2 = geometry.toScreenX(pair.f2().getX());
            int y2 = geometry.toScreenY(pair.f2().getY());

            if (isSelected) {
                g2.setColor(withAlpha(pairColor, 0.85f));
                g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g2.setColor(withAlpha(pairColor, 0.35f));
                g2.setStroke(
                        new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 5f }, 0));
            }

            g2.drawLine(x1, y1, x2, y2);

        }

        if (candidates.size() > 1 && mousePoint != null) {
            String tip = "Molette : Choisir la ligne";
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(tip) + 16;
            int h = fm.getHeight() + 8;
            int tx = mousePoint.x - w / 2;
            int ty = mousePoint.y - pawnSize - h - 10;

            g2.setColor(new Color(25, 23, 29, 220));
            g2.fillRoundRect(tx, ty, w, h, 14, 14);
            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawRoundRect(tx, ty, w - 1, h - 1, 14, 14);

            g2.setColor(new Color(245, 244, 248));
            g2.drawString(tip, tx + 8, ty + h / 2 + fm.getAscent() / 2 - 2);
        }
    }

    private void drawElements(Graphics2D g2, Flower selectedFlower, Flower hoveredFlower) {
        java.util.List<ElementOnBoard> elements = new java.util.ArrayList<>();
 
        if (cylinderIntro.active()) {
            for (Flower flower : model.getFleurs()) {
                if (flower.isOnBoard() && cylinderIntro.shouldDrawFlower(flower)) {
                    Point.Double pos = cylinderIntro.getFlowerPosition(flower, geometry);
                    elements.add(new ElementOnBoard(flower, pos.y));
                }
            }
        } else {
            for (Flower flower : model.getFleurs()) {
                if (flower.isOnBoard()) {
                    elements.add(new ElementOnBoard(flower, flower.getY()));
                }
            }
            for (Player player : model.getJoueurs()) {
                for (Pawn pawn : player.getPawns()) {
                    if (pawn.isPlaced()) {
                        elements.add(new ElementOnBoard(pawn, pawn.getY()));
                    }
                }
            }
        }

        elements.sort((left, right) -> Double.compare(left.y(), right.y()));
        for (ElementOnBoard element : elements) {
            drawElement(g2, element.item(), selectedFlower, hoveredFlower);
        }
    }

    private void drawElement(Graphics2D g2, Object item, Flower selectedFlower, Flower hoveredFlower) {
        if (item instanceof Flower flower) {
            if (cylinderIntro.active()) {
                drawCylinderIntroFlower(g2, flower);
            } else {
                drawFlower(g2, flower, selectedFlower, hoveredFlower);
            }
            return;
        }

        if (item instanceof Pawn pawn) {
            if (placement.isPawn(pawn)) {
                drawPlacementPawn(g2, pawn);
            } else {
                drawPawn(g2, pawn);
            }
        }
    }

    private boolean shouldDrawFlowerIntro() {
        return model.getPhase() == GameModel.GamePhase.PLACING && flowerIntro.active();
    }

    private void drawFlower(Graphics2D g2, Flower flower, Flower selectedFlower, Flower hoveredFlower) {
        int x = geometry.toScreenX(flower.getX());
        int y = geometry.toScreenY(flower.getY());
        int size = geometry.flowerSize();

        if (flower == selectedFlower) {
            drawGlow(g2, x, y, size + 16, new Color(255, 255, 255, 145));
        }

        GameAssets.drawFit(g2, GameAssets.flower(flower.getColor()), x, y, size);
    }

    private void drawFlowerIntro(Graphics2D g2, Flower flower) {
        float progress = flowerIntro.progressFor(flower);
        if (progress <= 0f) {
            return;
        }

        int x = geometry.toScreenX(flower.getX());
        int y = geometry.toScreenY(flower.getY());
        int size = geometry.flowerSize();
        float eased = AnimationMath.smooth(progress);
        int drawSize = Math.max(1, Math.round(size * (0.42f + 0.58f * eased)));

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, eased));
        drawGlow(g2, x, y, Math.round(drawSize * 1.45f), new Color(255, 255, 255, Math.round(34 * eased)));
        GameAssets.drawFit(g2, GameAssets.flower(flower.getColor()), x, y, drawSize);
        g2.setComposite(oldComposite);
    }

    private void drawPawn(Graphics2D g2, Pawn pawn) {
        int x = geometry.toScreenX(pawn.getX());
        int y = geometry.toScreenY(pawn.getY());
        int size = geometry.pawnSize();

        GameAssets.drawFit(g2, GameAssets.pawn(pawn.getOwner().getIndex()), x, y - size / 8, size);
    }

    private void drawPlacementPawn(Graphics2D g2, Pawn pawn) {
        float progress = placement.progress();
        float eased = Math.min(1.08f, AnimationMath.easeOutBack(Math.min(1f, progress / 0.82f)));
        float alpha = Math.max(0f, Math.min(1f, progress / 0.45f));
        int baseSize = geometry.pawnSize();
        int size = Math.max(1, Math.round(baseSize * (0.58f + 0.42f * eased)));
        int x = geometry.toScreenX(pawn.getX());
        int y = geometry.toScreenY(pawn.getY()) - size / 8;

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        drawGlow(g2, x, y, Math.round(size * 1.35f), placementGlow(pawn, alpha));
        GameAssets.drawFit(g2, GameAssets.pawn(pawn.getOwner().getIndex()), x, y, size);
        g2.setComposite(oldComposite);
    }

    private Color placementGlow(Pawn pawn, float alpha) {
        Color base = BoardColors.playerGlow(pawn);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.round(50 * alpha));
    }

    private void drawPlacementFlowerAnimation(Graphics2D g2) {
        PlacementAnimator.PlacementAnimation animation = placement.animation();
        if (animation == null) {
            return;
        }

        float progress = placement.progress();
        drawPlacementDisappearingFlower(g2, animation.pawn(), animation.firstFlower(), progress);
        drawPlacementDisappearingFlower(g2, animation.pawn(), animation.secondFlower(), progress);
    }

    private void drawPlacementDisappearingFlower(Graphics2D g2, Pawn pawn, Flower flower, float progress) {
        float eased = AnimationMath.smooth(progress);
        int startX = geometry.toScreenX(flower.getX());
        int startY = geometry.toScreenY(flower.getY());

        java.awt.Window win = SwingUtilities.getWindowAncestor(component);
        PlayerPanel targetPanel = null;
        if (win instanceof MainFrame frame) {
            if (pawn.getOwner().getIndex() == 0) {
                targetPanel = frame.getPanelTop();
            } else {
                targetPanel = frame.getPanelBottom();
            }
        }

        Point targetPos = getScoreItemCenter(targetPanel, flower.getColor());

        double x = startX + (targetPos.x - startX) * eased;
        double y = startY + (targetPos.y - startY) * eased;

        double startSize = geometry.flowerSize();
        double endSize = 22.0;
        double size = startSize + (endSize - startSize) * eased;

        double angle = eased * Math.PI * 2.0;

        var oldTransform = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);
        GameAssets.drawFit(g2, GameAssets.flower(flower.getColor()), 0, 0, (int) Math.round(size));
        g2.setTransform(oldTransform);
    }

    
    private void drawResolutionPhase(Graphics2D g2) {
        ResolutionAnimator.ResolutionPhase phase = resolution.currentPhase();
        if (wordSplash.active() || model.getPhase() != GameModel.GamePhase.RESOLVING || phase == null) {
            cachedVoronoi = null;
            lastPhase = null;
            return;
        }

        float progress = resolution.progress();
        Color playerColor = phase.color();

        if (phase != lastPhase || cachedVoronoi == null) {
            lastPhase = phase;
            updateVoronoiCache(phase);
        }

        if (cachedVoronoi != null) {
            float alpha = 0.18f * (progress < 0.2f ? progress / 0.2f : (progress < 0.8f ? 1f : (1f - progress) / 0.2f));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(cachedVoronoi, 0, 0, component.getWidth(), component.getHeight(), null);
            g2.setComposite(old);
        }

        g2.setStroke(RESOLUTION_LINE_STROKE);
        float lineFade = progress < 0.82f ? 1f : Math.max(0f, (1f - progress) / 0.18f);

        for (ResolutionAnimator.ResolutionStep step : phase.steps()) {
            g2.setColor(withAlpha(playerColor, 0.32f * lineFade));
            g2.drawLine(step.pawnX(), step.pawnY(), step.flowerX(), step.flowerY());
            
            if (phase.isCaptured()) {
                drawFlyingFlower(g2, phase, step);
            }
        }
    }

    private void updateVoronoiCache(ResolutionAnimator.ResolutionPhase phase) {
        int res = 1; 
        int w = component.getWidth() / res;
        int h = component.getHeight() / res;
        cachedVoronoi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        List<Pawn> allPawns = new java.util.ArrayList<>();
        for (Player p : model.getJoueurs()) {
            for (Pawn pw : p.getPawns()) if (pw.isPlaced()) allPawns.add(pw);
        }

        if (allPawns.isEmpty()) return;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double mx = geometry.toModelX(x * res);
                double my = geometry.toModelY(y * res);
                Pawn closest = null;
                double minDist = Double.MAX_VALUE;
                for (Pawn p : allPawns) {
                    double dx = p.getX() - mx;
                    double dy = p.getY() - my;
                    double d = dx * dx + dy * dy;
                    if (d < minDist) {
                        minDist = d;
                        closest = p;
                    }
                }
                if (closest != null && closest.getOwner() == phase.player()) {
                    cachedVoronoi.setRGB(x, y, phase.color().getRGB());
                }
            }
        }
    }

    private void drawGlow(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        g2.fillOval(x - size / 2, y - size / 2, size, size);
    }

    private Color withAlpha(Color color, float alpha) {
        int value = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), value);
    }

    private void drawCylinderIntroFlower(Graphics2D g2, Flower flower) {
        Point.Double pos = cylinderIntro.getFlowerPosition(flower, geometry);
        double scale = cylinderIntro.getFlowerScale(flower);
        double angle = cylinderIntro.getFlowerAngle(flower);
        int size = (int) Math.round(geometry.flowerSize() * scale);
        if (size <= 0) return;

        var oldTransform = g2.getTransform();
        g2.translate(pos.x, pos.y);
        g2.rotate(angle);
        GameAssets.drawFit(g2, GameAssets.flower(flower.getColor()), 0, 0, size);
        g2.setTransform(oldTransform);
    }

    private void drawFlyingFlower(Graphics2D g2, ResolutionAnimator.ResolutionPhase phase, ResolutionAnimator.ResolutionStep step) {
        java.awt.Window win = SwingUtilities.getWindowAncestor(component);
        PlayerPanel targetPanel = null;
        if (win instanceof MainFrame frame) {
            if (phase.player().getIndex() == 0) {
                targetPanel = frame.getPanelTop();
            } else {
                targetPanel = frame.getPanelBottom();
            }
        }

        Point targetPos = getScoreItemCenter(targetPanel, step.flower().getColor());
        float vanish = resolution.vanishProgress(); // 0.0 to 1.0
        double eased = AnimationMath.smooth(vanish);

        double x = step.flowerX() + (targetPos.x - step.flowerX()) * eased;
        double y = step.flowerY() + (targetPos.y - step.flowerY()) * eased;

        double startSize = geometry.flowerSize();
        double endSize = 22.0; // matching ICON_SIZE in PlayerPanel
        double size = startSize + (endSize - startSize) * eased;

        double angle = eased * Math.PI * 2.0;

        var oldTransform = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);
        GameAssets.drawFit(g2, GameAssets.flower(step.flower().getColor()), 0, 0, (int) Math.round(size));
        g2.setTransform(oldTransform);
    }

    private Point getScoreItemCenter(PlayerPanel playerPanel, FlowerColor color) {
        if (playerPanel == null) {
            return new Point(component.getWidth() / 2, component.getHeight() / 2);
        }
        
        JPanel scoreTabPanel = null;
        for (Component child : playerPanel.getComponents()) {
            if (child instanceof JPanel panel && panel.getLayout() instanceof java.awt.GridLayout) {
                if (panel.getComponentCount() == FlowerColor.values().length) {
                    scoreTabPanel = panel;
                    break;
                }
            }
        }
        
        if (scoreTabPanel != null) {
            int index = color.ordinal();
            if (index >= 0 && index < scoreTabPanel.getComponentCount()) {
                Component scoreItem = scoreTabPanel.getComponent(index);
                int sx = scoreItem.getX() + scoreItem.getWidth() / 2;
                int sy = scoreItem.getY() + scoreItem.getHeight() / 2;
                
                Point p = new Point(sx, sy);
                return SwingUtilities.convertPoint(scoreTabPanel, p, component);
            }
        }
        
        Point p = new Point(playerPanel.getWidth() / 2, playerPanel.getHeight() / 2);
        return SwingUtilities.convertPoint(playerPanel, p, component);
    }

    private void drawCylinder(Graphics2D g2) {
        BufferedImage img = GameAssets.lacunaCylinder();
        if (img == null) return;

        Point.Double pos = cylinderIntro.getCylinderPosition();
        double angle = cylinderIntro.getCylinderTiltAngle();

        int width = (int) Math.round(Math.min(component.getWidth(), component.getHeight()) * 0.38);
        if (width <= 0) width = 180;
        int height = (int) Math.round(width * img.getHeight() / (double) img.getWidth());

        var oldTransform = g2.getTransform();
        g2.translate(pos.x, pos.y);
        g2.rotate(angle);
        g2.drawImage(img, -width / 2, -height / 2, width, height, null);
        g2.setTransform(oldTransform);
    }

    private void drawLandingRipples(Graphics2D g2) {
        Composite oldComposite = g2.getComposite();

        if (cylinderIntro.active()) {
            for (Flower flower : model.getFleurs()) {
                if (flower.isOnBoard()) {
                    double progress = cylinderIntro.getFlowerRippleProgress(flower);
                    if (progress >= 0.0 && progress <= 1.0) {
                        drawRipple(g2, geometry.toScreenX(flower.getX()), geometry.toScreenY(flower.getY()), progress, flower.getColor());
                    }
                }
            }
        }

        if (placement.active()) {
            double progress = placement.getFlowerRippleProgress();
            if (progress >= 0.0 && progress <= 1.0) {
                PlacementAnimator.PlacementAnimation anim = placement.animation();
                if (anim != null) {
                    drawRipple(g2, geometry.toScreenX(anim.firstFlower().getX()), geometry.toScreenY(anim.firstFlower().getY()), progress, anim.firstFlower().getColor());
                    drawRipple(g2, geometry.toScreenX(anim.secondFlower().getX()), geometry.toScreenY(anim.secondFlower().getY()), progress, anim.secondFlower().getColor());
                }
            }
        }

        g2.setComposite(oldComposite);
    }

    private void drawRipple(Graphics2D g2, int cx, int cy, double progress, FlowerColor color) {
        double startRadius = geometry.flowerSize() * 0.4;
        double endRadius = geometry.flowerSize() * 1.5;
        int radius = (int) Math.round(startRadius + (endRadius - startRadius) * progress);

        float alpha = (float) (1.0 - progress);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.55f));

        Color baseColor = Theme.getColor(color);
        g2.setColor(baseColor);
        g2.setStroke(new BasicStroke(2.0f + (float) (1.5f * (1.0 - progress))));
        g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    private record ElementOnBoard(Object item, double y) {
    }
}
