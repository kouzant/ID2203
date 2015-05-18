package se.kth.ict.id2203.components.multipaxos;

import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {
		ArrayList<Values<Integer>> lala = new ArrayList<Values<Integer>>();
		lala.add(new Values<Integer>(2));
		Values<Integer> bla = new Values<Integer>(2);
		
		if (lala.contains(bla)) {
			System.out.println("Already here!");
		}
		
		System.out.println("Value: " + lala.get(0).getValue());
		
		ArrayList<Object> koko = new ArrayList<Object>();
		koko.add(new Integer(3));
		
		if (koko.contains(new Integer(3))) {
			System.out.println("Bla");
		}
	}

}
