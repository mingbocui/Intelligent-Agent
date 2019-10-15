package deliberative_rla;

import java.util.ArrayList;
import java.util.List;

public class SearchTree {
    public class Node {
        public Node parent;
        public List<Node> children;
        public State state;
        
        public Node() {
            this.parent = null;
            this.children = new ArrayList<>();
        }
    
        public Node(State state) {
            this();
            this.state = state;
        }
    
        public boolean isRoot() {
            return this.parent == null;
        }
        
        public boolean isLeaf() {
            return this.children.isEmpty();
        }
    }
    
    public Node root;
    
    public SearchTree() {
        this.root = new Node();
    }
    
    public SearchTree(State initialState) {
        this.root = new Node(initialState);
    }
}
