package peer;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.io.DataInputStream;
import java.io.IOException;

public class TCPChannel implements Runnable {
    private int port;
    private ServerSocket server;

    ConcurrentLinkedQueue<byte[]> messageQueue;

    Peer peer = null;
    private boolean running = true;

    TCPChannel(Peer peer) {
        this.port = 8081;
        try {
            this.server = new ServerSocket(this.port);
            
        } catch (IOException e) {
            System.out.println("Failed to initialize the Server socket. Try again after a few minutes");
            this.running = false;
        }
        this.messageQueue = new ConcurrentLinkedQueue<byte[]>();
        this.peer = peer;
    }

    @Override
    public void run() {
        Socket socket = null;
        while (running) {

            try {
                socket = this.server.accept();
                DataInputStream dIn = new DataInputStream(socket.getInputStream());
                int length = dIn.readInt();
                if (length > 0) {
                    byte[] data = new byte[length];
                    dIn.readFully(data, 0, data.length);
                    this.messageQueue.add(data);
                }
            } catch (IOException e) {
                System.out.println("Error: Server failed to listen");
                e.printStackTrace();
            }


        }
        this.closeSocket();
    }

    public void stopServer(){
        running = false;
    }

    public void closeSocket(){
        try{
        this.server.close();
        }
        catch(IOException e){
        }
    }
}