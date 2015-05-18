package se.kth.ict.id2203.components.multipaxos;

import se.kth.ict.id2203.ports.fpl.FplDeliver;
import se.sics.kompics.address.Address;

public class DecideMessage extends FplDeliver {

	private static final long serialVersionUID = 6314167232607148059L;

	private final Integer pts, pl, t;
	
	protected DecideMessage(Address source, Integer pts, Integer pl, Integer t) {
		super(source);
		
		this.pts = pts;
		this.pl = pl;
		this.t = t;
	}

	public Integer getPts() {
		return pts;
	}

	public Integer getPl() {
		return pl;
	}

	public Integer getT() {
		return t;
	}
}
