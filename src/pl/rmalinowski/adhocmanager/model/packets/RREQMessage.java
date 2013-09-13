package pl.rmalinowski.adhocmanager.model.packets;

import java.util.EnumMap;
import java.util.Map;

public class RREQMessage implements RoutingPacket {

	private static final long serialVersionUID = -2117920292379283560L;
	private Integer hopCount;
	private Integer id;
	private String destinationAddress;
	private Integer destinationSeq;
	private String originAddress;
	private Integer originSeq;
	private Map<Flag, Boolean> flags;
	
	public RREQMessage() {
		super();
		flags = new EnumMap<Flag, Boolean>(Flag.class);
	}
	public Integer getHopCount() {
		return hopCount;
	}
	public void setHopCount(Integer hopCount) {
		this.hopCount = hopCount;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
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
	public Integer getOriginSeq() {
		return originSeq;
	}
	public void setOriginSeq(Integer originSeq) {
		this.originSeq = originSeq;
	}
	public Map<Flag, Boolean> getFlags() {
		return flags;
	}
	public void setFlags(Map<Flag, Boolean> flags) {
		this.flags = flags;
	}
	

		
}
