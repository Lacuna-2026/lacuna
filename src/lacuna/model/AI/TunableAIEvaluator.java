package lacuna.model.AI;

import lacuna.model.*;

public class TunableAIEvaluator implements IAIEvaluator {

    private final int majorityBase;
    private final int diffMult;
    private final int diffBonus;
    private final int majorityWinThreshold;
    private final int majorityWinBase;
    private final int majorityWinStep;
    private final int majorityThreeBonus;

    private final int quickMyCountGt3;
    private final int quickMyCountEq3;
    private final int quickOppCountGe3;
    private final int quickFlowerWeight;

    private final double cfWeightDefault;
    private final double cfWeightLess;
    private final double cfWeightEqual;
    private final double cfWeightMyThree;

    public TunableAIEvaluator(int majorityBase, int diffMult, int diffBonus,
            int majorityWinThreshold, int majorityWinBase, int majorityWinStep, int majorityThreeBonus,
            int quickMyCountGt3, int quickMyCountEq3, int quickOppCountGe3, int quickFlowerWeight,
            double cfWeightDefault, double cfWeightLess, double cfWeightEqual, double cfWeightMyThree) {
        this.majorityBase = majorityBase;
        this.diffMult = diffMult;
        this.diffBonus = diffBonus;
        this.majorityWinThreshold = majorityWinThreshold;
        this.majorityWinBase = majorityWinBase;
        this.majorityWinStep = majorityWinStep;
        this.majorityThreeBonus = majorityThreeBonus;

        this.quickMyCountGt3 = quickMyCountGt3;
        this.quickMyCountEq3 = quickMyCountEq3;
        this.quickOppCountGe3 = quickOppCountGe3;
        this.quickFlowerWeight = quickFlowerWeight;

        this.cfWeightDefault = cfWeightDefault;
        this.cfWeightLess = cfWeightLess;
        this.cfWeightEqual = cfWeightEqual;
        this.cfWeightMyThree = cfWeightMyThree;
    }

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
                score += majorityBase + diff * diffMult;
            } else if (finalOpp[c] > finalAI[c]) {
                majOpp++;
                score -= majorityBase + (-diff) * diffMult;
            }
            score += diff * diffBonus;
        }

        if (majAI >= majorityWinThreshold) score += majorityWinBase + (majAI - (majorityWinThreshold - 1)) * majorityWinStep;
        if (majOpp >= majorityWinThreshold) score -= majorityWinBase - (majOpp - (majorityWinThreshold - 1)) * majorityWinStep;

        if (majAI == (majorityWinThreshold - 1)) score += majorityThreeBonus;
        if (majOpp == (majorityWinThreshold - 1)) score -= majorityThreeBonus;

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
        if (myCount > 3) score += quickMyCountGt3;
        else if (myCount == 3) score += quickMyCountEq3;
        if (oppCount >= 3) score += quickOppCountGe3;

        score += countFlowersWon(state, move.pawnX, move.pawnY,
            move.flower1Index, move.flower2Index) * quickFlowerWeight;

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
                double weight = cfWeightDefault;
                if (myCount[c] < oppCount[c]) weight = cfWeightLess;
                else if (myCount[c] == oppCount[c]) weight = cfWeightEqual;
                else if (myCount[c] == (majorityWinThreshold - 1)) weight = cfWeightMyThree;
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
