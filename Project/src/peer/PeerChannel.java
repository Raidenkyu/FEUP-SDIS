package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.time.Instant;


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
            peer.storage.addChunk(chunk);
            chunk.store(peer.chunkPath.toString(), peer.id);
            
            System.out.println("Stored chunk: " + chunk);
            
            String response = peer.makeHeader("STORED", chunk);
            
            waitUniformely();

            peer.channels.get("MC").send(response.getBytes());
        }

    }

    void MCListener(byte[] packetData) {
        
        String msg = new String(packetData);
        String[] args = msg.split(" +");

        if (args[0].equals("STORED")) {
            messageQueue.add(msg.getBytes());
        }
        else if (args[0].equals("GETCHUNK")) {
        	String fileId = args[3];
            int ChunkNo = Integer.parseInt(args[4]);
        	
            Chunk chunk = peer.storage.getChunk(fileId+ChunkNo);
            
            if (chunk == null)
            	return;
        	
			byte[] response = peer.makeMsg(peer.makeHeader("CHUNK", chunk), chunk);
			 
			int chunkNumber = peer.channels.get("MDR").messageQueue.size();
			waitUniformely();
			 
			if (chunkNumber >= peer.channels.get("MDR").messageQueue.size())
			{
				peer.channels.get("MDR").send(response);
				System.out.println("Sent chunk: " + chunk);
			}
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


}