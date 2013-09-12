package pl.rmalinowski.adhocmanager.model;

public class RoutingTableEntry {
	
	private Node destinationNode;
	private Integer hopCount;
	private String nextHopAddress;
	private Boolean isValid;
	private Long Timeout;
	
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
	public Boolean getIsValid() {
		return isValid;
	}
	public void setIsValid(Boolean isValid) {
		this.isValid = isValid;
	}
	public Long getTimeout() {
		return Timeout;
	}
	public void setTimeout(Long timeout) {
		Timeout = timeout;
	}

}
