package pl.rmalinowski.adhocmanager.model;

public class RoutingTableEntry {
	
	private Node destinationNode;
	private Integer hopCount;
	private String nextHopAddress;
	private Boolean valid;
	private Long timeout;
	private Integer sequenceNumber;
	
	public RoutingTableEntry(Node destinationNode) {
		super();
		this.destinationNode = destinationNode;
		this.sequenceNumber = 0;
		this.valid = false;
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
	public Boolean isValid() {
		return valid;
	}
	public void setValid(Boolean isValid) {
		this.valid = isValid;
	}
	public Long getTimeout() {
		return timeout;
	}
	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

}
