package lacuna.model.AI;

import lacuna.model.*;
import java.awt.geom.Line2D;
import java.util.*;

public class Minimax {

    private final AIState rootState;
    private final int aiPlayer;
    private final int baseDepth;
    private final double hitboxRadius;

    private static final double[] SAMPLE_T = { 0.15, 0.3, 0.5, 0.7, 0.85 };

    private final long timeLimitMs;
    private long searchStartTime;
    private boolean timeOut;

    private int[][] colorGroups;

    public Minimax(GameModel model, int aiPlayer, int baseDepth) {
        this(model, aiPlayer, baseDepth, 3000);
    }

    public Minimax(GameModel model, int aiPlayer, int baseDepth, long timeLimitMs) {
        this.rootState = AIState.fromModel(model);
        this.aiPlayer = aiPlayer;
        this.baseDepth = baseDepth;
        this.hitboxRadius = GameModel.HITBOX_RADIUS;
        this.timeLimitMs = timeLimitMs;
        buildColorGroups(rootState);
    }

    private void buildColorGroups(AIState state) {
        int numColors = FlowerColor.values().length;
        List<List<Integer>> groups = new ArrayList<>();
        for (int c = 0; c < numColors; c++) groups.add(new ArrayList<>());
        for (int i = 0; i < state.flowers.length; i++) {
            groups.get(state.flowers[i].color.ordinal()).add(i);
        }
        colorGroups = new int[numColors][];
        for (int c = 0; c < numColors; c++) {
            List<Integer> g = groups.get(c);
            colorGroups[c] = new int[g.size()];
            for (int i = 0; i < g.size(); i++) colorGroups[c][i] = g.get(i);
        }
    }

    public AIMove findBestMove() {
        searchStartTime = System.currentTimeMillis();
        timeOut = false;

        List<AIMove> moves = generateMoves(rootState);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        int depth = baseDepth;
        if (moves.size() <= 5) depth = baseDepth + 2;
        else if (moves.size() <= 15) depth = baseDepth + 1;

        moves.sort((a, b) -> Double.compare(
                quickEval(rootState, b), quickEval(rootState, a)));

        AIMove bestMove = moves.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (AIMove move : moves) {
            if (isTimeOut()) break;
            AIState child = applyMove(rootState, move);
            double score = minimax(child, depth - 1,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private boolean isTimeOut() {
        if (timeOut) return true;
        if (System.currentTimeMillis() - searchStartTime > timeLimitMs) {
            timeOut = true;
        }
        return timeOut;
    }

    private double minimax(AIState state, int depth, double alpha, double beta,
            boolean maximizing) {
        if (isTimeOut()) return evaluate(state);
        if (depth == 0 || state.isTerminal()) return evaluate(state);

        List<AIMove> moves = generateMoves(state);
        if (moves.isEmpty()) return evaluate(state);

        if (maximizing) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (AIMove move : moves) {
                AIState child = applyMove(state, move);
                double eval = minimax(child, depth - 1, alpha, beta, false);
                if (eval > maxEval) maxEval = eval;
                if (eval > alpha) alpha = eval;
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (AIMove move : moves) {
                AIState child = applyMove(state, move);
                double eval = minimax(child, depth - 1, alpha, beta, true);
                if (eval < minEval) minEval = eval;
                if (eval < beta) beta = eval;
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    public List<AIMove> generateMoves(AIState state) {
        List<AIMove> moves = new ArrayList<>();
        for (int[] group : colorGroups) {
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

        double bestX = (f1.x + f2.x) / 2.0;
        double bestY = (f1.y + f2.y) / 2.0;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (double t : SAMPLE_T) {
            double px = f1.x + t * (f2.x - f1.x);
            double py = f1.y + t * (f2.y - f1.y);

            if (!isPositionClear(state, px, py, idx1, idx2)) continue;

            double score = countFlowersWon(state, px, py, idx1, idx2);
            if (score > bestScore) {
                bestScore = score;
                bestX = px;
                bestY = py;
            }
        }
        return new double[] { bestX, bestY };
    }

    private double countFlowersWon(AIState state, double px, double py,
            int skipF1, int skipF2) {
        double score = 0;
        int player = state.currentPlayer;
        int opponent = 1 - player;
        int numColors = colorGroups.length;

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

    private AIState applyMove(AIState state, AIMove move) {
        AIState next = state.copy();
        next.flowers[move.flower1Index].ownerIndex = next.currentPlayer;
        next.flowers[move.flower2Index].ownerIndex = next.currentPlayer;

        int start = next.currentPlayer * GameModel.PAWNS_PER_PLAYER;
        int end = start + GameModel.PAWNS_PER_PLAYER;
        for (int i = start; i < end; i++) {
            if (!next.pawns[i].placed) {
                next.pawns[i].x = move.pawnX;
                next.pawns[i].y = move.pawnY;
                next.pawns[i].placed = true;
                break;
            }
        }
        next.pawnsPlaced[next.currentPlayer]++;
        return new AIState(next.flowers, next.pawns,
                1 - next.currentPlayer, next.pawnsPlaced);
    }

    private double quickEval(AIState state, AIMove move) {
        int player = state.currentPlayer;
        int colorOrd = state.flowers[move.flower1Index].color.ordinal();

        int myCount = 0, oppCount = 0;
        for (int idx : colorGroups[colorOrd]) {
            if (state.flowers[idx].ownerIndex == player) myCount++;
            else if (state.flowers[idx].ownerIndex == 1 - player) oppCount++;
        }
        myCount += 2;

        double score = 0;
        if (myCount > 3) score += 500;
        else if (myCount == 3) score += 200;
        if (oppCount >= 3) score += 300;

        score += countFlowersWon(state, move.pawnX, move.pawnY,
                move.flower1Index, move.flower2Index) * 50;

        return (player == aiPlayer) ? score : -score;
    }

    private double evaluate(AIState state) {
        int opponent = 1 - aiPlayer;
        int numColors = colorGroups.length;

        int[] finalAI = new int[numColors];
        int[] finalOpp = new int[numColors];

        for (AIFlower f : state.flowers) {
            int c = f.color.ordinal();

            if (f.ownerIndex == aiPlayer) {
                finalAI[c]++;
            } else if (f.ownerIndex == opponent) {
                finalOpp[c]++;
            } else {
                int closestOwner = findClosestPawnOwner(state, f);
                if (closestOwner == aiPlayer) finalAI[c]++;
                else if (closestOwner == opponent) finalOpp[c]++;
            }
        }

        double score = 0;
        int majAI = 0, majOpp = 0;

        for (int c = 0; c < numColors; c++) {
            int total = finalAI[c] + finalOpp[c];
            if (total == 0) continue;

            int diff = finalAI[c] - finalOpp[c];

            if (finalAI[c] > finalOpp[c]) {
                majAI++;
                score += 200 + diff * 30;
            } else if (finalOpp[c] > finalAI[c]) {
                majOpp++;
                score -= 200 + (-diff) * 30;
            }
            score += diff * 10;
        }

        if (majAI >= 4) score += 50000 + (majAI - 3) * 5000;
        if (majOpp >= 4) score -= 50000 - (majOpp - 3) * 5000;

        if (majAI == 3) score += 1000;
        if (majOpp == 3) score -= 1000;

        return score;
    }

    private int findClosestPawnOwner(AIState state, AIFlower flower) {
        double minDistSq = Double.MAX_VALUE;
        int closestOwner = -1;

        for (AIPawn p : state.pawns) {
            if (!p.placed) continue;
            double dx = p.x - flower.x;
            double dy = p.y - flower.y;
            double dSq = dx * dx + dy * dy;
            if (dSq < minDistSq) {
                minDistSq = dSq;
                closestOwner = p.ownerIndex;
            }
        }
        return closestOwner;
    }
}
