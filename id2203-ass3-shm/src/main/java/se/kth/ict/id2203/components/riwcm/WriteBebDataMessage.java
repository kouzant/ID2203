package se.kth.ict.id2203.components.riwcm;

import java.time.Instant;

import se.kth.ict.id2203.ports.beb.BebDeliver;
import se.sics.kompics.address.Address;

public class WriteBebDataMessage extends BebDeliver {

	private static final long serialVersionUID = 2552875477046418121L;
	
	private final Integer wr, val, ts, rid;
	
	public WriteBebDataMessage(Address source, Integer rid, Integer ts,
			Integer wr, Integer val) {
		super(source);
		
		this.rid = rid;
		this.ts = ts;
		this.wr = wr;
		this.val = val;
	}

	public Integer getR() {
		return rid;
	}
	
	public Integer getTs() {
		return ts;
	}

	public Integer getWr() {
		return wr;
	}

	public Integer getVal() {
		return val;
	}
}
