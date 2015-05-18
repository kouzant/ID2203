package se.kth.ict.id2203.pa.shm;

import se.kth.ict.id2203.sim.shm.SharedMemoryTester;

public class AutomaticCorrection {
	public static void main(String[] args) {
		String email = "antkou@kth.se";
		String password = "mXLBEA";
		SharedMemoryTester.correctAndSubmit(email, password);
	}
}
