package pl.rmalinowski.adhocmanager.model.packets;

import java.util.HashSet;
import java.util.Set;

import pl.rmalinowski.adhocmanager.model.ErrorNode;

public class RERRMessage extends Packet implements RoutingPacket {

	private static final long serialVersionUID = -7082857864999539176L;
	private Set<ErrorNode> unreachableNodes;

	public RERRMessage() {
		super();
		unreachableNodes = new HashSet<ErrorNode>();
	}

	public Set<ErrorNode> getUnreachableNodes() {
		return unreachableNodes;
	}

	public void setUnreachableNodes(Set<ErrorNode> unreachableNodes) {
		this.unreachableNodes = unreachableNodes;
	}

}
