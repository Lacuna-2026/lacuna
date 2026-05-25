package lacuna.view.board;

import lacuna.view.GameAssets;
import lacuna.model.FlowerColor;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;

public final class WordSplashAnimator {
    private static final int TICK_MS = 25;

    private final JComponent component;
    private Timer timer;
    private BufferedImage image;
    private long startedAt;
    private int durationMs;
    private Runnable onFinished;

    public WordSplashAnimator(JComponent component) {
        this.component = component;
    }

    public void start(BufferedImage image, int durationMs, Runnable onFinished) {
        stop();
        if (image == null || durationMs <= 0) {
            if (onFinished != null) {
                SwingUtilities.invokeLater(onFinished);
            }
            return;
        }

        this.image = image;
        this.durationMs = durationMs;
        this.onFinished = onFinished;
        this.startedAt = System.currentTimeMillis();
        timer = new Timer(TICK_MS, e -> tick());
        timer.setCoalesce(true);
        timer.start();
        component.repaint();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        image = null;
        onFinished = null;
    }

    public boolean active() {
        return image != null;
    }

    public void paint(Graphics2D g2) {
        BufferedImage currentImage = image;
        if (currentImage == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startedAt;
        float progress = Math.max(0f, Math.min(1f, elapsed / (float) durationMs));
        float alpha = progress < 0.88f ? 1f : Math.max(0f, (1f - progress) / 0.12f);

        int flowerSize = (int) Math.round(Math.min(component.getWidth(), component.getHeight()) * 0.85);
        if (flowerSize <= 0) return;
        
        double x, y;
        double angle;
        double scale;
        
        double continuousSpin = progress * Math.PI * 0.8; 

        if (progress < 0.35f) {
            float t = progress / 0.35f;
            float eased = AnimationMath.easeOutBack(t);
            double startX = component.getWidth() + flowerSize / 2.0;
            double startY = component.getHeight() + flowerSize / 2.0;
            double targetX = component.getWidth() / 2.0;
            double targetY = component.getHeight() / 2.0;

            x = startX + (targetX - startX) * eased;
            y = startY + (targetY - startY) * eased;
            scale = eased; 
            angle = continuousSpin + (1.0 - eased) * Math.PI * 0.5; // Slower entry spin
        } else if (progress < 0.65f) {
            x = component.getWidth() / 2.0;
            y = component.getHeight() / 2.0;
            scale = 1.0;
            angle = continuousSpin;
        } else {
            float t = (progress - 0.65f) / 0.35f;
            float eased = AnimationMath.smooth(t);
            double startX = component.getWidth() / 2.0;
            double startY = component.getHeight() / 2.0;
            double targetX = -flowerSize / 2.0;
            double targetY = -flowerSize / 2.0;

            x = startX + (targetX - startX) * eased;
            y = startY + (targetY - startY) * eased;
            scale = 1.0 - eased; 
            angle = continuousSpin - eased * Math.PI * 0.5; 
        }

        int drawW = (int) Math.round(flowerSize * scale);
        int drawH = (int) Math.round(flowerSize * scale);

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        
        g2.translate(x, y);

        g2.rotate(angle);
        drawVectorFlower(g2, drawW);
        g2.rotate(-angle); 

        String text = "";
        if (currentImage == GameAssets.letsPlay()) {
            text = "LET'S PLAY !";
        } else if (currentImage == GameAssets.holdUp()) {
            text = "HOLD UP !";
        } else if (currentImage == GameAssets.gameOver()) {
            text = "GAME OVER";
        }

        if (text.isEmpty()) {
            int textW = (int) Math.round(drawW * 0.8);
            int textH = (int) Math.round(textW * currentImage.getHeight() / (double) currentImage.getWidth());
            if (textH > drawH * 0.6) {
                textH = (int) Math.round(drawH * 0.6);
                textW = (int) Math.round(textH * currentImage.getWidth() / (double) currentImage.getHeight());
            }
            g2.drawImage(currentImage, -textW / 2, -textH / 2, textW, textH, null);
        } else {
            int fontSize = (int) Math.round(drawW * 0.14);
            if (fontSize > 5) {
                Font font = new Font("Segoe UI", Font.BOLD | Font.ITALIC, fontSize);
                drawTextWithOutline(g2, text, 0, 0, font, Color.WHITE, new Color(20, 18, 24), Math.max(1, fontSize / 14));
            }
        }

        g2.translate(-x, -y);
        g2.setComposite(oldComposite);
    }

    private void drawVectorFlower(Graphics2D g2, int size) {
        int r = size / 2;
        if (r <= 0) return;
        int numPetals = 8;
        
        for (int i = 0; i < numPetals; i++) {
            double angle = i * (2 * Math.PI / numPetals);
            Graphics2D gPetal = (Graphics2D) g2.create();
            gPetal.rotate(angle);
            
            int petalW = (int) (r * 0.55);
            int petalH = (int) (r * 0.95);
            if (petalH <= 0) petalH = 1;
            if (petalW <= 0) petalW = 1;
            
            Path2D.Double path = new Path2D.Double();
            path.moveTo(0, 0);
            path.curveTo(-petalW * 0.8, -petalH * 0.35, -petalW, -petalH * 0.75, 0, -petalH);
            path.curveTo(petalW, -petalH * 0.75, petalW * 0.8, -petalH * 0.35, 0, 0);
            
            float[] fractions = {0.0f, 0.7f, 1.0f};
            Color[] colors = {
                new Color(90, 30, 160), 
                new Color(150, 60, 240), 
                new Color(220, 140, 255) 
            };
            LinearGradientPaint gradient = new LinearGradientPaint(
                0, 0, 0, -petalH, fractions, colors
            );
            gPetal.setPaint(gradient);
            gPetal.fill(path);
            
            gPetal.setColor(new Color(30, 10, 60));
            gPetal.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gPetal.draw(path);
            
            gPetal.setColor(new Color(245, 210, 255, 120));
            gPetal.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gPetal.drawLine(0, (int) (-petalH * 0.2), 0, (int) (-petalH * 0.8));
            
            gPetal.dispose();
        }

        int centerRadius = (int) (r * 0.35);
        if (centerRadius <= 0) centerRadius = 1;
        
        float[] cFractions = {0.0f, 0.85f, 1.0f};
        Color[] cColors = {
            new Color(255, 235, 100),  
            new Color(255, 160, 20),   
            new Color(180, 80, 0)      
        };
        RadialGradientPaint rGrad = new RadialGradientPaint(
            0, 0, centerRadius, cFractions, cColors
        );
        g2.setPaint(rGrad);
        g2.fillOval(-centerRadius, -centerRadius, centerRadius * 2, centerRadius * 2);
        
        g2.setColor(new Color(40, 20, 0));
        g2.setStroke(new BasicStroke(4.0f));
        g2.drawOval(-centerRadius, -centerRadius, centerRadius * 2, centerRadius * 2);
        
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(-centerRadius * 3 / 5, -centerRadius * 3 / 5, centerRadius * 3 / 4, centerRadius * 3 / 8);
    }

    private void drawTextWithOutline(Graphics2D g2, String text, int x, int y, Font font, Color textColor, Color outlineColor, int outlineWidth) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textX = x - textW / 2;
        int textY = y + fm.getAscent() - fm.getHeight() / 2;

        g2.setColor(outlineColor);
        for (int dx = -outlineWidth; dx <= outlineWidth; dx++) {
            for (int dy = -outlineWidth; dy <= outlineWidth; dy++) {
                if (dx * dx + dy * dy <= outlineWidth * outlineWidth) {
                    g2.drawString(text, textX + dx, textY + dy);
                }
            }
        }

        g2.setColor(textColor);
        g2.drawString(text, textX, textY);
    }

    private void tick() {
        if (System.currentTimeMillis() - startedAt < durationMs) {
            component.repaint();
            return;
        }

        Runnable finished = onFinished;
        stop();
        component.repaint();
        if (finished != null) {
            finished.run();
        }
    }
}
