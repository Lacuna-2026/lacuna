package lacuna.view.board;

import lacuna.model.Flower;
import lacuna.model.GameModel;
import lacuna.view.GameAssets;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Point;
import java.util.*;

public final class CylinderIntroAnimator {

    private static final int TICK_MS = 16;

    private static final int ENTER_DURATION = 700;
    private static final int SHAKE_DURATION = 1200; 
    private static final int SWEEP_DURATION = 3200; 
    private static final int FLOWER_FLIGHT  = 980;
    private static final int LIFT_DURATION  = 850;

    private static final double TOTAL_ANGLE = 2 * Math.PI * 1.08;
    private static final double START_ANGLE = -Math.PI / 2.0;
    private static final double POUR_ANGLE  = Math.toRadians(28);
    private static final double ORBIT_RATIO = 0.72;
    private static final double SHAKE_PERIOD = 1600.0; 
    private static final double SHAKE_AMP_X  = 22.0;  
    private static final double SHAKE_AMP_Y  = 12.0; 

    private final GameModel  model;
    private final JComponent component;
    private BoardGeometry    geometry;
    private Timer            timer;
    private long             startedAt;
    private boolean          active;
    private Runnable         onFinished;

    private Map<Flower, Integer> spawnOffsets;

    public CylinderIntroAnimator(GameModel model, JComponent component) {
        this.model     = model;
        this.component = component;
    }

    public void start(BoardGeometry geometry, Runnable onFinished) {
        stop();
        this.geometry   = geometry;
        this.onFinished = onFinished;
        this.active     = true;
        this.startedAt  = System.currentTimeMillis();
        this.spawnOffsets = computeSpawnOffsets(geometry);
        timer = new Timer(TICK_MS, e -> tick());
        timer.setCoalesce(true);
        timer.start();
        component.repaint();
    }

    public void start(Runnable onFinished) {
        start(new BoardGeometry(component), onFinished);
    }

    public void stop() {
        if (timer != null) { timer.stop(); timer = null; }
        active = false;
    }

    public boolean active() { return active; }

    private int totalSweepMs() { return SHAKE_DURATION + SWEEP_DURATION; }
    private int flowerAbsSpawnMs(Flower flower) {
        return ENTER_DURATION + spawnOffsets.getOrDefault(flower, 0);
    }

    private int allLandedMs() {
        if (spawnOffsets == null || spawnOffsets.isEmpty())
            return ENTER_DURATION + totalSweepMs() + FLOWER_FLIGHT;
        int maxOff = spawnOffsets.values().stream().mapToInt(v -> v).max().orElse(0);
        return ENTER_DURATION + maxOff + FLOWER_FLIGHT;
    }

    private int totalDuration() { return allLandedMs() + LIFT_DURATION + 150; }

    private void tick() {
        long elapsed = System.currentTimeMillis() - startedAt;
        if (elapsed >= totalDuration()) {
            active = false; stop();
            component.repaint();
            if (onFinished != null) onFinished.run();
            return;
        }
        component.repaint();
    }

    private Point.Double boardCenter() {
        return new Point.Double(geometry.toScreenX(0), geometry.toScreenY(0));
    }

    private double orbitRadius() {
        return geometry.pixelScale() * ORBIT_RATIO;
    }

    private double sweepEase(double t) {
        return Math.pow(t, 1.65);
    }
    private Point.Double circlePos(double t) {
        Point.Double c = boardCenter();
        double r = orbitRadius();

        double angle = START_ANGLE + sweepEase(t) * TOTAL_ANGLE;

        double rWobble = 28 * Math.sin(2 * Math.PI * t * 2.3  + 0.70)
                       + 16 * Math.sin(2 * Math.PI * t * 3.7  - 1.10)
                       + 10 * Math.sin(2 * Math.PI * t * 5.1  + 0.35)
                       +  6 * Math.sin(2 * Math.PI * t * 7.3  - 0.80);

        double aWobble = 0.10 * Math.sin(2 * Math.PI * t * 1.8 + 0.40)
                       + 0.06 * Math.sin(2 * Math.PI * t * 3.1 - 0.90)
                       + 0.04 * Math.sin(2 * Math.PI * t * 4.6 + 1.20)
                       + 0.02 * Math.sin(2 * Math.PI * t * 6.2 - 0.30);

        return new Point.Double(
            c.x + (r + rWobble) * Math.cos(angle + aWobble),
            c.y + (r + rWobble) * Math.sin(angle + aWobble)
        );
    }

    private Point.Double combinedPos(double tFull) {
        double shakeRatio = (double) SHAKE_DURATION / totalSweepMs();
        if (tFull <= shakeRatio) {
            return circlePos(0.0);
        }
        double tOrbit = (tFull - shakeRatio) / (1.0 - shakeRatio);
        return circlePos(tOrbit);
    }

    private Point.Double cylPosAt(long elapsed) {
        int shakeStart = ENTER_DURATION;
        int orbitStart = ENTER_DURATION + SHAKE_DURATION;
        int orbitEnd   = orbitStart + SWEEP_DURATION;
        int allLanded  = allLandedMs();

        if (elapsed < shakeStart) {
            double tIn   = elapsed / (double) ENTER_DURATION;
            double eased = AnimationMath.smooth((float) tIn);
            Point.Double target = circlePos(0.0);
            Point.Double c = boardCenter();
            double r = orbitRadius();
            double fromX = c.x + (r * 1.6) * Math.cos(START_ANGLE);
            double fromY = c.y + (r * 1.6) * Math.sin(START_ANGLE);
            return new Point.Double(
                fromX + (target.x - fromX) * eased,
                fromY + (target.y - fromY) * eased
            );

        } else if (elapsed < orbitStart) {
            double shakeT = (elapsed - shakeStart) / SHAKE_PERIOD * (2 * Math.PI);
            Point.Double base = circlePos(0.0);
            double sx = SHAKE_AMP_X * Math.sin(shakeT);
            double sy = SHAKE_AMP_Y * Math.sin(shakeT * 0.63);
            return new Point.Double(base.x + sx, base.y + sy);

        } else if (elapsed < orbitEnd) {
            double tOrbit = (elapsed - orbitStart) / (double) SWEEP_DURATION;
            return circlePos(tOrbit);

        } else if (elapsed < allLanded) {
            return circlePos(1.0);

        } else {
            double tLift   = elapsed - allLanded;
            double progress = Math.min(1.0, tLift / (double) LIFT_DURATION);
            double eased   = AnimationMath.smooth((float) progress);
            Point.Double end = circlePos(1.0);
            double offY = -component.getHeight() * 0.5;
            return new Point.Double(end.x, end.y + (offY - end.y) * eased);
        }
    }

    public Point.Double getCylinderPosition() {
        long elapsed = System.currentTimeMillis() - startedAt;
        return cylPosAt(elapsed);
    }

    public double getCylinderTiltAngle() {
        long elapsed = System.currentTimeMillis() - startedAt;
        return getCylinderTiltAngleAt(elapsed);
    }

    private double getCylinderTiltAngleAt(long elapsed) {
        Point.Double pos = cylPosAt(elapsed);
        Point.Double c = boardCenter();
        return Math.atan2(c.y - pos.y, c.x - pos.x) - Math.PI / 2.0;
    }

    private double cylinderHeight() {
        java.awt.image.BufferedImage img = GameAssets.lacunaCylinder();
        if (img == null) return 180.0;
        double width = Math.min(component.getWidth(), component.getHeight()) * 0.38;
        if (width <= 0) width = 180.0;
        return width * img.getHeight() / img.getWidth();
    }

    public Point.Double getCylinderBottomPosition(long elapsed) {
        Point.Double pos = cylPosAt(elapsed);
        double angle = getCylinderTiltAngleAt(elapsed);
        double h = cylinderHeight();
        double halfH = h / 2.0;
        return new Point.Double(
            pos.x - halfH * Math.sin(angle),
            pos.y + halfH * Math.cos(angle)
        );
    }

    private static final int SPAWN_SAMPLES = 300;
    private static final int MIN_SPAWN_GAP = 30; 
    
    private Map<Flower, Integer> computeSpawnOffsets(BoardGeometry geo) {
        List<Flower> flowers = new ArrayList<>();
        for (Flower f : model.getFleurs()) {
            if (f.isOnBoard()) {
                flowers.add(f);
            }
        }

        if (flowers.isEmpty()) {
            return new IdentityHashMap<>();
        }

        flowers.sort((f1, f2) -> {
            double a1 = Math.atan2(f1.getY(), f1.getX());
            double a2 = Math.atan2(f2.getY(), f2.getX());
            
            double d1 = a1 - START_ANGLE;
            while (d1 < 0) d1 += 2 * Math.PI;
            while (d1 >= 2 * Math.PI) d1 -= 2 * Math.PI;
            
            double d2 = a2 - START_ANGLE;
            while (d2 < 0) d2 += 2 * Math.PI;
            while (d2 >= 2 * Math.PI) d2 -= 2 * Math.PI;
            
            return Double.compare(d1, d2);
        });

        Map<Flower, Integer> result = new IdentityHashMap<>();
        int totalMs = totalSweepMs();
        int n = flowers.size();

        for (int i = 0; i < n; i++) {
            double ratio = (n > 1) ? (double) i / (n - 1) : 0.0;
            int offset = (int) Math.round(ratio * totalMs);
            result.put(flowers.get(i), offset);
        }

        return result;
    }

    public boolean shouldDrawFlower(Flower flower) {
        if (!active) return true;
        long elapsed = System.currentTimeMillis() - startedAt;
        return elapsed >= flowerAbsSpawnMs(flower);
    }

    public Point.Double getFlowerPosition(Flower flower, BoardGeometry geo) {
        double tx = geo.toScreenX(flower.getX());
        double ty = geo.toScreenY(flower.getY());
        if (!active) return new Point.Double(tx, ty);

        long elapsed = System.currentTimeMillis() - startedAt;
        int  spawnMs = flowerAbsSpawnMs(flower);
        int  dt      = (int) (elapsed - spawnMs);

        if (dt < 0)           return getCylinderBottomPosition(elapsed);
        if (dt >= FLOWER_FLIGHT) return new Point.Double(tx, ty);

        double u = dt / (double) FLOWER_FLIGHT;

        Point.Double drop = getCylinderBottomPosition(spawnMs);
        double x, y;
        final double FLIGHT_END = 0.85;
        if (u <= FLIGHT_END) {
            double v = u / FLIGHT_END;
            double eased = AnimationMath.smooth((float) v);
            x = drop.x + (tx - drop.x) * eased;
            y = drop.y + (ty - drop.y) * eased;
        } else {
            double v = (u - FLIGHT_END) / (1.0 - FLIGHT_END);
            double bounce = 20.0 * Math.sin(Math.PI * v) * (1.0 - v * 0.7);
            x = tx;
            y = ty - bounce;
        }
        return new Point.Double(x, y);
    }

    public double getFlowerScale(Flower flower) {
        return 1.0;
    }

    public double getFlowerAngle(Flower flower) {
        if (!active) return 0.0;
        long elapsed = System.currentTimeMillis() - startedAt;
        int dt = (int) (elapsed - flowerAbsSpawnMs(flower));
        if (dt < 0 || dt >= FLOWER_FLIGHT) return 0.0;
        double u    = dt / (double) FLOWER_FLIGHT;
        double spin = flowerDrift(indexOf(flower), 2) >= 0 ? 1.0 : -1.0;
        final double FLIGHT_END = 0.85;
        if (u <= FLIGHT_END)
            return spin * Math.sin(Math.PI * u / FLIGHT_END) * Math.PI * 1.3;
        else
            return spin * Math.PI * 1.3 * (1.0 - (u - FLIGHT_END) / (1.0 - FLIGHT_END));
    }

    private double flowerDrift(int index, int salt) {
        long h = ((long) index * 2654435761L + (long) salt * 40503L) & 0x7FFFFFFFL;
        return (h % 10000L) / 5000.0 - 1.0;
    }

    private int indexOf(Flower target) {
        int i = 0;
        for (Flower f : model.getFleurs()) {
            if (!f.isOnBoard()) continue;
            if (f == target) return i;
            i++;
        }
        return i;
    }

    public double getFlowerRippleProgress(Flower flower) {
        if (!active) return -1.0;
        long elapsed = System.currentTimeMillis() - startedAt;
        int spawnMs = flowerAbsSpawnMs(flower);
        int dt = (int) (elapsed - spawnMs - FLOWER_FLIGHT);
        if (dt < 0 || dt >= 500) return -1.0;
        return dt / 500.0;
    }
}
