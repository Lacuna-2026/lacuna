package lacuna.model.AI;

public class AIMove {
    public final int flower1Index;
    public final int flower2Index;
    public final double pawnX, pawnY;

    public AIMove(int flower1Index, int flower2Index, double pawnX, double pawnY) {
        this.flower1Index = flower1Index;
        this.flower2Index = flower2Index;
        this.pawnX = pawnX;
        this.pawnY = pawnY;
    }

    @Override
    public String toString() {
        return String.format("Move[f%d-f%d @ (%.3f,%.3f)]",
                flower1Index, flower2Index, pawnX, pawnY);
    }
}
