package lacuna.view.menu;

import javax.swing.JToggleButton;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

final class ToggleSwitch extends JToggleButton {
    private static final Color OFF_TRACK = new Color(92, 90, 99);
    private static final Color ON_TRACK = new Color(35, 142, 238);
    private static final Color DISABLED_TRACK = new Color(78, 76, 86);
    private static final Color KNOB = new Color(248, 248, 248);
    private static final Color KNOB_SHADOW = new Color(0, 0, 0, 65);

    ToggleSwitch() {
        setPreferredSize(new Dimension(74, 36));
        setMaximumSize(new Dimension(74, 36));
        setMinimumSize(new Dimension(74, 36));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addItemListener(e -> repaint());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        MenuAssets.prepare(g2);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int trackHeight = height - 4;
        int trackY = 2;
        int arc = trackHeight;
        boolean on = isSelected();
        Color track = isEnabled() ? (on ? ON_TRACK : OFF_TRACK) : DISABLED_TRACK;

        g2.setColor(track);
        g2.fill(new RoundRectangle2D.Double(0, trackY, width, trackHeight, arc, arc));

        int knobSize = trackHeight - 8;
        int knobY = trackY + 4;
        int knobX = on ? width - knobSize - 6 : 6;

        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(KNOB_SHADOW);
        g2.fillOval(knobX, knobY + 2, knobSize, knobSize);
        g2.setColor(KNOB);
        g2.fillOval(knobX, knobY, knobSize, knobSize);
        g2.setColor(new Color(255, 255, 255, isEnabled() ? 155 : 80));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawOval(knobX + 1, knobY + 1, knobSize - 2, knobSize - 2);
        g2.dispose();
    }
}
