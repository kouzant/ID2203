package se.kth.ict.id2203.components.paxos;

public class ReadList {
	
	private final Integer ts, v;
	
	public ReadList(Integer ts, Integer v) {
		this.ts = ts;
		this.v = v;
	}

	public Integer getTs() {
		return ts;
	}

	public Integer getV() {
		return v;
	}
}
