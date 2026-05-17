package lacuna.model.AI;

import java.util.ArrayList;
import java.util.List;
import lacuna.model.*;

public class AIState {
    public final AIFlower[] flowers;
    public final AIPawn[] pawns;
    public final int currentPlayer;
    public final int[] pawnsPlaced;
    public final int[][] colorGroups;

    public AIState(AIFlower[] flowers, AIPawn[] pawns,
            int currentPlayer, int[] pawnsPlaced, int[][] colorGroups) {
        this.flowers = flowers;
        this.pawns = pawns;
        this.currentPlayer = currentPlayer;
        this.pawnsPlaced = pawnsPlaced;
        this.colorGroups = colorGroups;
    }

    public AIState copy() {
        AIFlower[] fCopy = new AIFlower[flowers.length];
        for (int i = 0; i < flowers.length; i++)
            fCopy[i] = flowers[i].copy();

        AIPawn[] pCopy = new AIPawn[pawns.length];
        for (int i = 0; i < pawns.length; i++)
            pCopy[i] = pawns[i].copy();

        return new AIState(fCopy, pCopy, currentPlayer,
                new int[] { pawnsPlaced[0], pawnsPlaced[1] }, this.colorGroups);
    }

    public boolean isTerminal() {
        return pawnsPlaced[0] >= GameModel.PAWNS_PER_PLAYER
                && pawnsPlaced[1] >= GameModel.PAWNS_PER_PLAYER;
    }

    public long computeHash() {
        long hash = 17;
        hash = hash * 31 + currentPlayer;
        for (AIFlower f : flowers) {
            hash = hash * 31 + f.ownerIndex;
        }
        for (AIPawn p : pawns) {
            if (p.placed) {
                hash = hash * 31 + Double.doubleToLongBits(p.x);
                hash = hash * 31 + Double.doubleToLongBits(p.y);
                hash = hash * 31 + p.ownerIndex;
            }
        }
        return hash;
    }

    public static AIState fromModel(GameModel model) {
        List<Flower> realFlowers = model.getFleurs();
        AIFlower[] flowers = new AIFlower[realFlowers.size()];
        for (int i = 0; i < realFlowers.size(); i++) {
            Flower f = realFlowers.get(i);
            int owner = -1;
            if (f.getOwner() != null)
                owner = f.getOwner().getIndex();
            flowers[i] = new AIFlower(i, f.getColor(), f.getX(), f.getY(), owner);
        }

        Player[] realPlayers = model.getJoueurs();
        int totalPawns = realPlayers[0].getPawns().size()
                + realPlayers[1].getPawns().size();
        AIPawn[] pawns = new AIPawn[totalPawns];
        int idx = 0;
        for (Player p : realPlayers) {
            for (Pawn pw : p.getPawns()) {
                pawns[idx++] = new AIPawn(
                        p.getIndex(),
                        pw.getX(), pw.getY(),
                        pw.isPlaced());
            }
        }

        int current = model.getJoueurCourant().getIndex();
        int[] placed = {
                realPlayers[0].getPawnsPlaced(),
                realPlayers[1].getPawnsPlaced()
        };

        int numColors = FlowerColor.values().length;
        List<List<Integer>> groups = new ArrayList<>();
        for (int c = 0; c < numColors; c++) groups.add(new ArrayList<>());
        for (int i = 0; i < flowers.length; i++) {
            groups.get(flowers[i].color.ordinal()).add(i);
        }
        int[][] colorGroups = new int[numColors][];
        for (int c = 0; c < numColors; c++) {
            List<Integer> g = groups.get(c);
            colorGroups[c] = new int[g.size()];
            for (int i = 0; i < g.size(); i++) colorGroups[c][i] = g.get(i);
        }

        return new AIState(flowers, pawns, current, placed, colorGroups);
    }
}
