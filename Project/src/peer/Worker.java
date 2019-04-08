package peer;

import java.io.IOException;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import peer.Chunk;

public class Worker implements Runnable {
    public String task;
    public Object[] args;

    public Peer peer = null;

    public Worker(String task, Object[] args, Peer peer) {
        this.task = task;

        this.args = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = args[i];
        }

        this.peer = peer;
    }

    @Override
    public void run() {
        if (task.equals("backup")) {
            String filename = (String) args[0];
            int replicationDegree = (Integer) args[1];
            byte data[] = this.getFileData(filename);
            System.out.println("Uploading to server...");

            backup(data, replicationDegree);

            System.out.println("Finished uploading.");

        } else if (task.equals("restore")) {
        	
        	String filename = (String) args[0];
            byte data[] = this.getFileData(filename);
            
            System.out.println("Downloading from server...");

            restore(data, filename);

            System.out.println("Finished downloading.");

        } else if (task.equals("delete")) {

            String filename = (String) args[0];
            byte data[] = this.getFileData(filename);

            System.out.println("Deleting file from server...");

            delete(data, filename);

            System.out.println("Finished deleting.");

        } else if (task.equals("reclaim")) {

            int space = (Integer) args[0];
            System.out.println("Reclaiming Space...");

            this.reclaim(space);

            System.out.println("Space reclaimed.");

        } else {
            System.out.println("Wrong task!");
            System.exit(-1);
        }
    }

    public void backup(byte[] data, int replicationDegree) {
        byte[] buffer;
        int numChunks = getNumChunks(data.length);

        String fileId = this.encrypt(data);

    	for (int i = 0; i < numChunks; i++) {
            int bufSize = data.length - i * Peer.chunkSize;
            if (bufSize > Peer.chunkSize)
                bufSize = Peer.chunkSize;

            buffer = new byte[bufSize];

            for (int j = 0; j < bufSize; j++) {
                buffer[j] = data[i * Peer.chunkSize + j];
            }
            
            Chunk chunk = new Chunk(buffer,i,fileId,replicationDegree);
            String header = this.peer.makeHeader("PUTCHUNK", chunk);
            byte[] msg = this.peer.makeMsg(header, chunk);                
           
            for (int tries = 1; tries <= 5; tries++) {
            	
                peer.channels.get("MDB").send(msg);

            	int stored = 0;
                long startTime = System.nanoTime();
                long deltaTime = 0;
                int numSeconds = (int)Math.pow(2, tries);
                
                while (stored < replicationDegree && deltaTime < numSeconds*1000*1000*1000) { // Keeps polling for a number of seconds equivalent to the variable tries
                    
                	byte[] response = peer.channels.get("MC").messageQueue.poll();
                	
                	if (response != null) {
                		String responseHeader = peer.parseHeader(response);
                		String[] args = responseHeader.split(" +");
                		
                        System.out.println("Received Message Header: " + responseHeader);
                		
                		if (args[3].equals(fileId) && Integer.parseInt(args[4]) == i)
                			stored++;
                	}
                	
                	deltaTime = (System.nanoTime() - startTime);
                }
                
                if (stored == replicationDegree) // Success
                {
                	System.out.println("File backed up sucessfully!");
                	break;
                }
                else
                {
                	System.out.println("Failed to backup chunk");
                }
            }
        }
                   
    }

    public void restore(byte[] data, String filename) {
        String fileId = this.encrypt(data);
        byte[] receivedMsg = null, dataBuffer = new byte[data.length];
        Chunk chunk = new Chunk(null, 0, fileId, 0);
        int numChunks = getNumChunks(data.length);
        boolean succeeded = true;

        try
        {
         	for (int i = 0; i < numChunks; i++)
         	{
         		chunk.index = i;
                byte[] msg = this.peer.makeHeader("GETCHUNK", chunk).getBytes();                
                peer.channels.get("MC").send(msg);

                long startTime = System.nanoTime();
                long deltaTime = 0;
                int timeout = 2;
                
                receivedMsg = null;
                
                while (deltaTime < timeout*1000*1000*1000) { // Keeps polling for 1 second
                    
                	receivedMsg = peer.channels.get("MDR").messageQueue.poll();
                	
                	if (receivedMsg != null) {
                		String responseHeader = peer.parseHeader(receivedMsg);
                		String[] args = responseHeader.split(" +");
                		
                        System.out.println("Received Message Header: " + responseHeader);
                		
                		if (args[3].equals(fileId) && Integer.parseInt(args[4]) == i)
                			break;
                	}
                	
                	try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                	
                	deltaTime = (System.nanoTime() - startTime);
                }
                
                if (receivedMsg == null)
                {
                    System.err.println("Error receiving chunk");
                    succeeded = false;
                	continue;
                }
                
                byte[] receivedData = peer.parseChunk(receivedMsg);
                
        		System.arraycopy(receivedData, 0, dataBuffer, receivedBytes, receivedData.length);
         	}
             
            if (succeeded)
            {
                File file = new File(filename + "_new");
                file.delete();
                file.createNewFile();

                FileOutputStream os = new FileOutputStream(file);
                os.write(dataBuffer, 0, dataBuffer.length);
                os.close();
            }
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
       
    }

    public void delete(byte[] data, String filename) {
        String fileId = this.encrypt(data);
        Chunk chunk = new Chunk(null, 0, fileId, 0);

        byte[] msg = this.peer.makeHeader("GETCHUNK", chunk).getBytes();
        peer.channels.get("MC").send(msg);

    }

    public void reclaim(int space) {
        long reclaimedSpace = 1000 * space;        
        Collection<Chunk> chunks = peer.storage.getChunks().values();
        
        for (Iterator<Chunk> it = chunks.iterator(); it.hasNext() && this.peer.storage.getUsedSpace() > reclaimedSpace;) {
        	
        	Chunk chunk = it.next();
            peer.storage.deleteChunk(chunk.key());
            String msg = this.peer.makeHeader("PUTCHUNK", chunk);
        	chunk.delete(peer.chunkPath.toString(), peer.id);
            peer.channels.get("MC").send(msg.getBytes());

        }
        
        this.peer.storage.setSpace(reclaimedSpace);


    }

    public void state() {

    }

    private byte[] getFileData(String filename) {

        try {
            File file = new File(filename);
            FileInputStream in = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            in.read(data, 0, data.length);
            in.close();

            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    

    private String encrypt(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data);
            String fileId = bytesToHex(encodedhash);

            return fileId;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String bytesToHex(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashInBytes.length; i++) {
            sb.append(Integer.toString((hashInBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();

    }

    private int getNumChunks(int numBytes)
    {
        int numChunks = (int) Math.ceil((double) numBytes / Peer.chunkSize);

        if (numBytes % Peer.chunkSize == 0)
            numChunks++;

        return numChunks;
    }


}