/**
 * This file is part of the ID2203 course assignments kit.
 * 
 * Copyright (C) 2009-2013 KTH Royal Institute of Technology
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.ict.id2203.components.crb;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.crb.CausalOrderReliableBroadcast;
import se.kth.ict.id2203.ports.crb.CrbBroadcast;
import se.kth.ict.id2203.ports.rb.RbBroadcast;
import se.kth.ict.id2203.ports.rb.ReliableBroadcast;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;

public class WaitingCrb extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(WaitingCrb.class);
	
	private Positive<ReliableBroadcast> rb = requires(ReliableBroadcast.class);
	private Negative<CausalOrderReliableBroadcast> corb = provides(CausalOrderReliableBroadcast.class);
	
	private final Address self;
	private final Set<Address> nodes;
	private Integer sequenceNumber;
	private int[] vector;
	private List<CrbDataMessage> pending;
	
	public WaitingCrb(WaitingCrbInit init) {
		this.self = init.getSelfAddress();
		this.nodes = new HashSet<Address>(init.getAllAddresses());
		this.sequenceNumber = 0;
		this.vector = new int[nodes.size()];
		pending = new LinkedList<CrbDataMessage>();
		
		for (int i = 0; i < vector.length; i++)
			vector[i] = 0;
		
		subscribe(startHandler, control);
		subscribe(bcastHandler, corb);
		subscribe(crbDeliver, rb);
	}
	
	private Handler<Start> startHandler = new Handler<Start>() {

		@Override
		public void handle(Start event) {
			logger.info("Causal Order Reliable Broadcast Component created");
		}
	};
	
	private Handler<CrbBroadcast> bcastHandler = new Handler<CrbBroadcast>() {

		@Override
		public void handle(CrbBroadcast event) {
			int[] newVector = vector.clone();
			newVector[self.getId() - 1] = sequenceNumber;
			sequenceNumber++;
			//logger.info("Vector {}", printVector(vector));
			//logger.info("New vector {}", printVector(newVector));
			CrbDataMessage msg = new CrbDataMessage(self, event.getDeliverEvent(), newVector);
			trigger(new RbBroadcast(msg), rb);
		}
	};
	
	private Handler<CrbDataMessage> crbDeliver = new Handler<CrbDataMessage>() {

		@Override
		public void handle(CrbDataMessage event) {
			//logger.info("Got delivery event");
			pending.add(event);
			
			//Sort the list by vector clock or by process identifier
			Collections.sort(pending, new Comparator<CrbDataMessage>() {
				public int compare (CrbDataMessage obj0, CrbDataMessage obj1) {
					if (obj0.getVector().equals(obj1.getVector())) {
						return obj0.getSource().getId() < obj1.getSource().getId() ? -1 : 1;
					} else if (lessThanEqual(obj0.getVector(), obj1.getVector())){
						return -1;
					} else {
						return 1;
					}
				}
			});
			
			Iterator<CrbDataMessage> pendingIter = pending.iterator();
			CrbDataMessage tmpMsg;
			
			while (pendingIter.hasNext()) {
				tmpMsg = pendingIter.next();
				
				if (lessThanEqual(tmpMsg.getVector(), vector)) {
					//logger.info("Sender: {}", tmpMsg.getSource().getId());
					//logger.info("My vector: {}", printVector(vector));
					//logger.info("Message vector: {}", printVector(tmpMsg.getVector()));
					pendingIter.remove();
					vector[tmpMsg.getSource().getId() - 1]++;
					trigger(tmpMsg.getData(), corb);
				} else {
					break;
				}
			}
		}
	};
	
	private boolean lessThanEqual(int[] vector0, int[] vector1) {
		boolean result = false;
		
		if (vector0.length == vector1.length) {
			for (int i = 0; i < vector0.length; i++) {
				if (vector0[i] <= vector1[i])
					result = true;
				else {
					result = false;
					break;
				}
			}
		}
			
		return result;
	}
	
	private String printVector(int[] vector) {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int i = 0; i < vector.length; i++) {
			sb.append(vector[i]);
			if (i != (vector.length - 1)) {
				sb.append(",");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}
}
