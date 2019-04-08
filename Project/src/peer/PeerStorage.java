package peer;

import java.util.ArrayList;
import java.util.Arrays;

public class PeerStorage {
    private long storageCapacity;
    private long usedSpace;
    private long freeSpace;
    private ArrayList<Chunk> chunks;
    private Peer peer;


    public PeerStorage(Peer p){
        this.storageCapacity = 1000 * 1000;
        this.usedSpace = 0;
        this.freeSpace = this.storageCapacity;
        this.chunks = new ArrayList<Chunk>();
        this.peer = p;
    }


    public ArrayList<Chunk> getChunks(){
        return this.chunks;
    }

    public boolean addChunk(Chunk chunk){
        long chunkSpace = chunk.data.length;
		
		if(chunkSpace > this.freeSpace){
			System.out.println("Operation Failed: Not enough space to store more Chunks");
			return false;
		}
		
		// update memory status
		this.freeSpace -= chunkSpace;
        this.usedSpace += chunkSpace;
        
        this.chunks.add(chunk);
        return true;
    }

    public boolean deleteChunk(int i){
		
		if(i >= this.chunks.size()){
			return false;
        }
        long chunkSpace = this.chunks.get(i).data.length;
		
		// update memory status
		this.freeSpace += chunkSpace;
        this.usedSpace -= chunkSpace;
        
        this.chunks.remove(i);
        return true;
    }

    public void setSpace(long reclaimedSpace){
        this.storageCapacity = reclaimedSpace;
        this.freeSpace = reclaimedSpace - this.usedSpace;
    }


    public long getUsedSpace(){
        return this.usedSpace;
    }

}