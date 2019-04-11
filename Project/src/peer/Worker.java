package peer;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Iterator;

import java.time.Instant;
import java.util.Random;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Worker implements Runnable {
    public String task;
    public boolean enhanced = false;
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

    public Worker(String task, Object[] args, Peer peer, boolean enh) {
        this.task = task;

        this.args = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = args[i];
        }
        this.peer = peer;
        this.enhanced = enh;
    }

    @Override
    public void run() {
        if (task.equals("backup")) {
            String filename = (String) args[0];
            int replicationDegree = (Integer) args[1];
            byte data[] = this.getFileData(filename);

            backup(data, replicationDegree);

        } else if (task.equals("restore")) {

            String filename = (String) args[0];
            byte data[] = this.getFileData(filename);
            
            restore(data, filename);

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

        } else if (task.equals("chunkBackup")) {
            Chunk chunk = (Chunk) args[0];
            System.out.println("Uploading to server...");

            chunkBackup(chunk);

            System.out.println("Finished uploading.");

        } else {
            System.out.println("Wrong task!");
            System.exit(-1);
        }
    }

    public void backup(byte[] data, int replicationDegree) {
        byte[] buffer;
        int numChunks = getNumChunks(data.length);
        String fileId = this.encrypt(data);
        boolean succeeded = true;
        String filename = (String) args[0];
        
        System.out.println("Uploading " + filename + " to server...");

        for (int i = 0; i < numChunks; i++) {
            int bufSize = data.length - i * Peer.chunkSize;
            if (bufSize > Peer.chunkSize)
                bufSize = Peer.chunkSize;

            buffer = new byte[bufSize];

            for (int j = 0; j < bufSize; j++) {
                buffer[j] = data[i * Peer.chunkSize + j];
            }

            Chunk chunk = new Chunk(buffer, i, fileId, replicationDegree);
            String header = this.peer.makeHeader("PUTCHUNK", chunk);
            byte[] msg = this.peer.makeMsg(header, chunk);
            
            chunk.data = null;
            Pair<String, Chunk> pair = new Pair<String, Chunk>(filename, chunk);
            peer.backedChunks.add(pair);

            for (int tries = 0; tries < 5; tries++) {

                peer.channels.get("MDB").send(msg);

                int stored = 0;
                long startTime = System.currentTimeMillis();
                long deltaTime = 0;
                int numSeconds = (int) Math.pow(2, tries);

                while (stored < replicationDegree && deltaTime < numSeconds * 1000) { // Keeps polling for a number of seconds equivalent to the variable tries

                    byte[] response = peer.channels.get("MC").messageQueue.poll();

                    if (response != null) {
                        String responseHeader = peer.parseHeader(response);
                        String[] args = responseHeader.split(" +");

                        if (args[3].equals(fileId) && Integer.parseInt(args[4]) == i) {
                            System.out.println("Received Message Header: " + responseHeader);
                            stored++;
                        } else
                            peer.channels.get("MC").messageQueue.add(response);
                    }

                    deltaTime = (System.currentTimeMillis() - startTime);
                }

                if (stored == replicationDegree) // Success
                {
                	System.out.println("Chunk backed up sucessfully!");	
                 	break;	
                }	
                else	
                {	
        			succeeded = false;	
                	System.err.print("Failed to backup chunk");	
                	if (tries < 4)	
                		System.err.println(", retrying with " + (int)Math.pow(2, tries+1) + " seconds.");	
                	else
                	{
                		for (int j = 0; j < peer.backedChunks.size(); j++)
                		{
                			if (peer.backedChunks.get(j).second.equals(chunk))
                			{
                				peer.backedChunks.remove(j);
                				break;
                			}
                		}
                		
                		System.err.println(", giving up.");
                		
                		peer.backedChunks.remove(pair);
                	}
                }

            }
        }

    	    	
    	if (succeeded)
            System.out.println("Uploaded " + filename + " successfully.");
       	else
    		System.out.println("Failed to upload " + filename + ".");
        
    }

    public void restore(byte[] data, String filename) {
        ConcurrentLinkedQueue<byte[]> messageQueue = null;
        if(this.enhanced){
            this.peer.launchTCPServer();
            messageQueue = this.peer.tcp.messageQueue;
        }
        else{
            messageQueue = this.peer.channels.get("MDR").messageQueue;
        }
        String fileId = this.encrypt(data);
        byte[] receivedMsg = null, dataBuffer = new byte[data.length];
        Chunk chunk = new Chunk(null, 0, fileId, 0);
        int numChunks = getNumChunks(data.length);
        boolean succeeded = true;
        
        System.out.println("Downloading " + filename + " from server...");

        try {
            for (int i = 0; i < numChunks; i++) {
                chunk.index = i;
                byte[] msg = this.peer.makeHeader("GETCHUNK", chunk).getBytes();
                peer.channels.get("MC").send(msg);

                long startTime = System.currentTimeMillis();
                long deltaTime = 0;
                int timeout = 2;

                receivedMsg = null;

                while (deltaTime < timeout * 1000) { // Keeps polling for 1 second

                    receivedMsg = messageQueue.poll();

                    if (receivedMsg != null) {
                        String responseHeader = peer.parseHeader(receivedMsg);
                        String[] args = responseHeader.split(" +");

                        if (args[3].equals(fileId) && Integer.parseInt(args[4]) == i)
                            break;
                        else
                            messageQueue.add(receivedMsg);
                    }

                    deltaTime = (System.currentTimeMillis() - startTime);
                }

                if (receivedMsg == null) {
                    System.err.println("Error receiving chunk");
                    succeeded = false;
                    continue;
                }

                System.out.println("Received Message Header: " + peer.parseHeader(receivedMsg));

                byte[] receivedData = peer.parseChunk(receivedMsg);
                
        		System.arraycopy(receivedData, 0, dataBuffer, i*Peer.chunkSize, receivedData.length);
         	}
             
            if (succeeded)
            {
            	String filepath = peer.chunkPath + File.separator + "peer" + peer.id + File.separator + "restore" + File.separator;
                File file = new File(filepath);
                file.mkdirs();
                
                filepath += filename;
                file = new File(filepath);
                file.delete();
                file.createNewFile();

                FileOutputStream os = new FileOutputStream(file);
                os.write(dataBuffer, 0, dataBuffer.length);
                os.close();
                
                System.out.println("Downloaded " + filename + " successfully.");
            }
            else
            {
            	System.err.println("Failed to download " + filename + ".");
            }
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }

        if(this.enhanced){
            this.peer.tcp.stopServer();
        }

    }

    public void delete(byte[] data, String filename) {
        String fileId = this.encrypt(data);
        Chunk chunk = new Chunk(null, 0, fileId, 0);
        
        for (int i = 0; i < peer.backedChunks.size(); i++)
        {
        	if (peer.backedChunks.get(i).first.equals(filename))
        	{
        		peer.backedChunks.remove(i);
        		i--;
        	}
        }

        byte[] msg = this.peer.makeHeader("DELETE", chunk).getBytes();
        peer.channels.get("MC").send(msg);
    }

    public void reclaim(int space) {
        long reclaimedSpace = 1000 * space;
        Collection<Chunk> chunks = peer.storage.getChunks().values();

        for (Iterator<Chunk> it = chunks.iterator(); it.hasNext()
                && this.peer.storage.getUsedSpace() > reclaimedSpace;) {

            Chunk chunk = it.next();
            System.out.println("Chunk " + chunk + " removed");
            peer.storage.deleteChunk(chunk.key());
            String msg = this.peer.makeHeader("REMOVED", chunk);
            chunk.delete(peer.chunkPath.toString(), peer.id);
            peer.channels.get("MC").send(msg.getBytes());

        }

        this.peer.storage.setSpace(reclaimedSpace);

    }

    public void chunkBackup(Chunk chunk) {

        this.waitUniformely();

        if(chunk.getActualReplicaitonDegree() >= chunk.desiredReplicationDegree){
            return;
        }
        String header = this.peer.makeHeader("PUTCHUNK", chunk);
        byte[] msg = this.peer.makeMsg(header, chunk);

        for (int tries = 0; tries < 5; tries++) {

            peer.channels.get("MDB").send(msg);

            int stored = 0;
            long startTime = System.currentTimeMillis();
            long deltaTime = 0;
            int numSeconds = (int) Math.pow(2, tries);

            while (stored < chunk.desiredReplicationDegree && deltaTime < numSeconds * 1000) { // Keeps polling for a number of seconds equivalent to the variable tries

                byte[] response = peer.channels.get("MC").messageQueue.poll();

                if (response != null) {
                    String responseHeader = peer.parseHeader(response);
                    String[] args = responseHeader.split(" +");

                    if (args[3].equals(chunk.fileId) && Integer.parseInt(args[4]) == chunk.index) {
                        System.out.println("Received Message Header: " + responseHeader);
                        stored++;
                    } else
                        peer.channels.get("MC").messageQueue.add(response);
                }

                deltaTime = (System.currentTimeMillis() - startTime);
            }

            if (stored == chunk.desiredReplicationDegree) // Success
            {
                System.out.println("Chunk backed up sucessfully!");
                break;
            } else
                System.out.println("Failed to backup chunk");
        }

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

    private int getNumChunks(int numBytes) {
        int numChunks = (int) Math.ceil((double) numBytes / Peer.chunkSize);

        if (numBytes % Peer.chunkSize == 0)
            numChunks++;

        return numChunks;
    }


    private void waitUniformely()
    {
        Random random = new Random(Instant.now().toEpochMilli());
        int delay = random.nextInt(400);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
