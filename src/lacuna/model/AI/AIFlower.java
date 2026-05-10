package lacuna.model.AI;

import lacuna.model.FlowerColor;

public class AIFlower {
    public final int index;
    public final FlowerColor color;
    public final double x, y;
    public int ownerIndex;

    public AIFlower(int index, FlowerColor color, double x, double y, int ownerIndex) {
        this.index = index;
        this.color = color;
        this.x = x;
        this.y = y;
        this.ownerIndex = ownerIndex;
    }

    public AIFlower copy() {
        return new AIFlower(index, color, x, y, ownerIndex);
    }

    public boolean isOnBoard() {
        return ownerIndex == -1;
    }
}
