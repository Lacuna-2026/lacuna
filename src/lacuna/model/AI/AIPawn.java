package lacuna.model.AI;

public class AIPawn {
    public final int ownerIndex;
    public double x, y;
    public boolean placed;

    public AIPawn(int ownerIndex, double x, double y, boolean placed) {
        this.ownerIndex = ownerIndex;
        this.x = x;
        this.y = y;
        this.placed = placed;
    }

    public AIPawn copy() {
        return new AIPawn(ownerIndex, x, y, placed);
    }
}
