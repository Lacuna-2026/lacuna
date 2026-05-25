package lacuna.view.menu;

import lacuna.network.PublicSession;

import javax.swing.*;
import java.awt.*;
import java.util.List;

final class PublicSessionsList extends JPanel {
    private static final Dimension LIST_SIZE = new Dimension(420, 96);

    private final DefaultListModel<PublicSession> model = new DefaultListModel<>();
    private final EmptyAwareList list = new EmptyAwareList(model);

    PublicSessionsList() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(LIST_SIZE);
        setMinimumSize(LIST_SIZE);
        setMaximumSize(LIST_SIZE);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(28);
        list.setVisibleRowCount(3);
        list.setFont(new Font("Segoe UI", Font.BOLD, 14));
        list.setForeground(MenuTheme.TEXT);
        list.setBackground(MenuTheme.FIELD_BACKGROUND);
        list.setSelectionForeground(Color.WHITE);
        list.setSelectionBackground(MenuTheme.SELECTION_PURPLE);
        list.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        list.setCellRenderer(new SessionRenderer());

        JScrollPane pane = new JScrollPane(list);
        pane.setPreferredSize(LIST_SIZE);
        pane.setMinimumSize(LIST_SIZE);
        pane.setMaximumSize(LIST_SIZE);
        pane.setBorder(BorderFactory.createLineBorder(MenuTheme.FIELD_BORDER, 1));
        pane.setBackground(MenuTheme.FIELD_BACKGROUND);
        pane.getViewport().setBackground(MenuTheme.FIELD_BACKGROUND);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(pane, BorderLayout.CENTER);
    }

    void addSelectionListener(Runnable listener) {
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                listener.run();
            }
        });
    }

    PublicSession getSelectedSession() {
        return list.getSelectedValue();
    }

    void setEmptyText(String emptyText) {
        list.setEmptyText(emptyText);
    }

    void clearSessions() {
        model.clear();
        list.clearSelection();
        list.repaint();
    }

    void setSessions(List<PublicSession> sessions, String selectedSessionId) {
        model.clear();

        PublicSession selected = null;
        for (PublicSession session : sessions) {
            model.addElement(session);
            if (session.getSessionId().equals(selectedSessionId)) {
                selected = session;
            }
        }

        if (selected != null) {
            list.setSelectedValue(selected, true);
        } else {
            list.clearSelection();
        }
        list.repaint();
    }

    private static final class EmptyAwareList extends JList<PublicSession> {
        private String emptyText = "Aucune session ouverte.";

        EmptyAwareList(DefaultListModel<PublicSession> model) {
            super(model);
        }

        void setEmptyText(String emptyText) {
            this.emptyText = emptyText;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getModel().getSize() > 0 || emptyText == null || emptyText.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            MenuAssets.prepare(g2);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(MenuTheme.MUTED_TEXT);
            FontMetrics fm = g2.getFontMetrics();
            int x = Math.max(12, (getWidth() - fm.stringWidth(emptyText)) / 2);
            int y = getHeight() / 2 + fm.getAscent() / 2 - 3;
            g2.drawString(emptyText, x, y);
            g2.dispose();
        }
    }

    private static final class SessionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean selected,
                boolean hasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, hasFocus);
            if (value instanceof PublicSession session) {
                label.setText(session.getOwnerName() + "    " + session.getSessionId());
            }
            label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            return label;
        }
    }
}
