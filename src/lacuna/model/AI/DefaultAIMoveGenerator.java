package lacuna.model.AI;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import lacuna.model.GameModel;

public class DefaultAIMoveGenerator implements IAIMoveGenerator {

    private final double hitboxRadius;
    private final boolean midpointOnly;

    public DefaultAIMoveGenerator() {
        this(false);
    }

    public DefaultAIMoveGenerator(boolean midpointOnly) {
        this.hitboxRadius = GameModel.HITBOX_RADIUS;
        this.midpointOnly = midpointOnly;
    }

    @Override
    public List<AIMove> generateMoves(AIState state) {
        List<AIMove> moves = new ArrayList<>();
        for (int[] group : state.colorGroups) {
            for (int a = 0; a < group.length; a++) {
                int i = group[a];
                if (!state.flowers[i].isOnBoard()) continue;
                for (int b = a + 1; b < group.length; b++) {
                    int j = group[b];
                    if (!state.flowers[j].isOnBoard()) continue;
                    if (isLineValid(state, i, j)) {
                        double[] best = bestPawnPosition(state, i, j);
                        moves.add(new AIMove(i, j, best[0], best[1]));
                    }
                }
            }
        }
        return moves;
    }

    private double[] bestPawnPosition(AIState state, int idx1, int idx2) {
        AIFlower f1 = state.flowers[idx1];
        AIFlower f2 = state.flowers[idx2];

        if (midpointOnly) {
            return new double[] {(f1.x + f2.x) / 2.0, (f1.y + f2.y) / 2.0};
        }

        double t = 0.5;
        double learningRate = 0.2;
        double epsilon = 1e-3;
        int maxIterations = 8;

        for (int iter = 0; iter < maxIterations; iter++) {
            double scorePlus = evaluateT(state, f1, f2, idx1, idx2, t + epsilon);
            double scoreMinus = evaluateT(state, f1, f2, idx1, idx2, t - epsilon);
            double grad = (scorePlus - scoreMinus) / (2 * epsilon);
            
            t += learningRate * grad;
            
            if (t < 0.05) t = 0.05;
            if (t > 0.95) t = 0.95;
            
            // Stop early if gradient is small enough
            if (Math.abs(grad) < 1e-4) {
                break;
            }
        }

        double bestX = f1.x + t * (f2.x - f1.x);
        double bestY = f1.y + t * (f2.y - f1.y);

        // Fallback to exactly 0.5 if position is somehow blocked after gradient descent
        if (!isPositionClear(state, bestX, bestY, idx1, idx2)) {
            bestX = (f1.x + f2.x) / 2.0;
            bestY = (f1.y + f2.y) / 2.0;
        }

        return new double[] { bestX, bestY };
    }

    private double evaluateT(AIState state, AIFlower f1, AIFlower f2, int idx1, int idx2, double t) {
        double px = f1.x + t * (f2.x - f1.x);
        double py = f1.y + t * (f2.y - f1.y);
        
        // Penalize heavily if position is not clear
        if (!isPositionClear(state, px, py, idx1, idx2)) {
            return -1000.0;
        }
        
        return countFlowersWon(state, px, py, idx1, idx2);
    }

    private double countFlowersWon(AIState state, double px, double py,
            int skipF1, int skipF2) {
        double score = 0;
        int player = state.currentPlayer;
        int opponent = 1 - player;
        int numColors = state.colorGroups.length;

        int[] myCount = new int[numColors];
        int[] oppCount = new int[numColors];
        for (AIFlower f : state.flowers) {
            int c = f.color.ordinal();
            if (f.ownerIndex == player) myCount[c]++;
            else if (f.ownerIndex == opponent) oppCount[c]++;
        }

        double newPawnDistSq;
        for (int i = 0; i < state.flowers.length; i++) {
            if (i == skipF1 || i == skipF2) continue;
            AIFlower f = state.flowers[i];
            if (!f.isOnBoard()) continue;

            double dx = px - f.x;
            double dy = py - f.y;
            newPawnDistSq = dx * dx + dy * dy;

            double minExistingDistSq = Double.MAX_VALUE;
            for (AIPawn p : state.pawns) {
                if (!p.placed) continue;
                double ddx = p.x - f.x;
                double ddy = p.y - f.y;
                double dSq = ddx * ddx + ddy * ddy;
                if (dSq < minExistingDistSq) minExistingDistSq = dSq;
            }

            if (newPawnDistSq < minExistingDistSq) {
                int c = f.color.ordinal();
                double weight = 1.0;
                if (myCount[c] < oppCount[c]) weight = 3.0;
                else if (myCount[c] == oppCount[c]) weight = 2.0;
                else if (myCount[c] == 3) weight = 2.5;
                score += weight;
            }
            // Add continuous distance reward only when there are existing pawns to compare against
            // (guards against Double.MAX_VALUE blowing up the score when no pawns are placed)
            if (minExistingDistSq < Double.MAX_VALUE) {
                score += (minExistingDistSq - newPawnDistSq) * 1e-5;
            }
        }
        return score;
    }

    private boolean isPositionClear(AIState state, double px, double py,
            int skip1, int skip2) {
        double minDistSq = hitboxRadius * 0.5 * hitboxRadius * 0.5;
        for (int i = 0; i < state.flowers.length; i++) {
            if (i == skip1 || i == skip2 || !state.flowers[i].isOnBoard()) continue;
            double dx = px - state.flowers[i].x;
            double dy = py - state.flowers[i].y;
            if (dx * dx + dy * dy < minDistSq) return false;
        }
        for (AIPawn pawn : state.pawns) {
            if (!pawn.placed) continue;
            double dx = px - pawn.x;
            double dy = py - pawn.y;
            if (dx * dx + dy * dy < minDistSq) return false;
        }
        return true;
    }

    private boolean isLineValid(AIState state, int idx1, int idx2) {
        AIFlower f1 = state.flowers[idx1];
        AIFlower f2 = state.flowers[idx2];
        Line2D.Double ligne = new Line2D.Double(f1.x, f1.y, f2.x, f2.y);

        for (int k = 0; k < state.flowers.length; k++) {
            if (k == idx1 || k == idx2 || !state.flowers[k].isOnBoard()) continue;
            if (ligne.ptSegDist(state.flowers[k].x, state.flowers[k].y) < hitboxRadius)
                return false;
        }
        for (AIPawn pawn : state.pawns) {
            if (!pawn.placed) continue;
            if (ligne.ptSegDist(pawn.x, pawn.y) < hitboxRadius) return false;
        }
        return true;
    }
}
