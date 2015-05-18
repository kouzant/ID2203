package se.kth.ict.id2203.components.paxos;

import se.kth.ict.id2203.ports.beb.BebDeliver;
import se.sics.kompics.address.Address;

public class AcceptMessage extends BebDeliver {

	private static final long serialVersionUID = 4077684345730752971L;

	private final Integer pts, pv, t;
	
	public AcceptMessage(Address source, Integer pts, Integer pv, Integer t) {
		super(source);
		
		this.pts = pts;
		this.pv = pv;
		this.t = t;
	}

	public Integer getPts() {
		return pts;
	}

	public Integer getPv() {
		return pv;
	}

	public Integer getT() {
		return t;
	}
}
