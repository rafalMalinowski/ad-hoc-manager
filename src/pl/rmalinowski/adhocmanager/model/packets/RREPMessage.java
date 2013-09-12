package pl.rmalinowski.adhocmanager.model.packets;

import java.util.EnumMap;
import java.util.Map;

public class RREPMessage implements RoutingPacket {

	private Integer hopCount;
	private String destinationAddress;
	private Integer destinationSeq;
	private String originAddress;
	private Map<Flag, Boolean> flags;
	private Integer lifetime;
	
	public RREPMessage() {
		super();
		flags = new EnumMap<Flag, Boolean>(Flag.class);
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
	public Map<Flag, Boolean> getFlags() {
		return flags;
	}
	public void setFlags(Map<Flag, Boolean> flags) {
		this.flags = flags;
	}
	public Integer getLifetime() {
		return lifetime;
	}
	public void setLifetime(Integer lifetime) {
		this.lifetime = lifetime;
	}
	
	
}
