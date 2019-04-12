package peer;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;

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
            this.server = new ServerSocket();
            this.server.setReuseAddress(true);
            this.server.bind(new InetSocketAddress(this.port));

        } catch (IOException e) {
            this.running = false;
            System.out.println("Error: Failed to initiate a TCP Server");
            e.printStackTrace();
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
                socket.close();
            } catch (IOException e) {
            }

        }
    }

    public void stopServer() {
        running = false;
        try {
            this.server.close();
            System.out.print("TCP Server successfully closed");
        } catch (IOException e) {
            System.out.println("Error: Failed to close TCP Server");
            e.printStackTrace();
        }
    }

}