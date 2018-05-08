package com.p.a;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

public interface ServerRemote extends Remote {

	public Date getDate() throws RemoteException;
	
}
