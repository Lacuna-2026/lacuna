package lacuna.model.AI;

public class DefaultAIEvaluator implements IAIEvaluator {

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
        if (myCount > 3) score += 500;
        else if (myCount == 3) score += 200;
        if (oppCount >= 3) score += 300;

        score += countFlowersWon(state, move.pawnX, move.pawnY,
                move.flower1Index, move.flower2Index) * 50;

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
                double weight = 1.0;
                if (myCount[c] < oppCount[c]) weight = 3.0;
                else if (myCount[c] == oppCount[c]) weight = 2.0;
                else if (myCount[c] == 3) weight = 2.5;
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
