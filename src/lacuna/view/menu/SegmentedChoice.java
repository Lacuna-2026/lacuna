package lacuna.view.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

final class SegmentedChoice extends JComponent {
    private final String[] labels;
    private final List<ActionListener> listeners = new ArrayList<>();
    private int selectedIndex = 0;

    SegmentedChoice(String... labels) {
        this.labels = labels;
        MenuTheme.size(this, 380, 44, 420, 44);
        setMinimumSize(new Dimension(280, 44));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectByPosition(e.getX());
            }
        });
    }

    String selectedValue() {
        return labels[selectedIndex];
    }

    void selectValue(String value) {
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(value)) {
                if (selectedIndex != i) {
                    selectedIndex = i;
                    repaint();
                    fireActionEvent();
                }
                return;
            }
        }
    }

    void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    private void selectByPosition(int x) {
        if (!isEnabled()) {
            return;
        }

        int next = Math.min(labels.length - 1, Math.max(0, x * labels.length / Math.max(1, getWidth())));
        if (next != selectedIndex) {
            selectedIndex = next;
            repaint();
            fireActionEvent();
        }
    }

    private void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selectedValue());
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        MenuAssets.prepare(g2);

        int arc = getHeight();
        g2.setColor(MenuTheme.FIELD_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

        paintSelectedSegment(g2, arc);
        paintLabels(g2);
        g2.dispose();
    }

    private void paintSelectedSegment(Graphics2D g2, int arc) {
        int segmentWidth = getWidth() / labels.length;
        int selectionX = selectedIndex * segmentWidth;
        int selectionW = selectedIndex == labels.length - 1 ? getWidth() - selectionX : segmentWidth;
        Color selected = isEnabled() ? new Color(107, 106, 115) : new Color(73, 72, 80);

        g2.setColor(selected);
        g2.fillRoundRect(selectionX + 3, 3, selectionW - 6, getHeight() - 6, arc - 8, arc - 8);
    }

    private void paintLabels(Graphics2D g2) {
        int segmentWidth = getWidth() / labels.length;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            int x = i * segmentWidth + segmentWidth / 2 - fm.stringWidth(label) / 2;
            int y = getHeight() / 2 + fm.getAscent() / 2 - 3;
            g2.setColor(isEnabled() ? MenuTheme.TEXT : MenuTheme.MUTED_TEXT);
            g2.drawString(label, x, y);
        }
    }
}
