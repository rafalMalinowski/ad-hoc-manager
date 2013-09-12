package pl.rmalinowski.adhocmanager.model;

public class RoutingTableEntry {
	
	private Node destinationNode;
	private Integer hopCount;
	private String nextHopAddress;
	private Boolean valid;
	private Long Timeout;
	private Integer sequenceNumber;
	
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
	public Boolean isValid() {
		return valid;
	}
	public void setValid(Boolean isValid) {
		this.valid = isValid;
	}
	public Long getTimeout() {
		return Timeout;
	}
	public void setTimeout(Long timeout) {
		Timeout = timeout;
	}
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

}
