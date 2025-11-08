package ca.concordia.filesystem.datastructures;

public class FNode {

    private int blockIndex; //WW - index of data block storing file associated with this node. negative index if not in use (-0, -1, -2, ...)
    private int next; //WW - index to an array of FNodes. if file size > 1 block, contains index of the next one. oth, next = -1;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }
}
