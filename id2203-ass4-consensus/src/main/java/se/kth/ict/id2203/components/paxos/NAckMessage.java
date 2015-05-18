package se.kth.ict.id2203.components.paxos;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class NAckMessage extends Pp2pDeliver {

	private static final long serialVersionUID = 7015392868641164317L;

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
