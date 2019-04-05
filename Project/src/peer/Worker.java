package peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.io.File;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;

import java.util.concurrent.ConcurrentHashMap;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import peer.Chunk;

public class Worker implements Runnable {
    public String task;
    public Object[] args;

    public Peer peer = null;
    int chunkSize;

    public Worker(String task, Object[] args, Peer peer) {
        this.task = task;

        this.args = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = args[i];
        }

        this.peer = peer;
        this.chunkSize = peer.chunkSize;
    }

    @Override
    public void run() {
        if (task.equals("backup")) {
            String filename = (String) args[0];
            int replicationDegree = (Integer) args[1];
            byte data[] = this.getFileData(filename);
            System.out.println("uploading to server...");

            backup(data, filename, replicationDegree);

            System.out.println("uploading to server...");

        } else if (task.equals("delete")) {

        } else if (task.equals("restore")) {

        } else if (task.equals("reclaim")) {

        } else {
            System.out.println("Wrong task!");
            System.exit(-1);
        }
    }

    public void backup(byte[] data, String filename, int replicationDegree) {
        byte[] buffer;
        int numChunks = (int) Math.ceil((double) data.length / chunkSize);

        String fileId = this.encrypt(data);


        for (int i = 0; i < numChunks; i++) {
            int bufSize = data.length - i * chunkSize;
            if (bufSize > chunkSize)
                bufSize = chunkSize;

            buffer = new byte[bufSize];

            for (int j = 0; j < bufSize; j++) {
                buffer[j] = data[i * chunkSize + j];
            }
            Chunk chunk = new Chunk(buffer,i,fileId,replicationDegree);
            String header = makeHeader("PUTCHUNK", chunk);
            byte[] msg = makeMsg(header, chunk);
            System.out.println("Msg Size: " + msg.length);
            peer.channels.get("MDB").send(msg);
        }

        int stored = 0;

        while (stored < replicationDegree) {
            String response = peer.channels.get("MC").popMessage(peer.id);

            

        }

    }

    public void restore() {

    }

    public void delete() {

    }

    public void reclaim() {

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




    private byte[] makeMsg(String header, Chunk chunk) {
        byte[] headerBytes = header.getBytes();
        byte[] chunkBytes = chunk.data;
        byte[] msg = new byte[headerBytes.length + chunkBytes.length];
        System.arraycopy(headerBytes, 0, msg, 0, headerBytes.length);
        System.arraycopy(chunkBytes, 0, msg, headerBytes.length, chunkBytes.length);

        return msg;
    }

    private String makeHeader(String op, Chunk chunk){
        String CRLF = "\r\n";
        String header = "PUTCHUNK";  
		header += " " + this.peer.version; 
		header += " " + this.peer.id;
		header += " " + chunk.fileId;
		header += " " + chunk.index;
		header += " " + chunk.replicationDegree;
        header += " " + CRLF + CRLF;
                        
        return header;
    }

}