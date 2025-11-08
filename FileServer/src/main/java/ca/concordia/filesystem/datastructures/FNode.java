package ca.concordia.filesystem.datastructures;

public class FNode {

    private int blockIndex; //WW - index of data block storing file associated with this node. negative index if not in use (-0, -1, -2, ...)
    private int next; //WW - (most likely next = blockIndex + 1). if file size > 1 block, contains index of the next one. oth, next = -1;

    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    //WW - Getters and Setters
    public int getBlockIndex() {
        return blockIndex;  
    }
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }
    public int getNext() {
        return next;
    }
    public void setNext(int next) {
        this.next = next;       
    }

}
