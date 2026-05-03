package lacuna;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BoardPanel extends JPanel {
    private static final double BOARD_SCALE = 0.95;
    private static final int FLOWER_RADIUS = 12;
    private static final int PAWNS_RADIUS = 10;

    private final GameModel model;
    private GameController controller;

    private Flower hoveredFlower = null;
    private Flower firstSelectedFlower = null;
    private Point mousePoint = null;

    public BoardPanel(GameModel model, GameController controller) {
        this.model = model;
        this.controller = controller;
        setBackground(new Color(24, 30, 20));

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
    }

    private static final double TILT_FACTOR = 0.65;

    private double getPixelScale() {
        return Math.min(getWidth(), getHeight()) / 2.0 * BOARD_SCALE;
    }

    private int toScreenX(double modX) {
        return (int) (getWidth() / 2.0 + modX * getPixelScale());
    }

    private int toScreenY(double modY) {
        return (int) (getHeight() / 2.0 + modY * getPixelScale() * TILT_FACTOR);
    }

    private double toModelX(int scrX) {
        return (scrX - getWidth() / 2.0) / getPixelScale();
    }

    private double toModelY(int scrY) {
        return (scrY - getHeight() / 2.0) / (getPixelScale() * TILT_FACTOR);
    }

    private Flower flowerAt(int x, int y) {
        double mx = toModelX(x);
        double my = toModelY(y);
        double threshold = (FLOWER_RADIUS + 4) / getPixelScale();

        for (Flower f : model.getFleurs()) {
            if (f.isOnBoard() && f.distanceTo(mx, my) <= threshold) {
                return f;
            }
        }
        return null;
    }

    private void updateHover(int x, int y) {
        if (model.getPhase() != GameModel.GamePhase.PLACING)
            return;

        Flower f = flowerAt(x, y);
        if (f != hoveredFlower) {
            hoveredFlower = f;
            repaint();
        } else if (firstSelectedFlower != null) {
            repaint();
        }
    }

    private void handleClick(int x, int y, int button) {
        if (model.getPhase() != GameModel.GamePhase.PLACING)
            return;

        if (button == MouseEvent.BUTTON3) {
            firstSelectedFlower = null;
            repaint();
            return;
        }


        if (firstSelectedFlower == null) {
            Flower clicked = flowerAt(x, y);
            if (clicked != null) {
                firstSelectedFlower = clicked;
                repaint();
            }
        } else {
            Flower clickedFlower = flowerAt(x, y);
            if (clickedFlower != null) {
                if (clickedFlower == firstSelectedFlower) {
                    firstSelectedFlower = null;
                } else if (clickedFlower.getColorIndex() == firstSelectedFlower.getColorIndex()) {
                    double mx = (firstSelectedFlower.getX() + clickedFlower.getX()) / 2.0;
                    double my = (firstSelectedFlower.getY() + clickedFlower.getY()) / 2.0;
                    if (model.estLigneValide(firstSelectedFlower, clickedFlower)) {
                        controller.onPlacementValid(firstSelectedFlower, clickedFlower, mx, my);
                        firstSelectedFlower = null;
                    } else {
                        JOptionPane.showMessageDialog(this, "Ligne invalide : elle traverse d'autres pièces !");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "La deuxième fleur doit être de la même couleur !");
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Veuillez cliquer sur la DEUXIÈME fleur de même couleur.\nLe pion sera placé au milieu.");
            }
            repaint();
        }
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        dessinerTapis(g2);

        if (firstSelectedFlower != null && mousePoint != null) {
            int sx = toScreenX(firstSelectedFlower.getX());
            int sy = toScreenY(firstSelectedFlower.getY());

            boolean valide = false;
            if (hoveredFlower != null && hoveredFlower != firstSelectedFlower
                    && hoveredFlower.getColorIndex() == firstSelectedFlower.getColorIndex()) {
                valide = model.estLigneValide(firstSelectedFlower, hoveredFlower);
            }

            g2.setColor(valide ? new Color(100, 255, 100, 150) : new Color(255, 255, 255, 80));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 5 }, 0));
            g2.drawLine(sx, sy, mousePoint.x, mousePoint.y);

            if (valide && hoveredFlower != null) {
                int mx = (sx + toScreenX(hoveredFlower.getX())) / 2;
                int my = (sy + toScreenY(hoveredFlower.getY())) / 2;
                int rx = PAWNS_RADIUS;
                int ry = (int) (PAWNS_RADIUS * TILT_FACTOR);
                g2.setColor(new Color(255, 255, 255, 100));
                g2.fillOval(mx - rx, my - ry, rx * 2, ry * 2);
            }
        }

        dessinerElements3D(g2);
    }

    private void dessinerTapis(Graphics2D g2) {
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int rx = (int) (getPixelScale() / BOARD_SCALE);
        int ry = (int) (rx * TILT_FACTOR);
        int epaisseur = 12;

        g2.setColor(new Color(15, 25, 15));
        g2.fillOval(cx - rx, cy - ry + epaisseur, rx * 2, ry * 2);
        g2.fillRect(cx - rx, cy, rx * 2, epaisseur);

        g2.setColor(new Color(30, 45, 30));
        g2.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);

        g2.setColor(new Color(50, 70, 50));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    private record Element3D(Object item, double y) {
    }

    private void dessinerElements3D(Graphics2D g2) {
        java.util.List<Element3D> elements = new java.util.ArrayList<>();

        for (Flower f : model.getFleurs()) {
            if (f.isOnBoard())
                elements.add(new Element3D(f, f.getY()));
        }
        for (Player p : model.getJoueurs()) {
            for (Pawn pw : p.getPawns()) {
                if (pw.isPlaced())
                    elements.add(new Element3D(pw, pw.getY()));
            }
        }

        elements.sort((e1, e2) -> Double.compare(e1.y(), e2.y()));

        for (Element3D e : elements) {
            if (e.item() instanceof Flower f)
                dessinerFleur3D(g2, f);
            else if (e.item() instanceof Pawn p)
                dessinerPion3D(g2, p);
        }
    }

    private void dessinerFleur3D(Graphics2D g2, Flower f) {
        int px = toScreenX(f.getX());
        int py = toScreenY(f.getY());
        int rx = FLOWER_RADIUS;
        int ry = (int) (FLOWER_RADIUS * TILT_FACTOR);
        int h = 4;

        if (f == firstSelectedFlower) {
            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillOval(px - rx - 6, py - ry - 6, (rx + 6) * 2, (ry + 6) * 2);
        } else if (f == hoveredFlower && firstSelectedFlower == null) {
            g2.setColor(new Color(255, 255, 255, 80));
            g2.fillOval(px - rx - 4, py - ry - 4, (rx + 4) * 2, (ry + 4) * 2);
        }

        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(px - rx, py - ry + 4, rx * 2, ry * 2);

        g2.setColor(f.getColor().darker().darker());
        g2.fillOval(px - rx, py - ry, rx * 2, ry * 2);
        g2.fillRect(px - rx, py - h, rx * 2, h);
        g2.drawArc(px - rx, py - ry, rx * 2, ry * 2, 180, 180);
        g2.drawLine(px - rx, py, px - rx, py - h);
        g2.drawLine(px + rx, py, px + rx, py - h);

        g2.setColor(f.getColor());
        g2.fillOval(px - rx, py - h - ry, rx * 2, ry * 2);
        g2.setColor(f.getColor().darker());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(px - rx, py - h - ry, rx * 2, ry * 2);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillOval(px - 2, py - h - 2, 4, 4);
    }

    private void dessinerPion3D(Graphics2D g2, Pawn pw) {
        int px = toScreenX(pw.getX());
        int py = toScreenY(pw.getY());
        int rx = PAWNS_RADIUS;
        int ry = (int) (PAWNS_RADIUS * TILT_FACTOR);
        int h = 18;

        Color c = pw.getOwner().getColor();

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(px - rx, py - ry + 6, rx * 2, ry * 2);

        g2.setColor(c.darker());
        g2.fillOval(px - rx, py - ry, rx * 2, ry * 2);
        g2.fillRect(px - rx, py - h, rx * 2, h);
        g2.setColor(c.darker().darker());
        g2.setStroke(new BasicStroke(1f));
        g2.drawArc(px - rx, py - ry, rx * 2, ry * 2, 180, 180);
        g2.drawLine(px - rx, py, px - rx, py - h);
        g2.drawLine(px + rx, py, px + rx, py - h);

        g2.setColor(c);
        g2.fillOval(px - rx, py - h - ry, rx * 2, ry * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(px - rx, py - h - ry, rx * 2, ry * 2);
    }
}
