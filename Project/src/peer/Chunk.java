package peer;

public class Chunk
{
    byte[] data;

    int index;
    int totalChunks;

    String filename;
    int replicationDegree;

    public Chunk(byte[] data, int index, int totalChunks, String filename)
    {
        this.data = data;
        this.index = index;
        this.totalChunks = totalChunks;
        this.filename = filename;
    }

    public boolean equals(Chunk chunk)
    {
        return (this.index == chunk.index && this.totalChunks == chunk.totalChunks && this.filename.equals(chunk.filename));
    }
}