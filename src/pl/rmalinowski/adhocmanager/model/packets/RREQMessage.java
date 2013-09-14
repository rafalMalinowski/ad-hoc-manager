package pl.rmalinowski.adhocmanager.model.packets;

public class RREQMessage extends Packet implements RoutingPacket {

	private static final long serialVersionUID = -2117920292379283560L;
	private Integer hopCount;
	private Integer id;
	private String destinationAddress;
	private Integer destinationSeq;
	private String originAddress;
	private Integer originSeq;
	private Boolean flagU;
	private Boolean flagG;
	private Boolean flagD;

	public RREQMessage() {
		super();
		flagD = false;
		flagU = false;
		flagG = false;
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

	public Boolean getFlagU() {
		return flagU;
	}

	public void setFlagU(Boolean flagU) {
		this.flagU = flagU;
	}

	public Boolean getFlagG() {
		return flagG;
	}

	public void setFlagG(Boolean flagG) {
		this.flagG = flagG;
	}

	public Boolean getFlagD() {
		return flagD;
	}

	public void setFlagD(Boolean flagD) {
		this.flagD = flagD;
	}

}
