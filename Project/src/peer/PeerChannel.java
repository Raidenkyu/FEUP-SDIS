package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

import java.net.DatagramPacket;

import java.time.Instant;
import java.lang.Math;

import java.util.Arrays;


public class PeerChannel implements Runnable {
    String id = "";
    MulticastSocket socket;
    int port = 8080;
    String multicastGroup = "";


    ConcurrentHashMap<Integer, ConcurrentLinkedQueue<String>> messageQueue = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<String>>();

    Peer peer = null;
    // Peer parent = Peer;

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
            byte[] data = new byte[1000];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] packetData = packet.getData();

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
        
        String[] args = header.trim().split(" +");
        if (args[0].equals("PUTCHUNK")) {
            int SenderId = Integer.parseInt(args[2]);

            if (peer.id == SenderId) // Initiator peer doesn't store its own files
                return;

            String fileId = (String) args[3];
            int ChunkNo = Integer.parseInt(args[4]), ReplicationDeg = Integer.parseInt(args[5]);
            byte[] data = this.peer.parseChunk(packetData);
            
            Chunk chunk = new Chunk(data, ChunkNo, fileId, ReplicationDeg);
            peer.chunks.add(chunk);
//            chunk.store(peer.chunkPath.toString(), peer.id);
            

            String response = "";

            response += "STORED ";
            response += args[1] + " ";
            response += args[2] + " ";
            response += args[3] + " ";
            response += args[4] + " ";
            response += this.peer.CRLF + this.peer.CRLF;
            
            waitUniformely();

            peer.channels.get("MC").send(response.getBytes());
        }

    }

    void MCListener(byte[] packetData) {
        
        String msg = this.peer.parseHeader(packetData);
        String[] args = msg.trim().split(" +");

        if (args[0].equals("STORED")) {
            queueMessage(peer.id, msg);
        }
        else if (args[0].equals("GETCHUNK")) {
        	int senderId = Integer.parseInt(args[2]);
        	String fileId = args[3];
        	int ChunkNo = Integer.parseInt(args[4]);
        	
        	for (int i = 0; i < peer.chunks.size(); i++) {
            	
            	if (peer.chunks.get(i).fileId.equals(fileId) && peer.chunks.get(i).index == ChunkNo) {
            		
            		 String response = "";

                     response += "CHUNK ";
                     response += args[1] + " ";
                     response += args[2] + " ";
                     response += args[3] + " ";
                     response += args[4] + " ";
                     response += this.peer.CRLF + this.peer.CRLF;
                     
                     // TODO add body
                     
                     waitUniformely();
                     
                     String chunkMsg = peer.channels.get("MDR").popMessage(senderId);

                     peer.channels.get("MDR").send(response.getBytes());
            	}
            }
        }
        else if (args[0].equals("DELETE")) {
            String fileId = args[3];
            
            for (int i = 0; i < peer.chunks.size(); i++) {
            	
            	if (peer.chunks.get(i).fileId.equals(fileId)) {
            		
            		peer.chunks.get(i).delete(peer.chunkPath.toString(), peer.id);
            		peer.chunks.remove(i);
            		i--;
            	}
            }
        }

    }

    void MDRListener(byte[] packetData) {

    }

    protected void send(byte[] data) {

        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(multicastGroup), port);

            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void queueMessage(Integer id, String message) {
        ConcurrentLinkedQueue<String> singleQueue = messageQueue.get(id);

        if (singleQueue == null) {
            singleQueue = new ConcurrentLinkedQueue<String>();
            messageQueue.put(id, singleQueue);
        }

        singleQueue.add(message);
    }

    public String popMessage(Integer id)
    {
        ConcurrentLinkedQueue<String> singleQueue = messageQueue.get(id);

        if (singleQueue != null)
        {
            return singleQueue.poll();
        }

        return null;
    }

    private void waitUniformely()
    {
        Random random = new Random(Instant.now().toEpochMilli());
        int delay = (int) Math.round((random.nextGaussian() + 1) / 2 * 400);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}