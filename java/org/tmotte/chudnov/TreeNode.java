package org.tmotte.chudnov;

public class TreeNode {
    public final boolean isRoot;
    public final long low, high;
    public final boolean isLeft;
    public TreeNode parent; // Never changes but want to nullify() for GC
    public Triple leftTriple, rightTriple;

    public static TreeNode makeRoot(long low, long high) {
        return new TreeNode(true, low, high, null, false);
    }

    public TreeNode(long low, long high, TreeNode parent, boolean isLeft) {
        this(false, low, high, parent, isLeft);
    }

    /** Does not create a root that you can compute pi from; just
        creates a single node that has no parent */
    public TreeNode(long low, long high) {
        this(false, low, high, null, false);
    }



    private TreeNode(boolean isRoot, long low, long high, TreeNode parent, boolean isLeft) {
        this.isRoot = isRoot;
        this.low=low;
        this.high=high;
        this.parent=parent;
        this.isLeft=isLeft;
    }

    public void nullify() {
        parent=null;
        leftTriple=null;
        rightTriple=null;
    }

    public void setTriple(Triple t, boolean isLeft) {
        if (isLeft) leftTriple = t;
        else rightTriple = t;
    }

    public boolean hasTriples() {
        return leftTriple!=null && rightTriple!=null;
    }

    public String toString() {
        return "["+low+" "+high+"] "+(isRoot ?"ROOT" :"");
    }

}
