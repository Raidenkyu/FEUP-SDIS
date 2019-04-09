package peer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

public class Chunk
{
    public byte[] data;

    public int index;

    public String fileId;
    
    public int desiredReplicationDegree;
    
    HashSet<String> peerSet;

    public Chunk(byte[] data, int index, String fileId, int desiredReplicationDegree)
    {
        this.data = data;
        this.index = index;
        this.fileId = fileId;
        this.desiredReplicationDegree = desiredReplicationDegree;
        peerSet = new HashSet<String>();
    }

    public void store(String chunkPath, int peerId)
    {
    	String filepath = chunkPath + File.separator + "peer" + peerId + File.separator + "backup" + File.separator + fileId + File.separator;
    	try {
    		File file = new File(filepath);
    		file.mkdirs();
    		filepath += "chk" + index;
    		
    		Files.write(Paths.get(filepath), data);
            
    	} catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void delete(String chunkPath, int peerId)
    {
    	String filepath = chunkPath + File.separator + "peer" + peerId + File.separator + "backup" + File.separator + fileId + File.separator + "chk" + index;
    	File file = new File(filepath);
    	file.delete();
    }
    
    public void addPeer(String peerId)
    {
    	peerSet.add(peerId);
    }
    
    public void removePeer(String peerId)
    {
    	peerSet.remove(peerId);
    }
    
    public int getActualReplicaitonDegree()
    {
    	return peerSet.size();
    }
    
    public boolean equals(Chunk chunk)
    {
        return (this.index == chunk.index && this.fileId.equals(chunk.fileId));
    }

    @Override
    public String toString()
    {
    	int limit = fileId.length();
    	
    	if (limit > 15)
    		limit = 15;
    	
        return (fileId.substring(0, limit) + "-" + index);
    }
    
    public String key()
    {
    	return fileId+index;
    }
}