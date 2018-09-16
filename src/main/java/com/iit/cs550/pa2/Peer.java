package com.iit.cs550.pa2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.BufferPoolMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.xml.ws.AsyncHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;



public class Peer {
	
	static Logger logger = LogManager.getLogger(Peer.class);
	
	public static void main(String[] args) {
		String config = "star";
		String rootdir = "peer" + args[0];
		new Peer("localhost", Integer.parseInt(args[0]), config, rootdir);
	}
	
	public Peer() {
		
	}
	
	public Peer(String address, int port, String config, String rootDir) {
		this.address = address;
		this.port = port;
		this.config = config;
		this.rootDir = rootDir;
		setupConfig();
		setupFile();
		new Thread(() -> waitPeerConnection()).start();; // different port for different client
		new Thread(() -> waitFileTCPConnection()).start(); //Listening to fixed port this.port + 20
		waitInput();
	}
	
	



	private void setupConfig() {
		// pa2/peer8888/config-star.txt 8888 as center
		// pa2/peer8888/config-mesh.txt
		/**
		 *   8888 8889 8890 8891 8892
		 *   8893 8894 8895 8896 8897
		 * 
		 * */
		File configFIle = new File("pa2/peer" + String.valueOf(this.port) + "/config-" + this.config + ".txt");
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(configFIle), "UTF-8"));
			while (true) {
				String line = in.readLine();
				if (line == null) {
					break;
				}
				String[] lineSplit = line.split(":");
				String host = lineSplit[0];
				int port = Integer.parseInt(lineSplit[1]);
				if(!host.equals("localhost") || port != this.port) {
					Peer peer = new Peer();
					peer.setAddress(address);
					peer.setPort(port);
					neighbors.add(peer);
					logger.info("{}:{} added {}:{} as neighbor", this.address, this.port, host, port);
				}
				
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private void setupFile() {
		File rootFile = new File(System.getProperty("user.dir"));
		File clientDir = new File(rootFile.getPath() + "/pa2/" + this.rootDir);
		logger.info("peer dir: {} \nworking dir: {}", 
				clientDir.getPath(), System.getProperty("user.dir"));
		File[] fileList = clientDir.listFiles();
		for (File f: fileList) {
			String filename = f.getName();
			if (filename.equals("config-mesh.txt") || filename.equals("config-star.txt")) {
				continue;
			}
			fileMap.put(f.getName(), f);
			logger.info("put file {} into file list.", f.getName());
		}
	}
	

	private void waitPeerConnection() {
		Executor executor = Executors.newFixedThreadPool(3);
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port);
			logger.info("Peer {}:{} listening to port {} for peer TCP connection", this.address, this.port, this.port);
			while(true) {
				executor.execute(new PeerServer(ss.accept(), messageList, fileMap, neighbors));
			}
		} catch (Exception e) {
			
		}
	}


	private void waitFileTCPConnection() {
		Executor executor = Executors.newFixedThreadPool(3);
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port + 20);
			logger.info("Peer {}:{} listening to port {} for file TCP connection", this.address, this.port, this.port + 20);
			while(true) {
				executor.execute(new FileServer(ss.accept(), fileMap));
			}
		} catch (Exception e) {
			
		}
	}


	private void waitInput() {
		Scanner in = new Scanner(System.in);
		while (true) {
			logger.info("Please enter the file you want to download:");
			String filename = in.nextLine();
			if(fileMap.containsKey(filename)) {
				logger.info("file {} exists", filename);
			}
			
			String message = String.format("%s:%s:%s:%d%n%s", UUID.randomUUID(), "q", this.port, Peer.Q_TTL, filename);
			boolean fileExist = false;
			for (String fname: fileMap.keySet()) {
				if (fname.equals(filename)) {
					logger.info("Peer {}:{}, found file {} on local machine, stop sending message.", this.address, this.port, filename);
					fileExist = true;
				}
			}
			if (fileExist) {
				continue;
			}
			for(Peer p: neighbors) {
				Peer.sendMessage(p.getAddress(), p.getPort(), message);
				logger.info("send message to peer {}:{}", p.getAddress(), p.getPort());
			}
		}
	}
	
	/**
	 * util method and variables
	 * */
	public static final int H_TTL = 6;
	public static final int Q_TTL = 3;
	private String config;
	private String address;
	private int port;
	private int filePort = port + 20;
	private String rootDir;
	private List<Peer> neighbors = new ArrayList<Peer>();
	/**
	 * key: message Id
	 * value: upstream peer
	 * */
	private Map<String, Peer> messageList = new CachedLinkedHashMap<String, Peer>();
	private Map<String, File> fileMap = new HashMap<String, File>();
	
	public String getAddress() {
		return address;
	}


	public void setAddress(String address) {
		this.address = address;
	}


	public int getPort() {
		return port;
	}


	public void setPort(int port) {
		this.port = port;
	}
	
	public static void sendMessage(String hostname, int port, String message) {
		try {
			Socket socket = new Socket(hostname, port);
			PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "8859_1"), true);
			out.print(message);
			out.flush();
			out.close();
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
}


class PeerServer implements Runnable {

	private Socket socket;
	private Map<String, Peer> messageList;
	private Map<String, File> fileList;
	private List<Peer> neighborList;
	
	public PeerServer(Socket socket, Map<String, Peer> messageList, Map<String, File> fileList, List<Peer> neighborList) {
		this.socket = socket;
		this.messageList = messageList;
		this.fileList = fileList;
		this.neighborList = neighborList;
		Peer.logger.info("Instantiate PeerServer class, client IP and port {}:{}", socket.getInetAddress().getHostName(), socket.getPort());
	}
	
	public void run() { 
		// TODO need test
		String line = "";
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "8859_1"));
			line = in.readLine();
			Peer.logger.info("read new line from socket:{}", line);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String[] lineSplit = line.split(":");
		String uuid = lineSplit[0];
		String descriptor = lineSplit[1];
		int peerId = Integer.parseInt(lineSplit[2]);
		String remoteIPAddress = socket.getInetAddress().getHostName();
		int remotePort = socket.getPort();
		int ttl = Integer.parseInt(lineSplit[3]);
		
		// only log message for q, if message is queryhit, then no need to log the message.
		if (messageList.get(uuid) != null && descriptor.equals("q")) {
			Peer.logger.info("Found query meessge with ID {}, drop message.", uuid);
			return;
		} else if (descriptor.equals("q")) {
			Peer peer = new Peer();
			peer.setAddress(remoteIPAddress);
			peer.setPort(remotePort);
			messageList.put(uuid, peer);
			Peer.logger.info("messageid {} upstream {}:{}", uuid, remoteIPAddress, peerId);
		}
		
		
		
		if (descriptor.equals("q")) {
			//query
			String filename;
			Peer.logger.info("received query message");
			try {
				
				filename = in.readLine();
				
				if (fileList.containsKey(filename)) {
					//send queryhit
					Peer.logger.info("found file, send queryhit message");
					String message = String.format("%s:%s:%s:%d%n%s:%d:%s", uuid, "h", socket.getLocalPort(), Peer.H_TTL, filename, socket.getLocalPort() + 20, socket.getLocalAddress().getHostName());// this is a new message, you should set new Peer ip and port
					Peer.sendMessage(remoteIPAddress, peerId, message);
				}
				
				if (ttl < 0) {
					return;
				}
				
				ttl--;
				
				for(Peer p: neighborList) {
					//forward query
					Peer.logger.info("ttl >= 0, forward query message");
					String nextIPAddress = p.getAddress();
					int nextPort = p.getPort();
					String message = String.format("%s:%s:%s:%d%n%s", uuid, descriptor, peerId, ttl,filename);
					Peer.sendMessage(nextIPAddress, nextPort, message);
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		} else if (descriptor.equals("h")) {
			try {
				line = in.readLine();
				lineSplit = line.split(":");
			} catch (IOException e) {
				e.printStackTrace();
			}
			String filename = lineSplit[0];
			int hitPort = Integer.parseInt(lineSplit[1]);
			String hitIPAddress = lineSplit[2];
			if (messageList.containsKey(uuid)) {
				//message id exists, forward queryhit
				
				Peer.logger.info("forward queryhit message");
				Peer upstream = messageList.get(uuid);
				ttl--;
				String message = String.format("%s:%s:%s:%d%n%s:%d:%s", uuid, descriptor, peerId, ttl, filename, hitPort, hitIPAddress);
				Peer.sendMessage(upstream.getAddress(), upstream.getPort(), message);
			} else {
				//establish TCP connection to the target peer
				Peer.logger.info("establish TCP connection to target peer.");
				try {
					Socket socket = new Socket(hitIPAddress, hitPort);
					PrintWriter filePeerout = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "8859_1"), true);
					InputStream filePeerin = socket.getInputStream();
					String[] path = {System.getProperty("user.dir"), "pa2", String.format("peer%d", this.socket.getLocalPort()), filename};
					StringBuffer sb= new StringBuffer();
					for (String s: path) {
						sb.append(File.separator + s);
					}
					String filePath = sb.toString();
					FileOutputStream fout = new FileOutputStream(new File(filePath));
					Peer.logger.info("receiving file and writing file to path {}", filePath);
					filePeerout.println(filename);
					filePeerout.flush();
					byte[] buffer = new byte[4 * 1024];
					for(int len; (len = filePeerin.read(buffer)) > 0;) {
						fout.write(buffer, 0, len);
					}
					filePeerin.close();
					filePeerout.close();
					fout.close();
					socket.close();
				Peer.logger.info("finished download file from peer {}:{}", hitIPAddress, hitPort);	
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
		}
	
	}
	

}
	

class FileServer implements Runnable {
	
	private Socket socket;
	private Map<String, File> fileMap;
	
	
	public FileServer(Socket socket, Map<String, File> fileList) {
		this.socket = socket;
		this.fileMap = fileList;
	}

	@Override
	public void run() {
		try {
			Peer.logger.info("Received file TCP connection");
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "8859_1"));
			OutputStream out = socket.getOutputStream();
			String filename = in.readLine();
			if(fileMap.containsKey(filename)) {
				File f = fileMap.get(filename);
				FileInputStream fin = new FileInputStream(f);
				byte[] buffer = new byte[4 * 1024];
				for (int len; (len = fin.read(buffer)) > 0;) {
					out.write(buffer, 0, len);
				}
				fin.close();
			}
			out.flush();
			out.close();
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}

//cache MAX_ENTRIES number of messages, if it exceeds this number, remove that message id. from java8 doc
class CachedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final int  MAX_ENTRIES = 100;

	@SuppressWarnings("rawtypes")
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return this.size() > MAX_ENTRIES;
	}
	
	
}

