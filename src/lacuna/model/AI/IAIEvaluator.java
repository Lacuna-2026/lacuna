package lacuna.model.AI;

public interface IAIEvaluator {
    /**
     * Evaluates the current state of the board.
     * @param state The state to evaluate.
     * @param aiPlayer The index of the AI player (to determine perspective).
     * @return The evaluation score. Positive is good for aiPlayer, negative is bad.
     */
    double evaluate(AIState state, int aiPlayer);

    /**
     * Performs a fast evaluation of a move for ordering before deeper search.
     * @param state The current state.
     * @param move The move to evaluate.
     * @param aiPlayer The index of the AI player.
     * @return A heuristic score for sorting the moves.
     */
    double quickEval(AIState state, AIMove move, int aiPlayer);
}
