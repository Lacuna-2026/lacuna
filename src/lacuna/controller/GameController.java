package lacuna.controller;

import lacuna.model.Flower;
import lacuna.model.FlowerColor;
import lacuna.model.GameModel;
import lacuna.model.Player;
import lacuna.model.AI.AIMove;
import lacuna.model.AI.Minimax;
import lacuna.view.GameResultDialog;
import lacuna.view.MainFrame;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameController {
    private final GameModel model;
    private final MainFrame mainFrame;
    private final int aiDifficulty; // 0=disabled, 1=facile, 2=moyen, 3=difficile
    private final int aiPlayerIndex; // l'IA joue toujours en tant que joueur 1

    public GameController(GameModel model, MainFrame mainFrame, int aiDifficulty) {
        this.model = model;
        this.mainFrame = mainFrame;
        this.aiDifficulty = aiDifficulty;
        this.aiPlayerIndex = 1; // l'IA est le joueur 2
    }

    public boolean isAiTurn() {
        return aiDifficulty > 0
            && model.getPhase() == GameModel.GamePhase.PLACING
            && model.getJoueurCourant().getIndex() == aiPlayerIndex;
    }

    public boolean onPlacementValid(Flower f1, Flower f2, double posX, double posY) {
        boolean success = model.placerPionEtCapturer(f1, f2, posX, posY);
        return success;
    }

    public void onPlacementAnimationFinished() {
        if (model.getPhase() == GameModel.GamePhase.RESOLVING) {
            mainFrame.jouerAnimationResolution(this::afficherResultat);
        } else if (isAiTurn()) {
            // C'est le tour de l'IA — lancer le calcul en arrière-plan
            triggerAiMove();
        }
    }

    /**
     * Lance le calcul du coup IA dans un thread séparé pour ne pas bloquer l'UI.
     * Quand le coup est trouvé, il est appliqué sur le EDT (Swing thread).
     */
    private void triggerAiMove() {
        int depth = switch (aiDifficulty) {
            case 1 -> 2;  // Facile — instantané
            case 2 -> 3;  // Moyen  — ~0.5s
            case 3 -> 4;  // Difficile — ~1-2s
            default -> 3;
        };

        long timeLimit = switch (aiDifficulty) {
            case 1 -> 500;
            case 2 -> 1500;
            case 3 -> 3000;
            default -> 1500;
        };

        new Thread(() -> {
            Minimax ai = new Minimax(model, aiPlayerIndex, depth, timeLimit);
            AIMove best = ai.findBestMove();

            SwingUtilities.invokeLater(() -> {
                if (best == null || model.getPhase() != GameModel.GamePhase.PLACING) return;

                Flower f1 = model.getFleurs().get(best.flower1Index);
                Flower f2 = model.getFleurs().get(best.flower2Index);
                boolean ok = model.placerPionEtCapturer(f1, f2, best.pawnX, best.pawnY);

                if (ok) {
                    // Après le coup de l'IA, vérifier la phase
                    if (model.getPhase() == GameModel.GamePhase.RESOLVING) {
                        mainFrame.jouerAnimationResolution(this::afficherResultat);
                    }
                    // Si c'est encore le tour de l'IA (ne devrait pas arriver), re-trigger
                    else if (isAiTurn()) {
                        // Petit délai pour laisser le repaint se faire
                        Timer delay = new Timer(300, e -> triggerAiMove());
                        delay.setRepeats(false);
                        delay.start();
                    }
                }
            });
        }, "AI-Thread").start();
    }

    private void afficherResultat() {
        Player vainqueur = model.getVainqueur();
        Map<FlowerColor, Integer> majorites = model.calculerMajoritesCouleurs();
        List<FlowerColor> j1Fleurs = new ArrayList<>();
        List<FlowerColor> j2Fleurs = new ArrayList<>();

        for (Map.Entry<FlowerColor, Integer> majorite : majorites.entrySet()) {
            if (majorite.getValue() == 0) j1Fleurs.add(majorite.getKey());
            if (majorite.getValue() == 1) j2Fleurs.add(majorite.getKey());
        }

        GameResultDialog.Choice choix = GameResultDialog.show(
            mainFrame,
            "Résultats",
            new GameResultDialog.PlayerFlowers[]{
                new GameResultDialog.PlayerFlowers(model.getJoueurs()[0].getName(), j1Fleurs),
                new GameResultDialog.PlayerFlowers(model.getJoueurs()[1].getName(), j2Fleurs)
            },
            vainqueur != null ? "Vainqueur : " + vainqueur.getName() : "Egalite"
        );

        if (choix == GameResultDialog.Choice.REPLAY) {
            SwingUtilities.invokeLater(mainFrame::relancerPartie);
        } else {
            SwingUtilities.invokeLater(mainFrame::retourMenuPrincipal);
        }
    }
}
