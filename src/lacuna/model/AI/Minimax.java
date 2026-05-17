package lacuna.model.AI;

import java.util.*;
import lacuna.model.*;

public class Minimax {

    private final AIState rootState;
    private final int aiPlayer;
    private final int baseDepth;

    private final long timeLimitMs;
    private long searchStartTime;
    private boolean timeOut;

    private final IAIEvaluator evaluator;
    private final IAIMoveGenerator moveGenerator;
    private final TranspositionTable tt;

    public Minimax(GameModel model, int aiPlayer, int baseDepth) {
        this(model, aiPlayer, baseDepth, 3000);
    }

    public Minimax(GameModel model, int aiPlayer, int baseDepth, long timeLimitMs) {
        this(model, aiPlayer, baseDepth, timeLimitMs, new DefaultAIEvaluator(), new DefaultAIMoveGenerator());
    }

    public Minimax(GameModel model, int aiPlayer, int baseDepth, long timeLimitMs, IAIEvaluator evaluator,
            IAIMoveGenerator moveGenerator) {
        this.rootState = AIState.fromModel(model);
        this.aiPlayer = aiPlayer;
        this.baseDepth = baseDepth;
        this.timeLimitMs = timeLimitMs;
        this.evaluator = evaluator;
        this.moveGenerator = moveGenerator;
        this.tt = new TranspositionTable();
    }

    public AIMove findBestMove() {
        searchStartTime = System.currentTimeMillis();
        timeOut = false;
        tt.clear();

        List<AIMove> moves = moveGenerator.generateMoves(rootState);
        if (moves.isEmpty())
            return null;
        if (moves.size() == 1)
            return moves.get(0);

        int depth = baseDepth;
        if (moves.size() <= 5)
            depth = baseDepth + 2;
        else if (moves.size() <= 15)
            depth = baseDepth + 1;

        moves.sort((a, b) -> Double.compare(
                quickEval(rootState, b), quickEval(rootState, a)));

        AIMove bestMove = moves.get(0);
        double bestScore = Double.NEGATIVE_INFINITY;

        for (AIMove move : moves) {
            if (isTimeOut())
                break;
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
        if (timeOut)
            return true;
        if (System.currentTimeMillis() - searchStartTime > timeLimitMs) {
            timeOut = true;
        }
        return timeOut;
    }

    private double quickEval(AIState state, AIMove move) {
        return evaluator.quickEval(state, move, aiPlayer);
    }

    private double minimax(AIState state, int depth, double alpha, double beta,
            boolean maximizing) {
        if (isTimeOut())
            return evaluator.evaluate(state, aiPlayer);

        long hash = state.computeHash();
        TranspositionTable.TTEntry entry = tt.get(hash);
        if (entry != null && entry.depth >= depth) {
            if (entry.type == TranspositionTable.NodeType.EXACT) {
                return entry.score;
            } else if (entry.type == TranspositionTable.NodeType.LOWERBOUND) {
                alpha = Math.max(alpha, entry.score);
            } else if (entry.type == TranspositionTable.NodeType.UPPERBOUND) {
                beta = Math.min(beta, entry.score);
            }
            if (alpha >= beta) {
                return entry.score;
            }
        }

        if (depth == 0 || state.isTerminal()) {
            double eval = evaluator.evaluate(state, aiPlayer);
            tt.put(hash, eval, depth, TranspositionTable.NodeType.EXACT);
            return eval;
        }

        List<AIMove> moves = moveGenerator.generateMoves(state);
        if (moves.isEmpty()) {
            double eval = evaluator.evaluate(state, aiPlayer);
            tt.put(hash, eval, depth, TranspositionTable.NodeType.EXACT);
            return eval;
        }

        double originalAlpha = alpha;
        double originalBeta = beta;
        double bestEval;

        if (maximizing) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (AIMove move : moves) {
                AIState child = applyMove(state, move);
                double eval = minimax(child, depth - 1, alpha, beta, false);
                if (eval > maxEval)
                    maxEval = eval;
                if (eval > alpha)
                    alpha = eval;
                if (beta <= alpha)
                    break;
            }
            bestEval = maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (AIMove move : moves) {
                AIState child = applyMove(state, move);
                double eval = minimax(child, depth - 1, alpha, beta, true);
                if (eval < minEval)
                    minEval = eval;
                if (eval < beta)
                    beta = eval;
                if (beta <= alpha)
                    break;
            }
            bestEval = minEval;
        }

        TranspositionTable.NodeType type = TranspositionTable.NodeType.EXACT;
        if (bestEval <= originalAlpha)
            type = TranspositionTable.NodeType.UPPERBOUND;
        else if (bestEval >= originalBeta)
            type = TranspositionTable.NodeType.LOWERBOUND;

        tt.put(hash, bestEval, depth, type);
        return bestEval;
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
                1 - next.currentPlayer, next.pawnsPlaced, next.colorGroups);
    }
}
