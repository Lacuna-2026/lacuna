package lacuna.view.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

class MenuButton extends JButton {
    private final Color colorStart;
    private final Color colorEnd;

    MenuButton(String text, Color colorStart, Color colorEnd) {
        super(text);
        this.colorStart = colorStart;
        this.colorEnd = colorEnd;
        MenuTheme.size(this, 380, 54, 420, 54);
        setFont(new Font("Segoe UI", Font.BOLD, 18));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    void prepare(String text, boolean enabled, boolean visible) {
        setText(text);
        setEnabled(enabled);
        setVisible(visible);
        clearActionListeners();
    }

    void clearActionListeners() {
        for (java.awt.event.ActionListener listener : getActionListeners()) {
            removeActionListener(listener);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        MenuAssets.prepare(g2);

        Color start = isEnabled() ? colorStart : new Color(78, 76, 86);
        Color end = isEnabled() ? colorEnd : new Color(78, 76, 86);
        int inset = getModel().isPressed() && isEnabled() ? 2 : 0;
        Shape shape = new RoundRectangle2D.Double(
            inset,
            inset,
            getWidth() - 1 - inset * 2,
            getHeight() - 1 - inset * 2,
            32,
            32
        );

        g2.setPaint(new GradientPaint(0, 0, start, getWidth(), 0, end));
        g2.fill(shape);
        if (getModel().isRollover() && isEnabled()) {
            g2.setColor(new Color(255, 255, 255, 26));
            g2.fill(shape);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

final class PrimaryButton extends MenuButton {
    PrimaryButton(String text) {
        super(text, new Color(102, 0, 202), new Color(128, 0, 214));
    }
}

final class SecondaryButton extends MenuButton {
    SecondaryButton(String text) {
        super(text, new Color(61, 60, 70), new Color(80, 79, 90));
    }
}

final class OrangeButton extends MenuButton {
    OrangeButton(String text) {
        super(text, new Color(208, 104, 35), new Color(235, 141, 52));
    }
}

final class RedButton extends MenuButton {
    RedButton(String text) {
        super(text, new Color(190, 48, 62), new Color(225, 73, 86));
    }
}
