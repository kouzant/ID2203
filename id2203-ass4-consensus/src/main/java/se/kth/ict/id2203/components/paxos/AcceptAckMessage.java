package se.kth.ict.id2203.components.paxos;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class AcceptAckMessage extends Pp2pDeliver {

	private static final long serialVersionUID = 8524353292980544263L;

	private final Integer ts, t;
	
	protected AcceptAckMessage(Address source, Integer ts, Integer t) {
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
