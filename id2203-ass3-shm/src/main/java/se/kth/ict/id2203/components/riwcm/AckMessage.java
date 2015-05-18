package se.kth.ict.id2203.components.riwcm;

import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class AckMessage extends Pp2pDeliver {
	private static final long serialVersionUID = 2129744840108864619L;

	private final Integer r;
	
	protected AckMessage(Address source, Integer r) {
		super(source);
		this.r = r;
	}

	public Integer getR() {
		return r;
	}
}
