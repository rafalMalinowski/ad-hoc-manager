package pl.rmalinowski.adhocmanager.model;

import java.util.HashSet;
import java.util.Set;

public class RoutingTableEntry {

	private Node destinationNode;
	private Integer hopCount;
	private String nextHopAddress;
	private Long validTimestamp;
	private Integer sequenceNumber;
	private Set<String> precursors;
	private Boolean validSequenceNumber;
	private RoutingTableEntryState state;

	public RoutingTableEntry(Node destinationNode) {
		super();
		this.destinationNode = destinationNode;
		this.state = RoutingTableEntryState.INVALID;
		this.precursors = new HashSet<String>();
		this.validSequenceNumber = false;
		this.sequenceNumber = 0;
	}

	public Node getDestinationNode() {
		return destinationNode;
	}

	public void setDestinationNode(Node destinationNode) {
		this.destinationNode = destinationNode;
	}

	public Integer getHopCount() {
		return hopCount;
	}

	public void setHopCount(Integer hopCount) {
		this.hopCount = hopCount;
	}

	public String getNextHopAddress() {
		return nextHopAddress;
	}

	public void setNextHopAddress(String nextHopAddress) {
		this.nextHopAddress = nextHopAddress;
	}

	public Long getValidTimestamp() {
		return validTimestamp;
	}

	public void setValidTimestamp(Long validTimestamp) {
		this.validTimestamp = validTimestamp;
	}

	public Integer getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public Set<String> getPrecursors() {
		return precursors;
	}

	public void setPrecursors(Set<String> precursors) {
		this.precursors = precursors;
	}

	public Boolean getValidSequenceNumber() {
		return validSequenceNumber;
	}

	public void setValidSequenceNumber(Boolean validSequenceNumber) {
		this.validSequenceNumber = validSequenceNumber;
	}

	public RoutingTableEntryState getState() {
		return state;
	}

	public void setState(RoutingTableEntryState state) {
		this.state = state;
	}

}
