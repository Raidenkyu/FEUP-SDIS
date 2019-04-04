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
}