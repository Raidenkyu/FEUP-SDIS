package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.time.Instant;
import java.util.Timer;


public class PeerChannel implements Runnable {
    String id = "";
    MulticastSocket socket;
    int port = 8080;
    String multicastGroup = "";


    ConcurrentLinkedQueue<byte[]> messageQueue = new ConcurrentLinkedQueue<byte[]>();

    Peer peer = null;

    public PeerChannel(String id, String multicastGroup, Peer peer) {
        this.id = id;
        this.multicastGroup = multicastGroup;
        this.peer = peer;
        
        this.connect();
    }

    public void connect() {
        try {
            socket = new MulticastSocket(port);
            // socket.setSoTimeout(2 * 1000); // 2 second timeout
            socket.joinGroup(InetAddress.getByName(multicastGroup));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.connect();

        while (true) // Listen for peers
        {
            byte[] data = new byte[1000 + Peer.chunkSize];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
                        
            byte[] packetData = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

            if (id.equals("MDB")) {
                this.MDBListener(packetData, packet.getAddress().getHostName());
            } else if (id.equals("MC")) {
                this.MCListener(packetData);
            } else if (id.equals("MDR")) {
                this.MDRListener(packetData);
            } else {
                System.err.println("Wrong PeerChannel ID!");
            }
        }

    }

    void MDBListener(byte[] packetData, String IP) {
        String header = this.peer.parseHeader(packetData);
        
        String[] args = header.split(" +");
        if (args[0].equals("PUTCHUNK")) {
            int SenderId = Integer.parseInt(args[2]);

            if (peer.id == SenderId) // Initiator peer doesn't store its own files
                return;

            String fileId = (String) args[3];
            int ChunkNo = Integer.parseInt(args[4]), ReplicationDeg = Integer.parseInt(args[5]);
            byte[] data = this.peer.parseChunk(packetData);
            
            Chunk chunk = new Chunk(data, ChunkNo, fileId, ReplicationDeg);
            
            if (peer.storage.addChunk(chunk))
            	chunk.store(peer.chunkPath.toString(), peer.id);
            
            System.out.println("Stored chunk: " + chunk);
            
            String response = peer.makeHeader("STORED", chunk);
            
            Timer timer =  new java.util.Timer();
            timer.schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			            	 peer.channels.get("MC").send(response.getBytes());
			            	 
			            	 this.cancel();
			            }
			        }, 
			        getUniformWait() 
			);
        }
    }

    void MCListener(byte[] packetData) {
        
        String header = this.peer.parseHeader(packetData);
        String[] args = header.split(" +");

        if (args[0].equals("STORED")) {
        	String fileId = args[3];
            int ChunkNo = Integer.parseInt(args[4]);
        	Chunk chunk = peer.storage.getChunk(fileId+ChunkNo);
        	
        	if (chunk == null)
        		messageQueue.add(packetData);
        	else
        	{
        		chunk.addPeer(args[2]);
        		System.out.println("Updated actual replication degree of chunk " + chunk + " to " + chunk.getActualReplicaitonDegree());
        	}
            
        }
        else if (args[0].equals("GETCHUNK")) {
        	String fileId = args[3];
            int ChunkNo = Integer.parseInt(args[4]);
        	
            Chunk chunk = peer.storage.getChunk(fileId+ChunkNo);
            
            if (chunk == null)
            	return;
        	
			byte[] response = peer.makeMsg(peer.makeHeader("CHUNK", chunk), chunk);
			 
			int chunkNumber = peer.channels.get("MDR").messageQueue.size();

			Timer timer = new java.util.Timer();
			timer.schedule( 
			        new java.util.TimerTask() {
			            @Override
			            public void run() {
			            	
			            	boolean shouldSend = true;
			            	for (int i = 0; i < peer.channels.get("MDR").messageQueue.size(); i++)
			            	{
			            		byte[] msg = peer.channels.get("MDR").messageQueue.poll();
			            		String[] args = peer.parseHeader(msg).split(" +");
			            		
			            		String fileId2 = args[3];
			                    int ChunkNo2 = Integer.parseInt(args[4]);
			            		
			            		if (fileId2.equals(fileId) && ChunkNo2 == ChunkNo)
			            		{
			            			shouldSend = false;
			            			break;
			            		}
			            		else
			            		{
			            			peer.channels.get("MDR").messageQueue.add(msg);
			            		}
			            	}
			            	
			            	if (shouldSend)
			            	{
			    				peer.channels.get("MDR").send(response);
			    				System.out.println("Sent chunk: " + chunk);
			            	}
			            	
			            	this.cancel();
			            }
			        }, 
			        getUniformWait()
			);			
        }
        else if (args[0].equals("DELETE")) {
            String fileId = args[3], key;
            Chunk chunk;
            
            for (int i = 0; ; i++) {
            		
            	key = fileId+i;
        		chunk = peer.storage.getChunk(key);
        		
        		if (chunk == null)
        			break;
        		
        		chunk.delete(peer.chunkPath.toString(), peer.id);
        		peer.storage.deleteChunk(key);
        	}
        }
        else if(args[0].equals("REMOVED")){
            String senderId = args[2];
        	String fileId = args[3];
            String ChunkNo = args[4];
            String chunkKey = fileId + ChunkNo;
            Chunk chunk = this.peer.storage.getChunk(chunkKey);
            if(chunk != null){
                chunk.removePeer(senderId);
                if(chunk.getActualReplicaitonDegree() > chunk.desiredReplicationDegree){
                    this.peer.chunkBackup(chunk);
                }
            }

        }

    }

    void MDRListener(byte[] packetData) {
    	 String header = this.peer.parseHeader(packetData);
         String[] args = header.split(" +");
         
         if (args[0].equals("CHUNK")) {
             int SenderId = Integer.parseInt(args[2]);

             if (peer.id == SenderId) // Initiator peer doesn't recover it's own files
                 return;

             messageQueue.add(packetData);
         }
    }

    protected void send(byte[] data) {

        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(multicastGroup), port);

            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
    
    private int getUniformWait()
    {
    	 Random random = new Random(Instant.now().toEpochMilli());
         return random.nextInt(400);
    }


}