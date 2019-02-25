package server;

import server.RegisteryServer;
import server.MulticastServer;

public class Server{

	
	public static void main(String[] args)
	{
		String multicastIP = args[0];
		int multicastPort = Integer.parseInt(args[1]);

		String registeryIP = args[2];
		int registeryPort = Integer.parseInt(args[3]);


		Thread registeryThread = new Thread(new RegisteryServer(registeryPort), "RegisteryServer");
		registeryThread.start();
		
		Thread multicastThread = new Thread(new MulticastServer(multicastIP, multicastPort, registeryIP, registeryPort), "MulticastServer");
		multicastThread.start();
	}

}
