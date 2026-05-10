package lacuna.model.AI;

import lacuna.model.*;

public class TestMinimax {

    public static void main(String[] args) {
        GameModel model = new GameModel("IA_0", "IA_1");

        System.out.println("=== Test Minimax pour Lacuna ===");
        System.out.println("Fleurs sur le tapis : " + model.getFleurs().size());
        System.out.println("Pions par joueur   : " + GameModel.PAWNS_PER_PLAYER);
        System.out.println();

        int depth = 3;
        int tour = 1;

        while (model.getPhase() == GameModel.GamePhase.PLACING) {
            int currentIdx = model.getJoueurCourant().getIndex();
            System.out.println("--- Tour " + tour + " | Joueur " + currentIdx
                    + " (" + model.getJoueurCourant().getName() + ") ---");

            long start = System.currentTimeMillis();
            Minimax ai = new Minimax(model, currentIdx, depth);
            AIMove best = ai.findBestMove();
            long elapsed = System.currentTimeMillis() - start;

            if (best == null) {
                System.out.println("  Aucun coup trouvé ! Fin anticipée.");
                break;
            }

            System.out.println("  Meilleur coup : " + best);
            System.out.println("  Temps de calcul : " + elapsed + " ms");

            Flower f1 = model.getFleurs().get(best.flower1Index);
            Flower f2 = model.getFleurs().get(best.flower2Index);
            boolean ok = model.placerPionEtCapturer(f1, f2, best.pawnX, best.pawnY);
            System.out.println("  Coup appliqué : " + (ok ? "OK" : "ECHEC"));

            if (ok) {
                System.out.println("  Fleurs capturées : " + f1.getColor()
                        + " (" + best.flower1Index + ", " + best.flower2Index + ")");
            }

            System.out.print("  Scores -> ");
            for (Player p : model.getJoueurs()) {
                System.out.print(p.getName() + ": " + p.getTotalScore() + " fleurs  ");
            }
            System.out.println();
            System.out.println();

            tour++;
        }

        System.out.println("=== Phase Résolution ===");
        int fleursRestantes = 0;
        for (Flower f : model.getFleurs()) {
            if (f.isOnBoard()) fleursRestantes++;
        }
        System.out.println("Fleurs restantes sur le tapis : " + fleursRestantes);

        model.resoudreProximite();

        System.out.println();
        System.out.println("=== Résultat Final ===");
        for (FlowerColor c : FlowerColor.values()) {
            int s0 = model.getJoueurs()[0].getScoreForColor(c);
            int s1 = model.getJoueurs()[1].getScoreForColor(c);
            String winner = s0 > s1 ? "IA_0" : (s1 > s0 ? "IA_1" : "EGAL");
            System.out.printf("  %-8s : IA_0=%d  IA_1=%d  -> %s%n", c, s0, s1, winner);
        }

        Player vainqueur = model.getVainqueur();
        System.out.println();
        if (vainqueur != null) {
            System.out.println(">>> VAINQUEUR : " + vainqueur.getName() + " <<<");
        } else {
            System.out.println(">>> EGALITE <<<");
        }
    }
}
