package se.kth.ict.id2203.components.paxos;

import se.kth.ict.id2203.ports.beb.BebDeliver;
import se.sics.kompics.address.Address;

public class PrepareMessage extends BebDeliver {
	
	private static final long serialVersionUID = -1551419783218735716L;

	private final Integer pts, t;
	
	public PrepareMessage(Address source, Integer pts, Integer t) {
		super(source);
		
		this.pts = pts;
		this.t = t;
	}

	public Integer getPts() {
		return pts;
	}

	public Integer getT() {
		return t;
	}
}
