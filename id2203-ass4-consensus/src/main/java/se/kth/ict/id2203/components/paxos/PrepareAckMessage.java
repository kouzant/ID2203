package se.kth.ict.id2203.components.paxos;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class PrepareAckMessage extends Pp2pDeliver {

	private static final long serialVersionUID = -3101569120524809550L;

	private Integer ats, av, ts, t;

	protected PrepareAckMessage(Address source, Integer ats, Integer av,
			Integer ts, Integer t) {
		super(source);

		this.ats = ats;
		this.av = av;
		this.ts = ts;
		this.t = t;
	}

	public Integer getAts() {
		return ats;
	}

	public Integer getAv() {
		return av;
	}

	public Integer getTs() {
		return ts;
	}

	public Integer getT() {
		return t;
	}
}
