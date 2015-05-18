package se.kth.ict.id2203.components.riwcm;

import se.kth.ict.id2203.ports.beb.BebDeliver;
import se.sics.kompics.address.Address;

public class ReadBebDataMessage extends BebDeliver {

	private static final long serialVersionUID = 7409260072557953633L;

	private final Integer r;
	
	public ReadBebDataMessage(Address source, Integer r) {
		super(source);
		this.r = r;
	}

	public Integer getR() {
		return r;
	}

}
