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
	
	int delay = 1500;

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

		try
		{
			socket = new DatagramSocket();
			printMessage("Broadcasting every " + delay/1000 + " seconds\n");
		
			while (true)
			{
				byte[] data = new String(registeryIP + ":" + registeryPort).getBytes();

				DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(multicastIP), multicastPort);

				socket.send(packet);

				Thread.sleep(2000);
			}
			
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
		

	}

}
