package pl.rmalinowski.adhocmanager.api;

import java.io.Serializable;
import java.util.List;

public interface INetworkLayerService{
	
	public void sendData(Serializable data, String address);
	
	public void sendBroadcastData(Serializable data);
	
	public String getText();
	
	public void reInitialize();
	
	public void searchForDevices();
	
	public void connectToNeighbours();
	
	public List<String> getNodes();
	
}
