package se.kth.ict.id2203.components.multipaxos;

import java.util.ArrayList;

import se.kth.ict.id2203.ports.fpl.FplDeliver;
import se.sics.kompics.address.Address;

public class PrepareAckMessage extends FplDeliver {

	private static final long serialVersionUID = -374423557948437340L;

	private final Integer ts, ats, al, t;
	private final ArrayList<Values<?>> seq;
	
	protected PrepareAckMessage(Address source, Integer ts, Integer ats,
			ArrayList<Values<?>> seq, Integer al, Integer t) {
		super(source);
		
		this.ts = ts;
		this.ats = ats;
		this.seq = seq;
		this.al = al;
		this.t = t;
	}

	public Integer getTs() {
		return ts;
	}

	public Integer getAts() {
		return ats;
	}

	public Integer getAl() {
		return al;
	}

	public Integer getT() {
		return t;
	}

	public ArrayList<Values<?>> getSeq() {
		return seq;
	}
}
