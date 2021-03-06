package pl.rmalinowski.adhocmanager.api;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import pl.rmalinowski.adhocmanager.model.RoutingTableEntry;

public interface INetworkLayerService{
	
	public void sendData(Serializable data, String address);
	
	public void sendBroadcastData(Serializable data);
	
	public void reInitialize();
	
	public void searchForDevices();

	public void stopSearchingForDevices();
	
	public void connectToNeighbours();
	
	public List<String> getNodes();

	public Set<RoutingTableEntry> getRoutingTable();
	
	public void cancelAll();
}
