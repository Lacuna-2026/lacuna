package lacuna.model.AI;

public class DefaultAIEvaluator implements IAIEvaluator {
 
    private static final int MAJORITY_BASE = 0;
    private static final int DIFF_MULT = 30;
    private static final int DIFF_BONUS = 10;
    private static final int MAJORITY_WIN_THRESHOLD = 4;
    private static final int MAJORITY_WIN_BASE = 50000;
    private static final int MAJORITY_WIN_STEP = 5000;
    private static final int MAJORITY_THREE_BONUS = 1000;

    private static final int QUICK_MYCOUNT_GT3 = 100;
    private static final int QUICK_MYCOUNT_EQ3 = 100;
    private static final int QUICK_OPP_COUNT_GE3 = 0;
    private static final int QUICK_FLOWER_WEIGHT = 20;

    private static final double CF_WEIGHT_DEFAULT = 1.0;
    private static final double CF_WEIGHT_LESS = 3.0;
    private static final double CF_WEIGHT_EQUAL = 2.0;
    private static final double CF_WEIGHT_MY_THREE = 2.5;
    
    @Override
    public double evaluate(AIState state, int aiPlayer) {
        int opponent = 1 - aiPlayer;
        int numColors = state.colorGroups.length;

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
                score += MAJORITY_BASE + diff * DIFF_MULT;
            } else if (finalOpp[c] > finalAI[c]) {
                majOpp++;
                score -= MAJORITY_BASE + (-diff) * DIFF_MULT;
            }
            score += diff * DIFF_BONUS;
        }

        if (majAI >= MAJORITY_WIN_THRESHOLD) score += MAJORITY_WIN_BASE + (majAI - (MAJORITY_WIN_THRESHOLD - 1)) * MAJORITY_WIN_STEP;
        if (majOpp >= MAJORITY_WIN_THRESHOLD) score -= MAJORITY_WIN_BASE - (majOpp - (MAJORITY_WIN_THRESHOLD - 1)) * MAJORITY_WIN_STEP;

        if (majAI == (MAJORITY_WIN_THRESHOLD - 1)) score += MAJORITY_THREE_BONUS;
        if (majOpp == (MAJORITY_WIN_THRESHOLD - 1)) score -= MAJORITY_THREE_BONUS;

        return score;
    }

    @Override
    public double quickEval(AIState state, AIMove move, int aiPlayer) {
        int player = state.currentPlayer;
        int colorOrd = state.flowers[move.flower1Index].color.ordinal();

        int myCount = 0, oppCount = 0;
        for (int idx : state.colorGroups[colorOrd]) {
            if (state.flowers[idx].ownerIndex == player) myCount++;
            else if (state.flowers[idx].ownerIndex == 1 - player) oppCount++;
        }
        myCount += 2;

        double score = 0;
        if (myCount > 3) score += QUICK_MYCOUNT_GT3;
        else if (myCount == 3) score += QUICK_MYCOUNT_EQ3;
        if (oppCount >= 3) score += QUICK_OPP_COUNT_GE3;

        score += countFlowersWon(state, move.pawnX, move.pawnY,
            move.flower1Index, move.flower2Index) * QUICK_FLOWER_WEIGHT;

        return (player == aiPlayer) ? score : -score;
    }

    public double countFlowersWon(AIState state, double px, double py,
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
                double weight = CF_WEIGHT_DEFAULT;
                if (myCount[c] < oppCount[c]) weight = CF_WEIGHT_LESS;
                else if (myCount[c] == oppCount[c]) weight = CF_WEIGHT_EQUAL;
                else if (myCount[c] == (MAJORITY_WIN_THRESHOLD - 1)) weight = CF_WEIGHT_MY_THREE;
                score += weight;
            }
        }
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
