package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TreeMap;

public class RegisteryServer implements Runnable {

	int bufferSize = 256;
	DatagramSocket socket;
    int port;
	TreeMap<String,String> cars = new TreeMap<String,String>();

    public RegisteryServer(int port)
    {
        this.port = port;
    }
	
	private boolean validPlate(String plate)
	{
		int numberCounter = 0, letterCounter = 0, dashCounter = 0;
		
		if (plate.length() != 8)
			return false;
		
		for (int i = 0; i < plate.length(); i++) {
			
			if (i == 2 || i == 5)
			{
				if (plate.charAt(i) == '-')
					dashCounter++;
				else
					return false;
			}
			else
			{
				if (plate.charAt(i) >= 'A' && plate.charAt(i) <= 'Z')
					letterCounter++;
				else if (plate.charAt(i) >= '0' && plate.charAt(i) <= '9')
					numberCounter++;
				else
					return false;
			}	
		}
		
		return (numberCounter == 4 && letterCounter == 2 && dashCounter == 2);
	}

    private void printMessage(String message)
	{
		System.out.println("[RegisteryServer] " + message);
	}

    private void printError(String message)
	{
		System.err.println("[RegisteryServer] " + message);
	}
	
    @Override
	public void run() {
		
		printMessage("Server is running...\n");
				
		try {
			socket = new DatagramSocket(port);
			
			while(true) {
				
				byte[] data = new byte[bufferSize];
				DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
				
				socket.receive(receivedPacket);
				InetAddress senderIP = receivedPacket.getAddress();
				String message = new String(receivedPacket.getData()).trim();
				
				String[] parts = message.split(";");
				String printMessage = "";
				
				if (!validPlate(parts[1])) {
					
					printError("Invalid plate");
				}
					
				
				for (int i = 0; i < parts.length; i++)
					printMessage += parts[i] + " ";
				
				printMessage(printMessage);
				
				if (parts[0].equals("register")) {
					
					String returnValue = cars.put(parts[1],parts[2]);
					
					if (returnValue == null) {
						message = cars.size() + ";" + parts[1] + ";" +  parts[2];  
					}
					else {
						printMessage("Plate already registered");
						message = -1 + ";" + parts[1] + ";" + parts[2];
					}
					
				}			
				else if (parts[0].equals("lookup")) {
					
					String name = cars.get(parts[1]);					
					
					if (name != null) {
						message = cars.size() + ";" + parts[1] + ";" + name;  
					}
					else {
						printMessage("Plate not registered");
						message = -1 + ";" + parts[1] + "ERROR";  
					}
					
				}
				else {
					printMessage("Invalid Message");
					message = -1 + ";" + parts[1] + ";" + parts[2];  
				}
				
				data = message.getBytes();
				DatagramPacket responsePacket = new DatagramPacket(data, data.length, receivedPacket.getAddress(), receivedPacket.getPort());
				
				socket.send(responsePacket);
				
				System.out.println("");
			}
			
			
		} catch (IOException e) {
			System.err.println("Failed to open server!");
		}
		
	}

}
