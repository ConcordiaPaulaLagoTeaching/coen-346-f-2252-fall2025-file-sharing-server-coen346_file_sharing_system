package ca.concordia.filesystem.datastructures;

import java.io.Serializable;

/**
 * FNode represents a file node that tracks data block allocation.
 * Each FNode corresponds to one data block in the file system.
 * 
 * Structure:
 * - blockIndex: The index of the data block. Negative if block is free.
 * - nextBlock: Index of next FNode in the chain, -1 if this is the last block
 * 
 * The blockIndex is negative when the block is not allocated to any file.
 * For example: blockIndex = -5 means FNode at index 5 is free
 *              blockIndex = 5 means FNode at index 5 is in use
 */
public class FNode implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int blockIndex;  // Index of data block (negative if free)
    private int nextBlock;   // Index of next FNode in chain (-1 if end)

    /**
     * Constructor for FNode
     * @param blockIndex The index of this node (use negative for free blocks)
     */
    public FNode(int blockIndex) {
        this.blockIndex = blockIndex;
        this.nextBlock = -1;  // Initially no next block
    }

    // Getters
    public int getBlockIndex() {
        return blockIndex;
    }

    public int getNextBlock() {
        return nextBlock;
    }

    // Setters
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public void setNextBlock(int nextBlock) {
        this.nextBlock = nextBlock;
    }

    /**
     * Check if this FNode is free (not allocated to any file)
     * @return true if this node is free
     */
    public boolean isFree() {
        return blockIndex < 0;
    }

    // Mark this FNode as in use
    public void markInUse() {
        if (blockIndex < 0) {
            blockIndex = -blockIndex;  // Convert negative to positive
        }
    }

    // Mark this FNode as free

    public void markFree() {
        if (blockIndex >= 0) {
            blockIndex = -blockIndex;  // Convert positive to negative
        }
        nextBlock = -1;  // Reset next pointer
    }

    @Override
    public String toString() {
        return String.format("FNode{blockIndex=%d, nextBlock=%d, free=%b}", 
                           blockIndex, nextBlock, isFree());
    }
}
