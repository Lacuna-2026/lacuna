package lacuna;

import javax.swing.*;


public class GameController {
    private final GameModel model;
    private final BoardPanel board;
    private final PlayerPanel panelTop;
    private final PlayerPanel panelBottom;

    public GameController(GameModel model, BoardPanel board, PlayerPanel top, PlayerPanel bottom) {
        this.model = model;
        this.board = board;
        this.panelTop = top;
        this.panelBottom = bottom;
    }

    public void onPlacementValid(Flower f1, Flower f2, double posX, double posY) {
        boolean success = model.placerPionEtCapturer(f1, f2, posX, posY);
        if (!success)
            return;

        panelTop.rafraichir();
        panelBottom.rafraichir();
        board.repaint();

        if (model.getPhase() == GameModel.GamePhase.RESOLVING) {
            Timer t = new Timer(1000, e -> {
                model.resoudreProximite();
                panelTop.rafraichir();
                panelBottom.rafraichir();
                board.repaint();
                afficherResultat();
            });
            t.setRepeats(false);
            t.start();
        }
    }

    private void afficherResultat() {
        Player vainqueur = model.getVainqueur();
        String msg = "Fin de partie !\n\n";

        int[] majorites = model.calculerMajoritesCouleurs();
        int j1Maj = 0, j2Maj = 0;

        for (int m : majorites) {
            if (m == 0)
                j1Maj++;
            if (m == 1)
                j2Maj++;
        }

        msg += model.getJoueurs()[0].getName() + " remporte " + j1Maj + " couleurs.\n";
        msg += model.getJoueurs()[1].getName() + " remporte " + j2Maj + " couleurs.\n\n";

        if (vainqueur != null) {
            msg += "Vainqueur : " + vainqueur.getName() + " !";
        } else {
            msg += "Égalité !";
        }

        int choix = JOptionPane.showOptionDialog(board, msg, "Victoire",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new String[] { "Rejouer", "Quitter" }, "Rejouer");

        if (choix == JOptionPane.YES_OPTION) {
            SwingUtilities.invokeLater(() -> {
                JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(board);
                if (parent instanceof MainFrame mf)
                    mf.relancerPartie();
            });
        } else {
            System.exit(0);
        }
    }
}
