package se.kth.ict.id2203.components.multipaxos;

import se.kth.ict.id2203.ports.fpl.FplDeliver;
import se.sics.kompics.address.Address;

public class AcceptAckMessage extends FplDeliver {

	private static final long serialVersionUID = 2127201910987197066L;

	private Integer ts, avSize, t;
	
	protected AcceptAckMessage(Address source, Integer ts, Integer avSize, Integer t) {
		super(source);
		
		this.ts = ts;
		this.avSize = avSize;
		this.t = t;
	}

	public Integer getTs() {
		return ts;
	}

	public Integer getAvSize() {
		return avSize;
	}

	public Integer getT() {
		return t;
	}

}
