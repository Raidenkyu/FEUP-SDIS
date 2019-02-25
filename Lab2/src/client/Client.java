package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;

public class Client {
	
	static int bufferSize = 256;
	static MulticastSocket multicastSocket;
	static DatagramSocket registerySocket;

	
	private static boolean validPlate(String plate)
	{
		int numberCounter = 0, letterCounter = 0, dashCounter = 0;
		
		for (int i = 0; i < plate.length(); i++)
		{
			
			if (plate.charAt(i) >= 'A' && plate.charAt(i) <= 'Z')
				letterCounter++;
			else if (plate.charAt(i) >= '0' && plate.charAt(i) <= '9')
				numberCounter++;
			else if (plate.charAt(i) == '-')
				dashCounter++;
			else
				return false;
			
		}
		
		return (numberCounter == 4 && letterCounter == 2 && dashCounter == 2);
	}
	
	private static void sendMessage(String message) throws IOException
	{
		byte[] data = message.getBytes();
		DatagramPacket packet = new DatagramPacket(data, data.length);
	
		registerySocket.send(packet);
	}
	
	private static String receiveMessage() throws IOException
	{
		byte[] data = new byte[bufferSize];
		DatagramPacket response = new DatagramPacket(data, data.length);
		
		registerySocket.receive(response);

		return new String(response.getData()).trim();
	}

	public static void main(String[] args)
	{
		
		try
		{
			String host = args[0];
			int port = Integer.parseInt(args[1]);	
			String msg = args[2];
		
			
			multicastSocket = new MulticastSocket(port);
			multicastSocket.setSoTimeout(2000); // 2 second timeout
			multicastSocket.joinGroup(InetAddress.getByName(host));

			byte[] data = new byte[bufferSize];
			DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);

			multicastSocket.receive(packet);
			
			String[] parts = new String(packet.getData()).trim().split(":");

			multicastSocket.close();

	
			
			if (msg.equals("register"))
				msg += ";" +  args[3] + ";" + args[4];
			else if (msg.equals("lookup"))
				msg += ";" +  args[3];
			else 
				System.out.println("Invalid Message");
			
			if (!validPlate(args[3]))
			{
				System.err.println("Invalid plate");
				return;
			}
			
			if (args.length > 3 && args[3].length() > 256)
			{
				System.err.println("Name is too long!");
				return;
			}
			
			registerySocket = new DatagramSocket();
			registerySocket.setSoTimeout(2000); // 2 second timeout
			registerySocket.connect(InetAddress.getByName(host),port);
			sendMessage(msg);
			
			parts = receiveMessage().split(";");
			registerySocket.close();
			
			String printMessage = "";
						
			for (int i = 1; i < parts.length; i++)
				printMessage += parts[i] + " ";
			
			if (parts[0].equals("-1"))
				printMessage += ": ERROR";
			else
				printMessage += ": " + parts[0];
			
			System.out.println(printMessage);
		
		}
	
		catch (IOException e)
		{
			System.err.println("Server is closed!");
		}
	}

}
