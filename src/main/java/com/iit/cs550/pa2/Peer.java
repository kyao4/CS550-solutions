package com.iit.cs550.pa2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class Peer {
	
	static Logger logger = LogManager.getLogger(Peer.class);
	
	public static void main(String[] args) {
		new Peer("localhost", Integer.parseInt(args[0]));
	}
	
	
	public Peer() {
		
	}
	
	public Peer(String address, int port) {
		this.address = address;
		this.port = port;
		setupConfig();
	}
	
	public Peer(String address, int port, boolean plain) {
		this.address = address;
		this.port = port;
	}
	

	private void setupConfig() {
		File configFIle = new File("pa2/config.txt");
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
				neighbors.add(new Peer(host, port, true));
				logger.info("{}:{}", host, port);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}


	private String address;
	private int port;
	private List<Peer> neighbors = new ArrayList<Peer>();
	
	
	
	
	
}
