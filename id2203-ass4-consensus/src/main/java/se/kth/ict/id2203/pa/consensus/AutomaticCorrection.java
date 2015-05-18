package se.kth.ict.id2203.pa.consensus;

import se.kth.ict.id2203.sim.consensus.ConsensusTester;

public class AutomaticCorrection {
	public static void main(String[] args) {
		String email = "antkou@kth.se";
		String password = "mXLBEA";
		ConsensusTester.correctAndSubmit(email, password);
	}
}
