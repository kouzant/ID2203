package se.kth.ict.id2203.components.multipaxos;

import java.util.ArrayList;

public class ReadItem {
	
	private final Integer ts;
	private final ArrayList<Values<?>> vsuf;
	
	public ReadItem(Integer ts, ArrayList<Values<?>> vsuf) {
		this.ts = ts;
		this.vsuf = vsuf;
	}

	public Integer getTs() {
		return ts;
	}

	public ArrayList<Values<?>> getVsuf() {
		return vsuf;
	}
}
