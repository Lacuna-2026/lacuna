package lacuna;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private BoardPanel plateau;
    private PlayerPanel panelTop;
    private PlayerPanel panelBottom;

    public MainFrame() {
        super("Lacuna");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(780, 620));

        String nom1 = demanderNom("Nom du Joueur 1 :", "Joueur 1");
        String nom2 = demanderNom("Nom du Joueur 2 :", "Joueur 2");

        construireInterface(nom1, nom2);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void construireInterface(String nom1, String nom2) {
        GameModel modele = new GameModel(nom1, nom2);

        panelTop = new PlayerPanel(modele, modele.getJoueurs()[0], true);
        panelBottom = new PlayerPanel(modele, modele.getJoueurs()[1], false);
        plateau = new BoardPanel(modele, null);
        GameController controleur = new GameController(modele, plateau, panelTop, panelBottom);
        plateau.setController(controleur);

        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(20, 22, 35));

        getContentPane().add(panelTop, BorderLayout.NORTH);
        getContentPane().add(plateau, BorderLayout.CENTER);
        getContentPane().add(panelBottom, BorderLayout.SOUTH);

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private String demanderNom(String invite, String valeurDefaut) {
        String saisie = JOptionPane.showInputDialog(null, invite, valeurDefaut);
        if (saisie == null || saisie.isBlank())
            return valeurDefaut;
        return saisie.trim();
    }

    public void relancerPartie() {
        String nom1 = demanderNom("Nom du Joueur 1 :", "Joueur 1");
        String nom2 = demanderNom("Nom du Joueur 2 :", "Joueur 2");
        construireInterface(nom1, nom2);
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
