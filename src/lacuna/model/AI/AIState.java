package lacuna.model.AI;

import lacuna.model.*;
import java.util.List;

public class AIState {
    public final AIFlower[] flowers;
    public final AIPawn[] pawns;
    public final int currentPlayer;
    public final int[] pawnsPlaced;

    public AIState(AIFlower[] flowers, AIPawn[] pawns,
            int currentPlayer, int[] pawnsPlaced) {
        this.flowers = flowers;
        this.pawns = pawns;
        this.currentPlayer = currentPlayer;
        this.pawnsPlaced = pawnsPlaced;
    }

    public AIState copy() {
        AIFlower[] fCopy = new AIFlower[flowers.length];
        for (int i = 0; i < flowers.length; i++)
            fCopy[i] = flowers[i].copy();

        AIPawn[] pCopy = new AIPawn[pawns.length];
        for (int i = 0; i < pawns.length; i++)
            pCopy[i] = pawns[i].copy();

        return new AIState(fCopy, pCopy, currentPlayer,
                new int[] { pawnsPlaced[0], pawnsPlaced[1] });
    }

    public boolean isTerminal() {
        return pawnsPlaced[0] >= GameModel.PAWNS_PER_PLAYER
                && pawnsPlaced[1] >= GameModel.PAWNS_PER_PLAYER;
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

        return new AIState(flowers, pawns, current, placed);
    }
}
