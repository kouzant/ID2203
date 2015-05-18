package se.kth.ict.id2203.components.rb;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.beb.BebBroadcast;
import se.kth.ict.id2203.ports.beb.BestEffortBroadcast;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.kth.ict.id2203.ports.rb.RbBroadcast;
import se.kth.ict.id2203.ports.rb.ReliableBroadcast;

public class EagerRb extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(EagerRb.class);

	private Positive<BestEffortBroadcast> beb = requires(BestEffortBroadcast.class);
	private Negative<ReliableBroadcast> rb = provides(ReliableBroadcast.class);

	private final Address self;
	private Integer sequenceNumber;
	private Set<RbDataMessage> delivered;

	public EagerRb(EagerRbInit init) {
		this.self = init.getSelfAddress();
		new HashSet<Address>(init.getAllAddresses());
		this.sequenceNumber = 0;
		this.delivered = new HashSet<RbDataMessage>();

		subscribe(startHandler, control);
		subscribe(rbcastHandler, rb);
		subscribe(rbDelivery, beb);
	}

	private Handler<Start> startHandler = new Handler<Start>() {

		@Override
		public void handle(Start event) {
			logger.info("Component Eager Reliable Broadcast created!");
		}
	};

	private Handler<RbBroadcast> rbcastHandler = new Handler<RbBroadcast>() {

		@Override
		public void handle(RbBroadcast event) {
			sequenceNumber++;
			RbDataMessage msg = new RbDataMessage(self,
					event.getDeliverEvent(), sequenceNumber);
			trigger(new BebBroadcast(msg), beb);
		}

	};
	
	private Handler<RbDataMessage> rbDelivery = new Handler<RbDataMessage>() {

		@Override
		public void handle(RbDataMessage event) {
			//logger.info("Received RB message from: {}", event.getSource());
			//logger.info("Sequence Number: {}", event.getSequenceNumber());
			
			if (!delivered.contains(event)) {
				delivered.add(event);
				//Deliver to application
				trigger(event.getData(), rb);
				//Re-bcast
				trigger(new BebBroadcast(event), beb);
			}
		}
	};
}
