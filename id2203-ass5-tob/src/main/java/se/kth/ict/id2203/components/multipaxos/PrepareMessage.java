package se.kth.ict.id2203.components.multipaxos;

import se.kth.ict.id2203.ports.fpl.FplDeliver;
import se.sics.kompics.address.Address;

public class PrepareMessage extends FplDeliver {
	private static final long serialVersionUID = 1172872392613517673L;

	private final Integer pts, al, t;
	
	protected PrepareMessage(Address source, Integer pts, Integer al, Integer t) {
		super(source);
		
		this.pts = pts;
		this.al = al;
		this.t = t;
	}

	public Integer getPts() {
		return pts;
	}

	public Integer getAl() {
		return al;
	}

	public Integer getT() {
		return t;
	}
}
