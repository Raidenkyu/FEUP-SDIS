package peer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Chunk
{
    byte[] data;

    int index;

    String fileId;
    int replicationDegree;

    public Chunk(byte[] data, int index, String fileId, int replicationDegree)
    {
        this.data = data;
        this.index = index;
        this.fileId = fileId;
        this.replicationDegree = replicationDegree;
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
    	String filepath = chunkPath + "\\" + "peer" + peerId + "\\" + "backup" + "\\" + fileId + "\\" + "chk" + index;
    	File file = new File(filepath);
    	file.delete();
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
}