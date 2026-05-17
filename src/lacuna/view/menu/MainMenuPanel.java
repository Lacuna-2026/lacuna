package lacuna.view.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

public class MainMenuPanel extends JPanel {
    private static final String MODE_NORMAL = "Normal";
    private static final String MODE_AI = "Vs AI";
    private static final String MODE_ONLINE = "Online";

    private final BufferedImage orangeFlower = MenuAssets.load("main_men_top_orange.png");
    private final BufferedImage blueFlower = MenuAssets.load("main_men_blue.png");
    private final BufferedImage pinkFlower = MenuAssets.load("main_men_pink.png");

    private final MenuCallback onPlay;
    private final SegmentedChoice modeSelect;
    private final JPanel modeFields;
    private final PrimaryButton playButton;

    /** Callback : (nom1, nom2, aiDifficulty) — aiDifficulty=0 pour mode Normal */
    @FunctionalInterface
    public interface MenuCallback {
        void start(String name1, String name2, int aiDifficulty);
    }

    public MainMenuPanel(MenuCallback onPlay) {
        this.onPlay = onPlay;
        this.modeSelect = new SegmentedChoice(MODE_NORMAL, MODE_AI, MODE_ONLINE);
        this.modeFields = MenuTheme.verticalPanel();
        this.playButton = new PrimaryButton("Jouer Lacuna");

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1100, 680));
        setMinimumSize(new Dimension(920, 620));
        setBackground(MenuTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(40, 58, 22, 58));

        modeSelect.addActionListener((ActionEvent e) -> updateModeFields());

        add(createContent(), BorderLayout.CENTER);

        showNormalFields();
    }

    private JComponent createContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints art = new GridBagConstraints();
        art.gridx = 0;
        art.gridy = 0;
        art.weightx = 0.52;
        art.weighty = 1;
        art.fill = GridBagConstraints.BOTH;
        content.add(new ProductArtPanel(), art);

        GridBagConstraints controls = new GridBagConstraints();
        controls.gridx = 1;
        controls.gridy = 0;
        controls.weightx = 0.48;
        controls.weighty = 1;
        controls.fill = GridBagConstraints.BOTH;
        controls.insets = new Insets(36, 48, 30, 44);
        content.add(createControls(), controls);

        return content;
    }

    private JComponent createControls() {
        JPanel controls = MenuTheme.verticalPanel();

        controls.add(Box.createVerticalGlue());
        controls.add(MenuTheme.label("Mode de jeu", 16, MenuTheme.MUTED_TEXT));
        controls.add(Box.createVerticalStrut(10));
        controls.add(modeSelect);
        controls.add(Box.createVerticalStrut(26));
        controls.add(modeFields);
        controls.add(Box.createVerticalStrut(28));
        controls.add(playButton);
        controls.add(Box.createVerticalGlue());

        return controls;
    }

    private void updateModeFields() {
        String mode = modeSelect.selectedValue();
        if (MODE_AI.equals(mode)) {
            showAiFields();
        } else if (MODE_ONLINE.equals(mode)) {
            showOnlineFields();
        } else {
            showNormalFields();
        }
    }

    private void showNormalFields() {
        modeFields.removeAll();

        JTextField playerOne = MenuTheme.textField("Joueur 1");
        JTextField playerTwo = MenuTheme.textField("Joueur 2");
        addLabeledField("Nom du joueur 1", playerOne);
        addLabeledField("Nom du joueur 2", playerTwo);

        playButton.prepare("Jouer Lacuna", true, true);
        playButton.addActionListener((ActionEvent e) -> {
            String nom1 = valueOrDefault(playerOne.getText(), "Joueur 1");
            String nom2 = valueOrDefault(playerTwo.getText(), "Joueur 2");
            onPlay.start(nom1, nom2, 0);
        });

        refreshFields();
    }

    private void showAiFields() {
        modeFields.removeAll();

        JTextField playerName = MenuTheme.textField("Votre nom");
        SegmentedChoice levels = new SegmentedChoice("Facile", "Moyen", "Difficile");

        addLabeledField("Votre nom", playerName);
        addLabeledField("Niveau de l'AI", levels);

        playButton.prepare("Jouer contre l'IA", true, true);
        playButton.addActionListener((ActionEvent e) -> {
            String nom = valueOrDefault(playerName.getText(), "Joueur");
            String level = levels.selectedValue();
            int difficulty = "Facile".equals(level) ? 1 : "Moyen".equals(level) ? 2 : 3;
            onPlay.start(nom, "IA", difficulty);
        });

        refreshFields();
    }

    private void showOnlineFields() {
        modeFields.removeAll();

        JTextField sessionCode = MenuTheme.textField("Code de session");
        JButton join = new SecondaryButton("Rejoindre");
        JButton create = new SecondaryButton("Creer session");

        sessionCode.setEnabled(false);
        join.setEnabled(false);
        create.setEnabled(false);

        addLabeledField("Code de session", sessionCode);
        modeFields.add(Box.createVerticalStrut(16));
        modeFields.add(join);
        modeFields.add(Box.createVerticalStrut(10));
        modeFields.add(create);
        modeFields.add(Box.createVerticalStrut(12));
        modeFields.add(MenuTheme.label("Mode online indisponible pour le moment.", 13, MenuTheme.MUTED_TEXT));

        playButton.prepare("", true, false);
        refreshFields();
    }

    private void addLabeledField(String label, JComponent field) {
        if (modeFields.getComponentCount() > 0) {
            modeFields.add(Box.createVerticalStrut(14));
        }
        modeFields.add(MenuTheme.label(label, 14, MenuTheme.MUTED_TEXT));
        modeFields.add(Box.createVerticalStrut(8));
        modeFields.add(field);
    }

    private void refreshFields() {
        modeFields.revalidate();
        modeFields.repaint();
        revalidate();
        repaint();
    }

    private static String valueOrDefault(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        MenuAssets.prepare(g2);

        g2.setColor(MenuTheme.BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());

        MenuAssets.drawFit(g2, orangeFlower, getWidth() / 2 - 135, -86, 270, 245);
        MenuAssets.drawFit(g2, blueFlower, getWidth() - 270, 98, 168, 96);
        MenuAssets.drawFit(g2, pinkFlower, getWidth() - 190, getHeight() - 154, 165, 112);
        g2.dispose();
    }
}
