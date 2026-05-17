package lacuna.model.AI;

import java.util.List;

public interface IAIMoveGenerator {
    /**
     * Generates all possible legal moves for the current player in the given state.
     * @param state The current AI state.
     * @return A list of valid AIMove objects.
     */
    List<AIMove> generateMoves(AIState state);
}
