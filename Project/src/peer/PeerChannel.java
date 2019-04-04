package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.concurrent.ThreadPoolExecutor;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

import java.net.DatagramPacket;

import java.net.SocketException;

import java.time.Instant;
import java.lang.Math;

import java.util.Arrays;


public class PeerChannel implements Runnable {
    String id = "";
    MulticastSocket socket;
    int port = 8080;
    String multicastGroup = "";

    private static final String CRLF = "\r\n";

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
        String header = this.parseHeader(packetData);
       
        String[] args = header.trim().split(" +");

        if (args[0].equals("PUTCHUNK")) {
            int SenderId = Integer.parseInt(args[2]);

            if (peer.id == SenderId) // Initiator peer doesn't store its own files
                return;

            String fileId = (String) args[3];
            int Chunkno = Integer.parseInt(args[4]), ReplicationDeg = Integer.parseInt(args[5]);
            byte[] data = this.parseChunk(packetData);
            peer.chunks.add(new Chunk(data, Chunkno, fileId, ReplicationDeg));

            String response = "";

            response += "STORED ";
            response += args[1] + " ";
            response += args[2] + " ";
            response += args[3] + " ";
            response += args[4] + " ";
            response += CRLF + CRLF;
            
            Random random = new Random(Instant.now().toEpochMilli());
            int delay = (int) Math.round((random.nextGaussian() + 1) / 2 * 400);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            peer.channels.get("MC").send(response.getBytes());
        }

    }

    void MCListener(byte[] packetData) {
        
        String msg = new String(packetData);
        String[] args = msg.trim().split(" +");
        if (args[0].equals("STORED")) {
            queueMessage(peer.id, msg);
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


    private String parseHeader(byte[] packetData){
        String msg = new String(packetData);
        String header = msg.substring(0,msg.indexOf(CRLF));
        System.out.println(header);
        return header;
    }

    private byte[] parseChunk(byte[] packetData){
        String msg = new String(packetData);
        int dataIndex = msg.indexOf(CRLF);
        dataIndex += 2*CRLF.length();
        byte[] chunkData = Arrays.copyOfRange(packetData, dataIndex, packetData.length);
        return chunkData;
    }


}