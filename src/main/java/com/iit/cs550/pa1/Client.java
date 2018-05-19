package com.iit.cs550.pa1;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Client {

	static Logger logger = LogManager.getLogger(Client.class);
	
	File[] fileList = null;
	
	IndexServer indexService = null;
	
	private String clientRootDir;
	
	public static void main(String[] args) {
		String host = args[0];
		String port = args[1];
		String cDir = args[2];
		logger.info("host: {}, port: {}, rootDir: {}", host, port, cDir);
		new Client(host, port, cDir);
	}
	
	public Client(String host, String port, String rootDir) {
		
		int portNum = Integer.parseInt(port);
		
		setupRMI();
		
		setupFile(host, portNum, rootDir);
		//new thread
		waitInput(rootDir);
		//new thread
		waitConnection(portNum, rootDir);
	}

	private void setupRMI() {
		try {
			indexService = (IndexServer) Naming.lookup("rmi://" + "localhost" + "/indexService");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		
	}

	private void setupFile(String host, int port, String rootDir) {
		File rootFile = new File(System.getProperty("user.dir"));
		File clientDir = new File(rootFile.getPath() + "/" + rootDir);
		logger.info("client dir: {} \nworking dir: {}", 
				clientDir.getPath(), System.getProperty("user.dir"));
		fileList = clientDir.listFiles();
		for (File f: fileList) {
			try {
				indexService.registry(host + ":" + port + ":" + rootDir,f.getName());
				logger.info("Added file {} with client {}:{}:{}", 
						f.getName(), host, port, rootDir);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void waitInput(String clientRootDir) {
		Thread thread = new Thread(new PeerClient(clientRootDir, indexService));
		thread.start();
	}


	
	private void waitConnection(int port, String rootDir) {
		//set up thread pool, listen to certain port.
		logger.info("start up thread pool, waiting for connect on port: {} \nretriving file from dir: {}", port, rootDir);
		Executor executor = Executors.newFixedThreadPool(3);
		ServerSocket ss;
		try {
			ss = new ServerSocket(port);
			while(true) {
				executor.execute(new PeerServer(ss.accept(), rootDir));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}

class PeerServer implements Runnable {
	
	private Socket socket;
	private String rootDir;
	
	public PeerServer(Socket socket, String rootDir) {
		this.socket = socket;
		this.rootDir = rootDir;
	}

	public void run() {
		// read file and send file to output stream.
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "8859_1"));
			OutputStream out = socket.getOutputStream();
			String filename = in.readLine();
			File rootFile = new File(System.getProperty("user.dir"));
			File clientDir = new File(rootFile.getPath() + "/" + rootDir);
			Client.logger.info("sending file: {} from dir: {}", filename, rootDir);
			File[] fileList = clientDir.listFiles();
			for (File f: fileList) {
				if (f.getName().equals(filename)) {
					FileInputStream fin = new FileInputStream(f);
					byte[] buffer = new byte[4 * 1024];
					for (int len; (len = fin.read(buffer)) > -1;) {
						out.write(buffer, 0, len);
					}
					fin.close();
					out.flush();
					out.close();
					Client.logger.info("file {} sent.",	filename);
					return;
				}
			}
			out.close();
			throw new IllegalArgumentException("File requested doesn't exist.");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
}

class PeerClient implements Runnable {
	
	private String clientDir;
	private IndexServer indexService;
	
	
	public PeerClient(String clientDir, IndexServer indexService) {
		this.clientDir = clientDir;
		this.indexService = indexService;
	}

	public void run() {
		Scanner console = new Scanner(System.in);
		System.out.println(console);
		while (true) {
			System.out.println("Please specify the file your want to download:");
			String filename = console.nextLine();
			try {
				List<String> peerList = (List<String>)indexService.search(filename);
				if (peerList == null) {
					throw new IllegalArgumentException("This file is not registered.");
				}
				
				//choose which peer to connect to, parse connection info.
				long seed = 1;
				Random rnums = new Random( seed );
				String peer = peerList.get(rnums.nextInt(peerList.size()));
				String[] peersplit = peer.split(":");
				String host = peersplit[0];
				String port = peersplit[1];
				String rootDir = peersplit[2];
				
				//establish TCP connection, send filename, receive file content.
				try {
					Socket server = new Socket(host, Integer.parseInt(port));
					OutputStream out = server.getOutputStream();
					PrintWriter pOut = new PrintWriter(new OutputStreamWriter(out, "8859_1"), true);
					InputStream in = server.getInputStream();
					pOut.println(filename);
					Client.logger.info("connect to host: {}:{}:{}, sent filename and retriving file with name {}", host, port, rootDir, filename);
					File newFile = new File(String.format("%s/%s/%s", System.getProperty("user.dir"), this.clientDir,filename));
					FileOutputStream fOut = new FileOutputStream(newFile);
					Client.logger.info("writing {} into dir: {}", filename, newFile.getPath());
					byte[] buffer = new byte[4 * 1024];
					for (int len; (len = in.read(buffer)) > -1;) {
						fOut.write(buffer, 0, len);
					}
					fOut.flush();
					fOut.close();
					server.close();
					Client.logger.info("finished retriving file content.");
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
				
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	
	}
}
