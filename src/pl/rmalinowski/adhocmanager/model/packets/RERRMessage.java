package pl.rmalinowski.adhocmanager.model.packets;

import java.util.List;

import pl.rmalinowski.adhocmanager.model.Node;

public class RERRMessage extends Packet implements RoutingPacket {

	private static final long serialVersionUID = -7082857864999539176L;
	private Boolean flagN;
	private List<Node> unreachableNodes;

	public RERRMessage() {
		super();
		flagN = false;
	}

	public Boolean isFlagN() {
		return flagN;
	}

	public void setFlagN(Boolean flagN) {
		this.flagN = flagN;
	}

	public List<Node> getUnreachableNodes() {
		return unreachableNodes;
	}

	public void setUnreachableNodes(List<Node> unreachableNodes) {
		this.unreachableNodes = unreachableNodes;
	}

}
