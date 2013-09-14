package pl.rmalinowski.adhocmanager.model.packets;

public class RREPMessage extends Packet implements RoutingPacket {

	private static final long serialVersionUID = -8562781898617320600L;
	private Integer hopCount;
	private String destinationAddress;
	private Integer destinationSeq;
	private String originAddress;
	private Integer lifetime;
	private Boolean flagA;

	public RREPMessage() {
		super();
		flagA = false;
	}

	public Integer getHopCount() {
		return hopCount;
	}

	public void setHopCount(Integer hopCount) {
		this.hopCount = hopCount;
	}

	public String getDestinationAddress() {
		return destinationAddress;
	}

	public void setDestinationAddress(String destinationAddress) {
		this.destinationAddress = destinationAddress;
	}

	public Integer getDestinationSeq() {
		return destinationSeq;
	}

	public void setDestinationSeq(Integer destinationSeq) {
		this.destinationSeq = destinationSeq;
	}

	public String getOriginAddress() {
		return originAddress;
	}

	public void setOriginAddress(String originAddress) {
		this.originAddress = originAddress;
	}

	public Integer getLifetime() {
		return lifetime;
	}

	public void setLifetime(Integer lifetime) {
		this.lifetime = lifetime;
	}

	public Boolean getFlagA() {
		return flagA;
	}

	public void setFlagA(Boolean flagA) {
		this.flagA = flagA;
	}
}
