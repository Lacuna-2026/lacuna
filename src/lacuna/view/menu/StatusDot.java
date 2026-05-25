package lacuna.view.menu;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

final class StatusDot extends JComponent {
    private boolean online;

    StatusDot() {
        setPreferredSize(new Dimension(14, 14));
        setMaximumSize(new Dimension(14, 14));
        setMinimumSize(new Dimension(14, 14));
    }

    void setOnline(boolean online) {
        this.online = online;
        repaint();
    }

    boolean isOnline() {
        return online;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        MenuAssets.prepare(g2);
        g2.setColor(online ? new Color(104, 214, 132) : new Color(235, 86, 99));
        g2.fillOval(1, 1, getWidth() - 2, getHeight() - 2);
        g2.setColor(new Color(255, 255, 255, 55));
        g2.drawOval(1, 1, getWidth() - 2, getHeight() - 2);
        g2.dispose();
    }
}
