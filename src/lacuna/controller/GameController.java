package lacuna.controller;

import lacuna.model.Flower;
import lacuna.model.FlowerColor;
import lacuna.model.GameModel;
import lacuna.model.Pawn;
import lacuna.model.Player;
import lacuna.model.AI.AIMove;
import lacuna.model.AI.Minimax;
import lacuna.view.BoardPanel;
import lacuna.view.GameResultDialog;
import lacuna.view.MainFrame;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameController {
    private static final int AI_THINK_DELAY_MS = 900;

    private final GameModel model;
    private final MainFrame mainFrame;
    private final BoardPanel boardPanel;
    private final int aiPlayerIndex;
    private final int aiDepth;
    private final lacuna.network.OnlineSessionConnection networkSession;
    private final int localPlayerIndex;
    private SwingWorker<Void, List<String>> networkWorker;
    private boolean localWantsRematch = false;
    private boolean opponentWantsRematch = false;

    private boolean aiThinking = false;
    private Timer aiTimer;
    private SwingWorker<AIMove, Void> aiWorker;

    public GameController(GameModel model, MainFrame mainFrame, BoardPanel boardPanel, int aiPlayerIndex, int aiDepth, lacuna.network.OnlineSessionConnection networkSession, int localPlayerIndex) {
        this.model = model;
        this.mainFrame = mainFrame;
        this.boardPanel = boardPanel;
        this.aiPlayerIndex = aiPlayerIndex;
        this.aiDepth = aiDepth;
        this.networkSession = networkSession;
        this.localPlayerIndex = localPlayerIndex;
        
        if (networkSession != null) {
            startNetworkListener();
        }
    }

    private void startNetworkListener() {
        networkWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    try {
                        List<String> data = networkSession.receiveData();
                        if (data != null && !data.isEmpty()) {
                            publish(data);
                        }
                    } catch (java.io.IOException e) {
                        if (!isCancelled()) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
                return null;
            }

            @Override
            protected void process(List<List<String>> chunks) {
                for (List<String> data : chunks) {
                    if (data.size() == 1 && data.get(0).equals("REMATCH")) {
                        opponentWantsRematch = true;
                        if (localWantsRematch) {
                            SwingUtilities.invokeLater(mainFrame::relancerPartie);
                        }
                    } else {
                        traiterCoupReseau(data);
                    }
                }
            }
            
            @Override
            protected void done() {
                if (localWantsRematch && !opponentWantsRematch) {
                    JOptionPane.showMessageDialog(mainFrame, "L'adversaire a refuse la revanche ou a quitte la partie.", "Deconnexion", JOptionPane.WARNING_MESSAGE);
                    mainFrame.retourMenuPrincipal();
                } else if (model.getVainqueur() == null && model.getPhase() != GameModel.GamePhase.RESOLVING && !localWantsRematch) {
                    JOptionPane.showMessageDialog(mainFrame, "L'adversaire a quitte la partie.", "Deconnexion", JOptionPane.WARNING_MESSAGE);
                    mainFrame.retourMenuPrincipal();
                }
            }
        };
        networkWorker.execute();
    }

    private void traiterCoupReseau(List<String> data) {
        if (data.size() < 4) return;
        try {
            int f1Index = Integer.parseInt(data.get(0));
            int f2Index = Integer.parseInt(data.get(1));
            double posX = Double.parseDouble(data.get(2));
            double posY = Double.parseDouble(data.get(3));
            
            Flower f1 = model.getFleurs().get(f1Index);
            Flower f2 = model.getFleurs().get(f2Index);
            
            Pawn pionLibre = null;
            for (Pawn pw : model.getJoueurCourant().getPawns()) {
                if (!pw.isPlaced()) { pionLibre = pw; break; }
            }
            final Pawn finalPionLibre = pionLibre;
            
            boardPanel.montrerIntentionIA(f1, f2, posX, posY, () -> {
                boolean success = model.placerPionEtCapturer(f1, f2, posX, posY);
                if (success && finalPionLibre != null) {
                    boardPanel.jouerAnimationPionIA(finalPionLibre, f1, f2, this::onPlacementAnimationFinished);
                }
            });
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public boolean isAiMode() {
        return aiPlayerIndex >= 0;
    }

    public boolean isAiTurn() {
        return isAiMode() && model.getJoueurCourant().getIndex() == aiPlayerIndex;
    }

    public boolean isAiThinking() {
        return aiThinking;
    }

    public boolean onPlacementValid(Flower f1, Flower f2, double posX, double posY) {
        if (networkSession != null && model.getJoueurCourant().getIndex() != localPlayerIndex) {
            return false;
        }
        
        boolean success = model.placerPionEtCapturer(f1, f2, posX, posY);
        if (success && networkSession != null) {
            int f1Index = model.getFleurs().indexOf(f1);
            int f2Index = model.getFleurs().indexOf(f2);
            List<String> data = new ArrayList<>();
            data.add(String.valueOf(f1Index));
            data.add(String.valueOf(f2Index));
            data.add(String.valueOf(posX));
            data.add(String.valueOf(posY));
            networkSession.sendData(data);
        }
        return success;
    }

    public void onPlacementAnimationFinished() {
        if (model.getPhase() == GameModel.GamePhase.RESOLVING) {
            mainFrame.jouerAnimationResolution(this::afficherResultat);
        } else if (model.getPhase() == GameModel.GamePhase.PLACING && isAiTurn()) {
            schedulerCoupIA();
        }
    }

    public void declencherCoupIASiNecessaire() {
        if (model.getPhase() == GameModel.GamePhase.PLACING && isAiTurn() && !aiThinking) {
            schedulerCoupIA();
        }
    }

    public void annulerCoup() {
        if (networkSession != null) return;
        if (model.getPhase() != GameModel.GamePhase.PLACING) return;
        if (!model.peutAnnuler()) return;

        if (aiThinking) {
            if (aiTimer != null) aiTimer.stop();
            if (aiWorker != null) aiWorker.cancel(true);
            aiThinking = false;
        }
        if (!model.peutAnnuler()) return;

        model.annulerDernierCoup();
        boardPanel.onUndoPerformed();
        declencherCoupIASiNecessaire();
    }

    private void schedulerCoupIA() {
        if (aiThinking) return;
        aiThinking = true;

        aiTimer = new Timer(AI_THINK_DELAY_MS, e -> {
            aiWorker = new SwingWorker<>() {
                @Override
                protected AIMove doInBackground() {
                    if (aiDepth <= 1) {
                        return choisirCoupSimple(false);
                    }
                    if (aiDepth == 2) {
                        return choisirCoupSimple(true);
                    }
                    Minimax minimax = new Minimax(model, aiPlayerIndex, aiDepth, 2000);
                    return minimax.findBestMove();
                }

                @Override
                protected void done() {
                    if (isCancelled()) return;
                    aiThinking = false;
                    try {
                        AIMove move = get();
                        if (move != null) {
                            jouerCoupIA(move);
                        }
                    } catch (Exception ex) {
                        if (!(ex instanceof java.util.concurrent.CancellationException)) {
                            ex.printStackTrace();
                        }
                    }
                }
            };
            aiWorker.execute();
        });
        aiTimer.setRepeats(false);
        aiTimer.start();
    }

    private AIMove choisirCoupSimple(boolean greedy) {
        lacuna.model.AI.AIState state = lacuna.model.AI.AIState.fromModel(model);
        List<AIMove> moves = new lacuna.model.AI.DefaultAIMoveGenerator(true).generateMoves(state);
        if (moves.isEmpty()) return null;

        lacuna.model.AI.IAIEvaluator evaluator = new lacuna.model.AI.DefaultAIEvaluator();

        if (!greedy) {
            java.util.Collections.shuffle(moves);
            for (AIMove move : moves) {
                if (evaluator.quickEval(state, move, aiPlayerIndex) > 0) {
                    return move;
                }
            }
            return moves.get(0);
        }

        AIMove bestMove = moves.get(0);
        double bestScore = evaluator.quickEval(state, bestMove, aiPlayerIndex);
        for (int i = 1; i < moves.size(); i++) {
            AIMove move = moves.get(i);
            double score = evaluator.quickEval(state, move, aiPlayerIndex);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private void jouerCoupIA(AIMove move) {
        List<Flower> fleurs = model.getFleurs();
        Flower f1 = fleurs.get(move.flower1Index);
        Flower f2 = fleurs.get(move.flower2Index);

        if (!model.estLigneValide(f1, f2)) return;

        Pawn pionLibre = null;
        for (Pawn pw : model.getJoueurCourant().getPawns()) {
            if (!pw.isPlaced()) { pionLibre = pw; break; }
        }
        
        final Pawn finalPionLibre = pionLibre;

        boardPanel.montrerIntentionIA(f1, f2, move.pawnX, move.pawnY, () -> {
            boolean success = model.placerPionEtCapturer(f1, f2, move.pawnX, move.pawnY);
            if (success && finalPionLibre != null) {
                boardPanel.jouerAnimationPionIA(finalPionLibre, f1, f2, this::onPlacementAnimationFinished);
            }
        });
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
            if (networkSession != null) {
                List<String> msg = new ArrayList<>();
                msg.add("REMATCH");
                networkSession.sendData(msg);
                
                localWantsRematch = true;
                if (opponentWantsRematch) {
                    SwingUtilities.invokeLater(mainFrame::relancerPartie);
                } else {
                    SwingUtilities.invokeLater(mainFrame::afficherAttenteRematch);
                }
            } else {
                SwingUtilities.invokeLater(mainFrame::relancerPartie);
            }
        } else {
            SwingUtilities.invokeLater(mainFrame::retourMenuPrincipal);
        }
    }
}
