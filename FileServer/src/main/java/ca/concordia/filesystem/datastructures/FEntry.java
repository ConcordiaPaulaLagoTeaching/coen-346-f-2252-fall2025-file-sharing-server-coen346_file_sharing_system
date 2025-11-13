package ca.concordia.filesystem.datastructures;

import java.util.LinkedList;

public class FEntry {

    private String name;
    private short filesize;
    private short firstBlock;
    private byte[] data;

    public FEntry(String name, byte[] data){
        //Check filename is max 11 bytes long
        if (name.length() > 11) {
            throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
        }
        this.name = name;
        this.data = data;
        this.filesize = (short) data.length;
        this.firstBlock = -1;
    }

    // Getters and Setters
    public String geteName() {
        return name;
    }
    
    public byte[] getData() {
    	return data;
    }

    public short getFilesize() {
        return filesize;
    }


    public short getFirstBlock() {
        return firstBlock;
    }
    
    
    public void setName(String name) {
	  if (name.length() > 11) {
		  throw new IllegalArgumentException("Filename cannot be longer than 11 characters.");
      }
	  this.name = name;
	  }
    
    public void setFilesize(short filesize) {
        if (filesize < 0) {
            throw new IllegalArgumentException("Filesize cannot be negative.");
        }
        this.filesize = filesize;
    }
  
  }


