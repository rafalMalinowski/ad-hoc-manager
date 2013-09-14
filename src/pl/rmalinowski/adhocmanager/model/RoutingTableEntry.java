package pl.rmalinowski.adhocmanager.model;

import java.util.List;

public class RoutingTableEntry {
	
	private Node destinationNode;
	private Integer hopCount;
	private String nextHopAddress;
	private Long validTimestamp;
	private Integer sequenceNumber;
	private List<String> precursors;
//	private Boolean sequenceNumberValid;
	private RoutingTableEntryState state;
	
	public RoutingTableEntry(Node destinationNode) {
		super();
		this.destinationNode = destinationNode;
//		this.sq
		this.state = RoutingTableEntryState.INVALID;
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
	public List<String> getPrecursors() {
		return precursors;
	}
	public void setPrecursors(List<String> precursors) {
		this.precursors = precursors;
	}
//	public Boolean getSequenceNumberValid() {
//		return sequenceNumberValid;
//	}
//	public void setSequenceNumberValid(Boolean sequenceNumberValid) {
//		this.sequenceNumberValid = sequenceNumberValid;
//	}
	public RoutingTableEntryState getState() {
		return state;
	}
	public void setState(RoutingTableEntryState state) {
		this.state = state;
	}
	

}
