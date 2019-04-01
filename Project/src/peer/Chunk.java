package peer;

public class Chunk
{
    byte[] data;

    int index;

    String FileId;
    int replicationDegree;

    public Chunk(byte[] data, int index, String FileId, int replicationDegree)
    {
        this.data = data;
        this.index = index;
        this.FileId = FileId;
        this.replicationDegree = replicationDegree;
    }

    public boolean equals(Chunk chunk)
    {
        return (this.index == chunk.index && this.FileId.equals(chunk.FileId));
    }
}