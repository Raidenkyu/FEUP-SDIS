package peer;

import java.util.concurrent.ConcurrentHashMap;

public class PeerStorage {
    private long storageCapacity;
    private long usedSpace;
    private long freeSpace;
    private ConcurrentHashMap<String, Chunk> chunks;
    private Peer peer;


    public PeerStorage(Peer p){
        this.storageCapacity = 1000 * 1000;
        this.usedSpace = 0;
        this.freeSpace = this.storageCapacity;
        this.chunks = new ConcurrentHashMap<String, Chunk>();
        this.peer = p;
    }


    public ConcurrentHashMap<String, Chunk> getChunks(){
        return this.chunks;
    }

    public boolean addChunk(Chunk chunk){
    	
    	if (chunks.contains(chunk))
    		return true;
    	
        long chunkSpace = chunk.data.length;
		
		if(chunkSpace > this.freeSpace){
			System.out.println("Operation Failed: Not enough space to store more Chunks");
			return false;
		}
		
		// update memory status
		this.freeSpace -= chunkSpace;
        this.usedSpace += chunkSpace;
        
        this.chunks.put(chunk.key(), chunk);
        return true;
    }

    public boolean deleteChunk(String key){
		
    	Chunk chunk = this.chunks.remove(key);
    	
    	if (chunk == null)
    		return false;
    	
        long chunkSpace = chunk.data.length;
		
		// update memory status
		this.freeSpace += chunkSpace;
        this.usedSpace -= chunkSpace;
        
        return true;
    }
    
    public Chunk getChunk(String key)
    {
    	return chunks.get(key);
    }

    public void setSpace(long reclaimedSpace){
        this.storageCapacity = reclaimedSpace;
        this.freeSpace = reclaimedSpace - this.usedSpace;
    }


    public long getUsedSpace(){
        return this.usedSpace;
    }
    
    public long getCapacity(){
        return this.storageCapacity;
    }

}