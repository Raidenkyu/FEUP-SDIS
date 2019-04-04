package peer;

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