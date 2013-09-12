package pl.rmalinowski.adhocmanager.api;

import java.io.Serializable;

public interface INetworkLayerService{
	
	public void sendData(Serializable data);
	
	public String getText();
	
	public void reInitialize();
	
	public void searchForDevices();
	
	public void connectToNeighbours();
	
}
