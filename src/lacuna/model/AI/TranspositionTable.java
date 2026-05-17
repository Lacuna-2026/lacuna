package lacuna.model.AI;

import java.util.HashMap;
import java.util.Map;

public class TranspositionTable {

    public enum NodeType {
        EXACT, LOWERBOUND, UPPERBOUND
    }

    public static class TTEntry {
        public final long hash;
        public final double score;
        public final int depth;
        public final NodeType type;

        public TTEntry(long hash, double score, int depth, NodeType type) {
            this.hash = hash;
            this.score = score;
            this.depth = depth;
            this.type = type;
        }
    }

    private final Map<Long, TTEntry> table;

    public TranspositionTable() {
        table = new HashMap<>();
    }

    public void put(long hash, double score, int depth, NodeType type) {
        TTEntry entry = table.get(hash);
        if (entry == null || depth >= entry.depth) {
            table.put(hash, new TTEntry(hash, score, depth, type));
        }
    }

    public TTEntry get(long hash) {
        return table.get(hash);
    }
    
    public void clear() {
        table.clear();
    }
}
