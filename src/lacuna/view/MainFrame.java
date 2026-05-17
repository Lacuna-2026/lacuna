package lacuna.view;

import lacuna.model.GameModel;
import lacuna.controller.GameController;
import lacuna.view.menu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private static final Integer TURN_GLOW_LAYER = JLayeredPane.DEFAULT_LAYER + 50;

    private BoardPanel plateau;
    private PlayerPanel panelTop;
    private PlayerPanel panelBottom;
    private GameController controleur;
    private int lastAiDifficulty;

    public MainFrame() {
        super("Lacuna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(920, 620));

        afficherMenu();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void afficherMenu() {
        MainMenuPanel menu = new MainMenuPanel(this::demarrerPartie);

        setContentPane(menu);
        revalidate();
        repaint();
    }

    private void demarrerPartie(String nom1, String nom2, int aiDifficulty) {
        lastAiDifficulty = aiDifficulty;
        construireInterface(nom1, nom2, aiDifficulty);
    }

    private void construireInterface(String nom1, String nom2, int aiDifficulty) {
        GameModel modele = new GameModel(nom1, nom2);

        panelTop = new PlayerPanel(modele, modele.getJoueurs()[0], true);
        panelBottom = new PlayerPanel(modele, modele.getJoueurs()[1], false);
        plateau = new BoardPanel(modele);
        TurnGlowPanel turnGlow = new TurnGlowPanel(modele);

        controleur = new GameController(modele, this, aiDifficulty);
        plateau.setController(controleur);

        JPanel hud = new JPanel(new BorderLayout());
        hud.setOpaque(false);
        hud.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        hud.add(panelTop, BorderLayout.NORTH);
        hud.add(panelBottom, BorderLayout.SOUTH);

        JLayeredPane gameRoot = new JLayeredPane() {
            @Override
            public void doLayout() {
                Dimension size = getSize();
                plateau.setBounds(0, 0, size.width, size.height);
                turnGlow.setBounds(0, 0, size.width, size.height);
                hud.setBounds(0, 0, size.width, size.height);
            }
        };
        gameRoot.setBackground(new Color(20, 22, 35));
        gameRoot.setOpaque(true);
        gameRoot.add(plateau, JLayeredPane.DEFAULT_LAYER);
        gameRoot.add(turnGlow, TURN_GLOW_LAYER);
        gameRoot.add(hud, JLayeredPane.PALETTE_LAYER);
        
        setContentPane(gameRoot);
        revalidate();
        repaint();
    }

    public void relancerPartie() {
        String[] noms = PlayerNamesDialog.show(this);
        if (noms == null) {
            retourMenuPrincipal();
            return;
        }
        construireInterface(noms[0], noms[1], lastAiDifficulty);
    }

    public void jouerAnimationResolution(Runnable apresAnimation) {
        plateau.jouerAnimationResolution(apresAnimation);
    }

    public void retourMenuPrincipal() {
        afficherMenu();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {
            }
            new MainFrame();
        });
    }
}
