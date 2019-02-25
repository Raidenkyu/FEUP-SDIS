package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastServer implements Runnable {

	DatagramSocket socket;
	String multicastIP;
	int multicastPort;
	String registeryIP;
	int registeryPort;

    public MulticastServer(String multicastIP, int multicastPort, String registeryIP, int registeryPort)
    {
		this.multicastIP = multicastIP;
        this.multicastPort = multicastPort;
		this.registeryIP = registeryIP;
		this.registeryPort = registeryPort;
    }

	private void printMessage(String message)
	{
		System.out.println("[MulticastServer] " + message);
	}

    private void printError(String message)
	{
		System.err.println("[MulticastServer] " + message);
	}
	
    @Override
	public void run() {
		
		printMessage("Server is running...\n");

		socket = new DatagramSocket();
		
		while (true)
		{
			byte[] data = new String(registeryIP + ":" + registeryPort).getBytes();

			DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(multicastIP), multicastPort);

			socket.send(packet);

			printMessage("Registery IP and port broadcasted");

			Thread.sleep(1000);
		}
		

		socket.close();

	}

}
