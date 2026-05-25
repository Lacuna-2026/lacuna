package lacuna.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private final String name;
    private final int index;
    private final List<Pawn> pawns;
    private final List<Flower> capturedFlowers;
    private int pawnsPlaced;

    public Player(String name, int index) {
        this.name = name;
        this.index = index;
        this.pawns = new ArrayList<>();
        this.capturedFlowers = new ArrayList<>();
        this.pawnsPlaced = 0;
    }

    public String getName() { return name; }
    public int getIndex() { return index; }
    public List<Pawn> getPawns() { return pawns; }
    public List<Flower> getCapturedFlowers() { return capturedFlowers; }

    public void addPawn(Pawn p) { pawns.add(p); }
    public void captureFlower(Flower f) {
        if (!capturedFlowers.contains(f)) {
            f.setOwner(this);
            capturedFlowers.add(f);
        }
    }

    public int getPawnsPlaced() { return pawnsPlaced; }
    public void incrementPawnsPlaced() { pawnsPlaced++; }
    public void decrementPawnsPlaced() { if (pawnsPlaced > 0) pawnsPlaced--; }
    public boolean hasFinishedPlacing() { return pawnsPlaced >= GameModel.PAWNS_PER_PLAYER; }

    public void uncaptureFlower(Flower f) {
        capturedFlowers.remove(f);
        if (f.getOwner() == this) {
            f.setOwner(null);
        }
    }

    public int getScoreForColor(FlowerColor color) {
        int count = 0;
        for (Flower f : capturedFlowers) {
            if (f.getColor() == color) count++;
        }
        return count;
    }
    
    public int getTotalScore() {
        return capturedFlowers.size();
    }

    @Override
    public String toString() { return name; }
}
