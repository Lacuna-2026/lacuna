package lacuna.model.AI;

public class TranspositionTable {

    public enum NodeType {
        EXACT, LOWERBOUND, UPPERBOUND
    }

    public static class TTEntry {
        public long hash;
        public double score;
        public int depth;
        public NodeType type;
        public boolean valid;
        public AIMove bestMove;
        
        public TTEntry() {
            this.valid = false;
        }
    }

    private static final int TABLE_SIZE = 1048576; // 2^20
    private static final int TABLE_MASK = TABLE_SIZE - 1;
    private final TTEntry[] table;

    public TranspositionTable() {
        table = new TTEntry[TABLE_SIZE];
        for (int i = 0; i < TABLE_SIZE; i++) {
            table[i] = new TTEntry();
        }
    }

    public void put(long hash, double score, int depth, NodeType type, AIMove bestMove) {
        int index = (int) (hash & TABLE_MASK);
        TTEntry entry = table[index];
        // Always replace if the new search is deeper or equal, or if the slot is empty
        if (!entry.valid || depth >= entry.depth) {
            entry.hash = hash;
            entry.score = score;
            entry.depth = depth;
            entry.type = type;
            entry.bestMove = bestMove;
            entry.valid = true;
        }
    }

    public TTEntry get(long hash) {
        int index = (int) (hash & TABLE_MASK);
        TTEntry entry = table[index];
        if (entry.valid && entry.hash == hash) {
            return entry;
        }
        return null;
    }
    
    public void clear() {
        for (int i = 0; i < TABLE_SIZE; i++) {
            table[i].valid = false;
            table[i].bestMove = null;
        }
    }
}
