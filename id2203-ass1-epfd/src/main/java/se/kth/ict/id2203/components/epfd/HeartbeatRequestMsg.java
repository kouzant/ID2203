package se.kth.ict.id2203.components.epfd;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class HeartbeatRequestMsg extends Pp2pDeliver{
	
	private static final long serialVersionUID = -1951545301104494951L;
	private final Integer sequenceNumber;
	
	public HeartbeatRequestMsg(Address source, Integer sequenceNumber) {
		super(source);
		this.sequenceNumber = sequenceNumber;
	}
	
	public Integer getSequenceNumber() {
		return sequenceNumber;
	}
}
