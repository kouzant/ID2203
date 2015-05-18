package se.kth.ict.id2203.components.multipaxos;

import se.kth.ict.id2203.ports.fpl.FplDeliver;
import se.sics.kompics.address.Address;

public class NAckMessage extends FplDeliver {

	private static final long serialVersionUID = 2999845082233654202L;

	private final Integer ts, t;
	
	protected NAckMessage(Address source, Integer ts, Integer t) {
		super(source);
		
		this.ts = ts;
		this.t = t;
	}

	public Integer getTs() {
		return ts;
	}

	public Integer getT() {
		return t;
	}
}
