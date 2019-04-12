package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.time.Instant;
import java.util.Timer;

import java.io.DataOutputStream;

import java.net.Socket;

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
                this.MCListener(packetData, packet.getAddress().getHostName());
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

        if (!args[1].equals(this.peer.version)) {
            return;
        }
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

            Timer timer = new java.util.Timer();
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    peer.channels.get("MC").send(response.getBytes());

                    this.cancel();
                }
            }, getUniformWait());
        }
    }

    void MCListener(byte[] packetData, String IP) {

        String header = this.peer.parseHeader(packetData);
        String[] args = header.split(" +");
        if (!args[1].equals(this.peer.version)) {
            return;
        }

        if (args[0].equals("STORED")) {
            String fileId = args[3];
            int ChunkNo = Integer.parseInt(args[4]);
            Chunk chunk = peer.storage.getChunk(fileId + ChunkNo);

            if (chunk == null) {
                for (int i = 0; i < peer.backedChunks.size(); i++) {
                    if (peer.backedChunks.get(i).second.key().equals(fileId + ChunkNo)) {
                        chunk = peer.backedChunks.get(i).second;
                        break;
                    }
                }

                if (chunk != null)
                    chunk.addPeer(args[2]);

                messageQueue.add(packetData);
            } 
            else 
            {
                chunk.addPeer(args[2]);
                chunk.update(peer.chunkPath, peer.id);
                System.out.println("Updated actual replication degree of chunk " + chunk + " to "
                        + chunk.getActualReplicaitonDegree());
            }

        } else if (args[0].equals("GETCHUNK")) {
            String fileId = args[3];
            int ChunkNo = Integer.parseInt(args[4]);

            Chunk chunk = peer.storage.getChunk(fileId + ChunkNo);

            if (chunk == null)
                return;

            byte[] response = peer.makeMsg(peer.makeHeader("CHUNK", chunk), chunk);

            if (this.peer.version.equals("1.0")) {
                Timer timer = new java.util.Timer();
                timer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {

                        boolean shouldSend = true;
                        for (int i = 0; i < peer.channels.get("MDR").messageQueue.size(); i++) {
                            byte[] msg = peer.channels.get("MDR").messageQueue.poll();
                            String[] args = peer.parseHeader(msg).split(" +");

                            String fileId2 = args[3];
                            int ChunkNo2 = Integer.parseInt(args[4]);

                            if (fileId2.equals(fileId) && ChunkNo2 == ChunkNo) {
                                shouldSend = false;
                                break;
                            } else
                                peer.channels.get("MDR").messageQueue.add(msg);
                        }

                        if (shouldSend) {
                            peer.channels.get("MDR").send(response);
                            System.out.println("Sent chunk: " + chunk);
                        }

                        this.cancel();
                    }
                }, getUniformWait());
            } else {
                try{
                Socket tcpSocket = new Socket(IP, 8081);
                DataOutputStream dOut = new DataOutputStream(tcpSocket.getOutputStream());

                dOut.writeInt(response.length);
                dOut.write(response);
                tcpSocket.close();
                }
                catch(IOException e){
                    System.out.println("Unable to connect to server");
                }
            }
        } else if (args[0].equals("DELETE")) {
            String fileId = args[3];
            Chunk chunk;

            Collection<Chunk> chunks = peer.storage.getChunks().values();

            for (Iterator<Chunk> it = chunks.iterator(); it.hasNext();) {
                chunk = it.next();

                if (chunk.fileId.equals(fileId)) {
                    System.out.println("Removed deleted chunk " + chunk);

                    chunk.delete(peer.chunkPath.toString(), peer.id);
                    peer.storage.deleteChunk(chunk.key());
                }
            }

        } else if (args[0].equals("REMOVED")) {
            String senderId = args[2];
            String fileId = args[3];
            String ChunkNo = args[4];
            String chunkKey = fileId + ChunkNo;
            Chunk chunk = this.peer.storage.getChunk(chunkKey);
            if (chunk != null) {
                chunk.removePeer(senderId);
                System.out.println("Chunk " + chunk + " actual replication degree=" + chunk.getActualReplicaitonDegree());
                if (chunk.getActualReplicaitonDegree() < chunk.desiredReplicationDegree) {
                    this.peer.chunkBackup(chunk);
                }
            }

        } else if (args[0].equals("GETINITIATOR")) { // Checks if the current peer is the one which initiated the specified file's backup, if yes send a message to the MC channel with INITIATOR operation
            
        	if (peer.enhanced())
        	{
        		String fileId = args[3];
                Chunk chunk = new Chunk(null, 0, fileId, 0);

                for (int i = 0; i < peer.backedChunks.size(); i++) {
                    if (peer.backedChunks.get(i).second.fileId.equals(fileId)) {
                        byte[] msg = peer.makeHeader("INITIATOR", chunk).getBytes();

                        peer.channels.get("MC").send(msg);

                        break;
                    }
                }
        	}
        	
        } else if (args[0].equals("INITIATOR")) {
            
        	if (peer.enhanced())
        	{
        		int senderId = Integer.parseInt(args[2]);
        		if (senderId != peer.id)
        			messageQueue.add(packetData);
        	}
        	
        }

    }

    void MDRListener(byte[] packetData) {
        String header = this.peer.parseHeader(packetData);
        String[] args = header.split(" +");

        if (!args[1].equals(this.peer.version)) {
            return;
        }

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


    private int getUniformWait() {
        Random random = new Random(Instant.now().toEpochMilli());
        return random.nextInt(400);
    }

}