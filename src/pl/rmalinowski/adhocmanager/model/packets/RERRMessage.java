package pl.rmalinowski.adhocmanager.model.packets;

import java.util.List;

import pl.rmalinowski.adhocmanager.model.Node;

public class RERRMessage implements RoutingPacket {

	private boolean flagN;
	private List<Node> unreachableNodes;
	
	public boolean isFlagN() {
		return flagN;
	}
	public void setFlagN(boolean flagN) {
		this.flagN = flagN;
	}
	public List<Node> getUnreachableNodes() {
		return unreachableNodes;
	}
	public void setUnreachableNodes(List<Node> unreachableNodes) {
		this.unreachableNodes = unreachableNodes;
	}
	
}
