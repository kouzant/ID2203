package se.kth.ict.id2203.components.multipaxos;

import java.util.ArrayList;

import se.kth.ict.id2203.ports.fpl.FplDeliver;
import se.sics.kompics.address.Address;

public class AcceptMessage extends FplDeliver {

	private static final long serialVersionUID = -8073417488609069835L;

	private final Integer pts, pvSize, t;
	private final ArrayList<Values<?>> v;
	
	protected AcceptMessage(Address source, Integer pts, ArrayList<Values<?>> v,
			Integer pvSize, Integer t) {
		super(source);
		
		this.pts = pts;
		this.v = v;
		this.pvSize = pvSize;
		this.t = t;
	}

	public Integer getPts() {
		return pts;
	}

	public Integer getPvSize() {
		return pvSize;
	}

	public Integer getT() {
		return t;
	}

	public ArrayList<Values<?>> getV() {
		return v;
	}
}
