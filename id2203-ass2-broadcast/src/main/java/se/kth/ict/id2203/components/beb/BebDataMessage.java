package se.kth.ict.id2203.components.beb;

import se.kth.ict.id2203.pa.broadcast.BebMessage;
import se.kth.ict.id2203.ports.beb.BebDeliver;
import se.kth.ict.id2203.ports.pp2p.Pp2pDeliver;
import se.sics.kompics.address.Address;

public class BebDataMessage extends Pp2pDeliver {
	private static final long serialVersionUID = 9183185042302932366L;
	private BebDeliver data;
	
	protected BebDataMessage(Address source, BebDeliver data) {
		super(source);
		this.data = data;
	}
	
	public BebDeliver getData() {
		return data;
	}
}
